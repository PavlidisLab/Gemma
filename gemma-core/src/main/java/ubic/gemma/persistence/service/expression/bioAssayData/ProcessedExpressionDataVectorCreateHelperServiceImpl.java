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

package ubic.gemma.persistence.service.expression.bioAssayData;

import cern.colt.list.DoubleArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.core.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.core.datastructure.matrix.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawOrProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

/**
 * Transactional methods.
 *
 * @author Paul
 * @see    ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService
 */
@Service
class ProcessedExpressionDataVectorCreateHelperServiceImpl
        implements ProcessedExpressionDataVectorCreateHelperService {

    private static final Log log = LogFactory.getLog( ProcessedExpressionDataVectorCreateHelperServiceImpl.class );

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Override
    @Transactional
    public void replaceProcessedDataVectors( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vecs ) {

        ee = eeService.thaw( ee );

        // assumption: all the same QT. Further assumption: bioassaydimension already persistent.
        QuantitationType qt = vecs.iterator().next().getQuantitationType();
        if ( qt.getId() == null ) {
            QuantitationType existingQt = quantitationTypeService.find( ee, qt );
            if ( existingQt != null ) {
                qt = existingQt;
            } else {
                qt = quantitationTypeService.create( qt );
            }
        }
        for ( ProcessedExpressionDataVector v : vecs ) {
            v.setQuantitationType( qt );
        }

        ee.getProcessedExpressionDataVectors().clear();
        ee.getProcessedExpressionDataVectors().addAll( vecs );
        ee.setNumberOfDataVectors( vecs.size() );

        eeService.update( ee );

        assert ee.getNumberOfDataVectors() != null;
    }

    @Override
    @Transactional
    public void reorderByDesign( ExpressionExperiment ee ) {
        ee = eeService.thaw( ee );
        if ( ee.getExperimentalDesign().getExperimentalFactors().size() == 0 ) {
            ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                    .info( ee.getShortName() + " does not have a populated experimental design, skipping" );
            return;
        }

        Collection<ProcessedExpressionDataVector> processedDataVectors = ee.getProcessedExpressionDataVectors();

        if ( processedDataVectors.size() == 0 ) {
            ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                    .info( ee.getShortName() + " does not have processed data" );
            return;
        }

        Collection<BioAssayDimension> dims = this.eeService.getBioAssayDimensions( ee );

        if ( dims.size() > 1 ) {
            this.checkAllBioAssayDimensionsMatch( dims );
        }

        BioAssayDimension bioassaydim = dims.iterator().next();
        List<BioMaterial> start = new ArrayList<>();
        for ( BioAssay ba : bioassaydim.getBioAssays() ) {

            start.add( ba.getSampleUsed() );
        }

        /*
         * Get the ordering we want.
         */
        List<BioMaterial> orderByExperimentalDesign = ExpressionDataMatrixColumnSort
                .orderByExperimentalDesign( start, ee.getExperimentalDesign().getExperimentalFactors() );

        /*
         * Map of biomaterials to the new order index.
         */
        final Map<BioMaterial, Integer> ordering = new HashMap<>();
        int i = 0;
        for ( BioMaterial bioMaterial : orderByExperimentalDesign ) {
            ordering.put( bioMaterial, i );
            i++;
        }

        /*
         * Map of the original order to new order of bioassays.
         */
        Map<Integer, Integer> indexes = new HashMap<>();

        Map<BioAssayDimension, BioAssayDimension> old2new = new HashMap<>();
        for ( BioAssayDimension bioAssayDimension : dims ) {

            List<BioAssay> bioAssays = bioAssayDimension.getBioAssays();
            assert bioAssays != null;

            /*
             * Initialize the new bioassay list.
             */
            List<BioAssay> resorted = new ArrayList<>( bioAssays.size() );
            for ( int m = 0; m < bioAssays.size(); m++ ) {
                resorted.add( null );
            }

            for ( int oldIndex = 0; oldIndex < bioAssays.size(); oldIndex++ ) {
                BioAssay bioAssay = bioAssays.get( oldIndex );

                BioMaterial sam1 = bioAssay.getSampleUsed();
                if ( ordering.containsKey( sam1 ) ) {
                    Integer newIndex = ordering.get( sam1 );

                    resorted.set( newIndex, bioAssay );

                    /*
                     * Should be the same for all dimensions....
                     */
                    assert !indexes.containsKey( oldIndex ) || indexes.get( oldIndex ).equals( newIndex );
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

        ByteArrayConverter converter = new ByteArrayConverter();
        for ( ProcessedExpressionDataVector v : processedDataVectors ) {

            BioAssayDimension revisedBioAssayDimension = old2new.get( v.getBioAssayDimension() );
            assert revisedBioAssayDimension != null;

            double[] data = converter.byteArrayToDoubles( v.getData() );

            /*
             * Put the data in the order of the bioAssayDimension.
             */
            Double[] resortedData = new Double[data.length];

            for ( int k = 0; k < data.length; k++ ) {
                resortedData[k] = data[indexes.get( k )];
            }

            v.setData( converter.toBytes( resortedData ) );
            v.setBioAssayDimension( revisedBioAssayDimension );

        }
        ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                .info( "Updating bioassay ordering of " + processedDataVectors.size() + " vectors" );
    }

    @Override
    @Transactional
    public Set<ProcessedExpressionDataVector> updateRanks( ExpressionExperiment ee ) {
        ee = eeService.thaw( ee );
        Set<ProcessedExpressionDataVector> processedVectors = ee.getProcessedExpressionDataVectors();
        StopWatch timer = new StopWatch();
        timer.start();
        ExpressionDataDoubleMatrix intensities = this.loadIntensities( ee, processedVectors );

        if ( intensities == null ) {
            return processedVectors;
        }

        ProcessedExpressionDataVectorCreateHelperServiceImpl.log.info( "Load intensities: " + timer.getTime() + "ms" );

        this.computeRanks( processedVectors, intensities );

        ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                .info( "Updating ranks data for " + processedVectors.size() + " vectors ..." );
        this.processedExpressionDataVectorService.update( processedVectors );
        ProcessedExpressionDataVectorCreateHelperServiceImpl.log.info( "Done" );

        return processedVectors;
    }

    /**
     * Computes expression intensities depending on which ArrayDesign TechnologyType is used.
     *
     * @return ExpressionDataDoubleMatrix
     */
    private ExpressionDataDoubleMatrix loadIntensities( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        assert !arrayDesignsUsed.isEmpty();
        ArrayDesign arrayDesign = arrayDesignsUsed.iterator().next();
        assert arrayDesign != null && arrayDesign.getTechnologyType() != null;

        ExpressionDataDoubleMatrix intensities;

        if ( arrayDesign.getTechnologyType().equals( TechnologyType.TWOCOLOR )
                || arrayDesign.getTechnologyType().equals( TechnologyType.DUALMODE ) ) {

            ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                    .info( "Computing intensities for two-color data from underlying data" );

            /*
             * Get vectors needed to compute intensities.
             */
            Collection<QuantitationType> quantitationTypes = eeService.getQuantitationTypes( ee );
            Collection<QuantitationType> usefulQuantitationTypes = ExpressionDataMatrixBuilder
                    .getUsefulQuantitationTypes( quantitationTypes );

            if ( usefulQuantitationTypes.isEmpty() ) {
                throw new IllegalStateException( "No useful quantitation types for " + ee.getShortName() );
            }

            Collection<? extends RawOrProcessedExpressionDataVector> vectors = rawExpressionDataVectorService.findAndThaw( usefulQuantitationTypes );
            if ( vectors.isEmpty() ) {
                vectors = processedExpressionDataVectorService.findAndThaw( usefulQuantitationTypes );
            }

            if ( vectors.isEmpty() ) {
                throw new IllegalStateException( "No vectors for useful quantitation types for " + ee.getShortName() );
            }

            ProcessedExpressionDataVectorCreateHelperServiceImpl.log.info( "Vectors loaded ..." );

            ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( processedVectors, vectors );
            intensities = builder.getIntensity();

            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData();

            if ( missingValues == null ) {
                ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                        .warn( "Could not locate missing value matrix for " + ee
                                + ", rank computation skipped (needed for two-color data)" );
                return intensities;
            }

            if ( intensities == null ) {
                ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                        .warn( "Could not locate intensity matrix for " + ee
                                + ", rank computation skipped (needed for two-color data)" );
                return null;
            }

            ProcessedExpressionDataVectorCreateHelperServiceImpl.log.info( "Masking ..." );
            this.maskMissingValues( intensities, missingValues );

        } else {
            ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                    .info( "Computing intensities directly from processed data" );
            intensities = new ExpressionDataDoubleMatrix( processedVectors );
        }

        return intensities;
    }

    /**
     * Make sure we have only one ordering!!! If the sample matching is botched, there will be problems.
     */
    private void checkAllBioAssayDimensionsMatch( Collection<BioAssayDimension> dims ) {
        ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                .info( "Data set has more than one bioassaydimension for its processed data vectors" );
        List<BioMaterial> ordering = new ArrayList<>();
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

    private void computeRanks(
            Collection<ProcessedExpressionDataVector> processedDataVectors, ExpressionDataDoubleMatrix intensities ) {

        DoubleArrayList ranksByMean = this.getRanks( intensities, ProcessedExpressionDataVectorDao.RankMethod.mean );
        assert ranksByMean != null;
        DoubleArrayList ranksByMax = this.getRanks( intensities, ProcessedExpressionDataVectorDao.RankMethod.max );
        assert ranksByMax != null;

        for ( ProcessedExpressionDataVector vector : processedDataVectors ) {
            CompositeSequence de = vector.getDesignElement();
            if ( intensities.getRow( de ) == null ) {
                ProcessedExpressionDataVectorCreateHelperServiceImpl.log
                        .warn( "No intensity value for " + de + ", rank for vector will be null" );
                vector.setRankByMean( null );
                vector.setRankByMax( null );
                continue;
            }
            int i = intensities.getRowIndex( de );
            double rankByMean = ranksByMean.get( i ) / ranksByMean.size();
            double rankByMax = ranksByMax.get( i ) / ranksByMax.size();
            vector.setRankByMean( rankByMean );
            vector.setRankByMax( rankByMax );
        }
    }

    private DoubleArrayList getRanks( ExpressionDataDoubleMatrix intensities,
            ProcessedExpressionDataVectorDao.RankMethod method ) {
        ProcessedExpressionDataVectorCreateHelperServiceImpl.log.debug( "Getting ranks" );
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
                    default:
                        throw new UnsupportedOperationException();
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
     */
    private void maskMissingValues( ExpressionDataDoubleMatrix inMatrix, ExpressionDataBooleanMatrix missingValues ) {
        ExpressionDataDoubleMatrixUtil.maskMatrix( inMatrix, missingValues );
    }

}
