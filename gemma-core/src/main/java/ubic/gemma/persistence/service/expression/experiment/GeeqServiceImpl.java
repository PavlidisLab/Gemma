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

import cern.colt.list.DoubleArrayList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.stat.StatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import org.springframework.lang.Nullable;
import java.util.*;

@Service
@Slf4j
public class GeeqServiceImpl extends AbstractVoEnabledService<Geeq, GeeqValueObject> implements GeeqService {

    /**
     * If there are fewer than this number of replicates per condition, but more than GEEQ_WORST_REPLICATION_THRESHOLD,
     * a medium score is given for replicates.
     */
    private static final int GEEQ_MEDIUM_REPLICATION_THRESHOLD = 5;

    /**
     * If there are fewer than this number of replicates per condition, the worst score is given for replicates.
     */
    private static final int GEEQ_WORST_REPLICATION_THRESHOLD = 2;

    /**
     * How many factors to look at to determine conditions that have very few replicates. Since we routinely only do
     * differential expression analysis for up to 3 factors, that value makes sense. (batch and continuous factors not
     * included)
     */
    private static final int MAX_EFS_REPLICATE_CHECK = 3;

    private static final String LOG_PREFIX = "|G|E|E|Q|";
    private static final String ERR_MSG_CORMAT = "Can not create cormat: ";
    private static final String ERR_MSG_CORMAT_MISSING_VALS = "Cormat retrieval failed because of missing missing values for ee id ";
    private static final String ERR_W_MEAN_BAD_ARGS = "Can not calculate weighted arithmetic mean from null or unequal length arrays.";
    private static final String ERR_B_EFFECT_BAD_STATE = "Batch effect scoring in odd state - null batch effect, but batch info should be present."
            + "The same problem will be present for batch confound as well.";

    private static final double P_00 = 0.0;
    public static final double BATCH_EFF_WEAK = GeeqServiceImpl.P_00;
    private static final double P_10 = 1.0;
    public static final double BATCH_CONF_NO_HAS = GeeqServiceImpl.P_10;
    public static final double BATCH_EFF_NONE = GeeqServiceImpl.P_10;
    private static final double N_10 = -GeeqServiceImpl.P_10;
    public static final double BATCH_CONF_HAS = GeeqServiceImpl.N_10;
    public static final double BATCH_EFF_STRONG = GeeqServiceImpl.N_10;

    private final ExpressionExperimentService expressionExperimentService;
    private final ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;
    private final OutlierDetectionService outlierDetectionService;
    private final GeeqAuditService geeqAuditService;
    private final SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    public GeeqServiceImpl( GeeqDao geeqDao, ExpressionExperimentService expressionExperimentService, ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService,
            OutlierDetectionService outlierDetectionService, GeeqAuditService geeqAuditService,
            SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService ) {
        super( geeqDao );
        this.expressionExperimentService = expressionExperimentService;
        this.expressionExperimentBatchInformationService = expressionExperimentBatchInformationService;
        this.outlierDetectionService = outlierDetectionService;
        this.geeqAuditService = geeqAuditService;
        this.sampleCoexpressionAnalysisService = sampleCoexpressionAnalysisService;
    }

    @Override
    @Transactional
    public Geeq calculateScore( ExpressionExperiment ee, ScoreMode mode ) {
        // reload in this session
        ee = expressionExperimentService.loadOrFail( ee.getId() );

        Geeq gq = ee.getGeeq();
        if ( gq == null ) {
            gq = new Geeq();
            ee.setGeeq( gq );
        }

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        try {
            // Update score values
            switch ( mode ) {
                case all:
                    log.info( GeeqServiceImpl.LOG_PREFIX + " Starting full geeq scoring for  " + ee );
                    this.scoreAll( ee );
                    break;
                case batch:
                    log.info( GeeqServiceImpl.LOG_PREFIX + " Starting batch info, confound and batch effect geeq re-scoring for " + ee );
                    this.scoreOnlyBatchArtifacts( ee );
                    break;
                case reps:
                    log.info( GeeqServiceImpl.LOG_PREFIX + " Starting replicates geeq re-scoring for " + ee );
                    this.scoreOnlyReplicates( ee );
                    break;
                default:
                    throw new IllegalArgumentException( "Unsupported mode: " + mode + " for " + ee );
            }
            log.debug( GeeqServiceImpl.LOG_PREFIX + " Finished geeq re-scoring for " + ee
                    + ", saving results..." );
        } catch ( Exception e ) {
            log.error( GeeqServiceImpl.LOG_PREFIX + " Scoring did not finish for " + ee + ".", e );
            gq.addOtherIssues( e.getMessage() );
        }

        // Recalculate final score
        this.updateQualityScore( gq );

        // Add note if experiment curation not finished
        if ( ee.getCurationDetails().getNeedsAttention() ) {
            gq.addOtherIssues( "Experiment was not fully curated when the score was calculated." );
        }

        stopwatch.stop();
        // Aspect-intercepted hoist target: the @Audited co-bean records a GeeqEvent on return.
        geeqAuditService.recordGeeqScoring( ee, "Geeq scoring (mode: " + mode + ")",
                "Issues noted: \n" + gq.getOtherIssues() );

        if ( stopwatch.getTime() > 1000 )
            log.info( GeeqServiceImpl.LOG_PREFIX + " finished for " + ee.getShortName() + " (" + stopwatch.getTime() + " ms)" );

        return gq;
    }


    private void updateQualityScore( Geeq gq ) {
        double[] quality = gq.getQualityScoreArray();
        double[] weights = gq.getQualityScoreWeightsArray();
        double score = this.getWeightedMean( quality, weights );
        gq.setDetectedQualityScore( score );
    }

    private void scoreAll( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee );

        // Reset description of scoring problems
        gq.setOtherIssues( "" );

        // Not a score, but a data-availability fact the retired suitability pass used to record on its way past.
        gq.setNoVectors( !expressionExperimentService.hasProcessedExpressionData( ee ) );

        // Quality score calculation
        DoubleMatrix<BioAssay, BioAssay> cormat = this.getCormat( ee, gq );
        double[] cormatLTri = this.getLowerTriCormat( cormat );

        this.scoreOutliers( gq, cormat );
        this.scoreSampleMeanCorrelation( gq, cormatLTri );
        this.scoreSampleMedianCorrelation( gq, cormatLTri );
        this.scoreSampleCorrelationVariance( gq, cormatLTri );
        this.scorePlatformsTech( ads, gq );
        this.scoreReplicates( ee, gq );
        boolean hasBatchInfo = this.scoreBatchInfo( ee, gq );
        boolean hasBatchConfound = this.scoreBatchConfound( ee, gq, hasBatchInfo );
        this.scoreBatchEffect( ee, gq, hasBatchInfo, hasBatchConfound );
    }

    private void scoreOnlyBatchArtifacts( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        boolean info = this.scoreBatchInfo( ee, gq );
        boolean confound = this.scoreBatchConfound( ee, gq, info );
        this.scoreBatchEffect( ee, gq, info, confound );
    }

    private void scoreOnlyReplicates( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        this.scoreReplicates( ee, gq );
    }

    /**
     * Blank the rows and columns of samples flagged as outliers.
     * <p>
     * The stored matrix keeps their values so a curator can review the call against them, but every GEEQ
     * score is about the samples that count, so they are masked here instead. Doing it at the point of use
     * also preserves {@code corrMatIssues == 2}, which is set when the matrix contains NaNs and therefore
     * already meant "this dataset has flagged outliers".
     */
    @Nullable
    static DoubleMatrix<BioAssay, BioAssay> maskOutliers( @Nullable DoubleMatrix<BioAssay, BioAssay> cormat ) {
        if ( cormat == null ) {
            return null;
        }
        for ( int i = 0; i < cormat.rows(); i++ ) {
            if ( !cormat.getRowName( i ).getIsOutlier() ) {
                continue;
            }
            for ( int j = 0; j < cormat.columns(); j++ ) {
                cormat.set( i, j, Double.NaN );
                cormat.set( j, i, Double.NaN );
            }
        }
        return cormat;
    }

    /*
     * Quality scoring methods
     */

    private void scoreOutliers( Geeq gq, @Nullable DoubleMatrix<BioAssay, BioAssay> cormat ) {
        double score;
        boolean hasCorrMat = true;
        boolean hasNaNs = false;
        boolean outliers = true;

        if ( cormat == null || cormat.rows() == 0 ) {
            hasCorrMat = false;
        } else {
            // Check if cormat has NaNs (diagonal is not checked, but there really should not be NaNs on the diagonal)
            Double[] doubleArray = ArrayUtils.toObject( this.getLowerTriangle( cormat.getRawMatrix() ) );
            List<Double> list = new ArrayList<>( Arrays.asList( doubleArray ) );
            hasNaNs = list.contains( Double.NaN );

            outliers = outlierDetectionService.identifyOutliersByMedianCorrelation( cormat ).size() > 0;
        }

        score = outliers ? GeeqServiceImpl.N_10 : GeeqServiceImpl.P_10; //
        gq.setCorrMatIssues( ( byte ) ( !hasCorrMat ? 1 : hasNaNs ? 2 : 0 ) );
        gq.setqScoreOutliers( score );
    }

    private void scoreSampleMeanCorrelation( Geeq gq, double[] cormatLTri ) {
        this.cormatOps( gq, cormatLTri, CormatOpsType.mean );
    }

    private void scoreSampleMedianCorrelation( Geeq gq, double[] cormatLTri ) {
        this.cormatOps( gq, cormatLTri, CormatOpsType.median );
    }

    private void scoreSampleCorrelationVariance( Geeq gq, double[] cormatLTri ) {
        this.cormatOps( gq, cormatLTri, CormatOpsType.variance );
    }

    private void scorePlatformsTech( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        boolean twoColor = false;

        for ( ArrayDesign ad : ads ) {
            if ( ad.getTechnologyType().equals( TechnologyType.TWOCOLOR ) ) {
                twoColor = true;
                break;
            }
        }

        score = twoColor ? GeeqServiceImpl.N_10 : GeeqServiceImpl.P_10;
        gq.setqScorePlatformsTech( score );
    }

    private void scoreReplicates( ExpressionExperiment ee, Geeq gq ) {
        double score;
        int replicates = -1;
        if ( ee.getExperimentalDesign() != null && !ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            replicates = this.leastReplicates( ee );

            if ( replicates < GEEQ_WORST_REPLICATION_THRESHOLD ) {
                score = GeeqServiceImpl.N_10;
            } else if ( replicates < GEEQ_MEDIUM_REPLICATION_THRESHOLD ) {
                score = GeeqServiceImpl.P_00;
            } else {
                score = GeeqServiceImpl.P_10;
            }
        } else { // no information, so we give no penalty or bonus
            score = GeeqServiceImpl.P_00;
            gq.setReplicatesIssues( ( byte ) 1 ); // no factors
        }

        // extra details
        if ( replicates == -1 ) {
            gq.setReplicatesIssues( ( byte ) 2 ); // somewhat redundant with no factors
        } else if ( replicates == -2 ) {
            gq.setReplicatesIssues( ( byte ) 3 ); // ALL values have only one sample (no replication at all)
        } else if ( replicates == 0 ) { // shouldn't happen
            gq.setReplicatesIssues( ( byte ) 4 );
        }

        gq.setqScoreReplicates( score );
    }

    private boolean scoreBatchInfo( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasUsableInfo = expressionExperimentBatchInformationService.checkHasUsableBatchInfo( ee );
        score = !hasUsableInfo ? GeeqServiceImpl.N_10 : GeeqServiceImpl.P_10;
        gq.setqScoreBatchInfo( score );
        return hasUsableInfo;
    }

    private void scoreBatchEffect( ExpressionExperiment ee, Geeq gq, boolean infoDetected, boolean confound ) {
        double score;
        boolean hasInfo = true;
        boolean hasStrong = false;
        boolean hasNone = false;
        boolean corrected = false;

        if ( infoDetected && !confound ) {
            boolean manual = gq.isManualBatchEffectActive();
            if ( manual ) {
                hasStrong = gq.isManualHasStrongBatchEffect();
                hasNone = gq.isManualHasNoBatchEffect();
            } else {
                BatchEffectDetails be = expressionExperimentBatchInformationService.getBatchEffectDetails( ee );
                hasInfo = be.hasBatchInformation();
                corrected = be.dataWasBatchCorrected();
                BatchEffectDetails.BatchEffectStatistics statistics = be.getBatchEffectStatistics();
                if ( statistics != null ) {
                    hasStrong = statistics.getPvalue() < 0.0001;
                    hasNone = statistics.getPvalue() > 0.1;
                }
            }
        }

        score = !infoDetected || !hasInfo || confound ? GeeqServiceImpl.P_00
                : hasStrong ? GeeqServiceImpl.BATCH_EFF_STRONG : hasNone ? GeeqServiceImpl.BATCH_EFF_NONE : GeeqServiceImpl.BATCH_EFF_WEAK;
        gq.setBatchCorrected( corrected );
        gq.setqScoreBatchEffect( score );
    }

    private boolean scoreBatchConfound( ExpressionExperiment ee, Geeq gq, boolean infoDetected ) {
        double score;
        boolean hasConfound = false;

        if ( infoDetected ) {
            boolean manual = gq.isManualBatchConfoundActive();
            if ( !manual ) {
                if ( expressionExperimentBatchInformationService.hasSignificantBatchConfound( ee ) ) {
                    // null can mean no confound but also no batch info, which is ok since both should result in score 0
                    hasConfound = true;
                }
            } else {
                hasConfound = gq.isManualHasBatchConfound();
            }
        }

        score = !infoDetected ? GeeqServiceImpl.P_00 : hasConfound ? GeeqServiceImpl.BATCH_CONF_HAS : GeeqServiceImpl.BATCH_CONF_NO_HAS;
        gq.setqScoreBatchConfound( score );

        return hasConfound;
    }

    /*
     * Support methods and other stuff
     */

    /**
     * Checks for all combinations of factor values in the experiments bio assays, and counts the amount of
     * their occurrences, then checks what the lowest amount is. The method only combines factor values from
     * first (up to) MAX_EFS_REPLICATE_CHECK categorical experimental factors it encounters, and always disregards
     * values from batch factors.
     *
     * @param ee an expression experiment to get the count for.
     * @return the lowest number of replicates (ignoring factor value combinations with only one replicate),
     * or -2 if <em>all</em> factor value combinations were present only once, or -1, if there were no usable
     * factors
     * to begin with.
     */
    private int leastReplicates( ExpressionExperiment ee ) {
        HashMap<String, Integer> factors = new HashMap<>();
        Collection<BioAssay> bas = ee.getBioAssays();
        List<ExperimentalFactor> keepEfs = new ArrayList<>( GeeqServiceImpl.MAX_EFS_REPLICATE_CHECK );

        for ( BioAssay ba : bas ) {
            // we need a copy here, otherwise the model will be mutated
            Collection<FactorValue> fvs = new HashSet<>( ba.getSampleUsed().getAllFactorValues() );

            //only keep up to MAX_EFS_REPLICATE_CHECK categorical factors, ignoring batch factor and DE_EXCLUDE
            Collection<FactorValue> removeFvs = new LinkedList<>();
            for ( FactorValue fv : fvs ) {
                ExperimentalFactor ef = fv.getExperimentalFactor();
                if ( ExperimentFactorUtils.isBatchFactor( ef )
                        || FactorValueUtils.isDeExcluded( fv )
                        || ef.getType().equals( FactorType.CONTINUOUS ) ) {
                    removeFvs.add( fv ); // always remove batch factor values and DE_EXCLUDE values
                } else {
                    if ( !keepEfs.contains( ef ) && keepEfs.size() <= GeeqServiceImpl.MAX_EFS_REPLICATE_CHECK ) {
                        keepEfs.add( ef ); // keep first MAX_EFS_REPLICATE_CHECK encountered factors
                    } else if ( !keepEfs.contains( ef ) ) {
                        removeFvs.add( fv ); // if from different factor, remove the value
                    }
                }
            }
            fvs.removeAll( removeFvs );

            // sort so the keys in the hash map are consistent
            Collection<Long> ids = IdentifiableUtils.getIds( fvs );
            Long[] arr = ids.toArray( new Long[0] );
            Arrays.sort( arr );
            String key = Arrays.toString( arr );

            // add new key or increment counter of existing one
            Integer cnt = factors.get( key );
            factors.put( key, cnt == null ? 1 : ++cnt );
        }

        List<Integer> counts = new ArrayList<>( factors.values() );
        Collections.sort( counts );

        if ( counts.isEmpty() ) {
            return -1;
        } else if ( counts.get( counts.size() - 1 ) == 1 ) {
            return -2; // all conditions have only one replicate
        } else {
            return counts.get( 0 );
        }

    }

    @Nullable
    private DoubleMatrix<BioAssay, BioAssay> getCormat( ExpressionExperiment ee, Geeq gq ) {
        DoubleMatrix<BioAssay, BioAssay> cormat = null;
        try {
            cormat = maskOutliers( sampleCoexpressionAnalysisService.loadBestMatrix( ee ) );
        } catch ( IllegalStateException e ) {
            log.warn(
                    GeeqServiceImpl.LOG_PREFIX + GeeqServiceImpl.ERR_MSG_CORMAT_MISSING_VALS + ee.getId() );
        } catch ( Exception e ) {
            String err = GeeqServiceImpl.ERR_MSG_CORMAT + e.getMessage();
            log.warn( GeeqServiceImpl.LOG_PREFIX + err );
            gq.addOtherIssues( err );
        }
        return cormat;
    }

    private double[] getLowerTriCormat( @Nullable DoubleMatrix<BioAssay, BioAssay> cormat ) {
        if ( cormat == null || cormat.rows() == 0 ) {
            return new double[] {};
        }
        double[] corTri = this.getLowerTriangle( cormat.getRawMatrix() );

        // We have to remove NaNs, some cormats have them (we notify user about this in the outlier score)
        // this is not very efficient, but the DoubleMatrix does not have a method to get an array of Doubles (not doubles)
        Double[] doubleArray = ArrayUtils.toObject( corTri );
        List<Double> list = new ArrayList<>( Arrays.asList( doubleArray ) );
        //noinspection StatementWithEmptyBody // because java standard libraries suck, we have to iterate like this to remove all NaNs, not just the first one.
        while ( list.remove( Double.NaN ) ) {
        }

        return ArrayUtils.toPrimitive( list.toArray( new Double[0] ) );
    }

    private void cormatOps( Geeq gq, double[] cormatLTri, CormatOpsType type ) {
        double score;
        double value = 0;
        boolean hasCorrMat = true;

        if ( cormatLTri.length == 0 ) {
            hasCorrMat = false;
        } else {
            switch ( type ) {
                case mean:
                    value = this.getMean( cormatLTri );
                    break;
                case median:
                    value = this.getMedian( cormatLTri );
                    break;
                case variance:
                    value = this.getVariance( cormatLTri );
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        score = !hasCorrMat ? GeeqServiceImpl.P_00 : value;
        switch ( type ) {
            case mean:
                gq.setqScoreSampleMeanCorrelation( score );
                break;
            case median:
                gq.setqScoreSampleMedianCorrelation( score );
                break;
            case variance:
                gq.setqScoreSampleCorrelationVariance( score );
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private double getWeightedMean( double[] vals, double[] weights ) {
        if ( vals.length != weights.length ) {
            throw new IllegalArgumentException( GeeqServiceImpl.ERR_W_MEAN_BAD_ARGS );
        }
        double sum = GeeqServiceImpl.P_00;
        double wSum = GeeqServiceImpl.P_00;
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
        return DescriptiveWithMissing.median( new DoubleArrayList( arr ) );
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

    private enum CormatOpsType {
        mean, median, variance
    }

}
