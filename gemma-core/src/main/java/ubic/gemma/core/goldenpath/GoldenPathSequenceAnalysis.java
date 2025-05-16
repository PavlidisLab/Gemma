/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.goldenpath;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ResultSetExtractor;
import ubic.basecode.util.SQLUtils;
import ubic.gemma.core.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.core.analysis.sequence.SequenceBinUtils;
import ubic.gemma.core.analysis.sequence.SequenceManipulation;
import ubic.gemma.core.loader.genome.gene.ncbi.NcbiGeneConverter;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Using the Goldenpath databases for comparing sequence alignments to gene locations.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class GoldenPathSequenceAnalysis extends GoldenPath {

    /**
     * If the exon overlap fraction with annotated (known/refseq) exons is less than this value, some additional
     * checking for mRNAs and ESTs may be done.
     */
    private static final double RECHECK_OVERLAP_THRESHOLD = 0.9;

    /**
     * cache results of mRNA queries.
     */
    private final LRUMap cache = new LRUMap( 2000 );

    public GoldenPathSequenceAnalysis( Taxon taxon ) {
        super( taxon );
    }

    /**
     * Given a physical location, identify overlapping genes or predicted genes.
     *
     * @param chromosome The chromosome name (the organism is set by the constructor)
     * @param queryStart The start base of the region to query (the start of the alignment to the genome)
     * @param queryEnd The end base of the region to query (the end of the alignment to the genome)
     * @param starts Locations of alignment block starts in target. (comma-delimited from blat)
     * @param sizes Sizes of alignment blocks (comma-delimited from blat)
     * @param strand Either + or - indicating the strand to look on, or null to search both strands.
     * @param method The constant representing the method to use to locate the 3' distance.
     * @param config configuration
     * @return A list of BioSequence2GeneProduct objects. The distance stored by a ThreePrimeData will be 0 if the
     *         sequence overhangs the found genes (rather than providing a negative distance). If no genes are found,
     *         the result is null; These are transient instances, not from Gemma's database
     */
    public Collection<BlatAssociation> findAssociations( String chromosome, Long queryStart, Long queryEnd,
            String starts, String sizes, String strand, ThreePrimeDistanceMethod method, ProbeMapperConfig config ) {

        if ( GoldenPath.log.isDebugEnabled() )
            GoldenPath.log.debug( "Seeking gene overlaps with: chrom=" + chromosome + " start=" + queryStart + " end="
                    + queryEnd + " strand=" + strand );

        if ( queryEnd < queryStart )
            throw new IllegalArgumentException( "End must not be less than start" );

        /*
         * These are transient instances only
         */
        Collection<GeneProduct> geneProducts = new HashSet<>();

        if ( config.isUseRefGene() ) {
            // starting with refgene means we can get the correct transcript name etc.
            geneProducts.addAll( this.findRefGenesByLocation( chromosome, queryStart, queryEnd, strand ) );
        }

        if ( config.isUseKnownGene() ) {
            // get known genes as well, in case all we got was an intron. Currently does not work with rat (rn6, rn7)
            geneProducts.addAll( this.findKnownGenesByLocation( chromosome, queryStart, queryEnd, strand ) );
        }

        if ( geneProducts.isEmpty() )
            return null;

        Collection<BlatAssociation> results = new ArrayList<>();
        for ( GeneProduct geneProduct : geneProducts ) {
            if ( GoldenPath.log.isDebugEnabled() )
                GoldenPath.log.debug( geneProduct );

            BlatAssociation blatAssociation = this
                    .computeLocationInGene( chromosome, queryStart, queryEnd, starts, sizes, geneProduct, method,
                            config );

            /*
             * We check against the actual threshold later. We can't fully check it now because not all the slots are
             * populated yet.
             */
            if ( config.getMinimumExonOverlapFraction() > 0.0 && blatAssociation.getOverlap() == 0 ) {
                GoldenPath.log.debug( "Result failed to meet exon overlap threshold (0)" );
                continue;
            }

            results.add( blatAssociation );
        }
        return results;
    }

    /**
     * Given a location, find the nearest gene on the same strand, including only "known", "refseq" or "ensembl"
     * transcripts.
     *
     * @param chromosome chromosome
     * @param queryStart start
     * @param queryEnd end
     * @param strand Either '+' or '-'
     * @param maxWindow the number of bases on each side to look, at most, in addition to looking inside the given
     *        region.
     * @return the Gene closest to the given location. This is a transient instance, not from Gemma's database.
     */
    public Gene findClosestGene( String chromosome, Long queryStart, Long queryEnd, String strand, int maxWindow ) {
        if ( queryEnd < queryStart )
            throw new IllegalArgumentException( "End must not be less than start" );

        long round = 0L;
        int numRounds = 5;
        int increment = ( int ) ( maxWindow / ( double ) numRounds );

        // we look in a window at most increment * numRounds.

        while ( round < numRounds ) {

            long left = queryStart + round * increment;
            long right = queryEnd + round * increment;

            Collection<GeneProduct> geneProducts = this.findRefGenesByLocation( chromosome, left, right, strand );
            geneProducts.addAll( this.findKnownGenesByLocation( chromosome, left, right, strand ) );

            Gene nearest = null;
            int closestSoFar = Integer.MAX_VALUE;

            for ( GeneProduct geneProduct : geneProducts ) {
                PhysicalLocation gpl = geneProduct.getPhysicalLocation();
                Long start = gpl.getNucleotide();
                Long end = start + gpl.getNucleotideLength();

                int gap = ( int ) Math.min( left - end, start - right );

                if ( gap < closestSoFar ) {
                    closestSoFar = gap;
                    nearest = geneProduct.getGene();
                }
            }

            if ( nearest != null )
                return nearest;
            round++;
        }

        return null;

    }

    /**
     * Check to see if there are ESTs that overlap with this region. We provisionally promote the ESTs to the status of
     * genes for this purpose.
     *
     * @param chromosome chromosome
     * @param regionStart the region to be checked
     * @param regionEnd end
     * @param strand the strand
     * @return The ESTs which overlap the query region. (using the all_est table)
     */
    public Collection<Gene> findESTs( final String chromosome, Long regionStart, Long regionEnd, String strand ) {

        String searchChrom = SequenceManipulation.blatFormatChromosomeName( chromosome );
        String query = "SELECT est.qName, est.qName, est.tStart, est.tEnd, est.strand, est.blockSizes, est.tStarts  "
                + " FROM all_est as est  WHERE "
                + "((est.tStart > ? AND est.tEnd < ?) OR (est.tStart < ? AND est.tEnd > ?) OR "
                + "(est.tStart > ?  AND est.tStart < ?) OR  (est.tEnd > ? AND  est.tEnd < ? )) and est.tName = ? ";

        query = query + " and " + SequenceBinUtils.addBinToQuery( "est", regionStart, regionEnd );

        if ( strand != null ) {
            query = query + " and est.strand = ?";
        }

        Object[] params;

        if ( strand == null )
            params = new Object[] { regionStart, regionEnd, regionStart, regionEnd, regionStart, regionEnd, regionStart,
                    regionEnd, searchChrom };
        else
            params = new Object[] { regionStart, regionEnd, regionStart, regionEnd, regionStart, regionEnd, regionStart,
                    regionEnd, searchChrom, strand };

        return this.queryAndExtract( chromosome, query, params );

    }

    /**
     * Find "Known" genes contained in or overlapping a region. Note that the NCBI symbol may be blank, when the gene is
     * not a refSeq gene.
     *
     * @param chromosome chromosome
     * @param start start
     * @param end end
     * @param strand strand
     * @return This is a collection of transient instances, not from Gemma's database.
     */
    public Collection<GeneProduct> findKnownGenesByLocation( String chromosome, Long start, Long end, String strand ) {
        String searchChrom = SequenceManipulation.blatFormatChromosomeName( chromosome );

        /*
         * Rat has changed again for rn6 and rn7, dropped the use of RGD symbols and there is no Known Gene track, so this must
         * be skipped for rat (3/2018, 7/2022)
         */

        /*
         * Many known genes map to refseq genes. We use those gene symbols instead. Use kgXRef only to get the
         * description.
         */
        String query = "SELECT r.name, r.geneName, r.txStart, r.txEnd, r.strand, r.exonStarts, r.exonEnds, CONCAT('Refseq gene: ', kgr.description) "
                + " FROM knownGene as kg INNER JOIN knownToRefSeq kr on kr.name=kg.name inner join kgXref kgr on kgr.kgID=kg.name "
                + " INNER JOIN refFlat r ON r.name=kr.value  WHERE "
                + "((kg.txStart >= ? AND kg.txEnd <= ?) OR (kg.txStart <= ? AND kg.txEnd >= ?) OR "
                + "(kg.txStart >= ?  AND kg.txStart <= ?) OR  (kg.txEnd >= ? AND  kg.txEnd <= ? )) and kg.chrom = ? ";

        if ( strand != null ) {
            query = query + " AND kg.strand = ? ";
        }

        Collection<GeneProduct> known2refseq = this.findGenesByQuery( start, end, searchChrom, strand, query );
        Collection<GeneProduct> result = new HashSet<>( known2refseq );

        /*
         * Ones that do not map to refseq using a left outer join.
         */
        query = "SELECT kgxr.mRNA, kgxr.geneSymbol, kg.txStart, kg.txEnd, kg.strand, kg.exonStarts, kg.exonEnds, CONCAT('Known gene: ', kgxr.description) "
                + " FROM knownGene as kg INNER JOIN"
                + " kgXref AS kgxr ON kg.name=kgxr.kgID LEFT OUTER JOIN knownToRefSeq kr on kr.name=kg.name WHERE kr.value IS NULL AND "
                + "((kg.txStart >= ? AND kg.txEnd <= ?) OR (kg.txStart <= ? AND kg.txEnd >= ?) OR "
                + "(kg.txStart >= ?  AND kg.txStart <= ?) OR  (kg.txEnd >= ? AND  kg.txEnd <= ? )) and kg.chrom = ? ";

        if ( strand != null ) {
            query = query + " AND kg.strand = ? ";
        }
        Collection<GeneProduct> knowng = this.findGenesByQuery( start, end, searchChrom, strand, query );
        result.addAll( knowng );

        return result;
    }

    /**
     * Find RefSeq genes contained in or overlapping a region.
     *
     * @param chromosome chromosome
     * @param start start
     * @param strand strand
     * @param end end
     * @return This is a collection of transient instances, not from Gemma's database.
     */
    public Collection<GeneProduct> findRefGenesByLocation( String chromosome, Long start, Long end, String strand ) {
        String searchChrom = SequenceManipulation.blatFormatChromosomeName( chromosome );

        /*
         * Use kgXRef only to get the description - sometimes missing thus the outer join.
         */
        String query = "SELECT r.name, r.geneName, r.txStart, r.txEnd, r.strand, r.exonStarts, r.exonEnds, CONCAT('Refseq gene: ', kgXref.description) "
                + "FROM refFlat as r left outer join kgXref on r.geneName = kgXref.geneSymbol " + "WHERE "
                + "((r.txStart >= ? AND r.txEnd <= ?) OR (r.txStart <= ? AND r.txEnd >= ?) OR "
                + "(r.txStart >= ?  AND r.txStart <= ?) OR  (r.txEnd >= ? AND  r.txEnd <= ? )) and r.chrom = ? ";

        if ( strand != null ) {
            query = query + " AND r.strand = ?  ";
        }
        return this.findGenesByQuery( start, end, searchChrom, strand, query );
    }

    /**
     * Check to see if there are mRNAs that overlap with this region. We promote the mRNAs to the status of genes for
     * this purpose.
     *
     * @param chromosome chromosome
     * @param regionStart the region to be checked
     * @param regionEnd end
     * @param strand the strand
     * @return The mRNAs which overlap the query region.
     */
    public Collection<Gene> findRNAs( final String chromosome, Long regionStart, Long regionEnd, String strand ) {

        String searchChrom = SequenceManipulation.blatFormatChromosomeName( chromosome );
        String query = "SELECT mrna.qName, mrna.qName, mrna.tStart, mrna.tEnd, mrna.strand, mrna.blockSizes, mrna.tStarts  "
                + " FROM all_mrna as mrna  WHERE "
                + "((mrna.tStart > ? AND mrna.tEnd < ?) OR (mrna.tStart < ? AND mrna.tEnd > ?) OR "
                + "(mrna.tStart > ?  AND mrna.tStart < ?) OR  (mrna.tEnd > ? AND  mrna.tEnd < ? )) and mrna.tName = ? ";

        query = query + " and " + SequenceBinUtils.addBinToQuery( "mrna", regionStart, regionEnd );

        if ( strand != null ) {
            query = query + " and mrna.strand = ?";
        }

        Object[] params;

        if ( strand == null )
            params = new Object[] { regionStart, regionEnd, regionStart, regionEnd, regionStart, regionEnd, regionStart,
                    regionEnd, searchChrom };
        else
            params = new Object[] { regionStart, regionEnd, regionStart, regionEnd, regionStart, regionEnd, regionStart,
                    regionEnd, searchChrom, strand };

        return this.queryAndExtract( chromosome, query, params );

    }

    /**
     * @param identifier A Genbank accession referring to an EST or mRNA. For other types of queries this will not
     *        return any results.
     * @return Set containing Lists of PhysicalLocation representing places GoldenPath says the sequence referred to by
     *         the identifier aligns. If no results are found the Set will be empty.
     */
    public Collection<BlatResult> findSequenceLocations( String identifier ) {

        Object[] params = new Object[] { identifier };
        String query;

        /* ESTs */
        query = "SELECT est.tName, est.blockSizes, est.tStarts,est.qStarts, est.strand, est.qSize, est.matches, "
                + "est.misMatches, est.qNumInsert, est.tNumInsert, est.qStart, est.qEnd, est.tStart, est.tEnd, est.repMatches FROM"
                + " all_est AS est WHERE est.qName = ?";
        Set<BlatResult> matchingBlocks = new HashSet<>( this.findLocationsByQuery( query, params ) );

        /* mRNA */
        query = "SELECT mrna.tName, mrna.blockSizes, mrna.tStarts, mrna.qStarts, mrna.strand, mrna.qSize, mrna.matches, "
                + "mrna.misMatches, mrna.qNumInsert, mrna.tNumInsert, mrna.qStart, mrna.qEnd, mrna.tStart, mrna.tEnd, mrna.repMatches"
                + " FROM all_mrna AS mrna WHERE mrna.qName = ?";
        matchingBlocks.addAll( this.findLocationsByQuery( query, params ) );

        return matchingBlocks;
    }

    /**
     * Given a physical location, find how close it is to the 3' end of a gene it is in, using default mapping settings.
     *
     * @param br BlatResult holding the parameters needed.
     * @param method The constant representing the method to use to locate the 3' distance.
     * @return a collection of distances
     */
    public Collection<? extends BioSequence2GeneProduct> getThreePrimeDistances( BlatResult br,
            ThreePrimeDistanceMethod method ) {
        return this.findAssociations( br.getTargetChromosome().getName(), br.getTargetStart(), br.getTargetEnd(),
                br.getTargetStarts(), br.getBlockSizes(), br.getStrand(), method, new ProbeMapperConfig() );
    }

    /**
     * Uses default mapping settings
     *
     * @param identifier identifier
     * @param method the method
     * @return bio seq 2 gene producs
     */
    public Collection<BioSequence2GeneProduct> getThreePrimeDistances( String identifier,
            ThreePrimeDistanceMethod method ) {
        Collection<BlatResult> locations = this.findSequenceLocations( identifier );
        Collection<BioSequence2GeneProduct> results = new HashSet<>();
        for ( BlatResult br : locations ) {
            results.addAll( this.getThreePrimeDistances( br, method ) );
        }
        return results;
    }

    private Collection<Gene> queryAndExtract( final String chromosome, String query, Object[] params ) {
        return this.getJdbcTemplate().query( query, params, new ResultSetExtractor<Collection<Gene>>() {

            @Override
            public Collection<Gene> extractData( ResultSet rs ) throws SQLException {

                Collection<Gene> r = new HashSet<>();
                while ( rs.next() ) {

                    Gene gene = Gene.Factory.newInstance();

                    gene.setNcbiGeneId( Integer.parseInt( rs.getString( 1 ) ) );
                    gene.setOfficialSymbol( rs.getString( 2 ) );
                    gene.setName( gene.getOfficialSymbol() );

                    PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
                    pl.setNucleotide( rs.getLong( 3 ) );
                    pl.setNucleotideLength( rs.getInt( 4 ) - rs.getInt( 3 ) );
                    pl.setStrand( rs.getString( 5 ) );
                    pl.setBin( SequenceBinUtils.binFromRange( ( int ) rs.getLong( 3 ), rs.getInt( 4 ) ) );

                    Chromosome c = new Chromosome( SequenceManipulation.deBlatFormatChromosomeName( chromosome ),
                            GoldenPathSequenceAnalysis.this.getTaxon() );
                    pl.setChromosome( c );

                    // note that we aren't setting the chromosome here; we already know that.
                    gene.setPhysicalLocation( pl );
                    r.add( gene );

                    Blob blockSizes = rs.getBlob( 6 );
                    Blob blockStarts = rs.getBlob( 7 );

                    GoldenPathSequenceAnalysis.this.setBlocks( gene, blockSizes, blockStarts );

                }
                return r;
            }
        } );
    }

    private Set<PhysicalLocation> blocksToPhysicalLocations( int[] blockSizes, int[] blockStarts,
            Chromosome chromosome ) {
        Set<PhysicalLocation> blocks = new HashSet<>();
        for ( int i = 0; i < blockSizes.length; i++ ) {
            long exonStart = blockStarts[i];
            int exonSize = blockSizes[i];
            PhysicalLocation block = PhysicalLocation.Factory.newInstance();
            block.setChromosome( chromosome );
            block.setNucleotide( exonStart );
            block.setNucleotideLength( exonSize );
            block.setBin( SequenceBinUtils.binFromRange( ( int ) exonStart, ( int ) ( exonStart + exonSize ) ) );
            blocks.add( block );
        }
        return blocks;
    }

    /**
     * Recompute the exonOverlap looking at EST evidence. This lets us be a much less conservative about how we compute
     * exon overlaps.
     *
     * @param chromosome chromosome
     * @param queryStart start
     * @param queryEnd end
     * @param starts starts
     * @param sizes sizes
     * @param exonOverlap Exon overlap we're starting with. We only care to improve on this.
     * @param strand of the region
     * @return The best overlap with any exons from an mRNA in the selected region.
     */
    @SuppressWarnings("unchecked")
    private int checkESTs( String chromosome, Long queryStart, Long queryEnd, String starts, String sizes,
            int exonOverlap, String strand ) {

        String key = "EST " + chromosome + "||" + queryStart.toString() + "||" + queryEnd.toString() + strand;

        Collection<Gene> ests;
        if ( cache.containsKey( key ) ) {
            ests = ( Collection<Gene> ) cache.get( key );
        } else {
            ests = this.findESTs( chromosome, queryStart, queryEnd, strand );
            cache.put( key, ests );
        }

        if ( ests.size() > 0 ) {
            if ( GoldenPath.log.isDebugEnabled() )
                GoldenPath.log
                        .debug( ests.size() + " ESTs found at chr" + chromosome + ":" + queryStart + "-" + queryEnd
                                + ", trying to improve overlap of  " + exonOverlap );

            int maxOverlap = exonOverlap;
            for ( Gene est : ests ) {
                int overlap = SequenceManipulation.getGeneExonOverlaps( chromosome, starts, sizes, null, est );
                if ( GoldenPath.log.isDebugEnabled() )
                    GoldenPath.log.debug( "overlap with " + est.getNcbiGeneId() + "=" + overlap );
                if ( overlap > maxOverlap ) {
                    if ( GoldenPath.log.isDebugEnabled() )
                        GoldenPath.log.debug( "Best EST overlap=" + overlap );
                    maxOverlap = overlap;
                }
            }

            exonOverlap = maxOverlap;
            if ( GoldenPath.log.isDebugEnabled() )
                GoldenPath.log.debug( "Overlap with ESTs is now " + exonOverlap );
        }

        return exonOverlap;
    }

    /**
     * Recompute the exonOverlap looking at mRNAs. This lets us be a little less conservative about how we compute exon
     * overlaps.
     *
     * @param chromosome chromosome
     * @param queryStart start
     * @param queryEnd end
     * @param starts starts
     * @param sizes sizes
     * @param exonOverlap Exon overlap we're starting with. We only care to improve on this.
     * @param strand of the region
     * @param gene gene
     * @return The best overlap with any exons from an mRNA in the selected region.
     */
    @SuppressWarnings("unchecked")
    private int checkRNAs( String chromosome, Long queryStart, Long queryEnd, String starts, String sizes,
            int exonOverlap, String strand, Gene gene ) {

        String key = "MRNA " + chromosome + "||" + queryStart.toString() + "||" + queryEnd.toString() + strand;

        Collection<Gene> mRNAs;
        if ( cache.containsKey( key ) ) {
            mRNAs = ( Collection<Gene> ) cache.get( key );
        } else {
            mRNAs = this.findRNAs( chromosome, queryStart, queryEnd, strand );
            cache.put( key, mRNAs );
        }

        if ( mRNAs.size() > 0 ) {
            if ( GoldenPath.log.isDebugEnabled() )
                GoldenPath.log
                        .debug( mRNAs.size() + " mRNAs found at chr" + chromosome + ":" + queryStart + "-" + queryEnd
                                + ", trying to improve overlap of  " + exonOverlap );

            int maxOverlap = exonOverlap;
            for ( Gene mRNA : mRNAs ) {

                if ( gene != null && !gene.getOfficialSymbol()
                        .equals( this.getGeneForMessage( mRNA.getOfficialSymbol() ) ) ) {
                    continue;
                }

                int overlap = SequenceManipulation.getGeneExonOverlaps( chromosome, starts, sizes, null, mRNA );
                if ( GoldenPath.log.isDebugEnabled() )
                    GoldenPath.log.debug( "overlap with " + mRNA.getNcbiGeneId() + "=" + overlap );
                if ( overlap > maxOverlap ) {
                    if ( GoldenPath.log.isDebugEnabled() )
                        GoldenPath.log.debug( "Best mRNA overlap=" + overlap );
                    maxOverlap = overlap;
                }
            }

            exonOverlap = maxOverlap;
            if ( GoldenPath.log.isDebugEnabled() )
                GoldenPath.log.debug( "Overlap with mRNAs is now " + exonOverlap );
        }

        return exonOverlap;
    }

    /**
     * Given a location and a gene product, compute the distance from the 3' end of the gene product as well as the
     * amount of overlap. If the location has low overlaps with known exons (threshold set by
     * RECHECK_OVERLAP_THRESHOLD), we optionally search for mRNAs in the region. If there are overlapping mRNAs, we use
     * the best overlap value. If the overlap is still not high enough we optionally check ESTs.
     *
     * @param chromosome chromosome
     * @param queryStart start
     * @param queryEnd end
     * @param starts Start locations of alignments of the query (target coordinates)
     * @param sizes Sizes of alignments of the query.
     * @param geneProduct GeneProduct with which the overlap and distance is to be computed.
     * @param method method
     * @param config The useEsts and useRNA options are relevant
     * @return a ThreePrimeData object containing the results.
     */
    private BlatAssociation computeLocationInGene( String chromosome, Long queryStart, Long queryEnd, String starts,
            String sizes, GeneProduct geneProduct, ThreePrimeDistanceMethod method, ProbeMapperConfig config ) {

        assert geneProduct != null : "GeneProduct is null";

        BlatAssociation blatAssociation = BlatAssociation.Factory.newInstance();
        blatAssociation.setGeneProduct( geneProduct );
        blatAssociation.setThreePrimeDistanceMeasurementMethod( method );
        PhysicalLocation geneLoc = geneProduct.getPhysicalLocation();

        assert geneLoc != null : "PhysicalLocation for GeneProduct " + geneProduct + " is null";
        assert geneLoc.getNucleotide() != null;

        int geneStart = geneLoc.getNucleotide().intValue();
        int geneEnd = geneLoc.getNucleotide().intValue() + geneLoc.getNucleotideLength();
        int exonOverlap = 0;
        if ( starts != null && sizes != null ) {
            exonOverlap = SequenceManipulation
                    .getGeneProductExonOverlap( starts, sizes, geneLoc.getStrand(), geneProduct );
            int totalSize = SequenceManipulation.totalSize( sizes );

            if ( config.isUseMrnas()
                    && exonOverlap / ( double ) ( totalSize ) < GoldenPathSequenceAnalysis.RECHECK_OVERLAP_THRESHOLD ) {
                int newOverlap = this
                        .checkRNAs( chromosome, queryStart, queryEnd, starts, sizes, exonOverlap, geneLoc.getStrand(),
                                geneProduct.getGene() );

                if ( newOverlap > exonOverlap ) {
                    GoldenPath.log.debug( "mRNA overlap was higher than primary transcript" );
                    exonOverlap = newOverlap;
                }
            }

            if ( config.isUseEsts()
                    && exonOverlap / ( double ) ( totalSize ) < GoldenPathSequenceAnalysis.RECHECK_OVERLAP_THRESHOLD ) {
                int newOverlap = this
                        .checkESTs( chromosome, queryStart, queryEnd, starts, sizes, exonOverlap, geneLoc.getStrand() );

                if ( newOverlap > exonOverlap ) {
                    GoldenPath.log.debug( "Exon overlap was higher than mrna or  primary transcript" );
                    exonOverlap = newOverlap;
                }
            }
            assert exonOverlap <= totalSize;
        }

        blatAssociation.setOverlap( exonOverlap );

        if ( method == ThreePrimeDistanceMethod.MIDDLE ) {
            int center = SequenceManipulation.findCenter( starts, sizes );
            if ( geneLoc.getStrand().equals( "+" ) ) {
                // then the 3' end is at the 'end'. : >>>>>>>>>>>>>>>>>>>>>*>>>>> (* is where we might be)
                blatAssociation.setThreePrimeDistance( ( long ) Math.max( 0, geneEnd - center ) );
            } else if ( geneProduct.getPhysicalLocation().getStrand().equals( "-" ) ) {
                // then the 3' end is at the 'start'. : <<<*<<<<<<<<<<<<<<<<<<<<<<<
                blatAssociation.setThreePrimeDistance( ( long ) Math.max( 0, center - geneStart ) );
            } else {
                throw new IllegalArgumentException( "Strand wasn't '+' or '-'" );
            }
        } else if ( method == ThreePrimeDistanceMethod.RIGHT ) {
            if ( geneLoc.getStrand().equals( "+" ) ) {
                // then the 3' end is at the 'end'. : >>>>>>>>>>>>>>>>>>>>>*>>>>> (* is where we might be)
                blatAssociation.setThreePrimeDistance( Math.max( 0, geneEnd - queryEnd ) );
            } else if ( geneProduct.getPhysicalLocation().getStrand().equals( "-" ) ) {
                // then the 3' end is at the 'start'. : <<<*<<<<<<<<<<<<<<<<<<<<<<<
                blatAssociation.setThreePrimeDistance( Math.max( 0, queryStart - geneStart ) );
            } else {
                throw new IllegalArgumentException( "Strand wasn't '+' or '-'" );
            }
        } else if ( method == ThreePrimeDistanceMethod.LEFT ) {
            throw new UnsupportedOperationException( "Left edge measure not supported" );
        } else {
            throw new IllegalArgumentException( "Unknown method" );
        }
        return blatAssociation;
    }

    /**
     * Generic method to retrieve Genes from the GoldenPath database. The query given must have the appropriate form.
     *
     * @param starti start
     * @param endi end
     * @param chromosome chromosome
     * @param query query
     * @return List of GeneProducts. This is a collection of transient instances, not from Gemma's database.
     */
    private Collection<GeneProduct> findGenesByQuery( Long starti, Long endi, final String chromosome, String strand,
            String query ) {
        // Cases:
        // 1. gene is contained within the region: txStart > start & txEnd < end;
        // 2. region is contained within the gene: txStart < start & txEnd > end;
        // 3. region overlaps start of gene: txStart > start & txStart < end.
        // 4. region overlaps end of gene: txEnd > start & txEnd < end
        //

        Object[] params;
        if ( strand != null ) {
            params = new Object[] { starti, endi, starti, endi, starti, endi, starti, endi, chromosome, strand };
        } else {
            params = new Object[] { starti, endi, starti, endi, starti, endi, starti, endi, chromosome };
        }

        return this.getJdbcTemplate().query( query, params, new ResultSetExtractor<Collection<GeneProduct>>() {

            @Override
            public Collection<GeneProduct> extractData( ResultSet rs ) throws SQLException {
                Collection<GeneProduct> r = new HashSet<>();
                while ( rs.next() ) {

                    GeneProduct product = GeneProduct.Factory.newInstance();

                    String name = rs.getString( 1 );

                    /*
                     * This happens for a very few cases in kgXref, where the gene is 'abParts'. We have to skip these.
                     */
                    if ( StringUtils.isBlank( name ) ) {
                        continue;
                    }

                    /*
                     * The name is our database identifier (either genbank or ensembl)
                     */
                    DatabaseEntry accession = DatabaseEntry.Factory.newInstance();
                    accession.setAccession( name );
                    if ( name.startsWith( "ENST" ) ) {
                        accession.setExternalDatabase( NcbiGeneConverter.getEnsembl() );
                    } else {
                        accession.setExternalDatabase( NcbiGeneConverter.getGenbank() );
                    }

                    product.getAccessions().add( accession );

                    Gene gene = Gene.Factory.newInstance();
                    gene.setOfficialSymbol( rs.getString( 2 ) );
                    gene.setName( gene.getOfficialSymbol() );
                    Taxon taxon = GoldenPathSequenceAnalysis.this.getTaxon();

                    assert taxon != null;
                    gene.setTaxon( taxon );

                    PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
                    pl.setNucleotide( rs.getLong( 3 ) );
                    pl.setNucleotideLength( rs.getInt( 4 ) - rs.getInt( 3 ) );
                    pl.setStrand( rs.getString( 5 ) );
                    pl.setBin( SequenceBinUtils.binFromRange( ( int ) rs.getLong( 3 ), rs.getInt( 4 ) ) );
                    PhysicalLocation genePl = PhysicalLocation.Factory.newInstance();
                    genePl.setStrand( pl.getStrand() );

                    Chromosome c = new Chromosome( SequenceManipulation.deBlatFormatChromosomeName( chromosome ),
                            taxon );
                    pl.setChromosome( c );
                    genePl.setChromosome( c );

                    /*
                     * this only contains the chromosome and strand: the nucleotide positions are only valid for the
                     * gene product
                     */
                    gene.setPhysicalLocation( genePl );

                    product.setName( name );

                    String descriptionFromGP = rs.getString( 8 );
                    if ( StringUtils.isBlank( descriptionFromGP ) ) {
                        product.setDescription( "Imported from GoldenPath" );
                    } else {
                        product.setDescription( "Imported from Golden Path: " + descriptionFromGP );
                    }
                    product.setPhysicalLocation( pl );
                    product.setGene( gene );

                    Blob exonStarts = rs.getBlob( 6 );
                    Blob exonEnds = rs.getBlob( 7 );
                    product.setExons( GoldenPathSequenceAnalysis.this.getExons( c, exonStarts, exonEnds ) );

                    /*
                     * For microRNAs, we don't get exons, so we just use the whole length for now.
                     */
                    if ( product.getExons().size() == 0 ) {
                        product.getExons().add( pl );
                    }

                    r.add( product );

                }
                return r;
            }

        } );

    }

    /**
     * Uses a query that can retrieve BlatResults from GoldenPath. The query must have the appropriate form.
     *
     * @param query query
     * @param params params
     * @return blat results
     */
    private Collection<BlatResult> findLocationsByQuery( final String query, final Object[] params ) {

        return this.getJdbcTemplate().query( query, params, new ResultSetExtractor<Collection<BlatResult>>() {
            @Override
            public Collection<BlatResult> extractData( ResultSet rs ) throws SQLException {
                Collection<BlatResult> r = new HashSet<>();
                while ( rs.next() ) {

                    BlatResult blatResult = BlatResult.Factory.newInstance();

                    Chromosome c = new Chromosome( SequenceManipulation.deBlatFormatChromosomeName( rs.getString( 1 ) ),
                            GoldenPathSequenceAnalysis.this.getTaxon() );

                    blatResult.setTargetChromosome( c );

                    Blob blockSizes = rs.getBlob( 2 );
                    Blob targetStarts = rs.getBlob( 3 );
                    Blob queryStarts = rs.getBlob( 4 );

                    blatResult.setBlockSizes( SQLUtils.blobToString( blockSizes, StandardCharsets.ISO_8859_1 ) );
                    blatResult.setTargetStarts( SQLUtils.blobToString( targetStarts, StandardCharsets.ISO_8859_1 ) );
                    blatResult.setQueryStarts( SQLUtils.blobToString( queryStarts, StandardCharsets.ISO_8859_1 ) );

                    blatResult.setStrand( rs.getString( 5 ) );

                    // need the query size to compute scores.
                    blatResult.setQuerySequence( BioSequence.Factory.newInstance() );
                    blatResult.getQuerySequence().setLength( rs.getLong( 6 ) );
                    blatResult.getQuerySequence().setName( ( String ) params[0] );

                    blatResult.setMatches( rs.getInt( 7 ) );
                    blatResult.setMismatches( rs.getInt( 8 ) );
                    blatResult.setQueryGapCount( rs.getInt( 9 ) );
                    blatResult.setTargetGapCount( rs.getInt( 10 ) );

                    blatResult.setQueryStart( rs.getInt( 11 ) );
                    blatResult.setQueryEnd( rs.getInt( 12 ) );

                    blatResult.setTargetStart( rs.getLong( 13 ) );
                    blatResult.setTargetEnd( rs.getLong( 14 ) );

                    blatResult.setRepMatches( rs.getInt( 15 ) );

                    r.add( blatResult );
                }
                return r;
            }

        } );

    }

    /**
     * Fill in the exon information for a gene, given the raw blobs from the GoldenPath database.
     * Be sure to pass the right Blob arguments!
     *
     * @param exonStarts starts
     * @param exonEnds ends
     * @throws SQLException sql problem
     */
    private Set<PhysicalLocation> getExons( Chromosome chrom, Blob exonStarts, Blob exonEnds )
            throws SQLException {

        Set<PhysicalLocation> exons = new HashSet<>();
        if ( exonStarts == null || exonEnds == null ) {
            return exons;
        }

        String exonStartLocations = SQLUtils.blobToString( exonStarts, StandardCharsets.ISO_8859_1 );
        String exonEndLocations = SQLUtils.blobToString( exonEnds, StandardCharsets.ISO_8859_1 );

        int[] exonStartsInts = SequenceManipulation.blatLocationsToIntArray( exonStartLocations );
        int[] exonEndsInts = SequenceManipulation.blatLocationsToIntArray( exonEndLocations );

        assert exonStartsInts.length == exonEndsInts.length;

        for ( int i = 0; i < exonEndsInts.length; i++ ) {
            int exonStart = exonStartsInts[i];
            int exonEnd = exonEndsInts[i];
            PhysicalLocation exon = PhysicalLocation.Factory.newInstance();

            exon.setChromosome( chrom );
            assert chrom.getTaxon() != null;

            exon.setNucleotide( ( long ) exonStart );
            exon.setNucleotideLength( exonEnd - exonStart );
            exon.setBin( SequenceBinUtils.binFromRange( exonStart, exonEnd ) );
            exons.add( exon );
        }

        return exons;
    }

    /**
     * Only for refseq genes.
     *
     * @param ncbiId mRNA accession eg. NR_000028
     * @return string
     */
    private String getGeneForMessage( String ncbiId ) {

        return this.getJdbcTemplate()
                .query( "SELECT rg.name2 FROM all_mrna m INNER JOIN refGene rg ON m.qName = rg.name WHERE m.qName = ? ",
                        new Object[] { ncbiId }, rs -> {
                            while ( rs.next() ) {
                                String string = rs.getString( 1 );
                                if ( StringUtils.isNotBlank( string ) )
                                    return string;
                            }
                            return null;
                        } );

    }

    /**
     * Handle the format used by the all_mrna and other GoldenPath tables, which go by sizes of blocks and their starts,
     * not the starts and ends.
     * Be sure to pass the right Blob arguments!
     *
     * @param gene gene
     * @param blockSizes sizes
     * @param blockStarts starts
     */
    private void setBlocks( Gene gene, Blob blockSizes, Blob blockStarts ) throws SQLException {
        if ( blockSizes == null || blockStarts == null )
            return;

        String exonSizes = SQLUtils.blobToString( blockSizes, StandardCharsets.ISO_8859_1 );
        String exonStarts = SQLUtils.blobToString( blockStarts, StandardCharsets.ISO_8859_1 );

        int[] exonSizeInts = SequenceManipulation.blatLocationsToIntArray( exonSizes );
        int[] exonStartInts = SequenceManipulation.blatLocationsToIntArray( exonStarts );

        assert exonSizeInts.length == exonStartInts.length;

        GeneProduct gp = GeneProduct.Factory.newInstance();
        Chromosome chromosome = null;
        if ( gene.getPhysicalLocation() != null )
            chromosome = gene.getPhysicalLocation().getChromosome();
        Set<PhysicalLocation> exons = this.blocksToPhysicalLocations( exonSizeInts, exonStartInts, chromosome );
        gp.setExons( exons );
        gp.setName( gene.getNcbiGeneId().toString() ); // this isn't right?
        Set<GeneProduct> products = new HashSet<>();
        products.add( gp );
        gene.setProducts( products );
    }

}
