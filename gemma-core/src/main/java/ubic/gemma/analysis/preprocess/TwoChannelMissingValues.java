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
import org.springframework.stereotype.Service;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.Constants;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
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
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

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
 * <li>This only works if there are signal values for both channels
 * <li>If there are background values, they are used to compute signal-to-noise ratios</li>
 * <li>If the signal values already contain missing data, these are still considered missing.</li>
 * <li>If there are no background values, all values will be considered 'present' unless the signal values are both zero
 * or missing.
 * <li>If the preferred quantitation type data is a missing value, then the data are considered missing (for
 * consistency).
 * </ol>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class TwoChannelMissingValues {

    public static final double DEFAULT_SIGNAL_TO_NOISE_THRESHOLD = 2.0;

    private static Log log = LogFactory.getLog( TwoChannelMissingValues.class.getName() );

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    /**
     * @param expExp The expression experiment to analyze. The quantitation types to use are selected automatically. If
     *        you want more control use other computeMissingValues methods.
     */
    public Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment expExp ) {
        return this.computeMissingValues( expExp, DEFAULT_SIGNAL_TO_NOISE_THRESHOLD, null );
    }

    /**
     * @param expExp The expression experiment to analyze. The quantitation types to use are selected automatically. If
     *        you want more control use other computeMissingValues methods.
     * @param signalToNoiseThreshold A value such as 1.5 or 2.0; only spots for which at least ONE of the channel signal
     *        is more than signalToNoiseThreshold*background (and the preferred data are not missing) will be considered
     *        present.
     * @param extraMissingValueIndicators Values that should be considered missing. For example, some data sets use '0'
     *        (foolish, but true). This can be null or empty and it will be ignored.
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the experiment.
     */
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
            for ( QuantitationType qt : bkgDataA.getQuantitationTypes() ) {
                if ( builder.getNumMissingValues( qt ) > 0 ) {
                    log.warn( "Missing values in bkgDataA" );
                    break;
                }
            }
            for ( QuantitationType qt : bkgDataB.getQuantitationTypes() ) {
                if ( builder.getNumMissingValues( qt ) > 0 ) {
                    log.warn( "Missing values in bkgDataB" );
                    break;
                }
            }
            for ( QuantitationType qt : signalDataA.getQuantitationTypes() ) {
                if ( builder.getNumMissingValues( qt ) > 0 ) {
                    log.warn( "Missing values in signalDataA" );
                    break;
                }
            }
            for ( QuantitationType qt : signalDataB.getQuantitationTypes() ) {
                if ( builder.getNumMissingValues( qt ) > 0 ) {
                    log.warn( "Missing values in signalDataB" );
                    break;
                }
            }
        }

        Collection<RawExpressionDataVector> dimRes = computeMissingValues( expExp, preferredData, signalDataA,
                signalDataB, bkgDataA, bkgDataB, signalToNoiseThreshold, extraMissingValueIndicators );

        finalResults.addAll( dimRes );

        AuditEventType type = MissingValueAnalysisEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( expExp, type, "Computed missing value data for data run on array designs: "
                + ads );

        return finalResults;
    }

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
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
    @SuppressWarnings("unchecked")
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

        QuantitationType present = getMissingDataQuantitationType( signalToNoiseThreshold );
        source.getQuantitationTypes().add( present );

        int count = 0;

        ExpressionDataDoubleMatrix baseChannel = signalChannelA == null ? signalChannelB : signalChannelA;

        for ( ExpressionDataMatrixRowElement element : baseChannel.getRowElements() ) {

            DesignElement designElement = element.getDesignElement();

            RawExpressionDataVector vect = RawExpressionDataVector.Factory.newInstance();
            vect.setQuantitationType( present );
            vect.setExpressionExperiment( source );
            vect.setDesignElement( designElement );
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

            // columsn only for this designelement!
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
                Boolean call = computeCall( signalToNoiseThreshold, sigAV, sigBV, bkgAV, bkgBV );

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

        log.info( "Persisting " + results.size() + " vectors ... " );
        results = ( Collection<RawExpressionDataVector> ) designElementDataVectorService.create( results );
        expressionExperimentService.update( source ); // this is needed to get the QT filled in properly.

        return results;
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
     * @param sigAV
     * @param sigBV
     * @param bkgAV
     * @param bkgBV
     * @return null if no decision could be made due to NaN in the values given.
     */
    private Boolean computeCall( double signalToNoiseThreshold, Double sigAV, Double sigBV, Double bkgAV, Double bkgBV ) {

        if ( !sigAV.isNaN() && !bkgAV.isNaN() && sigAV > bkgAV * signalToNoiseThreshold ) return true;

        if ( !sigBV.isNaN() && !bkgBV.isNaN() && sigBV > bkgBV * signalToNoiseThreshold ) return true;

        /*
         * We couldn't decide because neither of the above calculations could be done.
         */
        if ( ( sigAV.isNaN() || bkgAV.isNaN() ) && ( sigBV.isNaN() || bkgBV.isNaN() ) ) return null;

        return false;
    }

    /**
     * Construct the quantitation type that will be used for the generated DesignElementDataVEctors.
     * 
     * @param signalToNoiseThreshold
     * @return
     */
    private QuantitationType getMissingDataQuantitationType( double signalToNoiseThreshold ) {
        QuantitationType present = QuantitationType.Factory.newInstance();
        present.setName( "Detection call" );
        present.setDescription( "Detection call based on signal to noise threshold of " + signalToNoiseThreshold
                + " (Computed by " + Constants.APP_NAME + ")" );
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
            log
                    .warn( "Must have at least preferred and one intensity data matrix, missing value computation should not proceed" );
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
