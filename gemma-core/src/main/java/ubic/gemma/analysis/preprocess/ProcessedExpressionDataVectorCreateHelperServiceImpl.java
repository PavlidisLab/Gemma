/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cern.colt.list.DoubleArrayList;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalReversionEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;

/**
 * Transactional methods.
 * 
 * @author Paul
 * @version $Id$
 * @see ProcessedExpressionDataVectorCreateService
 */
@Service
public class ProcessedExpressionDataVectorCreateHelperServiceImpl implements
        ProcessedExpressionDataVectorCreateHelperService {

    private static Log log = LogFactory.getLog( ProcessedExpressionDataVectorCreateHelperServiceImpl.class );

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private ExpressionExperimentService eeService = null;

    @Autowired
    private ProcessedExpressionDataVectorService processedDataService = null;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private ExpressionExperimentDao eeDao;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateHelperService#createProcessedExpressionData
     * (ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public Collection<ProcessedExpressionDataVector> createProcessedExpressionData( ExpressionExperiment ee ) {
        ee = processedDataService.createProcessedDataVectors( ee );
        ee = this.eeService.thawLite( ee );

        Collection<ProcessedExpressionDataVector> processedVectors = ee.getProcessedExpressionDataVectors();

        assert processedVectors.size() > 0;

        audit( ee, "" );

        return processedVectors;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        AuditEventType eventType = ProcessedVectorComputationEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * @param ad
     * @param builder
     * @return
     */
    private Collection<ProcessedExpressionDataVector> computeRanks(
            Collection<ProcessedExpressionDataVector> processedDataVectors, ExpressionDataDoubleMatrix intensities ) {

        DoubleArrayList ranksByMean = getRanks( intensities, ProcessedExpressionDataVectorDao.RankMethod.mean );
        assert ranksByMean != null;
        DoubleArrayList ranksByMax = getRanks( intensities, ProcessedExpressionDataVectorDao.RankMethod.max );
        assert ranksByMax != null;

        for ( ProcessedExpressionDataVector vector : processedDataVectors ) {
            CompositeSequence de = vector.getDesignElement();
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
    @Override
    public Collection<ProcessedExpressionDataVector> updateRanks( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors ) {

        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        assert !arrayDesignsUsed.isEmpty();
        ArrayDesign arrayDesign = arrayDesignsUsed.iterator().next();
        assert arrayDesign != null && arrayDesign.getTechnologyType() != null;

        ExpressionDataDoubleMatrix intensities;

        if ( !arrayDesign.getTechnologyType().equals( TechnologyType.ONECOLOR )
                && !arrayDesign.getTechnologyType().equals( TechnologyType.NONE ) ) {

            /*
             * Get vectors needed to compute intensities.
             */
            Collection<QuantitationType> quantitationTypes = eeService.getQuantitationTypes( ee );
            Collection<QuantitationType> usefulQuantitationTypes = ExpressionDataMatrixBuilder
                    .getUsefulQuantitationTypes( quantitationTypes );

            if ( usefulQuantitationTypes.isEmpty() ) {
                throw new IllegalStateException( "No useful quantitation types for " + ee.getShortName() );
            }

            Collection<DesignElementDataVector> vectors = eeService
                    .getDesignElementDataVectors( usefulQuantitationTypes );

            if ( vectors.isEmpty() ) {
                throw new IllegalStateException( "No vectors for useful quantitation types for " + ee.getShortName() );
            }

            // designElementDataVectorService.thaw( vectors ); // should be in a transaction, don't need to do.
            ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( processedVectors, vectors );
            intensities = builder.getIntensity();

            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData();

            if ( missingValues == null ) {
                log.warn( "Could not locate missing value matrix for " + ee
                        + ", rank computation skipped (needed for two-color data)" );
                return processedVectors;
            }

            if ( intensities == null ) {
                log.warn( "Could not locate intensity matrix for " + ee
                        + ", rank computation skipped (needed for two-color data)" );
                return processedVectors;
            }
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

    @Override
    public void reorderByDesign( Long eeId ) {
        ExpressionExperiment ee = eeDao.load( eeId );

        if ( ee.getExperimentalDesign().getExperimentalFactors().size() == 0 ) {
            log.info( ee.getShortName() + " does not have a populated experimental design, skipping" );
            return;
        }

        Collection<ProcessedExpressionDataVector> processedDataVectors = ee.getProcessedExpressionDataVectors();

        if ( processedDataVectors.size() == 0 ) {
            log.info( ee.getShortName() + " does not have processed data" );
            return;
        }

        Collection<BioAssayDimension> dims = this.eeService.getBioAssayDimensions( ee );

        if ( dims.size() > 1 ) {
            checkAllBioAssayDimensionsMatch( dims );
        }

        BioAssayDimension bioassaydim = dims.iterator().next();
        List<BioMaterial> start = new ArrayList<BioMaterial>();
        for ( BioAssay ba : bioassaydim.getBioAssays() ) {

            start.add( ba.getSampleUsed() );
        }

        /*
         * Get the ordering we want.
         */
        List<BioMaterial> orderByExperimentalDesign = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( start,
                ee.getExperimentalDesign().getExperimentalFactors() );

        /*
         * Map of biomaterials to the new order index.
         */
        final Map<BioMaterial, Integer> ordering = new HashMap<BioMaterial, Integer>();
        int i = 0;
        for ( BioMaterial bioMaterial : orderByExperimentalDesign ) {
            ordering.put( bioMaterial, i );
            i++;
        }

        /*
         * Map of the original order to new order of bioassays.
         */
        Map<Integer, Integer> indexes = new HashMap<Integer, Integer>();

        Map<BioAssayDimension, BioAssayDimension> old2new = new HashMap<BioAssayDimension, BioAssayDimension>();
        for ( BioAssayDimension bioAssayDimension : dims ) {

            /*
             * This is a list. Andromda won't let us declare it that way.
             */
            Collection<BioAssay> bioAssays = bioAssayDimension.getBioAssays();

            assert bioAssays instanceof List<?>;

            /*
             * Initialize the new bioassay list.
             */
            List<BioAssay> resorted = new ArrayList<BioAssay>( bioAssays.size() );
            for ( int m = 0; m < bioAssays.size(); m++ ) {
                resorted.add( null );
            }

            for ( int oldIndex = 0; oldIndex < bioAssays.size(); oldIndex++ ) {
                BioAssay bioAssay = ( ( List<BioAssay> ) bioAssays ).get( oldIndex );

                BioMaterial sam1 = bioAssay.getSampleUsed();
                if ( ordering.containsKey( sam1 ) ) {
                    Integer newIndex = ordering.get( sam1 );

                    resorted.set( newIndex, bioAssay );

                    if ( indexes.containsKey( oldIndex ) ) {
                        /*
                         * Should be the same for all dimensions....
                         */
                        assert indexes.get( oldIndex ).equals( newIndex );
                    }

                    /*
                         * 
                         */
                    // log.info( oldIndex + " ---> " + newIndex );
                    indexes.put( oldIndex, newIndex );

                } else {
                    throw new IllegalStateException();
                }

            }

            BioAssayDimension newBioAssayDimension = BioAssayDimension.Factory.newInstance();
            newBioAssayDimension.setBioAssays( resorted );
            newBioAssayDimension.setName( "Processed data of ee " + ee.getShortName() + " ordered by design" );
            newBioAssayDimension.setDescription( "Data was reordered based on the experimental design." );

            newBioAssayDimension = bioAssayDimensionService.create( newBioAssayDimension );

            old2new.put( bioAssayDimension, newBioAssayDimension );

        }
        ByteArrayConverter conv = new ByteArrayConverter();

        for ( ProcessedExpressionDataVector v : processedDataVectors ) {

            // log.info( v.getDesignElement() );

            BioAssayDimension revisedBioAssayDimension = old2new.get( v.getBioAssayDimension() );
            assert revisedBioAssayDimension != null;

            double[] data = conv.byteArrayToDoubles( v.getData() );

            /*
             * Put the data in the order of the bioassaydimension.
             */
            Double[] resortedData = new Double[data.length];

            // log.info( StringUtils.join( ArrayUtils.toObject( data ), "," ) );

            for ( int k = 0; k < data.length; k++ ) {
                resortedData[k] = data[indexes.get( k )];
            }

            // log.info( StringUtils.join( resortedData, "," ) );

            v.setData( conv.toBytes( resortedData ) );
            v.setBioAssayDimension( revisedBioAssayDimension );

        }
        log.info( "Updating bioassay ordering of " + processedDataVectors.size() + " vectors" );
        // processedDataService.update( processedDataVectors ); // happens automatically on flush.

        this.auditTrailService.addUpdateEvent( ee, "Reordered the data vectors by experimental design" );

    }

    /**
     * Make sure we have only one ordering!!! If the sample matching is botched, there will be problems.
     * 
     * @param dims
     */
    private void checkAllBioAssayDimensionsMatch( Collection<BioAssayDimension> dims ) {
        log.info( "Data set has more than one bioassaydimension for its processed data vectors" );
        List<BioMaterial> ordering = new ArrayList<BioMaterial>();
        int i = 0;
        for ( BioAssayDimension dim : dims ) {
            int j = 0;
            for ( BioAssay ba : dim.getBioAssays() ) {

                BioMaterial sample = ba.getSampleUsed();

                if ( i == 0 ) {
                    ordering.add( sample );
                } else {
                    if ( !ordering.get( j ).equals( sample ) ) {
                        throw new IllegalStateException(
                                "Two dimensions didn't have the same BioMaterial ordering for the same data set." );
                    }
                    j++;
                }
            }
            i++;
        }
    }

}
