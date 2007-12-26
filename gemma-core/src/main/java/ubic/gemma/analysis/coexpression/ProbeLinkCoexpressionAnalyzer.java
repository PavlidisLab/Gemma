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
package ubic.gemma.analysis.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Perform probe-to-probe coexpression link analysis ("TMM-style").
 * 
 * @spring.bean id="probeLinkCoexpressionAnalyzer"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @author paul
 * @version $Id$
 */
public class ProbeLinkCoexpressionAnalyzer {
    private static Log log = LogFactory.getLog( ProbeLinkCoexpressionAnalyzer.class.getName() );
    CompositeSequenceService compositeSequenceService;
    GeneService geneService;

    /**
     * Call this!
     * <p>
     * Do not attempt to call the method GeneDao.getCoexpressedGenes() directly, the results that are returned will not
     * be correctly initialized.
     * 
     * @param gene
     * @param ees
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.coexpression.CoexpressionCollectionValueObject
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    public CoexpressionCollectionValueObject linkAnalysis( Gene gene, Collection<ExpressionExperiment> ees,
            int stringency ) {

        if ( stringency <= 0 ) stringency = 1;

        // DB query with minimal postprocessing
        CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                .getCoexpressedGenes( gene, ees, stringency );

        postProcess( coexpressions );

        return coexpressions;
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param allSpecificEE Map of genes to EEs that had at least one specific probe for that gene.
     * @param querySpecificEEs
     * @param coExValObj
     * @return ids of expression experiments that had only non-specific probes to the given gene. An ee is considered
     *         specific iff the ee is specific for the query gene and the target gene.
     */
    private Collection<Long> fillInNonspecificEEs( Map<Long, Collection<Long>> allSpecificEE,
            Collection<Long> querySpecificEEs, CoexpressionValueObject coExValObj ) {

        Collection<Long> result = new HashSet<Long>();
        for ( Long eeId : coExValObj.getExpressionExperiments() ) {
            if ( allSpecificEE.get( coExValObj.getGeneId() ).contains( eeId ) && querySpecificEEs.contains( eeId ) ) {
                // then it is specific for this gene pair.
                continue;
            }
            result.add( eeId );
        }

        coExValObj.setNonspecificEE( result );
        return result;
    }

    /**
     * Counting up how many support-threshold exceeding links each data set contributed.
     * 
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementEEContributions( Collection<Long> contributingEEs, CoexpressedGenesDetails coexpressions ) {

        for ( Long eeID : contributingEEs ) {
            ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( eeID );

            assert eeVo != null;

            if ( eeVo.getCoexpressionLinkCount() == null ) {
                eeVo.setCoexpressionLinkCount( new Long( 1 ) );
            } else {
                eeVo.setCoexpressionLinkCount( eeVo.getCoexpressionLinkCount() + 1 );
            }

        }
    }

    /**
     * Counting up how many links each data set contributed (including links that did not meet the stringency
     * threshold).
     * 
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementRawEEContributions( Collection<Long> contributingEEs, CoexpressedGenesDetails coexpressions ) {
        for ( Long eeID : contributingEEs ) {

            ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( eeID );

            assert eeVo != null;

            if ( eeVo.getRawCoexpressionLinkCount() == null ) {
                eeVo.setRawCoexpressionLinkCount( new Long( 1 ) );
            } else {
                eeVo.setRawCoexpressionLinkCount( eeVo.getRawCoexpressionLinkCount() + 1 );
            }
        }
    }

    /**
     * @param coexpressions
     * @param coexpressedGenes map of gene ids to CoexpressionValueObjects holding
     * @param coexpressionDetails
     */
    private void postProcess( CoexpressionCollectionValueObject coexpressions,
            CoexpressedGenesDetails coexpressionDetails ) {
        Map<Long, Collection<Long>> allSpecificEE = coexpressionDetails.getSpecificExpressionExperiments();

        Collection<Long> querySpecificEEs = coexpressions.getQueryGeneSpecificExpressionExperiments();

        Long queryGeneId = coexpressions.getQueryGene().getId();

        int positiveLinkCount = 0;
        int negativeLinkCount = 0;

        for ( CoexpressionValueObject coExValObj : coexpressionDetails.getCoexpressionData() ) {
            coExValObj.computeExperimentBits( new ArrayList<Long>( coexpressionDetails.getExpressionExperimentIds() ) );

            Collection<Long> nonspecificEE = fillInNonspecificEEs( allSpecificEE, querySpecificEEs, coExValObj );

            // Fill in information about the other genes these probes hybridize to, if any.
            for ( Long eeID : nonspecificEE ) {
                Collection<Long> probes = coExValObj.getProbes( eeID ); // just for that ee.
                for ( Long probeID : probes ) {
                    if ( coexpressionDetails.getNonSpecificGenes( eeID, probeID ) != null ) {
                        for ( Long geneID : coexpressionDetails.getNonSpecificGenes( eeID, probeID ) ) {

                            // FIXME this is probably wrong.
                            coExValObj.addNonSpecificGene( coExValObj.getGeneName() ); // FIXME

                            if ( geneID.equals( queryGeneId ) ) {
                                coExValObj.setHybridizesWithQueryGene( true );
                            }
                        }
                    }
                }

            }

            // FIXME This is some kind of internal issue for the coexvalobj and should not be here?
            coExValObj.getNonSpecificGenes().remove( coExValObj.getGeneOfficialName() );

            boolean added = false;

            incrementRawEEContributions( coExValObj.getExpressionExperiments(), coexpressionDetails );

            if ( coExValObj.getPositiveLinkSupport() != 0 ) {
                positiveLinkCount++;
                added = true;
                // add in coexpressions that match stringency
                coexpressionDetails.add( coExValObj );
                // add in expression experiments that match stringency
                // update the link count for that EE
                incrementEEContributions( coExValObj.getEEContributing2PositiveLinks(), coexpressionDetails );
            }

            if ( coExValObj.getNegativeLinkSupport() != 0 ) {
                negativeLinkCount++;
                // add in expression experiments that match stringency
                // update the link count for that EE
                incrementEEContributions( coExValObj.getEEContributing2NegativeLinks(), coexpressionDetails );

                if ( added ) continue; // no point in adding or counting the same element twice
                coexpressionDetails.add( coExValObj );
            }
        }

        // add count of pruned matches to coexpression data
        coexpressionDetails.setPositiveStringencyLinkCount( positiveLinkCount );
        coexpressionDetails.setNegativeStringencyLinkCount( negativeLinkCount );
    }

    /**
     * @param coexpressions
     */
    @SuppressWarnings("unchecked")
    private void postProcess( CoexpressionCollectionValueObject coexpressions ) {
        StopWatch watch = new StopWatch();
        watch.start();

        postProcessKnownGenes( coexpressions );
        postProcessProbeAlignedRegions( coexpressions );
        postProcessPredictedGenes( coexpressions );

        watch.stop();
        Long elapsed = watch.getTime();
        coexpressions.setPostProcessTime( elapsed );
        if ( elapsed > 1000 ) log.info( "Done postprocessing in " + elapsed + "ms." );
    }

    /**
     * @param coexpressions
     */
    @SuppressWarnings("unchecked")
    private void postProcessKnownGenes( CoexpressionCollectionValueObject coexpressions ) {
        CoexpressedGenesDetails knownGeneCoexpression = coexpressions.getKnownGeneCoexpression();
        postProcess( coexpressions, knownGeneCoexpression );
    }

    /**
     * @param coexpressions
     */
    private void postProcessPredictedGenes( CoexpressionCollectionValueObject coexpressions ) {
        CoexpressedGenesDetails predictedCoexpressionType = coexpressions.getPredictedCoexpressionType();
        postProcess( coexpressions, predictedCoexpressionType );
    }

    /**
     * @param coexpressions
     */
    private void postProcessProbeAlignedRegions( CoexpressionCollectionValueObject coexpressions ) {
        CoexpressedGenesDetails probeAlignedCoexpressionType = coexpressions.getProbeAlignedCoexpressionType();
        postProcess( coexpressions, probeAlignedCoexpressionType );
    }

}
