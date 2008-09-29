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
import org.springframework.beans.factory.InitializingBean;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.analysis.preprocess.ProcessedExpressionDataVectorCreateTask;
import ubic.gemma.grid.javaspaces.analysis.preprocess.SpacesProcessedExpressionDataVectorCreateCommand;
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
import ubic.gemma.util.progress.TaskRunningService;
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
 */
public class ProcessedExpressionDataVectorCreateService extends BaseSpacesTask implements
        ProcessedExpressionDataVectorCreateTask, InitializingBean {

    private static Log log = LogFactory.getLog( ProcessedExpressionDataVectorCreateService.class.getName() );

    private ExpressionExperimentService eeService = null;

    private ProcessedExpressionDataVectorService processedDataService = null;

    private DesignElementDataVectorService designElementDataVectorService = null;

    /* used in the spaces world */
    private String taskId = null;
    private long counter = 0;

    /**
     * @param ee
     * @param method2
     * @return the vectors that were modified.
     */

    public Collection<ProcessedExpressionDataVector> computeProcessedExpressionData( ExpressionExperiment ee ) {

        eeService.thawLite( ee );

        Collection<ProcessedExpressionDataVector> processedVectors = processedDataService
                .createProcessedDataVectors( ee );

        assert processedVectors.size() > 0;

        Collection<ProcessedExpressionDataVector> result = updateRanks( ee, processedVectors );
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
     * @param ee
     * @param processedVectors
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ProcessedExpressionDataVector> updateRanks( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors ) {
        processedDataService.thaw( processedVectors );

        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        ExpressionDataDoubleMatrix intensities;
        if ( !arrayDesignsUsed.iterator().next().getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
            Collection<DesignElementDataVector> vectors = eeService
                    .getDesignElementDataVectors( ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( ee ) );

            designElementDataVectorService.thaw( vectors );
            ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( processedVectors, vectors );
            intensities = builder.getIntensity();

            if ( intensities == null ) {
                throw new IllegalStateException( "Could not locate intensity matrix for " + ee );
            }

            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData();

            this.maskMissingValues( intensities, missingValues );

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#getTaskId()
     */
    public String getTaskId() {
        return taskId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.analysis.preprocess.ProcessedExpressionDataVectorCreateTask#execute(ubic.gemma.grid.javaspaces.analysis.preprocess.SpacesProcessedExpressionDataVectorCreateCommand)
     */
    public SpacesResult execute( SpacesProcessedExpressionDataVectorCreateCommand processedVectorCreateCommand ) {

        super.initProgressAppender( this.getClass() );

        String accession = processedVectorCreateCommand.getAccession();

        ExpressionExperiment ee = this.eeService.findByName( accession );
        eeService.thaw( ee );

        SpacesResult result = new SpacesResult();
        Collection<ProcessedExpressionDataVector> processedVectors = this.computeProcessedExpressionData( ee );
        result.setAnswer( processedVectors );

        counter++;
        result.setTaskID( counter );
        log.info( "Task execution complete ... returning result " + result.getAnswer() + " with id "
                + result.getTaskID() );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#execute(java.lang.Object)
     */
    public SpacesResult execute( Object command ) {
        // TODO Auto-generated method stub
        return null;
    }

}
