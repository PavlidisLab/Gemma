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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import cern.colt.list.DoubleArrayList;

/**
 * Compute the "processed" expression data vectors with the rank information filled in.
 * 
 * @author pavlidis
 * @author raymond
 * @version $Id$
 * @spring.bean id="processedExpressionDataVectorCreateService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="processedDataService" ref="processedExpressionDataVectorService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 */
public class ProcessedExpressionDataVectorCreateService {

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    private static Log log = LogFactory.getLog( ProcessedExpressionDataVectorCreateService.class.getName() );

    private ExpressionExperimentService eeService = null;

    private ProcessedExpressionDataVectorService processedDataService = null;

    private DesignElementDataVectorService designElementDataVectorService = null;

    private AuditTrailService auditTrailService;

    /**
     * @param ee
     * @param method2
     * @return the vectors that were modified.
     */

    public Collection<ProcessedExpressionDataVector> computeProcessedExpressionData( ExpressionExperiment ee ) {

        // eeService.thawLite( ee );

        Collection<ProcessedExpressionDataVector> processedVectors = processedDataService
                .createProcessedDataVectors( ee );

        assert processedVectors.size() > 0;

        Collection<ProcessedExpressionDataVector> result = updateRanks( ee, processedVectors );

        audit( ee, "" );

        return result;

    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    public void setProcessedDataService( ProcessedExpressionDataVectorService processedDataService ) {
        this.processedDataService = processedDataService;
    }

    /**
     * @param ad
     * @param builder
     * @return
     */
    private Collection<ProcessedExpressionDataVector> computeRanks(
            Collection<ProcessedExpressionDataVector> processedDataVectors, ExpressionDataDoubleMatrix intensities ) {

        DoubleArrayList ranksByMean = getRanks( intensities, ProcessedExpressionDataVectorDao.RankMethod.mean );
        DoubleArrayList ranksByMax = getRanks( intensities, ProcessedExpressionDataVectorDao.RankMethod.max );

        for ( ProcessedExpressionDataVector vector : processedDataVectors ) {
            DesignElement de = vector.getDesignElement();
            if ( intensities.getRow( de ) == null ) {
                log.warn( "No intensity value for " + de + ", rank for vector will be null" );
                vector.setRankByMean( null );
                vector.setRankByMax( null );
                continue;
            }
            Integer i = intensities.getRowIndex( de );
            assert i != null;
            double rankByMean = ranksByMean.get( i ) / ranksByMean.size();
            double rankByMax = ranksByMax.get( i ) / ranksByMax.size();
            vector.setRankByMean( rankByMean );
            vector.setRankByMax( rankByMax );
        }

        return processedDataVectors;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        AuditEventType eventType = ProcessedVectorComputationEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * @param intensities
     * @return
     */
    private DoubleArrayList getRanks( ExpressionDataDoubleMatrix intensities,
            ProcessedExpressionDataVectorDao.RankMethod method ) {
        log.debug( "Getting ranks" );
        DoubleArrayList result = new DoubleArrayList( intensities.rows() );

        for ( ExpressionDataMatrixRowElement de : intensities.getRowElements() ) {
            double[] rowObj = ArrayUtils.toPrimitive( intensities.getRow( de.getDesignElement() ) );
            double valueForRank = Double.MIN_VALUE;
            if ( rowObj != null ) {
                DoubleArrayList row = new DoubleArrayList( rowObj );
                switch ( method ) {
                    case max:
                        valueForRank = DescriptiveWithMissing.max( row );
                        break;
                    case mean:
                        valueForRank = DescriptiveWithMissing.mean( row );
                        break;
                }

            }
            result.add( valueForRank );
        }

        return Rank.rankTransform( result );
    }

    /**
     * Masking is done even if the array design is not two-color, so the decision whether to mask or not must be done
     * elsewhere.
     * 
     * @param inMatrix The matrix to be masked
     * @param missingValues
     * @param missingValueMatrix The matrix used as a mask.
     */
    private void maskMissingValues( ExpressionDataDoubleMatrix inMatrix, ExpressionDataBooleanMatrix missingValues ) {
        if ( missingValues != null ) ExpressionDataDoubleMatrixUtil.maskMatrix( inMatrix, missingValues );
    }

    /**
     * If possible, update the ranks for the processed data vectors. For data sets with only ratio expression values
     * provided, ranks will not be computable.
     * 
     * @param ee
     * @param processedVectors
     * @return The vectors after updating them, or just the original vectors if ranks could not be computed. (The
     *         vectors may be thawed in the process)
     */
    @SuppressWarnings("unchecked")
    private Collection<ProcessedExpressionDataVector> updateRanks( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors ) {
        processedDataService.thaw( processedVectors );

        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        ExpressionDataDoubleMatrix intensities;
        if ( !arrayDesignsUsed.iterator().next().getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {

            /*
             * Get vectors needed to compute intensities.
             */
            Collection quantitationTypes = eeService.getQuantitationTypes( ee );
            Collection<QuantitationType> usefulQuantitationTypes = ExpressionDataMatrixBuilder
                    .getUsefulQuantitationTypes( quantitationTypes );
            Collection<DesignElementDataVector> vectors = eeService
                    .getDesignElementDataVectors( usefulQuantitationTypes );

            designElementDataVectorService.thaw( vectors );
            ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( processedVectors, vectors );
            intensities = builder.getIntensity();

            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData();

            if ( missingValues == null || intensities == null ) {
                log.warn( "Could not locate intensity matrix for " + ee + ", rank computation skipped" );
                return processedVectors;
            } else {
                this.maskMissingValues( intensities, missingValues );
            }

        } else {
            intensities = new ExpressionDataDoubleMatrix( processedVectors );
        }

        Collection<ProcessedExpressionDataVector> updatedVectors = computeRanks( processedVectors, intensities );
        if ( updatedVectors == null ) {
            log.info( "Could not get preferred data vectors, not updating ranks data" );
            return processedVectors;
        }

        log.info( "Updating ranks data for " + updatedVectors.size() + " vectors" );
        this.processedDataService.update( updatedVectors );

        return updatedVectors;
    }

}
