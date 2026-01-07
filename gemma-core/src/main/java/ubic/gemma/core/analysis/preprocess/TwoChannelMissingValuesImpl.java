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
package ubic.gemma.core.analysis.preprocess;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.core.datastructure.matrix.TwoChannelExpressionDataMatrixBuilder;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;

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
 */
@Component
public class TwoChannelMissingValuesImpl implements TwoChannelMissingValues {

    private static final int QUANTILE_OF_SIGNAL_TO_USE_IF_NO_BKG_AVAILABLE = 1;
    private static final Log log = LogFactory.getLog( TwoChannelMissingValuesImpl.class.getName() );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional
    public Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment ee ) {
        return this.computeMissingValues( ee, TwoChannelMissingValues.DEFAULT_SIGNAL_TO_NOISE_THRESHOLD, null );
    }

    @Override
    @Transactional
    public Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment ee,
            double signalToNoiseThreshold, @Nullable Collection<Double> extraMissingValueIndicators ) {

        ee = expressionExperimentService.thawLite( ee );
        Collection<QuantitationType> usefulQuantitationTypes = TwoChannelExpressionDataMatrixBuilder
                .getUsefulQuantitationTypes( ee );

        // check that we don't already have a missing value QT
        for ( QuantitationType qt : usefulQuantitationTypes ) {
            if ( StandardQuantitationType.PRESENTABSENT.equals( qt.getType() ) ) {
                log.warn( "This experiment already has a missing value quantitation type, no action will be taken and empty collection returned" );
                return new HashSet<>();
            }
        }


        StopWatch timer = new StopWatch();
        timer.start();
        TwoChannelMissingValuesImpl.log.info( "Loading vectors ..." );

        Collection<RawExpressionDataVector> rawVectors = rawExpressionDataVectorService.findAndThaw( usefulQuantitationTypes );
        Collection<ProcessedExpressionDataVector> procVectors = new HashSet<>();

        if ( rawVectors.isEmpty() ) {
            procVectors = processedExpressionDataVectorService.findAndThaw( usefulQuantitationTypes );
        }

        timer.stop();
        this.logTimeInfo( timer, procVectors.size() + rawVectors.size() );

        Collection<? extends BulkExpressionDataVector> builderVectors = new HashSet<>(
                rawVectors.isEmpty() ? procVectors : rawVectors );

        TwoChannelExpressionDataMatrixBuilder builder = new TwoChannelExpressionDataMatrixBuilder( builderVectors );
        Collection<BioAssayDimension> dims = builder.getBioAssayDimensions();

        /*
         * Note we have to do this one array design at a time, because we are producing DesignElementDataVectors which
         * must be associated with the correct BioAssayDimension.
         */
        TwoChannelMissingValuesImpl.log.info( "Study has " + dims.size() + " bioassaydimensions" );

        if ( extraMissingValueIndicators != null && !extraMissingValueIndicators.isEmpty() ) {
            TwoChannelMissingValuesImpl.log.info( "There are " + extraMissingValueIndicators.size()
                    + " manually-set missing value indicators" );
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
                        TwoChannelMissingValuesImpl.log.warn( "Missing values in bkgDataA" );
                        break;
                    }
                }
            }
            if ( bkgDataB != null ) {
                for ( QuantitationType qt : bkgDataB.getQuantitationTypes() ) {
                    if ( builder.getNumMissingValues( qt ) > 0 ) {
                        TwoChannelMissingValuesImpl.log.warn( "Missing values in bkgDataB" );
                        break;
                    }
                }
            }
            if ( signalDataA != null ) {
                for ( QuantitationType qt : signalDataA.getQuantitationTypes() ) {
                    if ( builder.getNumMissingValues( qt ) > 0 ) {
                        TwoChannelMissingValuesImpl.log.warn( "Missing values in signalDataA" );
                        break;
                    }
                }
            }
            if ( signalDataB != null ) {
                for ( QuantitationType qt : signalDataB.getQuantitationTypes() ) {
                    if ( builder.getNumMissingValues( qt ) > 0 ) {
                        TwoChannelMissingValuesImpl.log.warn( "Missing values in signalDataB" );
                        break;
                    }
                }
            }
        }

        Collection<RawExpressionDataVector> dimRes = this
                .computeMissingValues( ee, preferredData, signalDataA, signalDataB, bkgDataA, bkgDataB,
                        signalToNoiseThreshold, extraMissingValueIndicators );

        return new HashSet<>( dimRes );
    }

    /**
     * Attempt to compute 'missing value' information for a two-channel data set. We attempt to do this even if we are
     * missing background intensity information or one intensity channel, though obviously it is better to have all four
     * sets of values.
     *
     * @param bkgChannelA                 background channel A
     * @param bkgChannelB                 background channel B
     * @param extraMissingValueIndicators extra missing value indicators
     * @param preferred                   preferred matrix
     * @param signalChannelA              signal channel A
     * @param signalChannelB              signal channel B
     * @param signalToNoiseThreshold      noise threshold
     * @param source                      the source
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the design elements and
     * biomaterial dimension represented in the inputs.
     */
    private Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment source,
            ExpressionDataDoubleMatrix preferred, @Nullable ExpressionDataDoubleMatrix signalChannelA,
            @Nullable ExpressionDataDoubleMatrix signalChannelB, @Nullable ExpressionDataDoubleMatrix bkgChannelA,
            @Nullable ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold,
            @Nullable Collection<Double> extraMissingValueIndicators ) {

        boolean okToProceed = this.validate( preferred, signalChannelA, signalChannelB, bkgChannelA, bkgChannelB,
                signalToNoiseThreshold );
        Collection<RawExpressionDataVector> results = new HashSet<>();

        if ( !okToProceed ) {
            TwoChannelMissingValuesImpl.log.warn( "Missing value computation cannot proceed" );
            return results;
        }

        int count = 0;

        ExpressionDataDoubleMatrix baseChannel = signalChannelA == null ? signalChannelB : signalChannelA;

        if ( baseChannel == null ) {
            throw new IllegalArgumentException( "At least one signal channel must be provided." );
        }

        Double signalThreshold = Double.NaN;
        if ( bkgChannelA == null && bkgChannelB == null ) {
            signalThreshold = this.computeSignalThreshold( preferred, signalChannelA, signalChannelB, baseChannel );
        }
        QuantitationType present = this.getMissingDataQuantitationType( signalToNoiseThreshold, signalThreshold );
        source.getQuantitationTypes().add( present );
        for ( ExpressionDataMatrixRowElement element : baseChannel.getRowElements() ) {
            count = this.examineVector( source, preferred, signalChannelA, signalChannelB, bkgChannelA, bkgChannelB,
                    signalToNoiseThreshold, extraMissingValueIndicators, results, count, baseChannel,
                    signalThreshold, present, element );

        }
        TwoChannelMissingValuesImpl.log.info( "Finished: " + count + " vectors examined for missing values" );

        log.info( "Persisting " + results.size() + " vectors... " );
        // saving twice is needed to get the QT filled in properly. ??Why??
        source = expressionExperimentService.save( source );
        source.getRawExpressionDataVectors().addAll( results );
        source = expressionExperimentService.save( source );
        auditTrailService.addUpdateEvent( source, MissingValueAnalysisEvent.class,
                "Computed missing value data" );

        return results;
    }

    private int examineVector( ExpressionExperiment source, ExpressionDataDoubleMatrix preferred,
            @Nullable ExpressionDataDoubleMatrix signalChannelA, @Nullable ExpressionDataDoubleMatrix signalChannelB,
            @Nullable ExpressionDataDoubleMatrix bkgChannelA, @Nullable ExpressionDataDoubleMatrix bkgChannelB,
            double signalToNoiseThreshold, @Nullable Collection<Double> extraMissingValueIndicators,
            Collection<RawExpressionDataVector> results, int count,
            ExpressionDataDoubleMatrix baseChannel, Double signalThreshold, QuantitationType present,
            ExpressionDataMatrixRowElement element ) {
        CompositeSequence designElement = element.getDesignElement();

        RawExpressionDataVector vect = RawExpressionDataVector.Factory.newInstance();
        vect.setQuantitationType( present );
        vect.setExpressionExperiment( source );
        vect.setDesignElement( designElement );
        vect.setBioAssayDimension( baseChannel.getBioAssayDimension( designElement ) );

        int numCols = baseChannel.columns( designElement );

        Boolean[] detectionCalls = new Boolean[numCols];
        double[] prefRow = preferred.getRowAsDoubles( designElement );

        double[] signalA = null;
        if ( signalChannelA != null ) {
            signalA = signalChannelA.getRowAsDoubles( designElement );
        }

        double[] signalB = null;
        if ( signalChannelB != null ) {
            signalB = signalChannelB.getRowAsDoubles( designElement );
        }
        double[] bkgA = null;
        double[] bkgB = null;

        if ( bkgChannelA != null )
            bkgA = bkgChannelA.getRowAsDoubles( designElement );

        if ( bkgChannelB != null )
            bkgB = bkgChannelB.getRowAsDoubles( designElement );

        // columns only for this design element!
        boolean gaps = false; // we use this to track
        for ( int col = 0; col < numCols; col++ ) {

            if ( this.checkMissingValue( extraMissingValueIndicators, detectionCalls, prefRow, col ) )
                continue;

            double bkgAV = Double.NaN;
            double bkgBV = Double.NaN;

            if ( bkgA != null )
                bkgAV = bkgA[col];
            if ( bkgB != null )
                bkgBV = bkgB[col];

            double sigAV = signalA == null ? Double.NaN : signalA[col];
            double sigBV = signalB == null ? Double.NaN : signalB[col];

            /*
             * Missing values here wreak havoc. Sometimes in multiarray studies data are missing.
             */
            Boolean call = this.computeCall( signalToNoiseThreshold, signalThreshold, sigAV, sigBV, bkgAV, bkgBV );

            if ( call == null )
                gaps = true;

            detectionCalls[col] = call;
        }

        if ( gaps ) {
            this.fillGapsInCalls( detectionCalls );
        }

        vect.setDataAsBooleans( ArrayUtils.toPrimitive( detectionCalls ) );
        results.add( vect );

        if ( ++count % 4000 == 0 ) {
            TwoChannelMissingValuesImpl.log.info( count + " vectors examined for missing values, " + results.size()
                    + " vectors generated so far." );
        }
        return count;
    }

    private boolean checkMissingValue( @Nullable Collection<Double> extraMissingValueIndicators, Boolean[] detectionCalls,
            @Nullable double[] prefRow, int col ) {
        // If the "preferred" value is already missing, we retain that, or if it is a special value
        double pref = prefRow == null ? Double.NaN : prefRow[col];
        if ( Double.isNaN( pref ) || ( extraMissingValueIndicators != null && extraMissingValueIndicators.contains( pref ) ) ) {
            detectionCalls[col] = false;
            return true;
        }
        return false;
    }

    /**
     * Determine a threshold based on the data.
     */
    private Double computeSignalThreshold( ExpressionDataDoubleMatrix preferred,
            @Nullable ExpressionDataDoubleMatrix signalChannelA, @Nullable ExpressionDataDoubleMatrix signalChannelB,
            ExpressionDataDoubleMatrix baseChannel ) {

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for ( ExpressionDataMatrixRowElement element : baseChannel.getRowElements() ) {
            CompositeSequence designElement = element.getDesignElement();

            int numCols = baseChannel.columns( designElement );
            for ( int col = 0; col < numCols; col++ ) {

                double[] signalA = null;
                if ( signalChannelA != null ) {
                    signalA = signalChannelA.getRowAsDoubles( designElement );
                }

                double[] signalB = null;
                if ( signalChannelB != null ) {
                    signalB = signalChannelB.getRowAsDoubles( designElement );
                }

                double sigAV = signalA == null ? Double.NaN : signalA[col];
                double sigBV = signalB == null ? Double.NaN : signalB[col];

                if ( !Double.isNaN( sigAV ) && sigAV < min ) {
                    min = sigAV;
                } else if ( !Double.isNaN( sigBV ) && sigBV < min ) {
                    min = sigBV;
                } else if ( !Double.isNaN( sigAV ) && sigAV > max ) {
                    max = sigAV;
                } else if ( !Double.isNaN( sigBV ) && sigBV > max ) {
                    max = sigBV;
                }

            }
        }

        Histogram h = new Histogram( "range", 100, min, max );
        for ( ExpressionDataMatrixRowElement element : baseChannel.getRowElements() ) {
            CompositeSequence designElement = element.getDesignElement();

            int numCols = baseChannel.columns( designElement );
            for ( int col = 0; col < numCols; col++ ) {

                double[] signalA = null;
                if ( signalChannelA != null ) {
                    signalA = signalChannelA.getRowAsDoubles( designElement );
                }

                double[] signalB = null;
                if ( signalChannelB != null ) {
                    signalB = signalChannelB.getRowAsDoubles( designElement );
                }

                double sigAV = signalA == null ? Double.NaN : signalA[col];
                double sigBV = signalB == null ? Double.NaN : signalB[col];

                if ( !Double.isNaN( sigAV ) )
                    h.fill( sigAV );
                if ( !Double.isNaN( sigBV ) )
                    h.fill( sigBV );

            }
        }

        Double thresh = h
                .getApproximateQuantile( TwoChannelMissingValuesImpl.QUANTILE_OF_SIGNAL_TO_USE_IF_NO_BKG_AVAILABLE );

        TwoChannelMissingValuesImpl.log.info( "Threshold based on signal=" + thresh );

        return thresh;
    }

    /**
     * Deal with cases when the data we are using to make calls are themselves missing. Note that in other cases where
     * we can't compute missingness at all, we assume everything is present.
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
                if ( b )
                    numPresentCall++;
                numCalls++;
            }
        }
        boolean decide = numCalls > 0 && numPresentCall / ( double ) numCalls > fractionThreshold;

        for ( int i = 0; i < detectionCalls.length; i++ ) {
            if ( detectionCalls[i] == null )
                detectionCalls[i] = decide;
        }
    }

    /**
     * Decide if the data point is 'present': it has to be above the threshold in one of the channels.
     *
     * @param signalThreshold might be used if we don't have background measurements.
     * @param bkgAV           can be null
     * @param bkgBV           can be null
     * @return call, or null if no decision could be made due to NaN in the values given.
     */
    @Nullable
    private Boolean computeCall( double signalToNoiseThreshold, double signalThreshold, double sigAV, double sigBV,
            double bkgAV, double bkgBV ) {

        if ( !Double.isNaN( sigAV ) && !Double.isNaN( bkgAV ) && sigAV > bkgAV * signalToNoiseThreshold )
            return true;

        if ( !Double.isNaN( sigBV ) && !Double.isNaN( bkgBV ) && sigBV > bkgBV * signalToNoiseThreshold )
            return true;

        // if no background valeues, use the signal threshold, if we have one; both values must meet.
        if ( !Double.isNaN( signalThreshold ) && Double.isNaN( bkgAV ) && Double.isNaN( bkgBV ) ) {
            return ( sigAV > signalThreshold || sigBV > signalThreshold );
        }

        // if both signals are unusable, false.
        if ( ( Double.isNaN( sigAV ) || sigAV == 0 ) && ( Double.isNaN( sigBV ) || sigBV == 0 ) ) {
            return false;
        }

        /*
         * We couldn't decide because none of the above calculations could be done.
         */
        if ( ( Double.isNaN( sigAV ) || Double.isNaN( bkgAV ) ) && ( Double.isNaN( sigBV ) || Double.isNaN( bkgBV ) ) )
            return null;

        // default: keep.
        return true;
    }

    /**
     * Construct the quantitation type that will be used for the generated DesignElementDataVEctors.
     */
    private QuantitationType getMissingDataQuantitationType( double signalToNoiseThreshold, Double signalThreshold ) {
        QuantitationType present = QuantitationType.Factory.newInstance();
        present.setName( "Detection call" );

        if ( !signalThreshold.isNaN() ) {
            present.setDescription(
                    "Detection call based on signal threshold of " + signalThreshold + " (Computed by Gemma)" );
        } else {
            present.setDescription( "Detection call based on signal to noise threshold of " + signalToNoiseThreshold
                    + " (Computed by Gemma)" );
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
        present.setIsBatchCorrected( false );
        present.setIsRecomputedFromRawData( false );

        return this.quantitationTypeService.create( present );
    }

    private void logTimeInfo( StopWatch timer, int size ) {

        TwoChannelMissingValuesImpl.log
                .info( String.format( "Loaded in %.2fs. Thawing %d vectors", timer.getTime() / 1000.0, size ) );
    }

    /**
     * Check to make sure all the pieces are correctly in place to do the computation.
     *
     * @return true if okay, false if not.
     */
    private boolean validate( @Nullable ExpressionDataDoubleMatrix preferred, @Nullable ExpressionDataDoubleMatrix signalChannelA,
            @Nullable ExpressionDataDoubleMatrix signalChannelB, @Nullable ExpressionDataDoubleMatrix bkgChannelA,
            @Nullable ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold ) {
        // not exhaustive...
        if ( preferred == null || ( signalChannelA == null && signalChannelB == null ) ) {
            TwoChannelMissingValuesImpl.log
                    .warn( "Must have at least preferred and one intensity data matrix, missing value computation should not proceed" );
            return false;
        }

        if ( ( bkgChannelA != null && bkgChannelA.rows() == 0 ) || ( bkgChannelB != null
                && bkgChannelB.rows() == 0 ) ) {
            TwoChannelMissingValuesImpl.log.warn( "Background values must not be empty when non-null" );
            return false;
        }

        if ( signalChannelA != null && signalChannelB != null ) {
            if ( !( signalChannelA.rows() == signalChannelB.rows() ) ) {
                TwoChannelMissingValuesImpl.log
                        .warn( "Collection sizes probably should match in channel A and B " + signalChannelA.rows()
                                + " != " + signalChannelB.rows() );
            }

            if ( !( signalChannelA.rows() == preferred.rows() ) ) { // vectors with all-missing data are already removed
                TwoChannelMissingValuesImpl.log
                        .warn( "Collection sizes probably should match in channel A and preferred type "
                                + signalChannelA.rows() + " != " + preferred.rows() );
            }
            int numSamplesA = signalChannelA.columns();
            int numSamplesB = signalChannelB.columns();

            if ( numSamplesA != numSamplesB || numSamplesB != preferred.columns() ) {
                TwoChannelMissingValuesImpl.log.warn( "Number of samples doesn't match!" );
                return false;
            }
        }

        if ( ( bkgChannelA != null && bkgChannelB != null ) && bkgChannelA.rows() != bkgChannelB.rows() )
            TwoChannelMissingValuesImpl.log
                    .warn( "Collection sizes probably should match for background  " + bkgChannelA.rows() + " != "
                            + bkgChannelB.rows() );

        if ( signalToNoiseThreshold <= 0.0 ) {
            TwoChannelMissingValuesImpl.log.warn( "Signal-to-noise threshold must be greater than zero" );
            return false;
        }

        return true;

    }

}
