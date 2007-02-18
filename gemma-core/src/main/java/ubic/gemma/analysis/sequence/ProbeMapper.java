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
package ubic.gemma.analysis.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.apps.Blat;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.ProbeAlignedRegionService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;

/**
 * Provides methods for mapping sequences to genes and gene products.
 * 
 * @spring.bean name="probeMapper"
 * @spring.property name="probeAlignedRegionService" ref="probeAlignedRegionService"
 * @spring.property name="chromosomeService" ref="chromosomeService"
 * @spring.property name="taxonService" ref="taxonService"
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapper {
    public static final double DEFAULT_IDENTITY_THRESHOLD = 0.80;
    public static final double DEFAULT_SCORE_THRESHOLD = 0.80;
    private Log log = LogFactory.getLog( ProbeMapper.class.getName() );
    private double identityThreshold = DEFAULT_IDENTITY_THRESHOLD;
    private double scoreThreshold = DEFAULT_SCORE_THRESHOLD;
    private double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;
    private ThreePrimeDistanceMethod threeprimeMethod = ThreePrimeDistanceMethod.RIGHT;

    private ProbeAlignedRegionService probeAlignedRegionService;
    private ChromosomeService chromosomeService;
    private TaxonService taxonService;

    /**
     * @return the blatScoreThreshold
     */
    public double getBlatScoreThreshold() {
        return this.blatScoreThreshold;
    }

    /**
     * @param goldenPathDb
     * @param blatResults
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults ) {
        return this.processBlatResults( goldenPathDb, blatResults, false );
    }

    /**
     * Given some blat results (possibly for multiple sequences) determine which if any gene products they should be
     * associatd with; if there are multiple results for a single sequence, these are further analyzed for specificity
     * and redundancy, so that there is a single BlatAssociation between any sequence andy andy gene product.
     * <p>
     * This is a major entrypoint for this API.
     * 
     * @param goldenPathDb
     * @param blatResults
     * @param ignoreStrand ignore the strand when scoring alignments.
     * @return A map of sequence names to collections of blat associations for each sequence.
     * @throws IOException
     */
    public Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults, boolean ignoreStrand ) {

        if ( log.isDebugEnabled() ) {
            log.debug( blatResults.size() + " Blat results to map " );
        }

        assert goldenPathDb != null;
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<String, Collection<BlatAssociation>>();
        int count = 0;
        int skipped = 0;

        // group results together by BioSequence
        Map<BioSequence, Collection<BlatResult>> biosequenceToBlatResults = new HashMap<BioSequence, Collection<BlatResult>>();

        for ( BlatResult blatResult : blatResults ) {
            if ( !biosequenceToBlatResults.containsKey( blatResult.getQuerySequence() ) ) {
                biosequenceToBlatResults.put( blatResult.getQuerySequence(), new HashSet<BlatResult>() );
            }
            biosequenceToBlatResults.get( blatResult.getQuerySequence() ).add( blatResult );
        }

        // Do them one sequence at a time.
        for ( BioSequence sequence : biosequenceToBlatResults.keySet() ) {
            Collection<BlatResult> blatResultsForSequence = biosequenceToBlatResults.get( sequence );
            if ( log.isDebugEnabled() ) {
                log.debug( blatResultsForSequence.size() + " Blat results for " + sequence );
            }

            Collection<BlatAssociation> blatAssociationsForSequence = new HashSet<BlatAssociation>();

            for ( BlatResult blatResult : blatResultsForSequence ) {
                assert blatResult.score() >= 0 : "Score was " + blatResult.score();
                assert blatResult.identity() >= 0 : "Identity was " + blatResult.identity();
                if ( blatResult.score() < scoreThreshold || blatResult.identity() < identityThreshold ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "Result for " + sequence + " skipped with score=" + blatResult.score()
                                + " identity=" + blatResult.identity() );
                    skipped++;
                    continue;
                }

                // here's the key line!
                Collection<BlatAssociation> resultsForOneBlatResult = processBlatResult( goldenPathDb, blatResult,
                        ignoreStrand );

                if ( resultsForOneBlatResult != null && resultsForOneBlatResult.size() > 0 ) {
                    blatAssociationsForSequence.addAll( resultsForOneBlatResult );
                } else {
                    // here we have to provide a 'provisional' mapping to a ProbeAlignedRegion.
                    ProbeAlignedRegion par = makePar( blatResult );
                    BlatAssociation parAssociation = makeBlatAssociationWithPar( blatResult, par );
                    blatAssociationsForSequence.add( parAssociation );
                    if ( log.isDebugEnabled() )
                        log.debug( "Adding PAR for " + sequence + " with alignment " + blatResult );
                }

                if ( ++count % 100 == 0 && log.isInfoEnabled() )
                    log.info( "Annotations computed for " + count + " blat results" );

            } // end of iteration over results for this sequence.

            if ( log.isDebugEnabled() ) {
                log.debug( blatAssociationsForSequence.size() + " associations for " + sequence );
            }

            if ( blatAssociationsForSequence.size() == 0 ) continue;

            // Another important step: fill in the specificity, remove duplicates
            scoreResults( blatAssociationsForSequence );

            if ( log.isDebugEnabled() ) {
                log.debug( blatAssociationsForSequence.size() + " associations for " + sequence
                        + " after redundancy reduction" );
            }

            String queryName = sequence.getName();
            assert StringUtils.isNotBlank( queryName );
            if ( !allRes.containsKey( queryName ) ) {
                allRes.put( queryName, new HashSet<BlatAssociation>() );
            }

            allRes.get( queryName ).addAll( blatAssociationsForSequence );

        } // end of iteration over sequence

        // if ( log.isInfoEnabled() && skipped > 0 )
        // log.info( "Skipped " + skipped + "/" + blatResults.size()
        // + " individual blat results that didn't meet criteria" );

        return allRes;
    }

    /**
     * @param blatResult
     * @param par
     * @return
     */
    private BlatAssociation makeBlatAssociationWithPar( BlatResult blatResult, ProbeAlignedRegion par ) {
        BlatAssociation parAssociation = BlatAssociation.Factory.newInstance();
        parAssociation.setGeneProduct( par.getProducts().iterator().next() );
        parAssociation.setBlatResult( blatResult );
        parAssociation.setBioSequence( blatResult.getQuerySequence() );
        parAssociation.setOverlap( blatResult.getQuerySequence().getLength().intValue() ); // by definition, because
        // this is for a new PAR
        assert parAssociation.getBioSequence() != null;
        return parAssociation;
    }

    /**
     * @param blatResult
     * @return
     */
    private ProbeAlignedRegion makePar( BlatResult blatResult ) {
        ProbeAlignedRegion par = ProbeAlignedRegion.Factory.newInstance();
        PhysicalLocation pl = blatResultToPhysicalLocation( blatResult );
        par.setPhysicalLocation( pl );

        /*
         * We form a hopefully unique name of the PAR using the query sequence and the coordinates of the blat result
         */
        par.setName( blatResult.getQuerySequence().getName() + ".par." + blatResult.getTargetChromosome().getName()
                + "." + blatResult.getTargetStart() + "." + blatResult.getTargetEnd() );

        /*
         * The official name isn't really what we have, as we made it up...but it will do
         */
        par.setOfficialSymbol( par.getName() );

        par.setDescription( "Based only on BLAT alignment to genome, symbol is assigned by system" );
        par.setTaxon( blatResult.getQuerySequence().getTaxon() );

        GeneProduct pargp = GeneProduct.Factory.newInstance();
        pargp.setName( par.getName() );
        pargp.setDescription( "Hypothetical RNA product based on alignment of probe to genome sequence" );
        pargp.setType( GeneProductType.RNA );
        pargp.setGene( par );
        pargp.setPhysicalLocation( pl );

        par.getProducts().add( pargp );
        return par;
    }

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankId
     * @throws IOException
     */
    public Map<String, Collection<BlatAssociation>> processGbId( GoldenPathSequenceAnalysis goldenPathDb,
            String genbankId ) {

        log.debug( "Entering processGbId with " + genbankId );

        Collection<BlatResult> blatResults = goldenPathDb.findSequenceLocations( genbankId );

        if ( blatResults == null || blatResults.size() == 0 ) {
            log.warn( "No results obtained for " + genbankId );
        }

        return processBlatResults( goldenPathDb, blatResults );

    }

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankIds
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processGbIds( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<String[]> genbankIds ) {
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<String, Collection<BlatAssociation>>();
        int count = 0;
        int skipped = 0;
        for ( String[] genbankIdAr : genbankIds ) {

            if ( genbankIdAr == null || genbankIdAr.length == 0 ) {
                continue;
            }

            if ( genbankIdAr.length > 1 ) {
                throw new IllegalArgumentException( "Input file must have just one genbank identifier per line" );
            }

            String genbankId = genbankIdAr[0];

            Map<String, Collection<BlatAssociation>> res = processGbId( goldenPathDb, genbankId );
            allRes.putAll( res );

            count++;
            if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " genbank identifiers" );
        }
        log.info( "Annotations computed for " + count + " genbank identifiers" );
        if ( log.isInfoEnabled() && skipped > 0 )
            log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        return allRes;
    }

    /**
     * Get BlatAssociation results for a single sequence. If you have multiple sequences to run it is always better to
     * use processSequences();
     * 
     * @param goldenPath
     * @param sequence
     * @return
     * @see processSequences
     */
    public Collection<BlatAssociation> processSequence( GoldenPathSequenceAnalysis goldenPath, BioSequence sequence ) {

        Blat b = new Blat();
        b.setBlatScoreThreshold( blatScoreThreshold );
        Collection<BlatResult> results;
        try {
            results = b.blatQuery( sequence, goldenPath.getTaxon() );
        } catch ( IOException e ) {
            throw new RuntimeException( "Error running blat", e );
        }
        Map<String, Collection<BlatAssociation>> allRes = processBlatResults( goldenPath, results );
        assert allRes.keySet().size() == 1;
        return allRes.values().iterator().next();
    }

    /**
     * Given a collection of sequences, blat them against the selected genome.
     * 
     * @param output
     * @param goldenpath for the genome to be used.
     * @param sequences
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processSequences( GoldenPathSequenceAnalysis goldenpath,
            Collection<BioSequence> sequences ) {
        Blat b = new Blat();
        b.setBlatScoreThreshold( blatScoreThreshold );

        try {
            Map<BioSequence, Collection<BlatResult>> results = b.blatQuery( sequences, goldenpath.getTaxon() );
            Collection<BlatResult> blatres = new HashSet<BlatResult>();
            for ( Collection<BlatResult> coll : results.values() ) {
                blatres.addAll( coll );
            }
            Map<String, Collection<BlatAssociation>> allRes = processBlatResults( goldenpath, blatres );
            return allRes;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * From a collection of BlatAssociations from a single BioSequence, reduce redundancy, fill in the specificity and
     * score and pick the one with the best scoring statistics.
     * <p>
     * This is a little complicated because a single sequence can yield many BlatResults to the same gene and/or gene
     * product. We reduce the results down to a single (best) result for any given gene product. We also score
     * specificity by the gene: if a sequence 'hits' multiple genes, then the specificity of the generated associations
     * will be less than 1.
     * 
     * @param blatAssociations for a single sequence.
     * @return the highest-scoring result (if there are ties this will be a random one)
     * @throws IllegalArgumentException if the blatAssociations are from multiple biosequences.
     */
    public BlatAssociation scoreResults( Collection<BlatAssociation> blatAssociations ) {

        /*
         * Break results down by gene product, and throw out duplicates (only allow one result per gene product)
         */

        Map<GeneProduct, Collection<BlatAssociation>> geneProducts2Associations = organizeBlatAssociationsByGeneProduct( blatAssociations );

        BlatAssociation globalBest = removeExtraHitsPerGeneProduct( blatAssociations, geneProducts2Associations );

        Map<Gene, Collection<BlatAssociation>> genes2Associations = organizeBlatAssociationsByGene( blatAssociations );

        /*
         * At this point there should be just one blatAssociation per gene product. However, all of these really might
         * be for the same gene. It is only in the case of truly multiple genes that we flag a lower specificity.
         */
        if ( genes2Associations.size() == 1 ) {
            return globalBest;
        }

        Collection<Gene> distinctGenes = getDistinctGenes( genes2Associations );

        // TODO: adjust this to account for differences between scores.
        for ( Gene gene : distinctGenes ) {
            for ( BlatAssociation blatAssociation : genes2Associations.get( gene ) ) {
                blatAssociation.setSpecificity( 1.0 / distinctGenes.size() );
            }
        }
        return globalBest;
    }

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    public void setBlatScoreThreshold( double blatScoreThreshold ) {
        this.blatScoreThreshold = blatScoreThreshold;
    }

    /**
     * @param identityThreshold
     */
    public void setIdentityThreshold( double identityThreshold ) {
        this.identityThreshold = identityThreshold;

    }

    /**
     * @param scoreThreshold
     */
    public void setScoreThreshold( double scoreThreshold ) {
        this.scoreThreshold = scoreThreshold;

    }

    /**
     * @param blatAssociation
     * @return
     */
    private double computeScore( BlatAssociation blatAssociation ) {
        BlatResult br = blatAssociation.getBlatResult();

        assert br != null;

        double blatScore = br.score();
        double overlap = ( double ) blatAssociation.getOverlap() / ( double ) ( br.getQuerySequence().getLength() );
        double score = computeScore( blatScore, overlap );

        blatAssociation.setScore( score );
        return score;
    }

    /**
     * Are the genes really different?
     */
    private Collection<Gene> getDistinctGenes( Map<Gene, Collection<BlatAssociation>> associations ) {

        // sort them so we detect multiple genes easily.
        List<Gene> geneList = new ArrayList<Gene>();
        geneList.addAll( associations.keySet() );
        if ( associations.size() > 2 ) {
            sortGenes( geneList );
        }

        Collection<Gene> distinctGenes = new HashSet<Gene>();
        Gene lastGene = null;
        distinctGenes.add( geneList.get( 0 ) );
        for ( Gene gene : geneList ) {
            assert gene != null;
            if ( lastGene != null ) {

                // int overlap = SequenceManipulation.computeOverlap( gene.getPhysicalLocation(), lastGene
                // .getPhysicalLocation() );
                // int length = gene.getPhysicalLocation().getNucleotideLength();
                //
                // if ( log.isDebugEnabled() )
                // log.debug( "Overlap is " + overlap + "/" + length + " between " + gene + " and " + lastGene );

                // if ( gene.getOfficialSymbol().equals( lastGene.getOfficialSymbol() ) ) {
                // if ( overlap > 0 ) {
                // // same gene.
                // } else {
                // // rare case where symbols are the same but not the same gene.
                // distinctGenes.add( gene );
                // }
                // } else {
                // // definitely not the same gene.
                // distinctGenes.add( gene );
                // }

                if ( gene.equals( lastGene ) ) {
                    log.debug( "" );
                    log.debug( gene + " is equal to " + lastGene );
                } else {
                    distinctGenes.add( gene );
                    log.debug( gene + " is not equal to " + lastGene );
                }

            }
            lastGene = gene;
        }

        if ( log.isDebugEnabled() ) log.debug( distinctGenes.size() + " genes." );
        return distinctGenes;
    }

    /**
     * @param blatAssociations
     * @return
     */
    private Map<Gene, Collection<BlatAssociation>> organizeBlatAssociationsByGene(
            Collection<BlatAssociation> blatAssociations ) {

        Map<Gene, Collection<BlatAssociation>> genes = new HashMap<Gene, Collection<BlatAssociation>>();
        for ( BlatAssociation blatAssociation : blatAssociations ) {
            Gene gene = blatAssociation.getGeneProduct().getGene();
            assert gene != null;
            if ( !genes.containsKey( gene ) ) {
                genes.put( gene, new HashSet<BlatAssociation>() );
            }
            genes.get( gene ).add( blatAssociation );
        }
        return genes;
    }

    /**
     * @param blatAssociations
     * @param geneProducts
     * @return
     */
    private Map<GeneProduct, Collection<BlatAssociation>> organizeBlatAssociationsByGeneProduct(
            Collection<BlatAssociation> blatAssociations ) {
        Map<GeneProduct, Collection<BlatAssociation>> geneProducts = new HashMap<GeneProduct, Collection<BlatAssociation>>();
        Collection<BioSequence> sequences = new HashSet<BioSequence>();
        for ( BlatAssociation blatAssociation : blatAssociations ) {
            assert blatAssociation.getBioSequence() != null;
            computeScore( blatAssociation );
            sequences.add( blatAssociation.getBioSequence() );

            if ( sequences.size() > 1 ) {
                throw new IllegalArgumentException( "Blat associations must all be for the same query sequence" );
            }

            assert blatAssociation.getGeneProduct() != null;
            GeneProduct geneProduct = blatAssociation.getGeneProduct();
            if ( !geneProducts.containsKey( geneProduct ) ) {
                geneProducts.put( geneProduct, new HashSet<BlatAssociation>() );
            }
            geneProducts.get( geneProduct ).add( blatAssociation );

            blatAssociation.setSpecificity( 1.0 );
        }

        return geneProducts;
    }

    /**
     * Process a single BlatResult.
     * 
     * @param goldenPathDb
     * @param blatResult
     * @param ignoreStrand If true, the strand is not used to evalute alignments.
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<BlatAssociation> processBlatResult( GoldenPathSequenceAnalysis goldenPathDb,
            BlatResult blatResult, boolean ignoreStrand ) {
        assert blatResult.getTargetChromosome() != null : "Chromosome not filled in for blat result";

        String strand = ignoreStrand == true ? null : blatResult.getStrand();

        Collection<BlatAssociation> blatAssociations = goldenPathDb.findAssociations( blatResult.getTargetChromosome()
                .getName(), blatResult.getTargetStart(), blatResult.getTargetEnd(), blatResult.getTargetStarts(),
                blatResult.getBlockSizes(), strand, threeprimeMethod );

        if ( blatAssociations != null && blatAssociations.size() > 0 ) {
            for ( BlatAssociation association : blatAssociations ) {
                association.setBlatResult( blatResult );
                association.setBioSequence( blatResult.getQuerySequence() );
            }
            return blatAssociations;
        }

        // no genes, have to look for pre-existing probealignedregions that overlap.
        if ( blatResult.getQuerySequence().getTaxon() == null )
            blatResult.getQuerySequence().setTaxon( goldenPathDb.getTaxon() );
        return findProbeAlignedRegionAssociations( blatResult, ignoreStrand );

    }

    /**
     * Identify ProbeAlignedRegions that overlap with the given blat result. A ProbeAlignedRegion is a region in which
     * there are no known or predicted genes, but a designElement aligned to.
     * 
     * @param blatResult
     * @param ignoreStrand
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<BlatAssociation> findProbeAlignedRegionAssociations( BlatResult blatResult, boolean ignoreStrand ) {

        PhysicalLocation pl = makePhysicalLocation( blatResult, ignoreStrand );

        Collection<ProbeAlignedRegion> pars = probeAlignedRegionService.findAssociations( pl );
        if ( log.isDebugEnabled() && pars.size() > 0 ) log.debug( "Found " + pars.size() + " PARS for " + blatResult );
        Collection<BlatAssociation> results = new HashSet<BlatAssociation>();
        for ( ProbeAlignedRegion region : pars ) {
            BlatAssociation ba = BlatAssociation.Factory.newInstance();
            GeneProduct product = region.getProducts().iterator().next();
            assert product.getId() != null;
            ba.setGeneProduct( product );
            ba.setBlatResult( blatResult );
            ba.setBioSequence( blatResult.getQuerySequence() );
            ba.setOverlap( SequenceManipulation.getGeneProductExonOverlap( blatResult.getTargetStarts(), blatResult
                    .getBlockSizes(), pl.getStrand(), product ) );
            results.add( ba );

        }
        return results;
    }

    /**
     * Turn the blat result into a physical location
     * 
     * @param blatResult
     * @param ignoreStrand
     * @return
     */
    private PhysicalLocation makePhysicalLocation( BlatResult blatResult, boolean ignoreStrand ) {
        PhysicalLocation pl = blatResultToPhysicalLocation( blatResult );
        if ( ignoreStrand ) {
            pl.setStrand( null );
        }
        return pl;
    }

    /**
     * @param blatResult
     * @return
     */
    private PhysicalLocation blatResultToPhysicalLocation( BlatResult blatResult ) {
        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        Chromosome chrom = blatResult.getTargetChromosome();
        Taxon taxon = blatResult.getQuerySequence().getTaxon();
        if ( taxon.getId() == null ) {
            taxon = taxonService.findOrCreate( taxon ); // usually only during tests.
        }

        assert taxon.getId() != null;

        chrom.setTaxon( taxon );
        if ( chrom.getId() == null ) {
            Chromosome foundChrom = chromosomeService.find( chrom );
            if ( foundChrom == null )
                chrom = chromosomeService.findOrCreate( chrom ); // typically only during tests.
            else
                chrom = foundChrom;
        }

        assert chrom.getId() != null;

        pl.setChromosome( chrom );
        pl.setNucleotide( blatResult.getTargetStart() );
        pl.setNucleotideLength( ( new Long( blatResult.getTargetEnd() - blatResult.getTargetStart() ) ).intValue() );
        pl.setStrand( blatResult.getStrand() );
        return pl;
    }

    /**
     * Now go over and compute scores and find the best one, for each gene product, removing all other hits (so there is
     * just one per gene product
     */
    private BlatAssociation removeExtraHitsPerGeneProduct( Collection<BlatAssociation> blatAssociations,
            Map<GeneProduct, Collection<BlatAssociation>> geneProduct2BlatAssociations ) {

        double globalMaxScore = 0.0;
        BlatAssociation globalBest = null;
        for ( GeneProduct geneProduct : geneProduct2BlatAssociations.keySet() ) {
            Collection<BlatAssociation> geneProductBlatAssociations = geneProduct2BlatAssociations.get( geneProduct );

            double maxScore = 0.0;
            BlatAssociation best = null;

            // Find the best one. If there are ties it's arbitrary which one we pick.
            for ( BlatAssociation blatAssociation : geneProductBlatAssociations ) {
                double score = blatAssociation.getScore();
                if ( score >= maxScore ) {
                    maxScore = score;
                    best = blatAssociation;
                }
            }

            assert best != null;

            // Remove the lower-scoring ones for this gene product
            Collection<BlatAssociation> toRemove = new HashSet<BlatAssociation>();
            for ( BlatAssociation blatAssociation : geneProductBlatAssociations ) {
                if ( blatAssociation != best ) {
                    toRemove.add( blatAssociation );
                    if ( log.isDebugEnabled() ) log.debug( "Removing " + blatAssociation );
                }
            }

            assert toRemove.size() < geneProductBlatAssociations.size();

            for ( BlatAssociation association : toRemove ) {
                blatAssociations.remove( association );
            }

            if ( best.getScore() > globalMaxScore ) {
                globalMaxScore = best.getScore();
                globalBest = best;
            }

        }
        return globalBest;
    }

    /**
     * @param geneList
     */
    private void sortGenes( List<Gene> geneList ) {
        Collections.sort( geneList, new Comparator<Gene>() {
            public int compare( Gene arg0, Gene arg1 ) {
                return arg0.getOfficialSymbol().compareTo( arg1.getOfficialSymbol() );
            }
        } );

    }

    /**
     * Compute a score we use to quantify the quality of a hit to a GeneProduct.
     * <p>
     * There are two criteria being considered: the quality of the alignment, and the amount of overlap.
     * 
     * @param blatScore A value from 0-1 indicating alignment quality.
     * @param overlap A value from 0-1 indicating how much of the alignment overlaps the GeneProduct being considered.
     * @return
     */
    protected int computeScore( double blatScore, double overlap ) {
        return ( int ) ( 1000 * blatScore * overlap );
    }

    /**
     * FIXME not used as is. Compute a score to quantify the specificity of a hit to a Gene (not a GeneProduct!). A
     * value between 0 and 1.
     * <p>
     * The criteria considered are: the number of equal or better hits, and the difference between this hit and the next
     * worst hit.
     * <p>
     * If there are n identical or better hits (including this one), the specificity is 1/n.
     * <p>
     * If this is the best hit, then the specificity is (scoremax - nextscore)/scoremax.
     * 
     * @param scores A list in decreasing order. If it is not sorted you won't get the right results!
     * @param score
     * @return
     */
    protected Double computeSpecificity( List<Double> scores, double score ) {

        if ( scores.size() == 1 ) {
            return 1.0;
        }

        // algorithm: compute the number of scores which are equal or higher than this one.
        int numBetter = 0;
        int i = 0;
        double nextBest = 0.0;
        double total = 0.0;
        for ( Double s : scores ) {

            total += s;

            if ( s >= score ) {
                numBetter++; // this is guaranteed to be at least one
            }
            if ( s < score ) {
                nextBest = s;
                break;
            }
            i++;
        }

        if ( numBetter > 1 ) {
            return 1.0 / numBetter;
        }

        return ( score - nextBest ) / score;

    }

    public void setProbeAlignedRegionService( ProbeAlignedRegionService probeAlignedRegionService ) {
        this.probeAlignedRegionService = probeAlignedRegionService;
    }

    public void setChromosomeService( ChromosomeService chromosomeService ) {
        this.chromosomeService = chromosomeService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }
}
