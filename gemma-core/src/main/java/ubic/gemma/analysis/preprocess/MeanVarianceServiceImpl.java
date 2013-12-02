/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.MeanVarianceEstimator;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Manage the mean-variance relationship.
 * 
 * @author ptan
 * @version $Id$
 */
@Component
public class MeanVarianceServiceImpl implements MeanVarianceService {

    private static ByteArrayConverter bac = new ByteArrayConverter();
    private static Log log = LogFactory.getLog( MeanVarianceServiceImpl.class );

    @Autowired
    private MeanVarianceServiceHelper meanVarianceServiceHelper;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * @param matrix on which mean variance relation is computed with
     * @param MeanVarianceRelation object, if null, a new object is created
     * @return MeanVarianceRelation object
     */
    private MeanVarianceRelation calculateMeanVariance( ExpressionDataDoubleMatrix matrix, MeanVarianceRelation mvr ) {

        if ( matrix == null ) {
            log.warn( "Experiment matrix is null" );
            return null;
        }

        DoubleMatrix2D mat = new DenseDoubleMatrix2D( matrix.rows(), matrix.columns() );
        for ( int row = 0; row < mat.rows(); row++ ) {
            mat.viewRow( row ).assign( matrix.getRawRow( row ) );
        }
        MeanVarianceEstimator mve = new MeanVarianceEstimator( mat );
        if ( mvr == null ) {
            mvr = MeanVarianceRelation.Factory.newInstance();
        }

        if ( mve.getMeanVariance() != null ) {
            mvr.setMeans( bac.doubleArrayToBytes( mve.getMeanVariance().viewColumn( 0 ).toArray() ) );
            mvr.setVariances( bac.doubleArrayToBytes( mve.getMeanVariance().viewColumn( 1 ).toArray() ) );
        }
        if ( mve.getLoess() != null ) {
            mvr.setLowessX( bac.doubleArrayToBytes( mve.getLoess().viewColumn( 0 ).toArray() ) );
            mvr.setLowessY( bac.doubleArrayToBytes( mve.getLoess().viewColumn( 1 ).toArray() ) );
        }

        return mvr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.MeanVarianceService#create(ExpressionExperiment, boolean)
     */
    @Override
    public MeanVarianceRelation create( ExpressionExperiment ee, boolean forceRecompute ) {

        if ( ee == null ) {
            log.warn( "Experiment is null" );
            return null;
        }

        log.info( "Starting mean-variance computation" );

        MeanVarianceRelation mvr = ee.getMeanVarianceRelation();

        if ( forceRecompute || mvr == null ) {

            log.info( "Recomputing mean-variance" );

            ExpressionExperiment updatedEe = expressionExperimentService.thawLiter( ee );
            mvr = updatedEe.getMeanVarianceRelation();
            ExpressionDataDoubleMatrix intensities = meanVarianceServiceHelper.getIntensities( updatedEe );
            if ( intensities == null ) {
                throw new IllegalStateException( "Could not locate intensity matrix for " + updatedEe.getShortName() );
            }

            Collection<QuantitationType> qtList = expressionExperimentService.getPreferredQuantitationType( updatedEe );
            QuantitationType qt = null;

            if ( qtList.size() == 0 ) {
                log.error( "Did not find any preferred quantitation type. Mean-variance relation was not computed." );
            } else {
                qt = qtList.iterator().next();
                log.warn( "Found more than one preferred quantitation type. Only the first preferred quantitation type ("
                        + qt + ") will be used." );

                try {
                    intensities = ExpressionDataDoubleMatrixUtil.filterAndLogTransform( qt, intensities );
                } catch ( UnknownLogScaleException e ) {
                    log.warn( "Problem log transforming data. Check that the appropriate log scale is used. Mean-variance will be computed as is." );
                }

                mvr = calculateMeanVariance( intensities, mvr );

                meanVarianceServiceHelper.createMeanVariance( ee, mvr );

            }
        }

        log.info( "Mean-variance computation is complete" );

        return mvr;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.MeanVarianceService#findOrCreate(ExpressionExperiment)
     */
    @Override
    public MeanVarianceRelation findOrCreate( ExpressionExperiment ee ) {
        return create( ee, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.MeanVarianceService#hasMeanVariance(ExpressionExperiment)
     */
    @Override
    public boolean hasMeanVariance( ExpressionExperiment ee ) {
        return ee.getMeanVarianceRelation() != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.MeanVarianceService#find(ubic.gemma.model.expression.experiment.ExpressionExperiment
     * )
     */
    @Override
    public MeanVarianceRelation find( ExpressionExperiment ee ) {
        ExpressionExperiment updatedEe = expressionExperimentService.thawLiter( ee );
        return updatedEe.getMeanVarianceRelation();
    }
}
