/*
 * The gemma project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.persistence.service.expression.experiment;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.openjena.atlas.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GeeqServiceImpl extends VoEnabledService<Geeq, GeeqValueObject> implements GeeqService {
    private static final int MAX_EFS_REPLICATE_CHECK = 2;
    private static final String LOG_PREFIX = "|G|E|E|Q|";
    private static final String ERR_W_MEAN_BAD_ARGS = "Can not calculate weighted arithmetic mean from null or unequal length arrays.";
    private static final String ERR_B_EFFECT_BAD_STATE =
            "Batch effect scoring in odd state - null batch effect, but batch info should be present."
                    + "The same problem will be present for batch confound as well.";

    private static final double P_05 = 0.5;
    private static final double P_10 = 1.0;
    private static final double P_00 = 0.0;
    private static final double N_03 = -0.3;
    private static final double N_05 = -P_05;
    private static final double N_07 = -0.7;
    private static final double N_10 = -P_10;

    private static final int PUB_LOW_YEAR = 2006;
    private static final int PUB_MID_YEAR = 2009;

    private ExpressionExperimentService expressionExperimentService;
    private ArrayDesignService arrayDesignService;
    private ExpressionDataMatrixService expressionDataMatrixService;
    private OutlierDetectionService outlierDetectionService;

    @Autowired
    public GeeqServiceImpl( GeeqDao geeqDao, ExpressionExperimentService expressionExperimentService,
            ArrayDesignService arrayDesignService, ExpressionDataMatrixService expressionDataMatrixService,
            OutlierDetectionService outlierDetectionService ) {
        super( geeqDao );
        this.expressionExperimentService = expressionExperimentService;
        this.arrayDesignService = arrayDesignService;
        this.expressionDataMatrixService = expressionDataMatrixService;
        this.outlierDetectionService = outlierDetectionService;
    }

    @Override
    public ExpressionExperiment resetBatchConfound( Long eeId ) {
        return doScoring( eeId, scoringMode.batchConfound );
    }

    @Override
    public ExpressionExperiment resetBatchEffect( Long eeId ) {
        return doScoring( eeId, scoringMode.batchEffect );
    }

    @Override
    public ExpressionExperiment calculateScore( Long eeId ) {
        return doScoring( eeId, scoringMode.all );
    }

    @Override
    public ExpressionExperiment setManualOverrides( Long eeId, GeeqValueObject gqVo ) {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        Geeq gq = ee.getGeeq();

        // Update manual quality score
        gq.setManualQualityScore( gqVo.getManualQualityScore() );
        gq.setManualQualityOverride( gqVo.getManualQualityOverride() );

        // Update manual suitability score
        gq.setManualSuitabilityScore( gqVo.getManualSuitabilityScore() );
        gq.setManualSuitabilityOverride( gqVo.getManualSuitabilityOverride() );

        // Set manual batch confound
        gq.setManualHasBatchConfound( gqVo.getManualHasBatchConfound() );
        gq.setManualBatchConfoundActive( gqVo.getManualBatchConfoundActive() );

        // Set manual batch effect
        gq.setManualHasStrongBatchEffect( gqVo.getManualHasStrongBatchEffect() );
        gq.setManualHasNoBatchEffect( gqVo.getManualHasNoBatchEffect() );
        gq.setManualBatchEffectActive( gqVo.getManualBatchEffectActive() );

        this.update( gq );
        return ee;
    }

    /**
     * Does all the preparations and calls the appropriate scoring methods.
     *
     * @param eeId the id of experiment to be scored.
     * @param mode the mode of scoring. All will redo all scores, batchEffect and batchConfound will only recalculate
     *             scores relevant to batch effect and batch confound, respectively.
     *             Scoring batch effect and confound is fairly fast, especially compared to the 'all' mode, which goes
     *             through almost all information associated with the experiment, and can therefore be very slow,
     *             depending on the experiment.
     * @return the updated experiment.
     */
    private ExpressionExperiment doScoring( Long eeId, scoringMode mode ) {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            return null;
        }

        ensureEeHasGeeq( ee );
        Geeq gq = ee.getGeeq();

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        // Update score values
        switch ( mode ) {
            case all:
                Log.info( this.getClass(), LOG_PREFIX + " Starting full geeq scoring for ee id " + eeId );
                gq = scoreAll( ee );
                break;
            case batchEffect:
                Log.info( this.getClass(), LOG_PREFIX + " Starting batch effect geeq re-scoring for ee id " + eeId );
                gq = scoreOnlyBatchEffect( ee );
                break;
            case batchConfound:
                Log.info( this.getClass(), LOG_PREFIX + " Starting batch confound geeq re-scoring for ee id " + eeId );
                gq = scoreOnlyBatchConfound( ee );
                break;
        }
        Log.info( this.getClass(), LOG_PREFIX + " Finished geeq re-scoring for ee id " + eeId + ", saving results..." );

        // Recalculate final scores
        gq = updateQualityScore( gq );
        gq = updateSuitabilityScore( gq );

        this.update( gq );

        stopwatch.stop();
        Log.info( this.getClass(), LOG_PREFIX + " took " + ( ( float ) stopwatch.elapsedTime( TimeUnit.SECONDS ) / 60f )
                + " minutes to process ee id " + eeId );

        return ee;
    }

    private Geeq updateSuitabilityScore( Geeq gq ) {
        double[] suitability = gq.getSuitabilityScoreArray();
        double[] weights = gq.getSuitabilityScoreWeightsArray();
        double score = getWeightedMean( suitability, weights );
        gq.setDetectedSuitabilityScore( score );
        return gq;
    }

    private Geeq updateQualityScore( Geeq gq ) {
        double[] quality = gq.getQualityScoreArray();
        double[] weights = gq.getQualityScoreWeightsArray();
        double score = getWeightedMean( quality, weights );
        gq.setDetectedQualityScore( score );
        return gq;
    }

    private Geeq scoreAll( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thaw( ee );
        Geeq gq = ee.getGeeq();
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee );

        // Suitability score calculation
        scorePublication( ee, gq );
        scorePlatformAmount( ads, gq );
        scorePlatformsTechMulti( ads, gq );
        scoreAvgPlatformPopularity( ads, gq );
        scoreAvgPlatformSize( ads, gq );
        scoreSampleSize( ee, gq );
        boolean hasRawData = scoreRawData( ee, gq );
        scoreMissingValues( ee, gq, hasRawData );

        // Quality score calculation
        DoubleMatrix<BioAssay, BioAssay> cormat = getCormat( ee );
        double[] cormatLTri = getLowerTriCormat( cormat );

        scoreOutliers( ee, gq, cormat );
        scoreSampleMeanCorrelation( gq, cormatLTri );
        scoreSampleMedianCorrelation( gq, cormatLTri );
        scoreSampleCorrelationVariance( gq, cormatLTri );
        scorePlatformsTech( ads, gq );
        scoreReplicates( ee, gq );
        boolean hasBatchInfo = scoreBatchInfo( ee, gq );
        scoreBatchEffect( ee, gq, hasBatchInfo );
        scoreBatchConfound( ee, gq, hasBatchInfo );

        return gq;
    }

    private Geeq scoreOnlyBatchEffect( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        scoreBatchEffect( ee, gq, scoreBatchInfo( ee, gq ) );
        return gq;
    }

    private Geeq scoreOnlyBatchConfound( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        scoreBatchConfound( ee, gq, scoreBatchInfo( ee, gq ) );
        return gq;
    }

    private void ensureEeHasGeeq( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        if ( gq == null ) {
            gq = new Geeq();
            gq = this.create( gq );
            ee.setGeeq( gq );
            expressionExperimentService.update( ee );
        }
    }

    /*
     * Suitability scoring methods
     */

    private void scorePublication( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasBib = true;
        boolean hasDate;
        BibliographicReference bib = null;
        Date date = null;

        if ( ee.getPrimaryPublication() != null ) {
            bib = ee.getPrimaryPublication();
        } else if ( ee.getOtherRelevantPublications() != null && ee.getOtherRelevantPublications().size() > 0 ) {
            bib = ee.getOtherRelevantPublications().iterator().next();
        } else {
            hasBib = false;
        }

        if ( hasBib ) {
            date = bib.getPublicationDate();
        }

        hasDate = date != null;

        Calendar cal = Calendar.getInstance();
        cal.set( PUB_LOW_YEAR + 1900, Calendar.JANUARY, 1 );
        Date d2006 = cal.getTime();
        cal.set( PUB_MID_YEAR + 1900, Calendar.JANUARY, 1 );
        Date d2009 = cal.getTime();

        score = !hasBib ? N_10 : !hasDate ? N_07 : date.before( d2006 ) ? N_05 : date.before( d2009 ) ? N_03 : P_10;
        gq.setSScorePublication( score );

    }

    private void scorePlatformAmount( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        score = ads.size() > 2 ? N_10 : ads.size() > 1 ? N_05 : P_10;
        gq.setSScorePlatformAmount( score );
    }

    private void scorePlatformsTechMulti( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        boolean mismatch = false;

        ArrayDesign prev = null;
        for ( ArrayDesign ad : ads ) {
            if ( prev == null ) {
                prev = ad;
            } else {
                mismatch = !ad.getTechnologyType().equals( prev.getTechnologyType() );
            }
        }

        score = mismatch ? N_10 : P_10;
        gq.setSScorePlatformsTechMulti( score );
    }

    private void scoreAvgPlatformPopularity( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        double scores[] = new double[ads.size()];

        int i = 0;
        for ( ArrayDesign ad : ads ) {
            int cnt = arrayDesignService.numExperiments( ad );
            scores[i++] = cnt < 10 ? N_10 : cnt < 20 ? N_05 : cnt < 50 ? P_00 : cnt < 100 ? P_05 : P_10;
        }

        score = getMean( scores );
        gq.setSScoreAvgPlatformPopularity( score );
    }

    private void scoreAvgPlatformSize( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        double scores[] = new double[ads.size()];

        int i = 0;
        for ( ArrayDesign ad : ads ) {
            long cnt = arrayDesignService.numGenes( ad );
            scores[i++] = cnt < 5000 ? N_10 : cnt < 10000 ? N_05 : cnt < 15000 ? P_00 : cnt < 18000 ? P_05 : P_10;
        }

        score = getMean( scores );
        gq.setSScoreAvgPlatformSize( score );
    }

    private void scoreSampleSize( ExpressionExperiment ee, Geeq gq ) {
        double score;

        int cnt = ee.getBioAssays().size();

        score = cnt < 20 ? N_10 : cnt < 50 ? N_05 : cnt < 100 ? P_00 : cnt < 200 ? P_05 : P_10;
        gq.setSScoreSampleSize( score );
    }

    private boolean scoreRawData( ExpressionExperiment ee, Geeq gq ) {
        double score;

        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );

        boolean dataReprocessedFromRaw = false;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsMaskedPreferred() != null && qt.getIsMaskedPreferred() && qt.getIsRecomputedFromRawData() ) {
                dataReprocessedFromRaw = true;
            }
        }

        score = !dataReprocessedFromRaw ? N_10 : P_10;
        gq.setSScoreRawData( score );
        return dataReprocessedFromRaw;
    }

    private void scoreMissingValues( ExpressionExperiment ee, Geeq gq, boolean hasRawData ) {
        double score;
        boolean hasProcessedVectors = true;
        boolean hasMissingValues = false;

        if ( !hasRawData ) {
            try {
                ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
                hasMissingValues = dmatrix.hasMissingValues();
            } catch ( IllegalArgumentException e ) {
                hasProcessedVectors = false;
            }
        }

        score = hasRawData || ( !hasMissingValues && hasProcessedVectors ) ? P_10 : N_10;
        gq.setNoVectors( !hasProcessedVectors );
        gq.setSScoreMissingValues( score );
    }

    /*
     * Quality scoring methods
     */

    private void scoreOutliers( ExpressionExperiment ee, Geeq gq, DoubleMatrix<BioAssay, BioAssay> cormat ) {
        double score;
        boolean hasCorrMat = true;
        boolean hasNaNs = false;
        float outliers;
        float samples;
        float percentage = 100f;

        if ( cormat == null || cormat.rows() == 0 ) {
            hasCorrMat = false;
        } else {
            // Check if cormat has NaNs (diagonal is not checked, but there really should not be NaNs on the diagonal)
            Double[] doubleArray = ArrayUtils.toObject( getLowerTriangle( cormat.getRawMatrix() ) );
            List<Double> list = new ArrayList<>( Arrays.asList( doubleArray ) );
            hasNaNs = list.contains( Double.NaN );

            outliers = outlierDetectionService.identifyOutliersByMedianCorrelation( ee, cormat ).size();
            samples = ee.getBioAssays().size();
            percentage = outliers / samples * 100f;
        }

        score = percentage > 5f ? N_10 : //
                percentage > 2f ? N_05 : //
                        percentage > 0.1f ? P_00 : //
                                percentage > P_00 ? P_05 : P_10; //
        gq.setCorrMatIssues( ( byte ) ( !hasCorrMat ? 1 : hasNaNs ? 2 : 0 ) );
        gq.setQScoreOutliers( score );
    }

    private void scoreSampleMeanCorrelation( Geeq gq, double[] cormatLTri ) {
        cormatOps( gq, cormatLTri, cormatOpsType.mean );
    }

    private void scoreSampleMedianCorrelation( Geeq gq, double[] cormatLTri ) {
        cormatOps( gq, cormatLTri, cormatOpsType.median );
    }

    private void scoreSampleCorrelationVariance( Geeq gq, double[] cormatLTri ) {
        cormatOps( gq, cormatLTri, cormatOpsType.variance );
    }

    private void scorePlatformsTech( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        boolean twoColor = false;

        for ( ArrayDesign ad : ads ) {
            if ( ad.getTechnologyType().getValue().equals( TechnologyType.TWOCOLOR.getValue() ) ) {
                twoColor = true;
                break;
            }
        }

        score = twoColor ? N_10 : P_10;
        gq.setQScorePlatformsTech( score );
    }

    private void scoreReplicates( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasDesign = false;
        double replicates = 0;

        if ( ee.getExperimentalDesign() != null ) {
            hasDesign = true;
            replicates = leastReplicates( ee );
        }

        score = replicates < 4 ? N_10 : //
                replicates < 10 ? P_00 : P_10;

        gq.setReplicatesIssues( ( byte ) ( //
                !hasDesign ? 1 : //
                        replicates == -1 ? 2 : //
                                replicates == 1 ? 3 : //
                                        replicates == 0 ? 4 : 0 ) );
        gq.setQScoreReplicates( score );
    }

    private boolean scoreBatchInfo( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasInfo = expressionExperimentService.checkHasBatchInfo( ee );

        score = !hasInfo ? N_10 : P_10;
        gq.setQScoreBatchInfo( score );
        return hasInfo;
    }

    private void scoreBatchEffect( ExpressionExperiment ee, Geeq gq, boolean infoDetected ) {
        double score;
        boolean hasInfo = true;
        boolean hasStrong = false;
        boolean hasNone = false;
        boolean corrected = false;

        if ( infoDetected ) {
            boolean manual = gq.getManualBatchEffectActive();
            if ( !manual ) {
                BatchEffectDetails be = expressionExperimentService.getBatchEffect( ee );
                if ( be == null ) {
                    Log.warn( this.getClass(), ERR_B_EFFECT_BAD_STATE );
                    hasInfo = false;
                } else {
                    hasStrong = be.getPvalue() < 0.0001;
                    hasNone = be.getPvalue() > 0.1;
                    corrected = be.getDataWasBatchCorrected();
                }
            } else {
                hasStrong = gq.getManualHasStrongBatchEffect();
                hasNone = gq.getManualHasNoBatchEffect();
            }
        }

        score = ( !infoDetected || !hasInfo ? P_00 : hasStrong ? N_10 : hasNone ? P_10 : P_00 );
        gq.setBatchCorrected( corrected );
        gq.setQScoreBatchEffect( score );
    }

    private void scoreBatchConfound( ExpressionExperiment ee, Geeq gq, boolean infoDetected ) {
        double score;
        boolean hasConfound = false;

        if ( infoDetected ) {
            boolean manual = gq.getManualBatchConfoundActive();
            if ( !manual ) {
                String confInfo = expressionExperimentService.getBatchConfound( ee );
                if ( confInfo != null ) {
                    // null can mean no confound but also no batch info, which is ok since both should result in score 0
                    hasConfound = true;
                }
            } else {
                hasConfound = gq.getManualHasBatchConfound();
            }
        }

        score = !infoDetected ? P_00 : hasConfound ? N_10 : P_10;
        gq.setQScoreBatchConfound( score );
    }

    /*
     * Support methods and other stuff
     */

    /**
     * Checks for all combinations of factor values in the experiments bio assays, and counts the amount of
     * their occurrences, then checks what the lowest amount is. The method only combines factor values from
     * first two experimental factors it encounters, and always disregards values from batch factors.
     *
     * @param ee an expression experiment to get the count for.
     * @return the lowest number of replicates (ignoring factor value combinations with only one replicate),
     * or 1, if all factor value combinations were present only once, or -1, if there were no factor values to
     * begin with.
     */
    private int leastReplicates( ExpressionExperiment ee ) {
        HashMap<FactorValue[], Integer> factors = new HashMap<>();
        Collection<BioAssay> bas = ee.getBioAssays();
        List<ExperimentalFactor> keepEfs = new ArrayList<>( MAX_EFS_REPLICATE_CHECK );

        for ( BioAssay ba : bas ) {
            Collection<FactorValue> fvs = ba.getSampleUsed().getFactorValues();

            //only keep two factors, remove batch factor
            Collection<FactorValue> removeFvs = new LinkedList<>();
            for ( FactorValue fv : fvs ) {
                ExperimentalFactor ef = fv.getExperimentalFactor();
                if ( ExperimentalDesignUtils.isBatch( ef ) ) {
                    removeFvs.add( fv ); // always remove batch factor values
                } else {
                    if ( keepEfs.size() <= MAX_EFS_REPLICATE_CHECK ) {
                        keepEfs.add( ef ); // keep first two encountered factors
                    } else if ( !keepEfs.contains( ef ) ) {
                        removeFvs.add( fv ); // if from different factor, remove the value
                    }
                }
            }
            fvs.removeAll( removeFvs );

            // sort so the keys in the hash map are consistent
            FactorValue[] arr = fvs.toArray( new FactorValue[fvs.size()] );
            Arrays.sort( arr, new FactorValueComparator() );

            // add new key or increment counter of existing one
            Integer cnt = factors.get( arr );
            factors.put( arr, cnt == null ? 1 : ++cnt );
        }

        // Hash-maps value collection can be almost anything
        List<Integer> counts = factors.values() instanceof List ?
                ( List<Integer> ) factors.values() :
                new ArrayList<>( factors.values() );

        int totalSize = counts.size();
        // ignore size-1s
        counts.remove( 1 );
        Collections.sort( counts );

        return ( totalSize < 1 ? -1 : counts.size() > 0 ? counts.get( 0 ) : 1 );
    }

    private DoubleMatrix<BioAssay, BioAssay> getCormat( ExpressionExperiment ee ) {
        DoubleMatrix<BioAssay, BioAssay> cormat = null;
        try {
            cormat = outlierDetectionService.getCorrelationMatrix( ee, true );
        } catch ( IllegalStateException e ) {
            Log.warn( this.getClass(),
                    LOG_PREFIX + " cormat retrieval failed because of missing missing values for ee id " + ee.getId() );
        }
        return cormat;
    }

    private double[] getLowerTriCormat( DoubleMatrix<BioAssay, BioAssay> cormat ) {
        if ( cormat == null || cormat.rows() == 0 ) {
            return new double[] {};
        }
        double[] corTri = getLowerTriangle( cormat.getRawMatrix() );

        // We have to remove NaNs, some cormats have them (we notify user about this in the outlier score)
        // this is not very efficient, but the DoubleMatrix does not have a method to get an array of Doubles (not doubles)
        Double[] doubleArray = ArrayUtils.toObject( corTri );
        List<Double> list = new ArrayList<>( Arrays.asList( doubleArray ) );
        //noinspection StatementWithEmptyBody // because java stardard libraries suck
        while ( list.remove( Double.NaN ) ) {}

        return ArrayUtils.toPrimitive( list.toArray( new Double[list.size()] ) );
    }

    private void cormatOps( Geeq gq, double[] cormatLTri, cormatOpsType type ) {
        double score;
        double value = 0;
        boolean hasCorrMat = true;

        if ( cormatLTri == null || cormatLTri.length == 0 ) {
            hasCorrMat = false;
        } else {
            switch ( type ) {
                case mean:
                    value = getMean( cormatLTri );
                    break;
                case median:
                    value = getMedian( cormatLTri );
                    break;
                case variance:
                    value = getVariance( cormatLTri );
                    break;
            }
        }

        score = !hasCorrMat ? P_00 : value;
        switch ( type ) {
            case mean:
                gq.setQScoreSampleMeanCorrelation( score );
                break;
            case median:
                gq.setQScoreSampleMedianCorrelation( score );
                break;
            case variance:
                gq.setQScoreSampleCorrelationVariance( score );
                break;
        }
    }

    private double getWeightedMean( double[] vals, double[] weights ) {
        if ( vals == null || weights == null || vals.length != weights.length ) {
            throw new IllegalArgumentException( ERR_W_MEAN_BAD_ARGS );
        }
        double sum = P_00;
        double wSum = P_00;
        for ( int i = 0; i < vals.length; i++ ) {
            sum += weights[i] * vals[i];
            wSum += weights[i];
        }
        return sum / wSum;

    }

    private double getMean( double[] arr ) {
        return StatUtils.mean( arr );
    }

    private double getMedian( double[] arr ) {
        Arrays.sort( arr );
        if ( arr.length % 2 == 0 ) {
            return ( arr[arr.length / 2] + arr[arr.length / 2 - 1] ) / 2;
        }
        return arr[arr.length / 2];
    }

    private double getVariance( double[] arr ) {
        return StatUtils.variance( arr );
    }

    private double[] getLowerTriangle( double[][] mat ) {
        // half of the square, minus half of one row (the diagonal)
        double[] tri = new double[( ( mat.length * mat[0].length ) / 2 ) - ( mat.length / 2 )];

        int k = 0;
        for ( int i = 0; i < mat.length; i++ ) {
            for ( int j = 0; j < mat[i].length; j++ ) {
                if ( i > j ) {
                    tri[k] = mat[i][j];
                    k++;
                }
            }
        }

        return tri;
    }

    private enum cormatOpsType {
        mean, median, variance
    }

    private enum scoringMode {
        all, batchEffect, batchConfound
    }

    private class FactorValueComparator implements Comparator<FactorValue> {

        @Override
        public int compare( FactorValue factorValue, FactorValue t1 ) {
            return factorValue.getId().compareTo( t1.getId() );
        }
    }

}
