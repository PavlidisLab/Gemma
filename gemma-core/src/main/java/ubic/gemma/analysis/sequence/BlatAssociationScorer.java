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
package ubic.gemma.analysis.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * Given a set of BlatAssociations that might be redundant, clean them up and score them.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatAssociationScorer {

    private static Log log = LogFactory.getLog( BlatAssociationScorer.class.getName() );

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
     * @return the highest-scoring result (if there are ties this will be a random one). Note that this return value is
     *         not all that useful because it assumes there is a "clear winner". The passed-in blatAssociations will be
     *         pruned to remove redundant entires, and will have score information filled in as well. It is intended
     *         that these 'refined' BlatAssociations will be used in further analysis.
     * @throws IllegalArgumentException if the blatAssociations are from multiple biosequences.
     */
    public static BlatAssociation scoreResults( Collection<BlatAssociation> blatAssociations ) {

        Map<GeneProduct, Collection<BlatAssociation>> geneProducts2Associations = organizeBlatAssociationsByGeneProduct( blatAssociations );

        BlatAssociation globalBest = removeExtraHitsPerGeneProduct( blatAssociations, geneProducts2Associations );

        assert blatAssociations.size() > 0;

        Map<Gene, Collection<BlatAssociation>> genes2Associations = organizeBlatAssociationsByGene( blatAssociations );

        assert genes2Associations.size() > 0;

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
     * @param blatAssociation
     * @return
     */
    private static double computeScore( BlatAssociation blatAssociation ) {
        BlatResult br = blatAssociation.getBlatResult();

        assert br != null;

        double blatScore = br.score();
        double overlap = ( double ) blatAssociation.getOverlap() / ( double ) ( br.getQuerySequence().getLength() );
        double score = computeScore( blatScore, overlap );

        blatAssociation.setScore( score );
        return score;
    }

    /**
     * Are the genes _really_ different?
     */
    private static Collection<Gene> getDistinctGenes( Map<Gene, Collection<BlatAssociation>> associations ) {

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
    private static Map<Gene, Collection<BlatAssociation>> organizeBlatAssociationsByGene(
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
     * Break results down by gene product, and throw out duplicates (only allow one result per gene product), fills in
     * score and initializes specificity.
     * 
     * @param blatAssociations
     * @param geneProducts
     * @return
     */
    private static Map<GeneProduct, Collection<BlatAssociation>> organizeBlatAssociationsByGeneProduct(
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

            blatAssociation.setSpecificity( 1.0 ); // an initial value.
        }

        return geneProducts;
    }

    /**
     * Compute scores and find the best one, for each gene product, removing all other hits (so there is just one per
     * gene product
     * 
     * @param blatAssociations
     * @param geneProduct2BlatAssociations
     * @return
     */
    private static BlatAssociation removeExtraHitsPerGeneProduct( Collection<BlatAssociation> blatAssociations,
            Map<GeneProduct, Collection<BlatAssociation>> geneProduct2BlatAssociations ) {

        double globalMaxScore = 0.0;
        BlatAssociation globalBest = null;
        Collection<BlatAssociation> keepers = new HashSet<BlatAssociation>();
        for ( GeneProduct geneProduct : geneProduct2BlatAssociations.keySet() ) {
            Collection<BlatAssociation> geneProductBlatAssociations = geneProduct2BlatAssociations.get( geneProduct );

            if ( geneProductBlatAssociations.size() == 1 ) {
                keepers = geneProductBlatAssociations;
                continue;
            }

            // Find the best one. If there are ties it's arbitrary which one we pick.
            double maxScore = 0.0;
            BlatAssociation best = null;
            for ( BlatAssociation blatAssociation : geneProductBlatAssociations ) {
                double score = blatAssociation.getScore();
                if ( score >= maxScore ) {
                    maxScore = score;
                    best = blatAssociation;
                }
            }

            assert best != null;

            // Remove the lower-scoring ones for this gene product
            Collection<BlatAssociation> toKeep = new HashSet<BlatAssociation>();
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
     * @param geneList
     */
    private static void sortGenes( List<Gene> geneList ) {
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
    protected static int computeScore( double blatScore, double overlap ) {
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
    protected static Double computeSpecificity( List<Double> scores, double score ) {

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

}
