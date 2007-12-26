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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.BitUtil;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.association.coexpression.HumanGeneCoExpression;
import ubic.gemma.model.association.coexpression.MouseGeneCoExpression;
import ubic.gemma.model.association.coexpression.OtherGeneCoExpression;
import ubic.gemma.model.association.coexpression.RatGeneCoExpression;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * Used to analyze already-persisted probe-level 'links' and turn them into gene-level coexpression information. The
 * results are tied to a specific Analysis that can be referred to by clients. A use case is when we want to provided
 * 'canned' results for certain types of data, for example, brain data sets. As this is a common case, it makes sense to
 * cache the results rather than querying the probe-links each time. The additional benefit is that the client can let
 * us decide which experiments are "brain datasets".
 * 
 * @spring.bean id="geneLinkCoexpressionAnalyzer"
 * @spring.property name="protocolService" ref="protocolService"
 * @spring.property name="gene2GeneCoexpressionService" ref="gene2GeneCoexpressionService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="geneCoexpressionAnalysisService" ref="geneCoexpressionAnalysisService"
 * @spring.property name = "probeLinkCoexpressionAnalyzer" ref="probeLinkCoexpressionAnalyzer"
 * @author paul
 * @version $Id$
 */
public class GeneLinkCoexpressionAnalyzer {
    private static Log log = LogFactory.getLog( GeneLinkCoexpressionAnalyzer.class.getName() );

    private static final int BATCH_SIZE = 500;
    Gene2GeneCoexpressionService gene2GeneCoexpressionService;
    GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;
    ProtocolService protocolService;
    ExpressionExperimentService expressionExperimentService;
    ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer;

    /**
     * @param expressionExperiments
     * @param toUseGenes
     * @param stringency
     * @param toUseAnalysisName
     */
    public void analyze( Set<ExpressionExperiment> expressionExperiments, Collection<Gene> toUseGenes, int stringency,
            String toUseAnalysisName ) {
        Collection<Gene> processedGenes = new HashSet<Gene>();

        log.info( "Starting gene link analysis '" + toUseAnalysisName + " on " + toUseGenes.size() + " genes in "
                + expressionExperiments.size() + " experiments with a stringency of " + stringency );

        Taxon taxon = null;
        Map<Long, Gene> genesToAnalyzeMap = new HashMap<Long, Gene>();
        for ( Gene g : toUseGenes ) {
            if ( taxon == null ) {
                taxon = g.getTaxon();
            } else if ( !taxon.equals( g.getTaxon() ) ) {
                throw new IllegalArgumentException( "Cannot analyze genes from multiple taxa" );
            }
            genesToAnalyzeMap.put( g.getId(), g );
        }

        GeneCoexpressionAnalysis analysis = intializeAnalysis( expressionExperiments, taxon, toUseGenes,
                toUseAnalysisName, stringency );
        assert analysis != null;

        int totalLinks = 0;

        Map<Long, Integer> eeIdOrder = getOrderingMap( expressionExperiments );

        try {
            for ( Gene gene : toUseGenes ) {
                CoexpressionCollectionValueObject coexpressions = probeLinkCoexpressionAnalyzer.linkAnalysis( gene,
                        expressionExperiments, stringency );
                if ( coexpressions.getNumKnownGenes() > 0 ) {
                    Collection<Gene2GeneCoexpression> created = persistCoexpressions( eeIdOrder, gene, coexpressions,
                            analysis, genesToAnalyzeMap, processedGenes );
                    totalLinks += created.size();
                }
                processedGenes.add( gene );
                if ( processedGenes.size() % 100 == 0 ) {
                    log.info( "Processed " + processedGenes.size() + " genes..." );
                }
            }
            analysis.setDescription( analysis.getDescription() + "; " + totalLinks + " gene pairs stored." );
            // analysisS.update( analysis );
        } catch ( Exception e ) {
            log.error( "There was an error during analysis. Cleaning up ..." );
            geneCoexpressionAnalysisService.delete( analysis );
            throw new RuntimeException( e );
        }
        log.info( totalLinks + " gene pairs stored." );
    }

    /**
     * @param experimentsAnalyzed
     * @return Map of location in the vector to EE ID.
     */
    public Map<Integer, Long> getPositionToIdMap( Collection<ExpressionExperiment> experimentsAnalyzed ) {
        List<Long> eeIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : experimentsAnalyzed ) {
            eeIds.add( ee.getId() );
        }
        Collections.sort( eeIds );
        Map<Integer, Long> eeOrderId = new HashMap<Integer, Long>();
        int location = 0;
        for ( Long id : eeIds ) {
            eeOrderId.put( location, id );
            location++;
        }
        return eeOrderId;
    }

    /**
     * @param ggc
     * @param eePositionToIdMap
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperiment> getSupportingExperiments( Gene2GeneCoexpression ggc,
            Map<Integer, Long> eePositionToIdMap ) {

        byte[] datasetsSupportingVector = ggc.getDatasetsSupportingVector();
        List<Long> ids = new ArrayList<Long>();
        for ( int i = 0; i < datasetsSupportingVector.length * Byte.SIZE; i++ ) {
            if ( BitUtil.get( datasetsSupportingVector, i ) ) {
                Long supportingEE = eePositionToIdMap.get( i );
                ids.add( supportingEE );
            }
        }
        /*
         * This is rather inefficient.
         */
        return expressionExperimentService.loadMultiple( ids );
    }

    public void setProbeLinkCoexpressionAnalyzer( ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer ) {
        this.probeLinkCoexpressionAnalyzer = probeLinkCoexpressionAnalyzer;
    }

    /**
     * Algorithm:
     * <ol>
     * <li>Initialize byte array large enough to hold all the EE information (ceil(numeeids /Byte.SIZE))
     * <li>Flip the bit at the right location.
     * </ol>
     * 
     * @param idsToFlip
     * @param eeIdOrder
     * @return
     */
    private byte[] computeSupportingDatasetVector( Collection<Long> idsToFlip, Map<Long, Integer> eeIdOrder ) {
        byte[] supportVector = new byte[( int ) Math.ceil( eeIdOrder.keySet().size() / ( double ) Byte.SIZE )];
        for ( int i = 0, j = supportVector.length; i < j; i++ ) {
            supportVector[i] = 0x0;
        }

        for ( Long id : idsToFlip ) {
            BitUtil.set( supportVector, eeIdOrder.get( id ) );
        }

        return supportVector;
    }

    /**
     * @param expressionExperiments
     * @param toUseGenes
     * @return
     */
    private Protocol createProtocol( Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> toUseGenes ) {
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Stored Gene2GeneCoexpressions" );
        protocol.setDescription( "Using: " + expressionExperiments.size() + " Expression Experiments,  "
                + toUseGenes.size() + " Genes" );
        protocol = protocolService.findOrCreate( protocol );
        return protocol;
    }

    /**
     * @param taxon
     * @return
     */
    private Gene2GeneCoexpression getNewGGCOInstance( Taxon taxon ) {
        Gene2GeneCoexpression g2gCoexpression;
        if ( taxon.getCommonName().equalsIgnoreCase( "mouse" ) )
            g2gCoexpression = MouseGeneCoExpression.Factory.newInstance();
        else if ( taxon.getCommonName().equalsIgnoreCase( "rat" ) )
            g2gCoexpression = RatGeneCoExpression.Factory.newInstance();
        else if ( taxon.getCommonName().equalsIgnoreCase( "human" ) )
            g2gCoexpression = HumanGeneCoExpression.Factory.newInstance();
        else
            g2gCoexpression = OtherGeneCoExpression.Factory.newInstance();
        return g2gCoexpression;
    }

    /**
     * @param experimentsAnalyzed
     * @return Map of EE IDs to the location in the vector.
     */
    private Map<Long, Integer> getOrderingMap( Collection<ExpressionExperiment> experimentsAnalyzed ) {
        List<Long> eeIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : experimentsAnalyzed ) {
            eeIds.add( ee.getId() );
        }
        Collections.sort( eeIds );
        Map<Long, Integer> eeIdOrder = new HashMap<Long, Integer>();
        int location = 0;
        for ( Long id : eeIds ) {
            eeIdOrder.put( id, location );
            location++;
        }
        return eeIdOrder;
    }

    /**
     * @param expressionExperiments
     * @param taxon
     * @param toUseGenes
     * @param analysisName
     * @param stringency
     * @return
     */
    private GeneCoexpressionAnalysis intializeAnalysis( Collection<ExpressionExperiment> expressionExperiments,
            Taxon taxon, Collection<Gene> toUseGenes, String analysisName, int stringency ) {

        GeneCoexpressionAnalysis analysis = GeneCoexpressionAnalysis.Factory.newInstance();

        analysis.setDescription( "Coexpression analysis for " + taxon.getCommonName() + " using "
                + expressionExperiments.size() + " expression experiments" );

        Protocol protocol = createProtocol( expressionExperiments, toUseGenes );

        analysis.setName( analysisName );
        analysis.setProtocol( protocol );
        analysis.setExperimentsAnalyzed( expressionExperiments );
        analysis.setSupportThreshold( stringency );

        return geneCoexpressionAnalysisService.create( analysis );

    }

    /**
     * @param toPersist
     * @param alreadyPersisted
     */
    @SuppressWarnings("unchecked")
    private Collection<Gene2GeneCoexpression> persistCoexpressions( Map<Long, Integer> eeIdOrder, Gene firstGene,
            CoexpressionCollectionValueObject toPersist, GeneCoexpressionAnalysis analysis,
            final Map<Long, Gene> genesToAnalyze, final Collection<Gene> alreadyPersisted ) {

        assert analysis != null;

        Taxon taxon = firstGene.getTaxon();

        Collection<Gene2GeneCoexpression> all = new ArrayList<Gene2GeneCoexpression>();
        Collection<Gene2GeneCoexpression> batch = new ArrayList<Gene2GeneCoexpression>();

        for ( CoexpressionValueObject co : toPersist.getAllGeneCoexpressionData() ) {

            if ( !genesToAnalyze.containsKey( co.getGeneId() ) ) {
                log.info( "coexpressed Gene " + co.getGeneId()
                        + " is not among the genes selected for analysis, so it will be skipped (while analyzing "
                        + firstGene + ")" );
                continue;
            }

            Gene secondGene = genesToAnalyze.get( co.getGeneId() );
            if ( alreadyPersisted.contains( secondGene ) ) continue; // only need to go in one direction

            if ( co.getNegativeLinkSupport() >= analysis.getSupportThreshold() ) {
                Gene2GeneCoexpression g2gCoexpression = getNewGGCOInstance( taxon );
                Collection<Long> contributing2NegativeLinks = co.getEEContributing2NegativeLinks();
                g2gCoexpression.setSourceAnalysis( analysis );
                g2gCoexpression.setFirstGene( firstGene );
                g2gCoexpression.setSecondGene( secondGene );
                g2gCoexpression.setPvalue( co.getNegPValue() );

                byte[] supportVector = computeSupportingDatasetVector( contributing2NegativeLinks, eeIdOrder );
                g2gCoexpression.setNumDataSets( co.getNegativeLinkSupport() );
                g2gCoexpression.setEffect( co.getNegativeScore() );
                g2gCoexpression.setDatasetsSupportingVector( supportVector );
                batch.add( g2gCoexpression );
                if ( batch.size() == BATCH_SIZE ) {
                    all.addAll( this.gene2GeneCoexpressionService.create( batch ) );
                    batch.clear();
                }
            }

            if ( co.getPositiveLinkSupport() >= analysis.getSupportThreshold() ) {
                Gene2GeneCoexpression g2gCoexpression = getNewGGCOInstance( taxon );
                Collection<Long> contributing2PositiveLinks = co.getEEContributing2NegativeLinks();
                g2gCoexpression.setSourceAnalysis( analysis );
                g2gCoexpression.setFirstGene( firstGene );
                g2gCoexpression.setSecondGene( secondGene );
                g2gCoexpression.setPvalue( co.getPosPValue() );

                byte[] supportVector = computeSupportingDatasetVector( contributing2PositiveLinks, eeIdOrder );
                g2gCoexpression.setNumDataSets( co.getPositiveLinkSupport() );
                g2gCoexpression.setEffect( co.getPositiveScore() );
                g2gCoexpression.setDatasetsSupportingVector( supportVector );
                batch.add( g2gCoexpression );
                if ( batch.size() == BATCH_SIZE ) {
                    all.addAll( this.gene2GeneCoexpressionService.create( batch ) );
                    batch.clear();
                }
            }

            if ( log.isDebugEnabled() )
                log.debug( "Persisted: " + firstGene.getOfficialSymbol() + " --> " + secondGene.getOfficialSymbol()
                        + " ( " + co.getNegativeScore() + " , +" + co.getPositiveScore() + " )" );
        }

        if ( batch.size() > 0 ) {
            all.addAll( this.gene2GeneCoexpressionService.create( batch ) );
            batch.clear();
        }
        if ( all.size() > 0 ) {
            log.info( "Persisted " + all.size() + " gene2geneCoexpressions for analysis: " + analysis.getName() );
        }
        return all;

    }

    public void setGene2GeneCoexpressionService( Gene2GeneCoexpressionService gene2GeneCoexpressionService ) {
        this.gene2GeneCoexpressionService = gene2GeneCoexpressionService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setProtocolService( ProtocolService protocolService ) {
        this.protocolService = protocolService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }
}
