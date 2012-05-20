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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.Constants;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Computes a missing value matrix for ratiometric data sets.
 * <p>
 * Supported formats and special cases:
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
 * <li>GSE1501 (NormCH2)
 * </ul>
 * <p>
 * The missing values are computed with the following considerations with respect to available data
 * </p>
 * <ol>
 * <li>If the preferred quantitation type data is a missing value, then the data are considered missing (for
 * consistency).</li>
 * <li>We then do additional checks if there is 'signal' data available.
 * <li>If there are background values, they are used to compute signal-to-noise ratios</li>
 * <li>If the signal values already contain missing data, these are still considered missing.</li>
 * <li>If there are no background values, we try to compute a threshold based on a quantile of the signal</li>
 * <li>Otherwise, values will be considered 'present' unless the signal values are zero or missing.</li>
 * </ol>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class TwoChannelMissingValuesImpl implements TwoChannelMissingValues {

    private static final int QUANTILE_OF_SIGNAL_TO_USE_IF_NO_BKG_AVAILABLE = 1;

    private static Log log = LogFactory.getLog( TwoChannelMissingValuesImpl.class.getName() );


    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private TwoChannelMissingValueHelperService twoChannelMissingValueHelperService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.TwoChannelMissingValues#computeMissingValues(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment)
     */
    @Override
    public Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment expExp ) {
        return this.computeMissingValues( expExp, DEFAULT_SIGNAL_TO_NOISE_THRESHOLD, null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.TwoChannelMissingValues#computeMissingValues(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, double, java.util.Collection)
     */
    @Override
    public Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment expExp,
            double signalToNoiseThreshold, Collection<Double> extraMissingValueIndicators ) {

        expExp = expressionExperimentService.thawLite( expExp );
        Collection<QuantitationType> usefulQuantitationTypes = ExpressionDataMatrixBuilder
                .getUsefulQuantitationTypes( expExp );
        StopWatch timer = new StopWatch();
        timer.start();
        log.info( "Loading vectors ..." );
        Collection<DesignElementDataVector> vectors = expressionExperimentService
                .getDesignElementDataVectors( usefulQuantitationTypes );

        timer.stop();
        logTimeInfo( timer, vectors );

        designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
        Collection<BioAssayDimension> dims = builder.getBioAssayDimensions();
        Collection<RawExpressionDataVector> finalResults = new HashSet<RawExpressionDataVector>();

        /*
         * Note we have to do this one array design at a time, because we are producing DesignElementDataVectors which
         * must be associated with the correct BioAssayDimension.
         */
        log.info( "Study has " + dims.size() + " bioassaydimensions" );
        Collection<BioAssay> bioAssays = expExp.getBioAssays();
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        for ( BioAssay ba : bioAssays ) {
            ads.add( ba.getArrayDesignUsed() );
        }

        if ( extraMissingValueIndicators != null && extraMissingValueIndicators.size() > 0 ) {
            log.info( "There are " + extraMissingValueIndicators.size() + " manually-set missing value indicators" );
        }

        ExpressionDataDoubleMatrix preferredData = builder.getPreferredData();
        ExpressionDataDoubleMatrix bkgDataA = builder.getBackgroundChannelA();
        ExpressionDataDoubleMatrix bkgDataB = builder.getBackgroundChannelB();
        ExpressionDataDoubleMatrix signalDataA = builder.getSignalChannelA();
        ExpressionDataDoubleMatrix signalDataB = builder.getSignalChannelB();

        if ( builder.isAnyMissing() ) {
            if ( bkgDataA != null ) {
                for ( QuantitationType qt : bkgDataA.getQuantitationTypes() ) {
                    if ( builder.getNumMissingValues( qt ) > 0 ) {
                        log.warn( "Missing values in bkgDataA" );
                        break;
                    }
                }
            }
            if ( bkgDataB != null ) {
                for ( QuantitationType qt : bkgDataB.getQuantitationTypes() ) {
                    if ( builder.getNumMissingValues( qt ) > 0 ) {
                        log.warn( "Missing values in bkgDataB" );
                        break;
                    }
                }
            }
            if ( signalDataA != null ) {
                for ( QuantitationType qt : signalDataA.getQuantitationTypes() ) {
                    if ( builder.getNumMissingValues( qt ) > 0 ) {
                        log.warn( "Missing values in signalDataA" );
                        break;
                    }
                }
            }
            if ( signalDataB != null ) {
                for ( QuantitationType qt : signalDataB.getQuantitationTypes() ) {
                    if ( builder.getNumMissingValues( qt ) > 0 ) {
                        log.warn( "Missing values in signalDataB" );
                        break;
                    }
                }
            }
        }

        Collection<RawExpressionDataVector> dimRes = computeMissingValues( expExp, preferredData, signalDataA,
                signalDataB, bkgDataA, bkgDataB, signalToNoiseThreshold, extraMissingValueIndicators );

        finalResults.addAll( dimRes );

        return finalResults;
    }

    /**
     * Attempt to compute 'missing value' information for a two-channel data set. We attempt to do this even if we are
     * missing background intensity information or one intensity channel, though obviously it is better to have all four
     * sets of values.
     * 
     * @param source
     * @param preferred
     * @param signalChannelA
     * @param signalChannelB
     * @param bkgChannelA
     * @param bkgChannelB
     * @param signalToNoiseThreshold
     * @param extraMissingValueIndicators
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the design elements and
     *         biomaterial dimension represented in the inputs.
     * @see computeMissingValues( ExpressionExperiment expExp, double signalToNoiseThreshold )
     */
    protected Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment source,
            ExpressionDataDoubleMatrix preferred, ExpressionDataDoubleMatrix signalChannelA,
            ExpressionDataDoubleMatrix signalChannelB, ExpressionDataDoubleMatrix bkgChannelA,
            ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold,
            Collection<Double> extraMissingValueIndicators ) {

        boolean okToProceed = validate( preferred, signalChannelA, signalChannelB, bkgChannelA, bkgChannelB,
                signalToNoiseThreshold );
        Collection<RawExpressionDataVector> results = new HashSet<RawExpressionDataVector>();

        if ( !okToProceed ) {
            log.warn( "Missing value computation cannot proceed" );
            return results;
        }

        ByteArrayConverter converter = new ByteArrayConverter();

        int count = 0;

        ExpressionDataDoubleMatrix baseChannel = signalChannelA == null ? signalChannelB : signalChannelA;

        Double signalThreshold = Double.NaN;
        if ( bkgChannelA == null && bkgChannelB == null ) {
            signalThreshold = computeSignalThreshold( preferred, signalChannelA, signalChannelB, baseChannel );
        }
        QuantitationType present = getMissingDataQuantitationType( signalToNoiseThreshold, signalThreshold );
        source.getQuantitationTypes().add( present );
        for ( ExpressionDataMatrixRowElement element : baseChannel.getRowElements() ) {

            CompositeSequence designElement = element.getDesignElement();

            RawExpressionDataVector vect = RawExpressionDataVector.Factory.newInstance();
            vect.setQuantitationType( present );
            vect.setExpressionExperiment( source );
            vect.setDesignElement( designElement );
            assert baseChannel != null;
            vect.setBioAssayDimension( baseChannel.getBioAssayDimension( designElement ) );

            int numCols = preferred.columns( designElement );

            Boolean[] detectionCalls = new Boolean[numCols];
            Double[] prefRow = preferred.getRow( designElement );

            Double[] signalA = null;
            if ( signalChannelA != null ) {
                signalA = signalChannelA.getRow( designElement );
            }

            Double[] signalB = null;
            if ( signalChannelB != null ) {
                signalB = signalChannelB.getRow( designElement );
            }
            Double[] bkgA = null;
            Double[] bkgB = null;

            if ( bkgChannelA != null ) bkgA = bkgChannelA.getRow( designElement );

            if ( bkgChannelB != null ) bkgB = bkgChannelB.getRow( designElement );

            // columns only for this designelement!
            boolean gaps = false; // we use this to track
            for ( int col = 0; col < numCols; col++ ) {

                // If the "preferred" value is already missing, we retain that, or if it is a special value
                Double pref = prefRow == null ? Double.NaN : prefRow[col];
                if ( pref == null || pref.isNaN()
                        || ( extraMissingValueIndicators != null && extraMissingValueIndicators.contains( pref ) ) ) {
                    detectionCalls[col] = false;
                    continue;
                }

                Double bkgAV = Double.NaN;
                Double bkgBV = Double.NaN;

                if ( bkgA != null ) bkgAV = bkgA[col];
                if ( bkgB != null ) bkgBV = bkgB[col];

                Double sigAV = ( signalA == null || signalA[col] == null ) ? Double.NaN : signalA[col];
                Double sigBV = ( signalB == null || signalB[col] == null ) ? Double.NaN : signalB[col];

                /*
                 * Missing values here wreak havoc. Sometimes in multiarray studies data are missing.
                 */
                Boolean call = computeCall( signalToNoiseThreshold, signalThreshold, sigAV, sigBV, bkgAV, bkgBV );

                if ( call == null ) gaps = true;

                detectionCalls[col] = call;
            }

            if ( gaps ) {
                fillGapsInCalls( detectionCalls );
            }

            vect.setData( converter.booleanArrayToBytes( ArrayUtils.toPrimitive( detectionCalls ) ) );
            results.add( vect );

            if ( ++count % 4000 == 0 ) {
                log.info( count + " vectors examined for missing values, " + results.size()
                        + " vectors generated so far." );
            }

        }
        log.info( "Finished: " + count + " vectors examined for missing values" );

        results = twoChannelMissingValueHelperService.persist( source, results );

        return results;
    }


    /**
     * Determine a threshold based on the data.
     * 
     * @param preferred
     * @param signalChannelA
     * @param signalChannelB
     * @param baseChannel
     */
    private Double computeSignalThreshold( ExpressionDataDoubleMatrix preferred,
            ExpressionDataDoubleMatrix signalChannelA, ExpressionDataDoubleMatrix signalChannelB,
            ExpressionDataDoubleMatrix baseChannel ) {

        Double min = Double.MAX_VALUE;
        Double max = Double.MIN_VALUE;

        for ( ExpressionDataMatrixRowElement element : baseChannel.getRowElements() ) {
            CompositeSequence designElement = element.getDesignElement();

            int numCols = preferred.columns( designElement );
            for ( int col = 0; col < numCols; col++ ) {

                Double[] signalA = null;
                if ( signalChannelA != null ) {
                    signalA = signalChannelA.getRow( designElement );
                }

                Double[] signalB = null;
                if ( signalChannelB != null ) {
                    signalB = signalChannelB.getRow( designElement );
                }

                Double sigAV = ( signalA == null || signalA[col] == null ) ? Double.NaN : signalA[col];
                Double sigBV = ( signalB == null || signalB[col] == null ) ? Double.NaN : signalB[col];

                if ( !sigAV.isNaN() && sigAV < min ) {
                    min = sigAV;
                } else if ( !sigBV.isNaN() && sigBV < min ) {
                    min = sigBV;
                } else if ( !sigAV.isNaN() && sigAV > max ) {
                    max = sigAV;
                } else if ( !sigBV.isNaN() && sigBV > max ) {
                    max = sigBV;
                }

            }
        }

        Histogram h = new Histogram( "range", 100, min, max );
        for ( ExpressionDataMatrixRowElement element : baseChannel.getRowElements() ) {
            CompositeSequence designElement = element.getDesignElement();

            int numCols = preferred.columns( designElement );
            for ( int col = 0; col < numCols; col++ ) {

                Double[] signalA = null;
                if ( signalChannelA != null ) {
                    signalA = signalChannelA.getRow( designElement );
                }

                Double[] signalB = null;
                if ( signalChannelB != null ) {
                    signalB = signalChannelB.getRow( designElement );
                }

                Double sigAV = ( signalA == null || signalA[col] == null ) ? Double.NaN : signalA[col];
                Double sigBV = ( signalB == null || signalB[col] == null ) ? Double.NaN : signalB[col];

                if ( !sigAV.isNaN() ) h.fill( sigAV );
                if ( !sigBV.isNaN() ) h.fill( sigBV );

            }
        }

        Double thresh = h.getApproximateQuantile( QUANTILE_OF_SIGNAL_TO_USE_IF_NO_BKG_AVAILABLE );

        log.info( "Threshold based on signal=" + thresh );

        return thresh;
    }

    /**
     * Deal with cases when the data we are using to make calls are themselves missing. Note that in other cases where
     * we can't compute missingness at all, we assume everything is present.
     * 
     * @param detectionCalls
     */
    private void fillGapsInCalls( Boolean[] detectionCalls ) {
        /*
         * Make a decision on those.
         */
        // double fractionThreshold = 0.5; // if half of calls we made are present, we call gaps pesent
        double fractionThreshold = 0.0; // call all gaps present, unless everything in the row is absent (or missing)
        int numPresentCall = 0;
        int numCalls = 0;
        for ( Boolean b : detectionCalls ) {
            if ( b != null ) {
                if ( b ) numPresentCall++;
                numCalls++;
            }
        }
        boolean decide = numCalls > 0 && numPresentCall / ( double ) numCalls > fractionThreshold;

        for ( int i = 0; i < detectionCalls.length; i++ ) {
            if ( detectionCalls[i] == null ) detectionCalls[i] = decide;
        }
    }

    /**
     * Decide if the data point is 'present': it has to be above the threshold in one of the channels.
     * 
     * @param signalToNoiseThreshold
     * @param signalThreshold might be used if we don't have background measurements.
     * @param sigAV
     * @param sigBV
     * @param bkgAV can be null
     * @param bkgBV can be null
     * @return call, or null if no decision could be made due to NaN in the values given.
     */
    private Boolean computeCall( double signalToNoiseThreshold, Double signalThreshold, Double sigAV, Double sigBV,
            Double bkgAV, Double bkgBV ) {

        if ( !sigAV.isNaN() && !bkgAV.isNaN() && sigAV > bkgAV * signalToNoiseThreshold ) return true;

        if ( !sigBV.isNaN() && !bkgBV.isNaN() && sigBV > bkgBV * signalToNoiseThreshold ) return true;

        // if no background valeues, use the signal threshold, if we have one; both values must meet.
        if ( !Double.isNaN( signalThreshold ) && bkgAV.isNaN() && bkgBV.isNaN() ) {
            return ( sigAV > signalThreshold || sigBV > signalThreshold );
        }

        // if both signals are unusable, false.
        if ( ( sigAV.isNaN() || sigAV == 0 ) && ( sigBV.isNaN() || sigBV == 0 ) ) {
            return false;
        }

        /*
         * We couldn't decide because none of the above calculations could be done.
         */
        if ( ( sigAV.isNaN() || bkgAV.isNaN() ) && ( sigBV.isNaN() || bkgBV.isNaN() ) ) return null;

        // default: keep.
        return true;
    }

    /**
     * Construct the quantitation type that will be used for the generated DesignElementDataVEctors.
     * 
     * @param signalToNoiseThreshold
     * @param signalThreshold
     * @return
     */
    private QuantitationType getMissingDataQuantitationType( double signalToNoiseThreshold, Double signalThreshold ) {
        QuantitationType present = QuantitationType.Factory.newInstance();
        present.setName( "Detection call" );

        if ( !signalThreshold.isNaN() ) {
            present.setDescription( "Detection call based on signal threshold of " + signalThreshold + " (Computed by "
                    + Constants.APP_NAME + ")" );
        } else {
            present.setDescription( "Detection call based on signal to noise threshold of " + signalToNoiseThreshold
                    + " (Computed by " + Constants.APP_NAME + ")" );
        }
        present.setGeneralType( GeneralType.CATEGORICAL );
        present.setIsBackground( false );
        present.setRepresentation( PrimitiveType.BOOLEAN );
        present.setScale( ScaleType.OTHER );
        present.setIsPreferred( false );
        present.setIsMaskedPreferred( false );
        present.setIsBackgroundSubtracted( false );
        present.setIsNormalized( false );
        present.setIsRatio( false );
        present.setType( StandardQuantitationType.PRESENTABSENT );
        return this.quantitationTypeService.create( present );
    }

    private void logTimeInfo( StopWatch timer, Collection<DesignElementDataVector> items ) {
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setMaximumFractionDigits( 2 );
        log.info( "Loaded in " + nf.format( timer.getTime() / 1000 ) + "s. Thawing " + items.size() + " vectors" );
    }

    /**
     * Check to make sure all the pieces are correctly in place to do the computation.
     * 
     * @param preferred
     * @param signalChannelA
     * @param signalChannelB
     * @param bkgChannelA
     * @param bkgChannelB
     * @param signalToNoiseThreshold
     * @return true if okay, false if not.
     */
    private boolean validate( ExpressionDataDoubleMatrix preferred, ExpressionDataDoubleMatrix signalChannelA,
            ExpressionDataDoubleMatrix signalChannelB, ExpressionDataDoubleMatrix bkgChannelA,
            ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold ) {
        // not exhaustive...
        if ( preferred == null || ( signalChannelA == null && signalChannelB == null ) ) {
            log.warn( "Must have at least preferred and one intensity data matrix, missing value computation should not proceed" );
            return false;
        }

        if ( ( bkgChannelA != null && bkgChannelA.rows() == 0 ) || ( bkgChannelB != null && bkgChannelB.rows() == 0 ) ) {
            log.warn( "Background values must not be empty when non-null" );
            return false;
        }

        if ( signalChannelA != null && signalChannelB != null ) {
            if ( !( signalChannelA.rows() == signalChannelB.rows() ) ) {
                log.warn( "Collection sizes probably should match in channel A and B " + signalChannelA.rows() + " != "
                        + signalChannelB.rows() );
            }

            if ( !( signalChannelA.rows() == preferred.rows() ) ) { // vectors with all-missing data are already removed
                log.warn( "Collection sizes probably should match in channel A and preferred type "
                        + signalChannelA.rows() + " != " + preferred.rows() );
            }
            int numSamplesA = signalChannelA.columns();
            int numSamplesB = signalChannelB.columns();

            if ( numSamplesA != numSamplesB || numSamplesB != preferred.columns() ) {
                log.warn( "Number of samples doesn't match!" );
                return false;
            }
        }

        if ( ( bkgChannelA != null && bkgChannelB != null ) && bkgChannelA.rows() != bkgChannelB.rows() )
            log.warn( "Collection sizes probably should match for background  " + bkgChannelA.rows() + " != "
                    + bkgChannelB.rows() );

        if ( signalToNoiseThreshold <= 0.0 ) {
            log.warn( "Signal-to-noise threshold must be greater than zero" );
            return false;
        }

        return true;

    }

}
