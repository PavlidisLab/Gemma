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
     * @return MeanVarianceRelation object
     */
    public static MeanVarianceRelation getMeanVariance( ExpressionDataDoubleMatrix matrix ) {

        DoubleMatrix2D mat = new DenseDoubleMatrix2D( matrix.rows(), matrix.columns() );
        for ( int row = 0; row < mat.rows(); row++ ) {
            mat.viewRow( row ).assign( matrix.getRawRow( row ) );
        }
        MeanVarianceEstimator mve = new MeanVarianceEstimator( mat );
        MeanVarianceRelation mvr = MeanVarianceRelation.Factory.newInstance();

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

        log.info( "Starting mean-variance computation" );

        ExpressionExperiment updatedEe = expressionExperimentService.thawLiter( ee );
        MeanVarianceRelation mvr = updatedEe.getMeanVarianceRelation();

        if ( forceRecompute || mvr == null ) {

            log.info( " Recomputing mean-variance " );

            Collection<QuantitationType> quantitationTypes = expressionExperimentService
                    .getQuantitationTypes( updatedEe );
            Collection<QuantitationType> usefulQuantitationTypes = ExpressionDataMatrixBuilder
                    .getUsefulQuantitationTypes( quantitationTypes );

            if ( usefulQuantitationTypes.isEmpty() ) {
                throw new IllegalStateException( "No useful quantitation types for " + updatedEe.getShortName() );
            }

            ExpressionDataDoubleMatrix intensities = meanVarianceServiceHelper.getIntensities( updatedEe );
            mvr = getMeanVariance( intensities );
            meanVarianceServiceHelper.createMeanVariance( updatedEe, mvr );
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
}
