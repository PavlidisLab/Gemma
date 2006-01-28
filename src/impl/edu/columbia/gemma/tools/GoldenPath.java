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
package edu.columbia.gemma.tools;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.util.SQLUtils;
import edu.columbia.gemma.genome.Chromosome;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.PhysicalLocation;
import edu.columbia.gemma.genome.gene.GeneProduct;

/**
 * Perform useful queries against GoldenPath (UCSC) databases.
 * <p>
 * This is partly temporary, until we have this in our own database.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPath {
    /**
     * 3' distances are measured from the center of the query
     */
    public static final String CENTER = "center";

    /**
     * 3' distances are measured from the 3' (right) edge of the query
     */
    public static final String RIGHTEND = "right";

    /**
     * 3' distance are measured from the 5' (left) edge of the query.
     */
    private static final String LEFTEND = "left";

    private static final Log log = LogFactory.getLog( GoldenPath.class );

    /**
     * If the exon overlap fraction with annotated (known/refseq) exons is less than this value, some additional
     * checking for mRNAs and ESTs may be done.
     */
    private static final double RECHECK_OVERLAP_THRESHOLD = 0.9;
    private Connection conn;

    private QueryRunner qr;

    /**
     * @param databaseName
     * @param host
     * @param user
     * @param password
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public GoldenPath( int port, String databaseName, String host, String user, String password ) throws SQLException,
            InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
        String url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?relaxAutoCommit=true";
        conn = DriverManager.getConnection( url, user, password );
        qr = new QueryRunner();
    }

    /**
     * Find "Known" genes contained in or overlapping a region. Note that the NCBI symbol may be blank, when the gene is
     * not a refSeq gene.
     * 
     * @param chromosome
     * @param start
     * @param end
     * @param strand
     * @return
     */
    public List<Gene> findKnownGenesByLocation( String chromosome, Long start, Long end, String strand ) {
        String searchChrom = SequenceManipulation.blatFormatChromosomeName( chromosome );
        String query = "SELECT kgxr.refSeq, kgxr.geneSymbol, kg.txStart, kg.txEnd, kg.strand, kg.exonStarts, kg.exonEnds "
                + " FROM knownGene as kg INNER JOIN"
                + " kgXref AS kgxr ON kg.name=kgxr.kgID WHERE "
                + "((kg.txStart > ? AND kg.txEnd < ?) OR (kg.txStart < ? AND kg.txEnd > ?) OR "
                + "(kg.txStart > ?  AND kg.txStart < ?) OR  (kg.txEnd > ? AND  kg.txEnd < ? )) and kg.chrom = ? ";

        if ( strand != null ) {
            query = query + " AND strand = ? order by txStart ";
        } else {
            query = query + " order by txStart ";
        }

        return findGenesByQuery( start, end, searchChrom, strand, query );
    }

    /**
     * Find RefSeq genes contained in or overlapping a region.
     * 
     * @param chromosome
     * @param start
     * @param strand
     * @param end
     */
    public List<Gene> findRefGenesByLocation( String chromosome, Long start, Long end, String strand ) {
        String searchChrom = SequenceManipulation.blatFormatChromosomeName( chromosome );
        String query = "SELECT name, geneName, txStart, txEnd, strand, exonStarts, exonEnds FROM refFlat WHERE "
                + "((txStart > ? AND txEnd < ?) OR (txStart < ? AND txEnd > ?) OR "
                + "(txStart > ?  AND txStart < ?) OR  (txEnd > ? AND  txEnd < ? )) and chrom = ? ";

        if ( strand != null ) {
            query = query + " AND strand = ? order by txStart ";
        } else {
            query = query + " order by txStart ";
        }

        return findGenesByQuery( start, end, searchChrom, strand, query );

    }

    /**
     * Given a physical location, find how close it is to the 3' end of a gene it is in.
     * 
     * @param chromosome The chromosome name (the organism is set by the constructor)
     * @param queryStart The start base of the region to query (the start of the alignment to the genome)
     * @param queryEnd The end base of the region to query (the end of the alignment to the genome)
     * @param starts Locations of alignment block starts. (comma-delimited from blat)
     * @param sizes Sizes of alignment blocks (comma-delimited from blat)
     * @param strand Either + or - indicating the strand to look on, or null to search both strands.
     * @param method The constant representing the method to use to locate the 3' distance.
     * @return A list of ThreePrimeData objects. The distance stored by a ThreePrimeData will be 0 if the sequence
     *         overhangs the found genes (rather than providing a negative distance). If no genes are found, the result
     *         is null;
     */
    public List<ThreePrimeData> getThreePrimeDistances( String chromosome, Long queryStart, Long queryEnd,
            String starts, String sizes, String strand, String method ) {

        if ( queryEnd < queryStart ) throw new IllegalArgumentException( "End must not be less than start" );

        // starting with refgene means we can get the correct transcript name etc.
        Collection<Gene> genes = findRefGenesByLocation( chromosome, queryStart, queryEnd, strand );

        // get known genes as well, in case all we got was an intron.
        genes.addAll( findKnownGenesByLocation( chromosome, queryStart, queryEnd, strand ) );

        if ( genes.size() == 0 ) return null;

        List<ThreePrimeData> results = new ArrayList<ThreePrimeData>();
        for ( Gene gene : genes ) {
            ThreePrimeData tpd = computeLocationInGene( chromosome, queryStart, queryEnd, starts, sizes, gene, method );
            results.add( tpd );
        }
        return results;
    }

    /**
     * Recompute the exonOverlap looking at mRNAs. This lets us be a little less conservative about how we compute exon
     * overlaps.
     * 
     * @param chromosome
     * @param queryStart
     * @param queryEnd
     * @param starts
     * @param sizes
     * @param exonOverlap Exon overlap we're starting with. We only care to improve on this.
     * @return The best overlap with any exons from an mRNA in the selected region.
     * @see getThreePrimeDistances
     *      <p>
     *      FIXME it will improve performance to cache the results of these queries, because we often look in the same
     *      place for other hits.
     */
    private int checkRNAs( String chromosome, Long queryStart, Long queryEnd, String starts, String sizes,
            int exonOverlap ) {
        List<Gene> mRNAs = findRNAs( chromosome, queryStart, queryEnd );

        if ( mRNAs.size() > 0 ) {
            log.debug( mRNAs.size() + " mRNAs found at chr" + chromosome + ":" + queryStart + "-" + queryEnd
                    + ", trying to improve overlap of  " + exonOverlap );

            int maxOverlap = exonOverlap;
            for ( Gene mRNA : mRNAs ) {
                int overlap = SequenceManipulation.getGeneExonOverlaps( chromosome, starts, sizes, null, mRNA );
                log.debug( "overlap with " + mRNA.getNcbiId() + "=" + overlap );
                if ( overlap > maxOverlap ) {
                    log.debug( "Best mRNA overlap=" + overlap );
                    maxOverlap = overlap;
                }
            }

            exonOverlap = maxOverlap;
            log.debug( "Overlap with mRNAs is now " + exonOverlap );
        }
        return exonOverlap;
    }

    /**
     * Given a location and a gene, compute the distance from the 3' end of the gene as well as the amount of overlap.
     * If the location has low overlaps with known exons (threshold set by RECHECK_OVERLAP_THRESHOLD), we search for
     * mRNAs in the region. If there are overlapping mRNAs, we use the best overlap value.
     * 
     * @param chromosome
     * @param queryStart
     * @param queryEnd
     * @param starts Start locations of alignments of the query.
     * @param sizes Sizes of alignments of the query.
     * @param gene Gene with which the overlap and distance is to be computed.
     * @param method
     * @return a ThreePrimeData object containing the results.
     * @see getThreePrimeDistances
     *      <p>
     *      FIXME this should take a PhysicalLocation as an argument.
     */
    private ThreePrimeData computeLocationInGene( String chromosome, Long queryStart, Long queryEnd, String starts,
            String sizes, Gene gene, String method ) {
        ThreePrimeData tpd = new ThreePrimeData( gene );
        PhysicalLocation geneLoc = gene.getPhysicalLocation();
        int geneStart = geneLoc.getNucleotide().intValue();
        int geneEnd = geneLoc.getNucleotide().intValue() + geneLoc.getNucleotideLength().intValue();
        int exonOverlap = 0;
        if ( starts != null & sizes != null ) {
            exonOverlap = SequenceManipulation.getGeneExonOverlaps( chromosome, starts, sizes, null, gene );
            int totalSize = SequenceManipulation.totalSize( sizes );
            assert exonOverlap <= totalSize;
            if ( exonOverlap / ( double ) ( totalSize ) < RECHECK_OVERLAP_THRESHOLD ) {
                exonOverlap = checkRNAs( chromosome, queryStart, queryEnd, starts, sizes, exonOverlap );
            }
        }

        tpd.setExonOverlap( exonOverlap );
        tpd.setInIntron( exonOverlap == 0 );

        if ( method == GoldenPath.CENTER ) {
            int center = SequenceManipulation.findCenter( starts, sizes );
            if ( geneLoc.getStrand().equals( "+" ) ) {
                // then the 3' end is at the 'end'. : >>>>>>>>>>>>>>>>>>>>>*>>>>> (* is where we might be)
                tpd.setDistance( new Long( Math.max( 0, geneEnd - center ) ) );
            } else if ( gene.getPhysicalLocation().getStrand().equals( "-" ) ) {
                // then the 3' end is at the 'start'. : <<<*<<<<<<<<<<<<<<<<<<<<<<<
                tpd.setDistance( new Long( Math.max( 0, center - geneStart ) ) );
            } else {
                throw new IllegalArgumentException( "Strand wasn't '+' or '-'" );
            }
        } else if ( method == GoldenPath.RIGHTEND ) {
            if ( geneLoc.getStrand().equals( "+" ) ) {
                // then the 3' end is at the 'end'. : >>>>>>>>>>>>>>>>>>>>>*>>>>> (* is where we might be)
                tpd.setDistance( Math.max( 0, geneEnd - queryEnd ) );
            } else if ( gene.getPhysicalLocation().getStrand().equals( "-" ) ) {
                // then the 3' end is at the 'start'. : <<<*<<<<<<<<<<<<<<<<<<<<<<<
                tpd.setDistance( Math.max( 0, queryStart - geneStart ) );
            } else {
                throw new IllegalArgumentException( "Strand wasn't '+' or '-'" );
            }
        } else if ( method == GoldenPath.LEFTEND ) {
            throw new UnsupportedOperationException( "Left edge measure not supported" );
        } else {
            throw new IllegalArgumentException( "Unknown method" );
        }
        return tpd;
    }

    /**
     * @param query Generic method to retrive Genes from the GoldenPath database. The query given must have the
     *        appropriate form.
     * @param starti
     * @param endi
     * @param chromosome
     * @param query
     * @return List of Genes.
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    private List<Gene> findGenesByQuery( Long starti, Long endi, final String chromosome, String strand, String query ) {
        // Cases:
        // 1. gene is contained within the region: txStart > start & txEnd < end;
        // 2. region is conained within the gene: txStart < start & txEnd > end;
        // 3. region overlaps start of gene: txStart > start & txStart < end.
        // 4. region overlaps end of gene: txEnd > start & txEnd < end
        //           
        try {

            Object[] params;
            if ( strand != null ) {
                params = new Object[] { starti, endi, starti, endi, starti, endi, starti, endi, chromosome, strand };
            } else {
                params = new Object[] { starti, endi, starti, endi, starti, endi, starti, endi, chromosome };
            }

            return ( List<Gene> ) qr.query( conn, query, params, new ResultSetHandler() {

                @SuppressWarnings("synthetic-access")
                public Object handle( ResultSet rs ) throws SQLException {
                    List<Gene> r = new ArrayList<Gene>();
                    while ( rs.next() ) {

                        Gene gene = Gene.Factory.newInstance();

                        gene.setNcbiId( rs.getString( 1 ) );

                        gene.setOfficialSymbol( rs.getString( 2 ) );
                        gene.setId( new Long( gene.getNcbiId().hashCode() ) );

                        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
                        pl.setNucleotide( rs.getLong( 3 ) );
                        pl.setNucleotideLength( rs.getInt( 4 ) - rs.getInt( 3 ) );
                        pl.setStrand( rs.getString( 5 ) );

                        Chromosome c = Chromosome.Factory.newInstance();
                        c.setName( SequenceManipulation.deBlatFormatChromosomeName( chromosome ) );
                        pl.setChromosome( c );

                        // note that we aren't setting the chromosome here; we already know that.
                        gene.setPhysicalLocation( pl );
                        r.add( gene );

                        Blob exonStarts = rs.getBlob( 6 );
                        Blob exonEnds = rs.getBlob( 7 );

                        setExons( gene, exonStarts, exonEnds );
                    }
                    return r;
                }

            } );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Check to see if there are mRNAs that overlap with this region. We promote the mRNAs to the status of genes for
     * this purpose.
     * 
     * @param chromosome
     * @param regionStart the region to be checked
     * @param regionEnd
     * @return The number of mRNAs which overlap the query region.
     */
    @SuppressWarnings("unchecked")
    private List<Gene> findRNAs( final String chromosome, Long regionStart, Long regionEnd ) {

        String searchChrom = SequenceManipulation.blatFormatChromosomeName( chromosome );
        String query = "SELECT mrna.qName, mrna.qName, mrna.tStart, mrna.tEnd, mrna.strand, mrna.blockSizes, mrna.tStarts  "
                + " FROM all_mrna as mrna  WHERE "
                + "((mrna.tStart > ? AND mrna.tEnd < ?) OR (mrna.tStart < ? AND mrna.tEnd > ?) OR "
                + "(mrna.tStart > ?  AND mrna.tStart < ?) OR  (mrna.tEnd > ? AND  mrna.tEnd < ? )) and mrna.tName = ? ";

        query = query + " order by mrna.tStart ";

        Object[] params = new Object[] { regionStart, regionEnd, regionStart, regionEnd, regionStart, regionEnd,
                regionStart, regionEnd, searchChrom };
        try {
            return ( List<Gene> ) qr.query( conn, query, params, new ResultSetHandler() {

                @SuppressWarnings("synthetic-access")
                public Object handle( ResultSet rs ) throws SQLException {
                    List<Gene> r = new ArrayList<Gene>();
                    while ( rs.next() ) {

                        Gene gene = Gene.Factory.newInstance();

                        gene.setNcbiId( rs.getString( 1 ) );

                        gene.setOfficialSymbol( rs.getString( 2 ) );
                        gene.setId( new Long( gene.getNcbiId().hashCode() ) );

                        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
                        pl.setNucleotide( rs.getLong( 3 ) );
                        pl.setNucleotideLength( rs.getInt( 4 ) - rs.getInt( 3 ) );
                        pl.setStrand( rs.getString( 5 ) );

                        Chromosome c = Chromosome.Factory.newInstance();
                        c.setName( SequenceManipulation.deBlatFormatChromosomeName( chromosome ) );
                        pl.setChromosome( c );

                        // note that we aren't setting the chromosome here; we already know that.
                        gene.setPhysicalLocation( pl );
                        r.add( gene );

                        Blob blockSizes = rs.getBlob( 6 );
                        Blob blockStarts = rs.getBlob( 7 );

                        setBlocks( gene, blockSizes, blockStarts );

                    }
                    return r;
                }
            } );

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Handle the format used by the all_mrna and other GoldenPath tables, which go by sizes of blocks and their starts,
     * not the starts and ends.
     * <p>
     * Be sure to pass the right Blob arguments!
     * 
     * @param gene
     * @param blockSizes
     * @param blockStarts
     */
    private void setBlocks( Gene gene, Blob blockSizes, Blob blockStarts ) throws SQLException {
        if ( blockSizes == null || blockStarts == null ) return;

        String exonSizes = SQLUtils.blobToString( blockSizes );
        String exonStarts = SQLUtils.blobToString( blockStarts );

        int[] exonSizeInts = SequenceManipulation.blatLocationsToIntArray( exonSizes );
        int[] exonStartInts = SequenceManipulation.blatLocationsToIntArray( exonStarts );

        assert exonSizeInts.length == exonStartInts.length;

        GeneProduct gp = GeneProduct.Factory.newInstance();
        Collection<PhysicalLocation> exons = new ArrayList<PhysicalLocation>();
        for ( int i = 0; i < exonSizeInts.length; i++ ) {
            long exonStart = exonStartInts[i];
            int exonSize = exonSizeInts[i];
            PhysicalLocation exon = PhysicalLocation.Factory.newInstance();
            if ( gene.getPhysicalLocation() != null ) exon.setChromosome( gene.getPhysicalLocation().getChromosome() );
            exon.setNucleotide( exonStart );
            exon.setNucleotideLength( new Integer( exonSize ) );
            exons.add( exon );
        }
        gp.setExons( exons );
        gp.setName( gene.getNcbiId() );
        Collection<GeneProduct> products = new HashSet<GeneProduct>();
        products.add( gp );
        gene.setProducts( products );
    }

    /**
     * Fill in the exon information for a gene, given the raw blobs from the GoldenPath database.
     * <p>
     * Be sure to pass the right Blob arguments!
     * 
     * @param gene
     * @param exonStarts
     * @param exonEnds
     * @throws SQLException
     */
    private void setExons( Gene gene, Blob exonStarts, Blob exonEnds ) throws SQLException {

        if ( exonStarts == null || exonEnds == null ) return;

        String exonStartLocations = SQLUtils.blobToString( exonStarts );
        String exonEndLocations = SQLUtils.blobToString( exonEnds );

        int[] exonStartsInts = SequenceManipulation.blatLocationsToIntArray( exonStartLocations );
        int[] exonEndsInts = SequenceManipulation.blatLocationsToIntArray( exonEndLocations );

        assert exonStartsInts.length == exonEndsInts.length;

        GeneProduct gp = GeneProduct.Factory.newInstance();
        Collection<PhysicalLocation> exons = new ArrayList<PhysicalLocation>();
        for ( int i = 0; i < exonEndsInts.length; i++ ) {
            int exonStart = exonStartsInts[i];
            int exonEnd = exonEndsInts[i];
            PhysicalLocation exon = PhysicalLocation.Factory.newInstance();
            if ( gene.getPhysicalLocation() != null ) exon.setChromosome( gene.getPhysicalLocation().getChromosome() );
            exon.setNucleotide( new Long( exonStart ) );
            exon.setNucleotideLength( new Integer( exonEnd - exonStart ) );
            exons.add( exon );
        }
        gp.setExons( exons );
        gp.setName( gene.getNcbiId() );
        Collection<GeneProduct> products = new HashSet<GeneProduct>();
        products.add( gp );
        gene.setProducts( products );
    }

    /**
     * Helper data transfer object.
     */
    public class ThreePrimeData {

        /**
         * The distance from the gene (measured from a point defined by the caller)
         */
        private Long distance;
        private int exonOverlap = 0;

        private Gene gene;

        private boolean inIntron = false;

        public ThreePrimeData( Gene gene ) {
            this.gene = gene;
        }

        public Long getDistance() {
            return this.distance;
        }

        /**
         * @return Returns the exonOverlap.
         */
        public int getExonOverlap() {
            return this.exonOverlap;
        }

        public Gene getGene() {
            return this.gene;
        }

        public boolean isInIntron() {
            return this.inIntron;
        }

        public void setDistance( Long i ) {
            this.distance = i;
        }

        /**
         * @param exonOverlap The exonOverlap to set.
         */
        public void setExonOverlap( int exonOverlap ) {
            this.exonOverlap = exonOverlap;
        }

        public void setInIntron( boolean inIntron ) {
            this.inIntron = inIntron;
        }

    }

}
