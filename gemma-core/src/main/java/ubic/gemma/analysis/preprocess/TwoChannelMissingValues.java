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

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * @spring.bean id="twoChannelMissingValues"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 * @author pavlidis
 * @version $Id$
 */
public class TwoChannelMissingValues {

    private static Log log = LogFactory.getLog( TwoChannelMissingValues.class.getName() );

    private AuditTrailService auditTrailService;

    private DesignElementDataVectorService designElementDataVectorService;

    private ExpressionExperimentService expressionExperimentService;

    private QuantitationTypeService quantitationTypeService;

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
    @SuppressWarnings("unchecked")
    public Collection<DesignElementDataVector> computeMissingValues( ExpressionExperiment expExp,
            double signalToNoiseThreshold, Collection<Double> extraMissingValueIndicators ) {

        expressionExperimentService.thawLite( expExp );
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
        Collection<DesignElementDataVector> finalResults = new HashSet<DesignElementDataVector>();

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

        Collection<DesignElementDataVector> dimRes = computeMissingValues( expExp, preferredData, signalDataA,
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
    protected Collection<DesignElementDataVector> computeMissingValues( ExpressionExperiment source,
            ExpressionDataDoubleMatrix preferred, ExpressionDataDoubleMatrix signalChannelA,
            ExpressionDataDoubleMatrix signalChannelB, ExpressionDataDoubleMatrix bkgChannelA,
            ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold,
            Collection<Double> extraMissingValueIndicators ) {

        validate( preferred, signalChannelA, signalChannelB, bkgChannelA, bkgChannelB, signalToNoiseThreshold );

        ByteArrayConverter converter = new ByteArrayConverter();
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        QuantitationType present = getMissingDataQuantitationType( signalToNoiseThreshold );
        source.getQuantitationTypes().add( present );

        int count = 0;
        for ( ExpressionDataMatrixRowElement element : signalChannelA.getRowElements() ) {

            DesignElement designElement = element.getDesignElement();

            DesignElementDataVector vect = RawExpressionDataVector.Factory.newInstance();
            vect.setQuantitationType( present );
            vect.setExpressionExperiment( source );
            vect.setDesignElement( designElement );
            vect.setBioAssayDimension( signalChannelA.getBioAssayDimension( designElement ) );

            int numCols = preferred.columns( designElement );

            boolean[] detectionCalls = new boolean[numCols];
            Double[] prefRow = preferred.getRow( designElement );

            Double[] signalA = signalChannelA.getRow( designElement );
            Double[] signalB = signalChannelB != null ? signalChannelB.getRow( designElement ) : null;
            Double[] bkgA = null;
            Double[] bkgB = null;

            if ( bkgChannelA != null ) bkgA = bkgChannelA.getRow( designElement );

            if ( bkgChannelB != null ) bkgB = bkgChannelB.getRow( designElement );

            // columsn only for this designelement!

            for ( int col = 0; col < numCols; col++ ) {

                // If the "preferred" value is already missing, we retain that, or if it is a special value
                Double pref = prefRow == null ? Double.NaN : prefRow[col];
                if ( pref == null || pref.isNaN()
                        || ( extraMissingValueIndicators != null && extraMissingValueIndicators.contains( pref ) ) ) {
                    detectionCalls[col] = false;
                    continue;
                }

                Double bkgAV = 0.0;
                Double bkgBV = 0.0;

                if ( bkgA != null ) bkgAV = bkgA[col];

                if ( bkgB != null ) bkgBV = bkgB[col];

                Double sigAV = signalA[col] == null ? 0.0 : signalA[col];
                Double sigBV = signalB[col] == null ? 0.0 : signalB[col];

                boolean call = computeCall( signalToNoiseThreshold, sigAV, sigBV, bkgAV, bkgBV );
                detectionCalls[col] = call;
            }

            vect.setData( converter.booleanArrayToBytes( detectionCalls ) );
            results.add( vect );

            if ( ++count % 4000 == 0 ) {
                log.info( count + " vectors examined for missing values, " + results.size()
                        + " vectors generated so far." );
            }

        }
        log.info( "Finished: " + count + " vectors examined for missing values" );

        log.info( "Persisting " + results.size() + " vectors ... " );
        results = designElementDataVectorService.create( results );
        expressionExperimentService.update( source ); // this is needed to get the QT filled in properly.

        return results;
    }

    private boolean computeCall( double signalToNoiseThreshold, Double sigAV, Double sigBV, Double bkgAV, Double bkgBV ) {
        if ( sigAV == null && sigBV == null ) return false;

        if ( ( sigAV == null || sigAV.isNaN() ) && ( sigBV == null || sigBV.isNaN() ) ) return false;

        if ( sigAV != null && sigAV > bkgAV * signalToNoiseThreshold ) return true;

        if ( sigBV != null && sigBV > bkgBV * signalToNoiseThreshold ) return true;

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
            log.warn( "Collection sizes probably should match in channel A and B " + signalChannelA.rows() + " != "
                    + signalChannelB.rows() );
        }

        if ( !( signalChannelA.rows() == preferred.rows() ) ) { // vectors with all-missing data are already removed
            log.warn( "Collection sizes probably should match in channel A and preferred type " + signalChannelA.rows()
                    + " != " + preferred.rows() );
        }

        if ( ( bkgChannelA != null && bkgChannelB != null ) && bkgChannelA.rows() != bkgChannelB.rows() )
            log.warn( "Collection sizes probably should match for background  " + bkgChannelA.rows() + " != "
                    + bkgChannelB.rows() );

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
