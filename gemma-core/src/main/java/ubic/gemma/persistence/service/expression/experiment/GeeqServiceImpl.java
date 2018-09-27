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
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GeeqServiceImpl extends AbstractVoEnabledService<Geeq, GeeqValueObject> implements GeeqService {
    private static final int MAX_EFS_REPLICATE_CHECK = 2;
    private static final String LOG_PREFIX = "|G|E|E|Q| ";
    private static final String ERR_MSG_MISSING_VALS = "Can not calculate missing values: ";
    private static final String ERR_MSG_CORMAT = "Can not create cormat: ";
    private static final String ERR_MSG_CORMAT_MISSING_VALS = "Cormat retrieval failed because of missing missing values for ee id ";
    private static final String ERR_W_MEAN_BAD_ARGS = "Can not calculate weighted arithmetic mean from null or unequal length arrays.";
    private static final String ERR_B_EFFECT_BAD_STATE =
            "Batch effect scoring in odd state - null batch effect, but batch info should be present."
                    + "The same problem will be present for batch confound as well.";

    private static final double P_00 = 0.0;
    public static final double BATCH_EFF_WEAK = GeeqServiceImpl.P_00;
    private static final double P_03 = 0.3;
    private static final double P_05 = 0.5;
    private static final double P_10 = 1.0;
    public static final double BATCH_CONF_NO_HAS = GeeqServiceImpl.P_10;
    public static final double BATCH_EFF_NONE = GeeqServiceImpl.P_10;
    private static final double N_03 = -GeeqServiceImpl.P_03;
    private static final double N_05 = -GeeqServiceImpl.P_05;
    private static final double N_10 = -GeeqServiceImpl.P_10;
    public static final double BATCH_CONF_HAS = GeeqServiceImpl.N_10;
    public static final double BATCH_EFF_STRONG = GeeqServiceImpl.N_10;
    private static final String DE_EXCLUDE = "DE_Exclude";
    private final ExpressionExperimentService expressionExperimentService;
    private final ArrayDesignService arrayDesignService;
    private final ExpressionDataMatrixService expressionDataMatrixService;
    private final OutlierDetectionService outlierDetectionService;
    private final AuditTrailService auditTrailService;
    private final SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    public GeeqServiceImpl( GeeqDao geeqDao, ExpressionExperimentService expressionExperimentService,
            ArrayDesignService arrayDesignService, ExpressionDataMatrixService expressionDataMatrixService,
            OutlierDetectionService outlierDetectionService, AuditTrailService auditTrailService,
            SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService ) {
        super( geeqDao );
        this.expressionExperimentService = expressionExperimentService;
        this.arrayDesignService = arrayDesignService;
        this.expressionDataMatrixService = expressionDataMatrixService;
        this.outlierDetectionService = outlierDetectionService;
        this.auditTrailService = auditTrailService;
        this.sampleCoexpressionAnalysisService = sampleCoexpressionAnalysisService;
    }

    @Override
    public void calculateScore( Long eeId, String mode ) {
        this.doScoring( eeId, mode );
    }

    @Override
    public void setManualOverrides( Long eeId, GeeqAdminValueObject gqVo ) {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        ee = expressionExperimentService.thawLiter( ee );
        Geeq gq = ee.getGeeq();

        // Update manual quality score value
        if ( gq.getManualQualityScore() != gqVo.getManualQualityScore() ) {
            gq.setLastManualOverride( this.createGeeqEvent( ee, "Manual quality score value changed",
                    this.fromTo( gq.getManualQualityScore(), gqVo.getManualQualityScore() ) ) );
            gq.setManualQualityScore( gqVo.getManualQualityScore() );
        }
        // Update manual quality score override
        if ( gq.isManualQualityOverride() != gqVo.isManualQualityOverride() ) {
            gq.setLastManualOverride( this.createGeeqEvent( ee, "Manual quality score override changed",
                    this.fromTo( gq.isManualQualityOverride(), gqVo.isManualQualityOverride() ) ) );
            gq.setManualQualityOverride( gqVo.isManualQualityOverride() );
        }

        // Update manual suitability score value
        if ( gq.getManualSuitabilityScore() != gqVo.getManualSuitabilityScore() ) {
            gq.setLastManualOverride( this.createGeeqEvent( ee, "Manual suitability score value changed",
                    this.fromTo( gq.getManualSuitabilityScore(), gqVo.getManualSuitabilityScore() ) ) );
            gq.setManualSuitabilityScore( gqVo.getManualSuitabilityScore() );
        }
        // Update manual suitability score override
        if ( gq.isManualSuitabilityOverride() != gqVo.isManualSuitabilityOverride() ) {
            gq.setLastManualOverride( this.createGeeqEvent( ee, "Manual suitability score override changed",
                    this.fromTo( gq.isManualSuitabilityOverride(), gqVo.isManualSuitabilityOverride() ) ) );
            gq.setManualSuitabilityOverride( gqVo.isManualSuitabilityOverride() );
        }

        // Update manual batch confound value
        if ( gq.isManualHasBatchConfound() != gqVo.isManualHasBatchConfound() ) {
            gq.setLastBatchConfoundChange( this.createGeeqEvent( ee, "Manual batch confound value changed",
                    this.fromTo( gq.isManualHasBatchConfound(), gqVo.isManualHasBatchConfound() ) ) );
            gq.setManualHasBatchConfound( gqVo.isManualHasBatchConfound() );
        }
        // Update manual batch confound override
        if ( gq.isManualBatchConfoundActive() != gqVo.isManualBatchConfoundActive() ) {
            gq.setLastBatchConfoundChange( this.createGeeqEvent( ee, "Manual batch confound override changed",
                    this.fromTo( gq.isManualBatchConfoundActive(), gqVo.isManualBatchConfoundActive() ) ) );
            gq.setManualBatchConfoundActive( gqVo.isManualBatchConfoundActive() );
        }

        // Update manual batch effect strong value
        if ( gq.isManualHasStrongBatchEffect() != gqVo.isManualHasStrongBatchEffect() ) {
            gq.setLastBatchEffectChange( this.createGeeqEvent( ee, "Manual strong batch effect value changed",
                    this.fromTo( gq.isManualHasStrongBatchEffect(), gqVo.isManualHasStrongBatchEffect() ) ) );
            gq.setManualHasStrongBatchEffect( gqVo.isManualHasStrongBatchEffect() );
        }
        // Update manual batch effect no value
        if ( gq.isManualHasNoBatchEffect() != gqVo.isManualHasNoBatchEffect() ) {
            gq.setLastBatchEffectChange( this.createGeeqEvent( ee, "Manual no batch effect value changed",
                    this.fromTo( gq.isManualHasNoBatchEffect(), gqVo.isManualHasNoBatchEffect() ) ) );
            gq.setManualHasNoBatchEffect( gqVo.isManualHasNoBatchEffect() );
        }
        // Update manual batch effect override
        if ( gq.isManualBatchEffectActive() != gqVo.isManualBatchEffectActive() ) {
            gq.setLastBatchEffectChange( this.createGeeqEvent( ee, "Manual batch effect override changed",
                    this.fromTo( gq.isManualBatchEffectActive(), gqVo.isManualBatchEffectActive() ) ) );
            gq.setManualBatchEffectActive( gqVo.isManualBatchEffectActive() );
        }

        this.update( gq );
        Log.info( this.getClass(), GeeqServiceImpl.LOG_PREFIX + " Updated manual override settings for ee id " + eeId );
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
     */
    private void doScoring( Long eeId, String mode ) {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            return;
        }

        this.ensureEeHasGeeq( ee );
        Geeq gq = ee.getGeeq();

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        try {
            // Update score values
            switch ( mode ) {
                case GeeqService.OPT_MODE_ALL:
                    Log.info( this.getClass(),
                            GeeqServiceImpl.LOG_PREFIX + " Starting full geeq scoring for ee id " + eeId );
                    gq = this.scoreAll( ee );
                    break;
                case GeeqService.OPT_MODE_BATCH:
                    Log.info( this.getClass(),
                            GeeqServiceImpl.LOG_PREFIX + " Starting batch info, confound and batch effect geeq re-scoring for ee id " + eeId );
                    gq = this.scoreOnlyBatchArtifacts( ee );
                    break;
                case GeeqService.OPT_MODE_REPS:
                    Log.info( this.getClass(),
                            GeeqServiceImpl.LOG_PREFIX + " Starting replicates geeq re-scoring for ee id " + eeId );
                    gq = this.scoreOnlyReplicates( ee );
                    break;
                case GeeqService.OPT_MODE_PUB:
                    Log.info( this.getClass(),
                            GeeqServiceImpl.LOG_PREFIX + " Starting publication geeq re-scoring for ee id " + eeId );
                    gq = this.scoreOnlyPublication( ee );
                    break;
                default:
                    Log.warn( this.getClass(),
                            GeeqServiceImpl.LOG_PREFIX + " Did not recognize the given mode " + mode + " for ee id "
                                    + eeId );
            }
            Log.info( this.getClass(), GeeqServiceImpl.LOG_PREFIX + " Finished geeq re-scoring for ee id " + eeId
                    + ", saving results..." );
        } catch ( Exception e ) {
            Log.info( this.getClass(),
                    GeeqServiceImpl.LOG_PREFIX + " Major problem encountered, scoring did not finish for ee id " + eeId
                            + "." );
            e.printStackTrace();
            gq.addOtherIssues( e.getMessage() );
        }

        // Recalculate final scores
        gq = this.updateQualityScore( gq );
        gq = this.updateSuitabilityScore( gq );

        // Add note if experiment curation not finished
        if ( ee.getCurationDetails().getNeedsAttention() ) {
            gq.addOtherIssues( "Experiment was not fully curated when the score was calculated." );
        }

        stopwatch.stop();
        gq.setLastRun( this.createGeeqEvent( ee, "Re-ran geeq scoring (mode: " + mode + ")",
                "Took " + stopwatch.elapsedMillis() + "ms.\nUnexpected problems encountered: \n" + gq
                        .getOtherIssues() ) );

        this.update( gq );
        Log.info( this.getClass(),
                GeeqServiceImpl.LOG_PREFIX + " took " + ( ( float ) stopwatch.elapsedTime( TimeUnit.SECONDS ) / 60f )
                        + " minutes to process ee id " + eeId );

    }

    private Geeq updateSuitabilityScore( Geeq gq ) {
        double[] suitability = gq.getSuitabilityScoreArray();
        double[] weights = gq.getSuitabilityScoreWeightsArray();
        double score = this.getWeightedMean( suitability, weights );
        gq.setDetectedSuitabilityScore( score );
        return gq;
    }

    private Geeq updateQualityScore( Geeq gq ) {
        double[] quality = gq.getQualityScoreArray();
        double[] weights = gq.getQualityScoreWeightsArray();
        double score = this.getWeightedMean( quality, weights );
        gq.setDetectedQualityScore( score );
        return gq;
    }

    private Geeq scoreAll( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thaw( ee );
        Geeq gq = ee.getGeeq();
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee );

        // Reset description of scoring problems
        gq.setOtherIssues( "" );

        // Suitability score calculation
        this.scorePublication( ee, gq );
        this.scorePlatformAmount( ads, gq );
        this.scorePlatformsTechMulti( ads, gq );
        this.scoreAvgPlatformPopularity( ads, gq );
        this.scoreAvgPlatformSize( ads, gq );
        this.scoreSampleSize( ee, gq );
        boolean hasRawData = this.scoreRawData( ee, gq );
        this.scoreMissingValues( ee, gq, hasRawData );

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

        return gq;
    }

    private Geeq scoreOnlyBatchArtifacts( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLiter( ee );
        Geeq gq = ee.getGeeq();
        boolean info = this.scoreBatchInfo( ee, gq );
        boolean confound = this.scoreBatchConfound( ee, gq, info );
        this.scoreBatchEffect( ee, gq, info, confound );
        return gq;
    }

    private Geeq scoreOnlyReplicates( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thaw( ee );
        Geeq gq = ee.getGeeq();
        this.scoreReplicates( ee, gq );
        return gq;
    }

    private Geeq scoreOnlyPublication( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLiter( ee );
        Geeq gq = ee.getGeeq();
        this.scorePublication( ee, gq );
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
        boolean hasBib;
        BibliographicReference bib = null;

        if ( ee.getPrimaryPublication() != null ) {
            bib = ee.getPrimaryPublication();
        } else if ( ee.getOtherRelevantPublications() != null && ee.getOtherRelevantPublications().size() > 0 ) {
            bib = ee.getOtherRelevantPublications().iterator().next();
        }

        hasBib = bib != null;

        score = !hasBib ? GeeqServiceImpl.N_10 : GeeqServiceImpl.P_10;
        gq.setsScorePublication( score );

    }

    private void scorePlatformAmount( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        score = ads.size() > 2 ? GeeqServiceImpl.N_10 : ads.size() > 1 ? GeeqServiceImpl.N_05 : GeeqServiceImpl.P_10;
        gq.setsScorePlatformAmount( score );
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

        score = mismatch ? GeeqServiceImpl.N_10 : GeeqServiceImpl.P_10;
        gq.setsScorePlatformsTechMulti( score );
    }

    private void scoreAvgPlatformPopularity( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        double scores[] = new double[ads.size()];

        int i = 0;
        for ( ArrayDesign ad : ads ) {
            int cnt = arrayDesignService.numExperiments( ad );
            scores[i++] = cnt < 10 ?
                    GeeqServiceImpl.N_10 :
                    cnt < 20 ?
                            GeeqServiceImpl.N_05 :
                            cnt < 50 ? GeeqServiceImpl.P_00 : cnt < 100 ? GeeqServiceImpl.P_05 : GeeqServiceImpl.P_10;
        }

        score = this.getMean( scores );
        gq.setsScoreAvgPlatformPopularity( score );
    }

    private void scoreAvgPlatformSize( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        double scores[] = new double[ads.size()];

        int i = 0;
        for ( ArrayDesign ad : ads ) {
            long cnt = arrayDesignService.numGenes( ad );
            scores[i++] = cnt < 5000 ?
                    GeeqServiceImpl.N_10 :
                    cnt < 10000 ?
                            GeeqServiceImpl.N_05 :
                            cnt < 15000 ?
                                    GeeqServiceImpl.P_00 :
                                    cnt < 18000 ? GeeqServiceImpl.P_05 : GeeqServiceImpl.P_10;
        }

        score = this.getMean( scores );
        gq.setsScoreAvgPlatformSize( score );
    }

    private void scoreSampleSize( ExpressionExperiment ee, Geeq gq ) {
        double score;

        int cnt = ee.getBioAssays().size();

        score = cnt < 10 ?
                GeeqServiceImpl.N_10 :
                cnt < 20 ? GeeqServiceImpl.N_03 : cnt < 50 ? GeeqServiceImpl.P_03 : GeeqServiceImpl.P_10;
        gq.setsScoreSampleSize( score );
    }

    private boolean scoreRawData( ExpressionExperiment ee, Geeq gq ) {
        double score;

        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );

        boolean dataReprocessedFromRaw = false;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsRecomputedFromRawData() ) {
                dataReprocessedFromRaw = true;
            }
        }

        score = dataReprocessedFromRaw ? GeeqServiceImpl.P_10 : GeeqServiceImpl.N_10;
        gq.setsScoreRawData( score );
        return dataReprocessedFromRaw;
    }

    private void scoreMissingValues( ExpressionExperiment ee, Geeq gq, boolean hasRawData ) {
        double score;
        boolean hasProcessedVectors = true;
        boolean hasMissingValues = false;
        String problems = "";

        if ( !hasRawData ) {
            try {
                ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
                hasMissingValues = dmatrix.hasMissingValues();
            } catch ( IllegalArgumentException e ) {
                hasProcessedVectors = false;
            } catch ( Exception e ) {
                hasProcessedVectors = false;
                problems = GeeqServiceImpl.ERR_MSG_MISSING_VALS + e.getMessage();
            }
        }

        score = hasRawData || ( !hasMissingValues && hasProcessedVectors ) ?
                GeeqServiceImpl.P_10 :
                GeeqServiceImpl.N_10;
        gq.setNoVectors( !hasProcessedVectors );
        gq.addOtherIssues( problems );
        gq.setsScoreMissingValues( score );
    }

    /*
     * Quality scoring methods
     */

    private void scoreOutliers( Geeq gq, DoubleMatrix<BioAssay, BioAssay> cormat ) {
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
        this.cormatOps( gq, cormatLTri, cormatOpsType.mean );
    }

    private void scoreSampleMedianCorrelation( Geeq gq, double[] cormatLTri ) {
        this.cormatOps( gq, cormatLTri, cormatOpsType.median );
    }

    private void scoreSampleCorrelationVariance( Geeq gq, double[] cormatLTri ) {
        this.cormatOps( gq, cormatLTri, cormatOpsType.variance );
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

        score = twoColor ? GeeqServiceImpl.N_10 : GeeqServiceImpl.P_10;
        gq.setqScorePlatformsTech( score );
    }

    private void scoreReplicates( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasDesign = false;
        double replicates = 0;

        if ( ee.getExperimentalDesign() != null ) {
            hasDesign = true;
            replicates = this.leastReplicates( ee );
        }

        score = !hasDesign || replicates < 4 ? GeeqServiceImpl.N_10 : //
                replicates < 10 ? GeeqServiceImpl.P_00 : GeeqServiceImpl.P_10;

        gq.setReplicatesIssues( ( byte ) ( //
                !hasDesign ? 1 : //
                        replicates == -1 ? 2 : //
                                replicates == 1 ? 3 : //
                                        replicates == 0 ? 4 : 0 ) );
        gq.setqScoreReplicates( score );
    }

    private boolean scoreBatchInfo( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasInfo = expressionExperimentService.checkHasBatchInfo( ee );

        score = !hasInfo ? GeeqServiceImpl.N_10 : GeeqServiceImpl.P_10;
        gq.setqScoreBatchInfo( score );
        return hasInfo;
    }

    private void scoreBatchEffect( ExpressionExperiment ee, Geeq gq, boolean infoDetected, boolean confound ) {
        double score;
        boolean hasInfo = true;
        boolean hasStrong = false;
        boolean hasNone = false;
        boolean corrected = false;

        if ( infoDetected && !confound ) {
            boolean manual = gq.isManualBatchEffectActive();
            if ( !manual ) {
                BatchEffectDetails be = expressionExperimentService.getBatchEffect( ee );
                if ( be == null ) {
                    Log.warn( this.getClass(), GeeqServiceImpl.ERR_B_EFFECT_BAD_STATE );
                    hasInfo = false;
                } else {
                    hasStrong = be.getPvalue() < 0.0001;
                    hasNone = be.getPvalue() > 0.1;
                    corrected = be.getDataWasBatchCorrected();
                }
            } else {
                hasStrong = gq.isManualHasStrongBatchEffect();
                hasNone = gq.isManualHasNoBatchEffect();
            }
        }

        score =  !infoDetected || !hasInfo || confound ?
                GeeqServiceImpl.P_00 :
                hasStrong ?
                        GeeqServiceImpl.BATCH_EFF_STRONG :
                        hasNone ? GeeqServiceImpl.BATCH_EFF_NONE : GeeqServiceImpl.BATCH_EFF_WEAK;
        gq.setBatchCorrected( corrected );
        gq.setqScoreBatchEffect( score );
    }

    private boolean scoreBatchConfound( ExpressionExperiment ee, Geeq gq, boolean infoDetected ) {
        double score;
        boolean hasConfound = false;

        if ( infoDetected ) {
            boolean manual = gq.isManualBatchConfoundActive();
            if ( !manual ) {
                String confInfo = expressionExperimentService.getBatchConfound( ee );
                if ( confInfo != null ) {
                    // null can mean no confound but also no batch info, which is ok since both should result in score 0
                    hasConfound = true;
                }
            } else {
                hasConfound = gq.isManualHasBatchConfound();
            }
        }

        score = !infoDetected ?
                GeeqServiceImpl.P_00 :
                hasConfound ? GeeqServiceImpl.BATCH_CONF_HAS : GeeqServiceImpl.BATCH_CONF_NO_HAS;
        gq.setqScoreBatchConfound( score );

        return hasConfound;
    }

    /*
     * Support methods and other stuff
     */

    private AuditEvent createGeeqEvent( ExpressionExperiment ee, String note, String details ) {
        return auditTrailService.addUpdateEvent( ee, GeeqEvent.class, note, details );
    }

    private String fromTo( Object from, Object to ) {
        return "From: " + from + " To: " + to;
    }

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
        HashMap<String, Integer> factors = new HashMap<>();
        Collection<BioAssay> bas = ee.getBioAssays();
        List<ExperimentalFactor> keepEfs = new ArrayList<>( GeeqServiceImpl.MAX_EFS_REPLICATE_CHECK );

        for ( BioAssay ba : bas ) {
            Collection<FactorValue> fvs = ba.getSampleUsed().getFactorValues();

            //only keep two factors, remove batch factor
            Collection<FactorValue> removeFvs = new LinkedList<>();
            for ( FactorValue fv : fvs ) {
                ExperimentalFactor ef = fv.getExperimentalFactor();
                if ( ExperimentalDesignUtils.isBatch( ef ) || DE_EXCLUDE
                        .equalsIgnoreCase( fv.getDescriptiveString() ) ) {
                    removeFvs.add( fv ); // always remove batch factor values and DE_EXCLUDE values
                } else {
                    if ( !keepEfs.contains( ef ) && keepEfs.size() <= GeeqServiceImpl.MAX_EFS_REPLICATE_CHECK ) {
                        keepEfs.add( ef ); // keep first two encountered factors
                    } else if ( !keepEfs.contains( ef ) ) {
                        removeFvs.add( fv ); // if from different factor, remove the value
                    }
                }
            }
            fvs.removeAll( removeFvs );

            // sort so the keys in the hash map are consistent
            Collection<Long> ids = EntityUtils.getIds( fvs );
            Long[] arr = ids.toArray( new Long[0] );
            Arrays.sort( arr );
            String key = Arrays.toString( arr );

            // add new key or increment counter of existing one
            Integer cnt = factors.get( key );
            factors.put( key, cnt == null ? 1 : ++cnt );
        }

        // Hash-maps value collection can be almost anything
        List<Integer> counts = factors.values() instanceof List ?
                ( List<Integer> ) factors.values() :
                new ArrayList<>( factors.values() );

        int totalSize = counts.size();
        // ignore size-1
        // noinspection StatementWithEmptyBody // because java standard libraries suck
        while ( counts.remove( ( Integer ) 1 ) ) {}
        Collections.sort( counts );

        return ( totalSize < 1 ? -1 : counts.size() > 0 ? counts.get( 0 ) : 1 );
    }

    private DoubleMatrix<BioAssay, BioAssay> getCormat( ExpressionExperiment ee, Geeq gq ) {
        DoubleMatrix<BioAssay, BioAssay> cormat = null;
        try {
            cormat = sampleCoexpressionAnalysisService.loadTryRegressedThenFull( ee );
        } catch ( IllegalStateException e ) {
            Log.warn( this.getClass(),
                    GeeqServiceImpl.LOG_PREFIX + GeeqServiceImpl.ERR_MSG_CORMAT_MISSING_VALS + ee.getId() );
        } catch ( Exception e ) {
            String err = GeeqServiceImpl.ERR_MSG_CORMAT + e.getMessage();
            Log.warn( this.getClass(), GeeqServiceImpl.LOG_PREFIX + err );
            gq.addOtherIssues( err );
        }
        return cormat;
    }

    private double[] getLowerTriCormat( DoubleMatrix<BioAssay, BioAssay> cormat ) {
        if ( cormat == null || cormat.rows() == 0 ) {
            return new double[] {};
        }
        double[] corTri = this.getLowerTriangle( cormat.getRawMatrix() );

        // We have to remove NaNs, some cormats have them (we notify user about this in the outlier score)
        // this is not very efficient, but the DoubleMatrix does not have a method to get an array of Doubles (not doubles)
        Double[] doubleArray = ArrayUtils.toObject( corTri );
        List<Double> list = new ArrayList<>( Arrays.asList( doubleArray ) );
        //noinspection StatementWithEmptyBody // because java standard libraries suck, we have to iterate like this to remove all NaNs, not just the first one.
        while ( list.remove( Double.NaN ) ) {}

        return ArrayUtils.toPrimitive( list.toArray( new Double[0] ) );
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
                    value = this.getMean( cormatLTri );
                    break;
                case median:
                    value = this.getMedian( cormatLTri );
                    break;
                case variance:
                    value = this.getVariance( cormatLTri );
                    break;
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
        }
    }

    private double getWeightedMean( double[] vals, double[] weights ) {
        if ( vals == null || weights == null || vals.length != weights.length ) {
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

}
