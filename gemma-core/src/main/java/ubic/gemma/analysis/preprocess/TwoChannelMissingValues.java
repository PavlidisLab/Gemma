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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Computes a missing value matrix for ratiometric data sets.
 * <p>
 * Supported formats:
 * <ul>
 * <li>Genepix: CH1B_MEDIAN etc; (various versions)</li>
 * <li>Incyte GEMTools: RAW_DATA etc (no background values)</li>
 * <li>Quantarray: CH1_BKD etc</li>
 * <li>F635.Median / F532.Median (genepix as rendered in some data sets)</li>
 * <li>CH1_SMTM (found in GPL230)</li>
 * <li>Caltech (GPL260)</li>
 * <li>Agilent (Ch2BkgMedian etc or CH2_SIG_MEAN etc)</li>
 * <li>GSE3251 (ch1.Background etc)
 * <li>GPL560 (*_CY3 vs *CY5)
 * </ul>
 * <p>
 * The missing values are computed with the following considerations with respect to available data
 * </p>
 * <ol>
 * <li>This only works if there are signal values for both channels
 * <li>If there are background values, they are used to compute signal-to-noise ratios</li>
 * <li>If the signal values already contain missing data, these are still considered missing.</li>
 * <li>If there are no background values, all values will be considered 'present' unless the signal values are both
 * zero or missing.
 * <li>If the preferred quantitation type data is a missing value, then the data are considered missing (for
 * consistency).
 * </ol>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TwoChannelMissingValues {

    private static Log log = LogFactory.getLog( TwoChannelMissingValues.class.getName() );

    /**
     * @param expExp The expression experiment to analyze. The quantitation types to use are selected automatically. If
     *        you want more control use other computeMissingValues methods.
     * @param ad The array design to consider; this can be null, but should be included if the expression experiment
     *        uses more than one array design.
     * @param signalToNoiseThreshold A value such as 1.5 or 2.0; only spots for which at least ONE of the channel signal
     *        is more than signalToNoiseThreshold*background (and the preferred data are not missing) will be considered
     *        present.
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the experiment.
     */
    public Collection<DesignElementDataVector> computeMissingValues( ExpressionExperiment expExp, ArrayDesign ad,
            double signalToNoiseThreshold ) {
        Collection<DesignElementDataVector> allVectors = expExp.getDesignElementDataVectors();
        Collection<DesignElementDataVector> finalResults = new HashSet<DesignElementDataVector>();

        Collection<BioAssayDimension> dimensions = new HashSet<BioAssayDimension>();
        for ( DesignElementDataVector vector : allVectors ) {
            ArrayDesign adUsed = vector.getBioAssayDimension().getBioAssays().iterator().next().getArrayDesignUsed();
            if ( ad == null || adUsed.equals( ad ) ) {
                dimensions.add( vector.getBioAssayDimension() );
            }
        }

        QuantitationType signalChannelA = null;
        QuantitationType signalChannelB = null;
        QuantitationType backgroundChannelA = null;
        QuantitationType backgroundChannelB = null;

        QuantitationType bkgSubChannelA = null;
        QuantitationType preferred = null;

        for ( DesignElementDataVector vector : allVectors ) {
            QuantitationType qType = vector.getQuantitationType();
            String name = qType.getName();
            if ( qType.getIsPreferred() ) {
                if ( preferred != null && !qType.equals( preferred ) )
                    throw new IllegalStateException( "More than one preferred quantitation type found for " + expExp
                            + " (already had " + preferred + ", just got " + qType + ")" );
                preferred = qType;
            } else if ( name.equals( "CH1B_MEDIAN" ) || name.equals( "CH1_BKD" )
                    || name.toLowerCase().matches( "b532[\\s_\\.](mean|median)" )
                    || name.equals( "BACKGROUND_CHANNEL 1MEDIAN" ) || name.equals( "G_BG_MEDIAN" )
                    || name.equals( "Ch1BkgMedian" ) || name.equals( "ch1.Background" ) || name.equals( "CH1_BKG_MEAN" )
                    || name.equals( "CH1_BKD_ Median" ) ) {
                backgroundChannelA = qType;
            } else if ( name.equals( "CH2B_MEDIAN" ) || name.equals( "CH2_BKD" )
                    || name.toLowerCase().matches( "b635[\\s_\\.](mean|median)" )
                    || name.equals( "BACKGROUND_CHANNEL 2MEDIAN" ) || name.equals( "R_BG_MEDIAN" )
                    || name.equals( "Ch2BkgMedian" ) || name.equals( "ch2.Background" ) || name.equals( "CH2_BKG_MEAN" )
                    || name.equals( "CH2_BKD_ Median" ) ) {
                backgroundChannelB = qType;
            } else if ( name.matches( "CH1(I)?_MEDIAN" ) || name.matches( "CH1(I)?_MEAN" ) || name.equals( "RAW_DATA" )
                    || name.toLowerCase().matches( "f532[\\s_\\.](mean|median)" )
                    || name.equals( "SIGNAL_CHANNEL 1MEDIAN" ) || name.toLowerCase().matches( "ch1_smtm" )
                    || name.equals( "G_MEAN" ) || name.equals( "Ch1SigMedian" ) || name.equals( "ch1.Intensity" )
                    || name.equals( "CH1_SIG_MEAN" ) || name.equals( "CH1_ Median" ) 
                    || name.toUpperCase().matches("\\w{2}\\d{3}_CY3")) {
                signalChannelA = qType;
            } else if ( name.matches( "CH2(I)?_MEDIAN" ) || name.matches( "CH2(I)?_MEAN" )
                    || name.equals( "RAW_CONTROL" ) || name.toLowerCase().matches( "f635[\\s_\\.](mean|median)" )
                    || name.equals( "SIGNAL_CHANNEL 2MEDIAN" ) || name.toLowerCase().matches( "ch2_smtm" )
                    || name.equals( "R_MEAN" ) || name.equals( "Ch2SigMedian" ) || name.equals( "ch2.Intensity" )
                    || name.equals( "CH2_SIG_MEAN" ) || name.equals( "CH2_ Median" ) 
                    || name.toUpperCase().matches("\\w{2}\\d{3}_CY5")) {
                signalChannelB = qType;
            } else if ( name.matches( "CH1D_MEAN" ) ) {
                bkgSubChannelA = qType; // specific for SGD data bug
            }
            if ( signalChannelA != null && signalChannelB != null && backgroundChannelA != null
                    && backgroundChannelB != null && preferred != null ) {
                break; // no need to go through them all.
            }
        }

        boolean channelANeedsReconstruction = false;
        if ( signalChannelA == null || signalChannelB == null ) {

            /*
             * Okay, this can happen for some Stanford data sets where the CH1 data was not submitted. But we can
             * sometimes reconstruct the values from the background
             */

            if ( signalChannelB != null && bkgSubChannelA != null && backgroundChannelA != null ) {
                log.info( "Invoking work-around for missing channel 1 intensities" );
                channelANeedsReconstruction = true;
            } else {
                throw new IllegalStateException( "Could not find signals for both channels: " + "Channel A ="
                        + signalChannelA + ", Channel B=" + signalChannelB );
            }
        }

        if ( preferred == null ) {
            throw new IllegalStateException( "No preferred quantitation type for data set" );
        }

        if ( backgroundChannelA == null || backgroundChannelB == null ) {
            log.warn( "No background values found, proceeding with raw signals" );
        }

        for ( BioAssayDimension bioAssayDimension : dimensions ) {

            ExpressionDataDoubleMatrix preferredDAta = new ExpressionDataDoubleMatrix( expExp, bioAssayDimension,
                    preferred );

            ExpressionDataDoubleMatrix bkgDataA = null;
            if ( backgroundChannelA != null ) {
                bkgDataA = new ExpressionDataDoubleMatrix( expExp, bioAssayDimension, backgroundChannelA );
            }

            ExpressionDataDoubleMatrix bkgDataB = null;
            if ( backgroundChannelB != null ) {
                bkgDataB = new ExpressionDataDoubleMatrix( expExp, bioAssayDimension, backgroundChannelB );
            }

            ExpressionDataDoubleMatrix signalDataA = null;
            if ( channelANeedsReconstruction ) {
                // use background-subtracted data and add bkg back on later.
                assert bkgDataA != null;
                assert bkgSubChannelA != null;
                signalDataA = new ExpressionDataDoubleMatrix( expExp, bioAssayDimension, bkgSubChannelA );
            } else if ( signalChannelA != null ) {
                signalDataA = new ExpressionDataDoubleMatrix( expExp, bioAssayDimension, signalChannelA );
            }

            ExpressionDataDoubleMatrix signalDataB = null;
            if ( signalChannelB != null ) {
                signalDataB = new ExpressionDataDoubleMatrix( expExp, bioAssayDimension, signalChannelB );
            }

            Collection<DesignElementDataVector> dimRes = computeMissingValues( expExp, bioAssayDimension,
                    preferredDAta, signalDataA, signalDataB, bkgDataA, bkgDataB, signalToNoiseThreshold,
                    channelANeedsReconstruction );

            finalResults.addAll( dimRes );
        }

        return finalResults;

    }

    /**
     * @param source
     * @param bioAssayDimension
     * @param preferred
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
            BioAssayDimension bioAssayDimension, ExpressionDataDoubleMatrix preferred,
            ExpressionDataDoubleMatrix signalChannelA, ExpressionDataDoubleMatrix signalChannelB,
            ExpressionDataDoubleMatrix bkgChannelA, ExpressionDataDoubleMatrix bkgChannelB,
            double signalToNoiseThreshold, boolean channelANeedsReconstruction ) {

        validate( preferred, signalChannelA, signalChannelB, bkgChannelA, bkgChannelB, signalToNoiseThreshold );

        ByteArrayConverter converter = new ByteArrayConverter();
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        QuantitationType present = getQuantitationType( signalToNoiseThreshold );

        int count = 0;
        for ( DesignElement designElement : signalChannelA.getRowElements() ) {
            DesignElementDataVector vect = DesignElementDataVector.Factory.newInstance();
            vect.setQuantitationType( present );
            vect.setExpressionExperiment( source );
            vect.setDesignElement( designElement );
            vect.setBioAssayDimension( bioAssayDimension );

            boolean[] detectionCalls = new boolean[preferred.columns()];
            Double[] prefRow = preferred.getRow( designElement );
            Double[] signalA = signalChannelA != null ? signalChannelA.getRow( designElement ) : null;
            Double[] signalB = signalChannelB != null ? signalChannelB.getRow( designElement ) : null;
            Double[] bkgA = null;
            Double[] bkgB = null;

            if ( bkgChannelA != null ) bkgA = bkgChannelA.getRow( designElement );

            if ( bkgChannelB != null ) bkgB = bkgChannelB.getRow( designElement );

            for ( int col = 0; col < prefRow.length; col++ ) {

                // If the "preferred" value is already missing, we retain that.
                Double pref = prefRow[col];
                if ( pref == null || pref.isNaN() ) {
                    detectionCalls[col] = false;
                    continue;
                }

                Double bkgAV = 0.0;
                Double bkgBV = 0.0;

                if ( bkgA != null ) bkgAV = bkgA[col];

                if ( bkgB != null ) bkgBV = bkgB[col];

                Double sigAV = signalA[col] == null ? 0.0 : signalA[col];

                /*
                 * Put the background value back on.
                 */
                if ( channelANeedsReconstruction ) {
                    sigAV = sigAV + bkgAV;
                }
                Double sigBV = signalB[col] == null ? 0.0 : signalB[col];

                boolean call = computeCall( signalToNoiseThreshold, sigAV, sigBV, bkgAV, bkgBV );
                detectionCalls[col] = call;
            }

            vect.setData( converter.booleanArrayToBytes( detectionCalls ) );
            results.add( vect );

            if ( ++count % 4000 == 0 ) {
                log.info( count + " vectors examined for missing values" );
            }

        }
        log.info( "Finished: " + count + " vectors examined for missing values" );
        return results;
    }

    private boolean computeCall( double signalToNoiseThreshold, Double sigAV, Double sigBV, Double bkgAV, Double bkgBV ) {
        if ( ( sigAV == null && sigBV == null ) || ( sigAV.isNaN() && sigBV.isNaN() ) ) return false;
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
     * @param preferred
     * @param signalChannelA
     * @param signalChannelB
     * @param bkgChannelA
     * @param bkgChannelB
     * @param signalToNoiseThreshold
     */
    private void validate( ExpressionDataDoubleMatrix preferred, ExpressionDataDoubleMatrix signalChannelA,
            ExpressionDataDoubleMatrix signalChannelB, ExpressionDataDoubleMatrix bkgChannelA,
            ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold ) {
        // not exhaustive...
        if ( preferred == null || signalChannelA == null || signalChannelB == null ) {
            throw new IllegalArgumentException( "Must have data matrices" );
        }

        if ( ( bkgChannelA != null && bkgChannelA.rows() == 0 ) || ( bkgChannelB != null && bkgChannelB.rows() == 0 ) ) {
            throw new IllegalArgumentException( "Background values must not be empty when non-null" );
        }

        if ( !( signalChannelA.rows() == signalChannelB.rows() ) ) {
            throw new IllegalArgumentException( "Collection sizes must match" );
        }

        if ( !( signalChannelA.rows() == preferred.rows() ) ) {
            throw new IllegalArgumentException( "Collection sizes must match" );
        }

        if ( ( bkgChannelA != null && bkgChannelB != null ) && bkgChannelA.rows() != bkgChannelB.rows() )
            throw new IllegalArgumentException( "Collection sizes must match" );

        if ( signalToNoiseThreshold <= 0.0 ) {
            throw new IllegalArgumentException( "Signal-to-noise threshold must be greater than zero" );
        }

        int numSamplesA = signalChannelA.columns();
        int numSamplesB = signalChannelB.columns();

        if ( numSamplesA != numSamplesB || numSamplesB != preferred.columns() ) {
            throw new IllegalArgumentException( "Number of samples doesn't match!" );
        }

    }
}
