/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.analysis.sequence;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.*;

/**
 * Given a set of BlatAssociations that might be redundant, clean them up and score them.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class BlatAssociationScorer {

    private static final Log log = LogFactory.getLog( BlatAssociationScorer.class.getName() );

    /**
     * Compute how much the BLAT alignment with the target gene product is as a fraction of the query sequence length.
     * Assumes that the overlap with a transcript has already been computed.
     *
     * @param blatAssociation blat assoc
     * @return double
     */
    public static double computeOverlapFraction( BlatAssociation blatAssociation ) {
        return ( double ) blatAssociation.getOverlap() / ( double ) blatAssociation.getBlatResult().getQuerySequence()
                .getLength();
    }

    /**
     * From a collection of BlatAssociations from a single BioSequence, reduce redundancy, fill in the specificity and
     * score and pick the one with the best scoring statistics.
     * This is a little complicated because a single sequence can yield many BlatResults to the same gene and/or gene
     * product. We reduce the results down to a single (best) result for any given gene product. We also score
     * specificity by the gene: if a sequence 'hits' multiple genes, then the specificity of the generated associations
     * will be less than 1.
     *
     * @param blatAssociations for a single sequence.
     * @return the highest-scoring result (if there are ties this will be a random one). Note that this return value is
     * not all that useful because it assumes there is a "clear winner". The passed-in blatAssociations will be
     * pruned to remove redundant entries, and will have score information filled in as well. It is intended
     * that these 'refined' BlatAssociations will be used in further analysis.
     * @throws IllegalArgumentException if the blatAssociations are from multiple biosequences.
     */
    public static BlatAssociation scoreResults( Collection<BlatAssociation> blatAssociations ) {

        Map<GeneProduct, List<BlatAssociation>> geneProducts2Associations = BlatAssociationScorer
                .organizeBlatAssociationsByGeneProductAndInitializeScores( blatAssociations );

        BlatAssociation globalBest = BlatAssociationScorer
                .removeExtraHitsPerGeneProduct( blatAssociations, geneProducts2Associations );

        assert !blatAssociations.isEmpty();

        Map<Gene, List<BlatAssociation>> genes2Associations = BlatAssociationScorer
                .organizeBlatAssociationsByGene( blatAssociations );

        assert !genes2Associations.isEmpty();

        /*
         * At this point there should be just one blatAssociation per gene product. However, all of these really might
         * be for the same gene. It is only in the case of truly multiple genes that we flag a lower specificity.
         */
        if ( genes2Associations.size() == 1 ) {
            return globalBest;
        }

        Map<PhysicalLocation, Collection<Gene>> geneClusters = BlatAssociationScorer.clusterGenes( genes2Associations );

        // compute specificity at the level of genes. First, get the best score for each gene cluster.
        Map<PhysicalLocation, Double> scores = new HashMap<>();
        for ( PhysicalLocation pl : geneClusters.keySet() ) {
            Double geneScore = 0.0;

            for ( Gene cgene : geneClusters.get( pl ) ) {
                for ( BlatAssociation blatAssociation : genes2Associations.get( cgene ) ) {
                    Double alignScore = blatAssociation.getScore();
                    if ( alignScore > geneScore ) {
                        geneScore = alignScore;
                    }
                }
            }
            scores.put( pl, geneScore );
        }

        for ( PhysicalLocation pl : geneClusters.keySet() ) {

            Double alignScore = scores.get( pl );

            for ( Gene cgene : geneClusters.get( pl ) ) {
                // All members of the cluster get the same specificity.
                for ( BlatAssociation blatAssociation : genes2Associations.get( cgene ) ) {
                    blatAssociation
                            .setSpecificity( BlatAssociationScorer.computeSpecificity( scores.values(), alignScore ) );
                }
            }
        }

        return globalBest;
    }

    /**
     * Compute a score we use to quantify the quality of a hit to a GeneProduct.
     * There are two criteria being considered: the quality of the alignment, and the amount of overlap.
     *
     * @param blatScore A value from 0-1 indicating alignment quality.
     * @param overlap   A value from 0-1 indicating how much of the alignment overlaps the GeneProduct being considered.
     * @return int
     */
    private static int computeScore( double blatScore, double overlap ) {
        return ( int ) ( 1000 * blatScore * overlap );
    }

    /**
     * Compute a score to quantify the specificity of a hit to a Gene (not a GeneProduct!). A value between 0 and 1.
     * The specificity is estimated as the fraction of the signal we expect to come from a given gene. The amount of
     * signal for each gene is estimated using the score for that gene. The total signal is the sum of scores.
     * FIXME this assumes a linear relationship between score and hybridization signal, which is unrealistic. Because
     * the scores for our hits are already filtered, this method gives results fairly close to 1/N.
     * FIXME This is not actually used to filter BLAT results (and there's no suggestion we need to). The value gets set
     * into BlatAssociation.specificity but this is never read. We should either use it or lose it.
     *
     * @param scores r of all the genes for the hit in question.
     * @param score  score
     * @return double
     */
    static Double computeSpecificity( Collection<Double> scores, double score ) {
        if ( scores.size() == 1 ) {
            return 1.0;
        }
        double total = 0.0;
        for ( Double s : scores ) {
            total += s;
        }
        if ( total == 0.0 ) {
            return 0.0;
        }
        return score / total;
    }

    /**
     * @param associations assocs.
     * @return map of physical locations for the alignments, and which genes are found there.
     */
    private static Map<PhysicalLocation, Collection<Gene>> clusterGenes(
            Map<Gene, List<BlatAssociation>> associations ) {

        Map<PhysicalLocation, Collection<Gene>> clusters = new HashMap<>();

        for ( Gene gene : associations.keySet() ) {

            List<BlatAssociation> geneAssoc = associations.get( gene );

            for ( BlatAssociation ba : geneAssoc ) {
                PhysicalLocation pl = ba.getBlatResult().getTargetAlignedRegion();
                if ( !clusters.containsKey( pl ) ) {
                    clusters.put( pl, new HashSet<>() );
                }
                clusters.get( pl ).add( gene );
            }
        }

        // debugging information about clusters.
        if ( BlatAssociationScorer.log.isDebugEnabled() ) {
            for ( PhysicalLocation pl : clusters.keySet() ) {
                if ( clusters.get( pl ).size() > 1 ) {
                    BlatAssociationScorer.log
                            .debug( "Cluster at " + pl + " with " + clusters.get( pl ).size() + " members:\n"
                                    + StringUtils.join( clusters.get( pl ).iterator(), "\n" ) );
                }
            }
        }

        return clusters;
    }

    private static void computeScore( BlatAssociation blatAssociation ) {
        BlatResult br = blatAssociation.getBlatResult();

        assert br.getQuerySequence().getLength() > 0;

        double blatScore = score( br );
        double overlap = BlatAssociationScorer.computeOverlapFraction( blatAssociation );
        double score = BlatAssociationScorer.computeScore( blatScore, overlap );

        blatAssociation.setScore( score );
    }

    private static Map<Gene, List<BlatAssociation>> organizeBlatAssociationsByGene(
            Collection<BlatAssociation> blatAssociations ) {
        Map<Gene, List<BlatAssociation>> genes = new HashMap<>();
        for ( BlatAssociation blatAssociation : blatAssociations ) {
            Gene gene = blatAssociation.getGeneProduct().getGene();
            if ( !genes.containsKey( gene ) ) {
                genes.put( gene, new ArrayList<>() );
            }
            genes.get( gene ).add( blatAssociation );
        }
        return genes;
    }

    /**
     * Break results down by gene product, and throw out duplicates (only allow one result per gene product), fills in
     * score and initializes specificity
     *
     * @param blatAssociations blat assocs
     * @return map
     */
    private static Map<GeneProduct, List<BlatAssociation>> organizeBlatAssociationsByGeneProductAndInitializeScores(
            Collection<BlatAssociation> blatAssociations ) {
        Map<GeneProduct, List<BlatAssociation>> geneProducts = new HashMap<>();
        Collection<BioSequence> sequences = new HashSet<>();

        for ( BlatAssociation blatAssociation : blatAssociations ) {
            assert blatAssociation.getBioSequence() != null;

            BlatAssociationScorer.computeScore( blatAssociation );
            sequences.add( blatAssociation.getBioSequence() );

            if ( sequences.size() > 1 ) {
                throw new IllegalArgumentException( "Blat associations must all be for the same query sequence" );
            }

            assert blatAssociation.getGeneProduct() != null;
            GeneProduct geneProduct = blatAssociation.getGeneProduct();
            if ( !geneProducts.containsKey( geneProduct ) ) {
                geneProducts.put( geneProduct, new ArrayList<>() );
            }
            geneProducts.get( geneProduct ).add( blatAssociation );

            blatAssociation.setSpecificity( 1.0 ); // an initial value.
        }

        return geneProducts;
    }

    /**
     * Compute scores and find the best one, for each gene product, removing all other hits (so there is just one per
     * gene product
     *
     * @param blatAssociations             blat assocs.
     * @param geneProduct2BlatAssociations gene prods 2 blat assocs
     * @return blat accoc
     */
    private static BlatAssociation removeExtraHitsPerGeneProduct( Collection<BlatAssociation> blatAssociations,
            Map<GeneProduct, List<BlatAssociation>> geneProduct2BlatAssociations ) {

        double globalMaxScore = 0.0;
        BlatAssociation globalBest = null;
        List<BlatAssociation> keepers = new ArrayList<>();

        for ( GeneProduct geneProduct : geneProduct2BlatAssociations.keySet() ) {
            List<BlatAssociation> geneProductBlatAssociations = geneProduct2BlatAssociations.get( geneProduct );

            if ( geneProductBlatAssociations.isEmpty() )
                continue;

            BlatAssociation ba = geneProductBlatAssociations.iterator().next();

            // Find the best one. If there are ties it's arbitrary which one we pick.
            double maxScore = ba.getScore();
            BlatAssociation best = ba;
            for ( BlatAssociation blatAssociation : geneProductBlatAssociations ) {
                double score = blatAssociation.getScore();
                if ( score >= maxScore ) {
                    maxScore = score;
                    best = blatAssociation;
                }
            }

            // Remove the lower-scoring ones for this gene product
            List<BlatAssociation> toKeep = new ArrayList<>();
            toKeep.add( best );
            keepers.add( best );
            geneProduct2BlatAssociations.put( geneProduct, toKeep );

            if ( best.getScore() > globalMaxScore ) {
                globalMaxScore = best.getScore();
                globalBest = best;
            }

        }
        blatAssociations.retainAll( keepers );
        return globalBest;
    }

    /**
     * Based on the JKSrc method in psl.c, but without double-penalizing for mismatches. We also consider repeat matches
     * to be the same as regular matches.
     *
     * @return Value between 0 and 1, representing the fraction of matches, minus a gap penalty.
     * @param blatResult
     */
    public static Double score( BlatResult blatResult ) {

        long length;
        if ( blatResult.getQuerySequence() == null ) {
            throw new IllegalArgumentException( "Sequence cannot be null" );
        }

        if ( blatResult.getQuerySequence().getLength() != null && blatResult.getQuerySequence().getLength() != 0 ) {
            length = blatResult.getQuerySequence().getLength();
        } else {
            if ( StringUtils.isNotBlank( blatResult.getQuerySequence().getSequence() ) ) {
                length = blatResult.getQuerySequence().getSequence().length();
            } else {
                throw new IllegalArgumentException(
                        "Sequence is missing; cannot compute score for " + blatResult.getQuerySequence() );
            }
        }

        assert length > 0;

        // Note: we count repeat matches just like regular matches.
        long matches = blatResult.getMatches() + blatResult.getRepMatches();

        /*
         * This might happen if the sequence in our system was polyA/T trimmed, which we don't do any more, but there
         * could be remnants. When blat results come back from goldenpath (rather than computed by us) the lengths can
         * disagree. Other reasons for this unclear.
         */
        if ( matches > length ) {
            BlatAssociationScorer.log.warn( "Blat result for " + blatResult.getQuerySequence() + " More matches than sequence length: " + blatResult
                    .getMatches() + " match + " + blatResult.getRepMatches() + " repMatch = " + matches + " > " + length );
            matches = length;
        }

        /*
         * return sizeMul (psl->match + ( psl->repMatch>>1)) - sizeMul psl->misMatch - psl->qNumInsert -
         * psl->tNumInsert; Note that: "Currently the program does not distinguish between matches and repMatches.
         * repMatches is always zero." (http://genome.ucsc.edu/goldenPath/help/blatSpec.html)
         */
        double score = ( double ) ( matches - blatResult.getQueryGapCount() - blatResult.getTargetGapCount() ) / ( double ) length;

        // because of repeat matches, score _can_ be negative in some situations (typically, lots of gaps).
        if ( score < 0.0 && blatResult.getRepMatches() == 0 ) {
            throw new IllegalStateException(
                    "Score was " + score + "; matches=" + matches + " repMatches=" + blatResult.getRepMatches()
                            + " queryGaps=" + blatResult.getQueryGapCount() + " targetGaps=" + blatResult.getTargetGapCount()
                            + " length=" + length + " sequence=" + blatResult.getQuerySequence() + " id=" + blatResult.getId() );
        }

        assert score >= 0.0 && score <= 1.0 :
                "Score was " + score + "; matches=" + matches + " queryGaps=" + blatResult.getQueryGapCount() + " targetGaps="
                        + blatResult.getTargetGapCount() + " length=" + length + " sequence=" + blatResult.getQuerySequence()
                        + " id=" + blatResult.getId();

        return score;
    }

    /**
     * Fraction identity computation, as in psl.c. Modified to INCLUDE repeat matches in the match count.
     * See <a href="http://genome.ucsc.edu/FAQ/FAQblat#blat4">Blat4 at UCSC</a>.
     *
     * @return Value between 0 and 1.
     * @param blatResult
     */
    public static Double identity( BlatResult blatResult ) {
        int sizeMul = 1; // assuming DNA; use 3 for protein.
        long qAliSize = sizeMul * ( blatResult.getQueryEnd() - blatResult.getQueryStart() );
        long tAliSize = blatResult.getTargetEnd() - blatResult.getTargetStart();
        long aliSize = Math.min( qAliSize, tAliSize );

        if ( aliSize <= 0 )
            return 0.0;

        long sizeDif = qAliSize - tAliSize;
        if ( sizeDif < 0 ) {
            sizeDif = 0; // here assuming "isMRna" is true. ("The parameter isMRna should be set to TRUE, regardless
            // of whether the input sequence is mRNA or protein")
        }
        int insertFactor = blatResult.getQueryGapCount(); // assumes isMRna is true.
        int total = ( sizeMul * ( blatResult.getMatches() + blatResult.getRepMatches() + blatResult.getMismatches() ) );
        int milliBad = 0;
        if ( total != 0 ) {
            milliBad = ( 1000 * ( blatResult.getMismatches() * sizeMul + insertFactor + ( int ) Math
                    .round( 3.0 * Math.log( 1.0 + sizeDif ) ) ) ) / total;
        }
        assert milliBad >= 0 && milliBad <= 1000 :
                "MilliBad was outside of range 0-1000: " + milliBad + " for result " + blatResult;
        return ( 100.0 - milliBad * 0.1 ) / 100.0;
    }
}
