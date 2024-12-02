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
import org.springframework.util.Assert;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.core.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixBuilder;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.util.*;

import static ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort.orderByExperimentalDesign;

/**
 * Transactional methods.
 *
 * @author Paul
 * @see    ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService
 */
@Service
class ProcessedExpressionDataVectorHelperServiceImpl
        implements ProcessedExpressionDataVectorHelperService {

    private static final Log log = LogFactory.getLog( ProcessedExpressionDataVectorHelperServiceImpl.class );

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Override
    @Transactional
    public void reorderByDesign( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            log.info( ee + " does not have a populated experimental design, skipping" );
            return;
        }

        Collection<ProcessedExpressionDataVector> processedDataVectors = ee.getProcessedExpressionDataVectors();

        if ( processedDataVectors.isEmpty() ) {
            log.info( ee + " does not have processed data; no BioAssayDimension can be reordered." );
            return;
        }

        BioAssayDimension dim = processedDataVectors.iterator().next().getBioAssayDimension();
        Assert.isTrue( processedDataVectors.stream().allMatch( v -> v.getBioAssayDimension().equals( dim ) ),
                "All vectors must share the same dimension: " + dim );

        List<BioMaterial> originalSamples = new ArrayList<>();
        for ( BioAssay ba : dim.getBioAssays() ) {
            originalSamples.add( ba.getSampleUsed() );
        }

        /*
         * Get the ordering we want.
         */
        List<BioMaterial> orderedSamples = new ArrayList<>( originalSamples );
        orderByExperimentalDesign( originalSamples, ee.getExperimentalDesign().getExperimentalFactors() );

        if ( originalSamples.equals( orderedSamples ) ) {
            log.info( ee + " already has correct ordering; no need to reorder processed vectors." );
            return;
        }

        List<BioAssay> bioAssays = dim.getBioAssays();

        // reordered bioassays
        BioAssay[] orderedBioAssays = new BioAssay[bioAssays.size()];
        // mapping of old-to-new indices for sorting vectors efficiently
        int[] indexes = new int[bioAssays.size()];
        for ( int oldIndex = 0; oldIndex < bioAssays.size(); oldIndex++ ) {
            BioAssay bioAssay = bioAssays.get( oldIndex );
            int newIndex = orderedSamples.indexOf( bioAssay.getSampleUsed() );
            if ( newIndex == -1 ) {
                throw new IllegalStateException( "No index found for " + bioAssay.getSampleUsed() + " in " + orderedSamples );
            }
            orderedBioAssays[newIndex] = bioAssay;
            indexes[oldIndex] = newIndex;
        }

        BioAssayDimension newBioAssayDimension = BioAssayDimension.Factory.newInstance();
        newBioAssayDimension.setBioAssays( Arrays.asList( orderedBioAssays ) );
        newBioAssayDimension.setName( "Processed data of ee " + ee.getShortName() + " ordered by design" );
        newBioAssayDimension.setDescription( "Data was reordered based on the experimental design." );
        newBioAssayDimension = bioAssayDimensionService.create( newBioAssayDimension );

        for ( ProcessedExpressionDataVector v : processedDataVectors ) {
            assert newBioAssayDimension != null;
            double[] data = v.getDataAsDoubles();
            // put the data in the order of the bioAssayDimension.
            double[] resortedData = new double[data.length];
            for ( int k = 0; k < data.length; k++ ) {
                resortedData[k] = data[indexes[k]];
            }
            v.setDataAsDoubles( resortedData );
            v.setBioAssayDimension( newBioAssayDimension );
        }

        eeService.update( ee );
        log.info( "Updating bioassay ordering of " + processedDataVectors.size() + " vectors" );
    }

    @Override
    @Transactional
    public void updateRanks( ExpressionExperiment ee ) {
        Set<ProcessedExpressionDataVector> processedVectors = ee.getProcessedExpressionDataVectors();
        StopWatch timer = new StopWatch();
        timer.start();
        ExpressionDataDoubleMatrix intensities = this.loadIntensities( ee, processedVectors );

        if ( intensities == null ) {
            return;
        }

        ProcessedExpressionDataVectorHelperServiceImpl.log.info( "Load intensities: " + timer.getTime() + "ms" );

        ProcessedExpressionDataVectorHelperServiceImpl.log.info( "Updating ranks data for " + processedVectors.size() + " vectors..." );
        this.computeRanks( processedVectors, intensities );
        eeService.update( ee );
        ProcessedExpressionDataVectorHelperServiceImpl.log.info( "Updating ranks is done" );
    }

    /**
     * Computes expression intensities depending on which ArrayDesign TechnologyType is used.
     *
     * @return ExpressionDataDoubleMatrix
     */
    private ExpressionDataDoubleMatrix loadIntensities( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        if ( arrayDesignsUsed.isEmpty() ) {
            log.warn( String.format( "%s does not have any associated platform, cannot compute intensities for two-color data.", ee ) );
            return null;
        }
        ArrayDesign arrayDesign = arrayDesignsUsed.iterator().next();
        assert arrayDesign != null && arrayDesign.getTechnologyType() != null;

        ExpressionDataDoubleMatrix intensities;

        if ( arrayDesign.getTechnologyType().equals( TechnologyType.TWOCOLOR )
                || arrayDesign.getTechnologyType().equals( TechnologyType.DUALMODE ) ) {

            ProcessedExpressionDataVectorHelperServiceImpl.log
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

            Collection<? extends BulkExpressionDataVector> vectors = rawExpressionDataVectorService.findAndThaw( usefulQuantitationTypes );
            if ( vectors.isEmpty() ) {
                vectors = processedExpressionDataVectorService.findAndThaw( usefulQuantitationTypes );
            }

            if ( vectors.isEmpty() ) {
                throw new IllegalStateException( "No vectors for useful quantitation types for " + ee.getShortName() );
            }

            ProcessedExpressionDataVectorHelperServiceImpl.log.info( "Vectors loaded ..." );

            ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( processedVectors, vectors );
            intensities = builder.getIntensity();

            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData();

            if ( missingValues == null ) {
                ProcessedExpressionDataVectorHelperServiceImpl.log
                        .warn( "Could not locate missing value matrix for " + ee
                                + ", rank computation skipped (needed for two-color data)" );
                return intensities;
            }

            if ( intensities == null ) {
                ProcessedExpressionDataVectorHelperServiceImpl.log
                        .warn( "Could not locate intensity matrix for " + ee
                                + ", rank computation skipped (needed for two-color data)" );
                return null;
            }

            ProcessedExpressionDataVectorHelperServiceImpl.log.info( "Masking ..." );
            this.maskMissingValues( intensities, missingValues );

        } else {
            ProcessedExpressionDataVectorHelperServiceImpl.log
                    .info( "Computing intensities directly from processed data" );
            intensities = new ExpressionDataDoubleMatrix( processedVectors );
        }

        return intensities;
    }

    /**
     * Make sure we have only one ordering!!! If the sample matching is botched, there will be problems.
     */
    private void checkAllBioAssayDimensionsMatch( Collection<BioAssayDimension> dims ) {
        ProcessedExpressionDataVectorHelperServiceImpl.log
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
                ProcessedExpressionDataVectorHelperServiceImpl.log
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
        ProcessedExpressionDataVectorHelperServiceImpl.log.debug( "Getting ranks" );
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
        maskMatrix( inMatrix, missingValues );
    }

    /**
     * Use the mask matrix to turn some values in a matrix to NaN. Ideally, they matrices are conformant, but if they
     * are not (as some rows are sometimes missing for some quantitation types), this method attempts to handle it
     * anyway (see below). The rows and columns do not have to be in the same order, but they do have to have the same
     * column keys and row keys (with the exception of missing rows). The result is stored in matrix.
     *
     * @param matrix matrix
     * @param mask if null, masking is not attempted.
     */
    private void maskMatrix( ExpressionDataDoubleMatrix matrix, @Nullable ExpressionDataBooleanMatrix mask ) {
        if ( mask == null ) return;
        // checkConformant( a, b );
        if ( matrix.columns() != mask.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + matrix.columns() + " != " + mask.columns() );
        int columns = matrix.columns();
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            if ( mask.getRow( del ) == null ) {
                log.warn( "Mask Matrix is missing a row for " + del );
                continue;
            }
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                boolean present = mask.get( del, bm );
                if ( !present ) {
                    matrix.set( del, bm, Double.NaN );
                }

            }
        }
    }
}
