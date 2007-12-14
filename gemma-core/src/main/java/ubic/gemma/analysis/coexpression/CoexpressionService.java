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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionTypeValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.coexpression.GeneCoexpressionResults;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Perform probe-to-probe coexpression link analysis ("TMM-style").
 * 
 * @spring.bean id="coexpressionService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @author paul
 * @version $Id$
 */
public class CoexpressionService {
    private static Log log = LogFactory.getLog( CoexpressionService.class.getName() );
    CompositeSequenceService compositeSequenceService;
    GeneService geneService;

    /**
     * Call this!
     * 
     * @param gene
     * @param ees
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @return
     */
    public CoexpressionCollectionValueObject linkAnalysis( Gene gene, Collection<ExpressionExperiment> ees,
            int stringency ) {

        if ( stringency <= 0 ) stringency = 1;

        CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                .getCoexpressedGenes( gene, ees, stringency );

        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting postprocessing" );
        postProcessing( new GeneCoexpressionResults(), coexpressions );

        watch.stop();
        Long elapsed = watch.getTime();
        coexpressions.setPostProcessTime( elapsed );
        log.info( "Done postprocessing; time for postprocessing: " + elapsed );
        return coexpressions;
    }

    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementEEContributions( Collection<Long> contributingEEs, CoexpressionTypeValueObject coexpressions ) {

        for ( Long eeID : contributingEEs ) {
            ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( eeID );

            if ( eeVo == null ) {
                log.warn( "Looked for " + eeID + " but not in coexpressions object" );
                continue;
            }
            if ( eeVo.getCoexpressionLinkCount() == null )
                eeVo.setCoexpressionLinkCount( new Long( 1 ) );
            else
                eeVo.setCoexpressionLinkCount( eeVo.getCoexpressionLinkCount() + 1 );

        }
    }

    /**
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementRawEEContributions( Collection<Long> contributingEEs,
            CoexpressionTypeValueObject coexpressions ) {

        for ( Long eeID : contributingEEs ) {
            ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( eeID );

            if ( eeVo == null ) {
                log.warn( "Looked for " + eeID + " but not in coexpressions object" );
                continue;
            }
            if ( eeVo.getRawCoexpressionLinkCount() == null )
                eeVo.setRawCoexpressionLinkCount( new Long( 1 ) );
            else
                eeVo.setRawCoexpressionLinkCount( eeVo.getRawCoexpressionLinkCount() + 1 );
        }
    }

    /**
     * @param genes
     * @param coexpressions
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void postProcessGeneImpls( Map<Long, CoexpressionValueObject> genes,
            CoexpressionCollectionValueObject coexpressions ) {

        int positiveLinkCount = 0;
        int negativeLinkCount = 0;
        int numStringencyGenes = 0;

        Map<Long, Collection<Gene>> querySpecificity = compositeSequenceService.getGenes( coexpressions
                .getQueryGeneProbes() );
        coexpressions.addQueryGeneSpecifityInfo( querySpecificity );
        Collection<Long> allQuerySpecificEE = coexpressions.getQueryGeneSpecificExpressionExperiments();

        Map<Long, Collection<Long>> allSpecificEE = coexpressions.getGeneCoexpressionType()
                .getSpecificExpressionExperiments();
        List<Long> allEEIds = new ArrayList<Long>( coexpressions.getGeneCoexpressionType().getExpressionExperimentIds() );

        for ( Long geneId : genes.keySet() ) {
            CoexpressionValueObject coExValObj = genes.get( geneId );

            // determine which EE's that contributed to this gene's coexpression were non-specific
            // an ee is specific iff the ee is specific for the query gene and the target gene.
            Collection<Long> nonspecificEE = new HashSet<Long>( coExValObj.getExpressionExperiments() );
            Collection<Long> specificEE = new HashSet<Long>( allSpecificEE.get( geneId ) ); // get the EE's that are
            // specific for the target
            // gene
            specificEE.retainAll( allQuerySpecificEE ); // get the EE's that are specific for both the target and
            // the query gene
            nonspecificEE.removeAll( specificEE );
            coExValObj.setNonspecificEE( nonspecificEE );
            coExValObj.computeExperimentBits( allEEIds );

            if ( coExValObj.getGeneName().equalsIgnoreCase( "RPL27" ) ) log.debug( "at gene rpl27" );

            // figure out which genes where culprits for making this gene non-specific
            Collection<Long> probes = coExValObj.getProbes();
            for ( Long eeID : nonspecificEE ) {
                for ( Long probeID : probes ) {
                    if ( coexpressions.getGeneCoexpressionType().getNonSpecificGenes( eeID, probeID ) != null ) {
                        for ( Long geneID : coexpressions.getGeneCoexpressionType().getNonSpecificGenes( eeID, probeID ) ) {
                            coExValObj.addNonSpecificGene( genes.get( geneID ).getGeneName() );
                            if ( geneID == coexpressions.getQueryGene().getId() )
                                coExValObj.setHybridizesWithQueryGene( true );
                        }
                    }
                }

            }
            coExValObj.getNonSpecificGenes().remove( coExValObj.getGeneOfficialName() );

            boolean added = false;

            incrementRawEEContributions( coExValObj.getExpressionExperiments(), coexpressions.getGeneCoexpressionType() );

            if ( coExValObj.getPositiveLinkCount() != null ) {
                numStringencyGenes++;
                positiveLinkCount++;
                added = true;
                // add in coexpressions that match stringency
                coexpressions.getCoexpressionData().add( coExValObj );
                // add in expression experiments that match stringency
                // update the link count for that EE
                incrementEEContributions( coExValObj.getEEContributing2PositiveLinks(), coexpressions
                        .getGeneCoexpressionType() );
            }

            if ( coExValObj.getNegativeLinkCount() != null ) {
                negativeLinkCount++;
                // add in expression experiments that match stringency
                // update the link count for that EE
                incrementEEContributions( coExValObj.getEEContributing2NegativeLinks(), coexpressions
                        .getGeneCoexpressionType() );

                if ( added ) continue; // no point in adding or counting the same element twice
                coexpressions.getCoexpressionData().add( coExValObj );
                numStringencyGenes++;

            }
        }

        // add count of pruned matches to coexpression data
        coexpressions.getGeneCoexpressionType().setPositiveStringencyLinkCount( positiveLinkCount );
        coexpressions.getGeneCoexpressionType().setNegativeStringencyLinkCount( negativeLinkCount );
        coexpressions.getGeneCoexpressionType().setNumberOfGenes( genes.size() );
    }

    /**
     * @param stringency
     * @param geneMap
     * @param coexpressions
     */
    @SuppressWarnings("unchecked")
    private void postProcessing( GeneCoexpressionResults geneMap, CoexpressionCollectionValueObject coexpressions ) {
        postProcessGeneImpls( geneMap.getGeneImplMap(), coexpressions );
        postProcessProbeAlignedRegions( geneMap.getProbeAlignedRegionMap(), coexpressions );
        postProcessPredictedGenes( geneMap.getPredictedGeneMap(), coexpressions );
    }

    /**
     * @param genes
     * @param coexpressions
     */
    private void postProcessPredictedGenes( Map<Long, CoexpressionValueObject> genes,
            CoexpressionCollectionValueObject coexpressions ) {

        List<Long> allEEIds = new ArrayList<Long>( coexpressions.getGeneCoexpressionType().getExpressionExperimentIds() );

        CoexpressionTypeValueObject predictedCoexpressionType = coexpressions.getPredictedCoexpressionType();
        for ( Long geneId : genes.keySet() ) {
            CoexpressionValueObject coExValObj = genes.get( geneId );
            incrementRawEEContributions( coExValObj.getExpressionExperiments(), predictedCoexpressionType );

            coExValObj.computeExperimentBits( allEEIds );
            if ( ( coExValObj.getPositiveLinkCount() != null ) || ( coExValObj.getNegativeLinkCount() != null ) ) {
                coexpressions.getPredictedCoexpressionData().add( coExValObj );

                if ( coExValObj.getPositiveLinkCount() != null )
                    incrementEEContributions( coExValObj.getEEContributing2PositiveLinks(), predictedCoexpressionType );
                else
                    incrementEEContributions( coExValObj.getEEContributing2NegativeLinks(), predictedCoexpressionType );
            }
        }
        predictedCoexpressionType.setNumberOfGenes( genes.size() );
    }

    /**
     * @param genes
     * @param coexpressions
     */
    private void postProcessProbeAlignedRegions( Map<Long, CoexpressionValueObject> genes,
            CoexpressionCollectionValueObject coexpressions ) {

        int numStringencyProbeAlignedRegions = 0;
        List<Long> allEEIds = new ArrayList<Long>( coexpressions.getGeneCoexpressionType().getExpressionExperimentIds() );

        CoexpressionTypeValueObject probeAlignedCoexpressionType = coexpressions.getProbeAlignedCoexpressionType();
        for ( Long geneId : genes.keySet() ) {
            CoexpressionValueObject coExValObj = genes.get( geneId );

            coExValObj.computeExperimentBits( allEEIds );
            incrementRawEEContributions( coExValObj.getExpressionExperiments(), probeAlignedCoexpressionType );

            if ( ( coExValObj.getPositiveLinkCount() != null ) || ( coExValObj.getNegativeLinkCount() != null ) ) {
                numStringencyProbeAlignedRegions++;
                coexpressions.getProbeAlignedCoexpressionData().add( coExValObj );

                if ( coExValObj.getPositiveLinkCount() != null )
                    incrementEEContributions( coExValObj.getEEContributing2PositiveLinks(),
                            probeAlignedCoexpressionType );
                else
                    incrementEEContributions( coExValObj.getEEContributing2NegativeLinks(),
                            probeAlignedCoexpressionType );
            }

        }

        probeAlignedCoexpressionType.setNumberOfGenes( genes.size() );
    }

}
