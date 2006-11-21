/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.util.HashSet;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Computes a missing value matrix for ratiometric data sets.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TwoChannelMissingValues {

    /**
     * @param expExp The expression experiment to analyze. The quantitation types to use are selected automatically. If
     *        you want more control use computeMissingValues(signalDataA, signalDataB, bkgDataA,
     *        bkgDataB,signalToNoiseThreshold)
     * @param signalToNoiseThreshold A value such as 1.5 or 2.0; only spots for which at least ONE of the channel signal
     *        is more than signalToNoiseThreshold*background will be considered present.
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the experiment.
     */
    public Collection<DesignElementDataVector> computeMissingValues( ExpressionExperiment expExp,
            double signalToNoiseThreshold ) {
        Collection<DesignElementDataVector> allVectors = expExp.getDesignElementDataVectors();
        Collection<DesignElementDataVector> finalResults = new HashSet<DesignElementDataVector>();

        Collection<BioAssayDimension> dimensions = new HashSet<BioAssayDimension>();
        for ( DesignElementDataVector vector : allVectors ) {
            dimensions.add( vector.getBioAssayDimension() );
        }

        QuantitationType signalChannelA = null;
        QuantitationType signalChannelB = null;
        QuantitationType backgroundChannelA = null;
        QuantitationType backgroundChannelB = null;
        // FIXME this only supports Genepix and QuantArray data, and in a very primitive way.
        for ( DesignElementDataVector vector : allVectors ) {
            QuantitationType qType = vector.getQuantitationType();
            String name = qType.getName();
            // if ( qType.getType().equals( StandardQuantitationType.MEASUREDSIGNAL ) ) {
            // if ( qType.getIsBackground() == true ) {
            if ( name.equals( "CH1B_MEDIAN" ) || name.equals( "CH1_BKD" ) ) {
                backgroundChannelA = qType;
            } else if ( name.equals( "CH2B_MEDIAN" ) || name.equals( "CH2_BKD" ) ) {
                backgroundChannelB = qType;
            } else
            // } else {
            if ( name.equals( "CH1I_MEDIAN" ) || name.equals( "CH1_MEAN" ) ) {
                signalChannelA = qType;
            } else if ( name.equals( "CH2I_MEDIAN" ) || name.equals( "CH2_MEAN" ) ) {
                signalChannelB = qType;
            }
            // }
            // }
            if ( signalChannelA != null && signalChannelB != null && backgroundChannelA != null
                    && backgroundChannelB != null ) {
                break; // no need to go through them all.
            }
        }

        if ( !( signalChannelA != null && signalChannelB != null && backgroundChannelA != null && backgroundChannelB != null ) ) {
            throw new IllegalStateException( "Could not determine all the signals and backgrounds" );
        }

        for ( BioAssayDimension bioAssayDimension : dimensions ) {
            ExpressionDataDoubleMatrix signalDataA = new ExpressionDataDoubleMatrix( expExp, signalChannelA );
            ExpressionDataDoubleMatrix signalDataB = new ExpressionDataDoubleMatrix( expExp, signalChannelB );
            ExpressionDataDoubleMatrix bkgDataA = new ExpressionDataDoubleMatrix( expExp, backgroundChannelA );
            ExpressionDataDoubleMatrix bkgDataB = new ExpressionDataDoubleMatrix( expExp, backgroundChannelB );
            Collection<DesignElementDataVector> dimRes = computeMissingValues( expExp, bioAssayDimension, signalDataA,
                    signalDataB, bkgDataA, bkgDataB, signalToNoiseThreshold );

            finalResults.addAll( dimRes );
        }

        return finalResults;

    }

    /**
     * @param source
     * @param bioAssayDimension
     * @param signalChannelA
     * @param signalChannelB
     * @param bkgChannelA
     * @param bkgChannelB
     * @param signalToNoiseThreshold
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the design elements and
     *         biomaterial dimension represented in the inputs.
     * @see computeMissingValues( ExpressionExperiment expExp, double signalToNoiseThreshold )
     */
    public Collection<DesignElementDataVector> computeMissingValues( ExpressionExperiment source,
            BioAssayDimension bioAssayDimension, ExpressionDataDoubleMatrix signalChannelA,
            ExpressionDataDoubleMatrix signalChannelB, ExpressionDataDoubleMatrix bkgChannelA,
            ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold ) {

        validate( signalChannelA, signalChannelB, bkgChannelA, bkgChannelB, signalToNoiseThreshold );

        ByteArrayConverter converter = new ByteArrayConverter();
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        QuantitationType present = getQuantitationType( signalToNoiseThreshold );

        for ( DesignElement designElement : signalChannelA.getRowElements() ) {
            DesignElementDataVector vect = DesignElementDataVector.Factory.newInstance();
            vect.setQuantitationType( present );
            vect.setExpressionExperiment( source );
            vect.setDesignElement( designElement );
            vect.setBioAssayDimension( bioAssayDimension );

            boolean[] detectionCalls = new boolean[signalChannelB.columns()];

            Double[] signalA = signalChannelA.getRow( designElement );
            Double[] signalB = signalChannelB.getRow( designElement );
            Double[] bkgA = bkgChannelA.getRow( designElement );
            Double[] bkgB = bkgChannelB.getRow( designElement );

            for ( int col = 0; col < signalA.length; col++ ) {
                Double sigAV = signalA[col];
                Double sigBV = signalB[col];
                Double bkgAV = bkgA[col];
                Double bkgBV = bkgB[col];
                boolean call = computeCall( signalToNoiseThreshold, sigAV, sigBV, bkgAV, bkgBV );
                detectionCalls[col] = call;
            }

            vect.setData( converter.booleanArrayToBytes( detectionCalls ) );
            results.add( vect );

        }

        return results;
    }

    private boolean computeCall( double signalToNoiseThreshold, Double sigAV, Double sigBV, Double bkgAV, Double bkgBV ) {
        return sigAV > bkgAV * signalToNoiseThreshold || sigBV > bkgBV * signalToNoiseThreshold;
    }

    /**
     * @param signalToNoiseThreshold
     * @return
     */
    private QuantitationType getQuantitationType( double signalToNoiseThreshold ) {
        QuantitationType present = QuantitationType.Factory.newInstance();
        present.setName( "Detection call" );
        present.setDescription( "Detection call based on signal to noise threshold of " + signalToNoiseThreshold
                + " (Computed by Gemma)" );
        present.setGeneralType( GeneralType.CATEGORICAL );
        present.setIsBackground( false );
        present.setRepresentation( PrimitiveType.BOOLEAN );
        present.setScale( ScaleType.OTHER );
        present.setType( StandardQuantitationType.PRESENTABSENT );
        return present;
    }

    /**
     * @param signalChannelA
     * @param signalChannelB
     * @param bkgChannelA
     * @param bkgChannelB
     * @param signalToNoiseThreshold
     */
    private void validate( ExpressionDataDoubleMatrix signalChannelA, ExpressionDataDoubleMatrix signalChannelB,
            ExpressionDataDoubleMatrix bkgChannelA, ExpressionDataDoubleMatrix bkgChannelB,
            double signalToNoiseThreshold ) {
        if ( signalChannelA == null || signalChannelA.rows() == 0 || signalChannelB == null
                || signalChannelB.rows() == 0 || bkgChannelA == null || bkgChannelA.rows() == 0 || bkgChannelB == null
                || bkgChannelB.rows() == 0 ) {
            throw new IllegalArgumentException( "Collections must not be empty" );
        }

        if ( !( signalChannelA.rows() == signalChannelB.rows() ) && ( bkgChannelA.rows() == bkgChannelB.rows() ) ) {
            throw new IllegalArgumentException( "Collection sizes must match" );
        }

        if ( signalToNoiseThreshold <= 0.0 ) {
            throw new IllegalArgumentException( "Signal-to-noise threshold must be greater than zero" );
        }

        int numSamplesA = signalChannelA.columns();
        int numSamplesB = signalChannelB.columns();

        if ( numSamplesA != numSamplesB ) {
            throw new IllegalArgumentException( "Number of samples doesn't match!" );
        }

    }
}
