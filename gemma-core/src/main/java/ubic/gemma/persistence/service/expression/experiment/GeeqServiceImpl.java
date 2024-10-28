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
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.stat.StatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.util.*;

@Service
@CommonsLog
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
    private static final String ERR_MSG_MISSING_VALS = "Can not calculate missing values: ";
    private static final String ERR_MSG_CORMAT = "Can not create cormat: ";
    private static final String ERR_MSG_CORMAT_MISSING_VALS = "Cormat retrieval failed because of missing missing values for ee id ";
    private static final String ERR_W_MEAN_BAD_ARGS = "Can not calculate weighted arithmetic mean from null or unequal length arrays.";
    private static final String ERR_B_EFFECT_BAD_STATE = "Batch effect scoring in odd state - null batch effect, but batch info should be present."
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
    private final ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;
    private final ArrayDesignService arrayDesignService;
    private final ExpressionDataMatrixService expressionDataMatrixService;
    private final OutlierDetectionService outlierDetectionService;
    private final AuditTrailService auditTrailService;
    private final SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    public GeeqServiceImpl( GeeqDao geeqDao, ExpressionExperimentService expressionExperimentService, ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService,
            ArrayDesignService arrayDesignService, ExpressionDataMatrixService expressionDataMatrixService,
            OutlierDetectionService outlierDetectionService, AuditTrailService auditTrailService,
            SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService ) {
        super( geeqDao );
        this.expressionExperimentService = expressionExperimentService;
        this.expressionExperimentBatchInformationService = expressionExperimentBatchInformationService;
        this.arrayDesignService = arrayDesignService;
        this.expressionDataMatrixService = expressionDataMatrixService;
        this.outlierDetectionService = outlierDetectionService;
        this.auditTrailService = auditTrailService;
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
                case pub:
                    log.info( GeeqServiceImpl.LOG_PREFIX + " Starting publication geeq re-scoring for " + ee );
                    this.scoreOnlyPublication( ee );
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

        // Recalculate final scores
        this.updateQualityScore( gq );
        this.updateSuitabilityScore( gq );

        // Add note if experiment curation not finished
        if ( ee.getCurationDetails().getNeedsAttention() ) {
            gq.addOtherIssues( "Experiment was not fully curated when the score was calculated." );
        }

        stopwatch.stop();
        this.createGeeqEvent( ee, "Geeq scoring (mode: " + mode + ")",
                "Issues noted: \n" + gq
                        .getOtherIssues() );

        if ( stopwatch.getTime() > 1000 )
            log.info( GeeqServiceImpl.LOG_PREFIX + " finished for " + ee.getShortName() + " (" + stopwatch.getTime() + " ms)" );

        return gq;
    }


    private void updateSuitabilityScore( Geeq gq ) {
        double[] suitability = gq.getSuitabilityScoreArray();
        double[] weights = gq.getSuitabilityScoreWeightsArray();
        double score = this.getWeightedMean( suitability, weights );
        gq.setDetectedSuitabilityScore( score );
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

    private void scoreOnlyPublication( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        this.scorePublication( ee, gq );
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
        double[] scores = new double[ads.size()];

        // FIXME factor out magic numbers. Rationale: rarely used platforms are less favored
        int i = 0;
        for ( ArrayDesign ad : ads ) {
            long cnt = arrayDesignService.numExperiments( ad );
            scores[i++] = cnt < 10 ? GeeqServiceImpl.N_10
                    : cnt < 20 ? GeeqServiceImpl.N_05 : cnt < 50 ? GeeqServiceImpl.P_00 : cnt < 100 ? GeeqServiceImpl.P_05 : GeeqServiceImpl.P_10;
        }

        score = this.getMean( scores );
        gq.setsScoreAvgPlatformPopularity( score );
    }

    /**
     *
     */
    private void scoreAvgPlatformSize( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        double[] scores = new double[ads.size()];

        int i = 0;
        for ( ArrayDesign ad : ads ) {

            Taxon taxon = arrayDesignService.getTaxon( ad.getId() );
            long cnt = arrayDesignService.numGenes( ad );

            /*
             * FIXME we don't deal with miRNA platforms correctly
             */

            // human, rat, mouse, zebrafish and worm all have on the order 20k protein-coding genes.
            switch ( taxon.getCommonName() ) {
                case "human":
                case "rat":
                case "mouse":
                case "zebrafish":
                case "worm":
                    scores[i++] = cnt < 5000 ? GeeqServiceImpl.N_10
                            : cnt < 10000 ? GeeqServiceImpl.N_05
                            : cnt < 15000 ? GeeqServiceImpl.P_00 : cnt < 18000 ? GeeqServiceImpl.P_05 : GeeqServiceImpl.P_10;
                    break;
                case "yeast":
                    // Yeast has about 6k protein-coding genes
                    scores[i++] = cnt < 1000 ? GeeqServiceImpl.N_10
                            : cnt < 2500 ? GeeqServiceImpl.N_05
                            : cnt < 4000 ? GeeqServiceImpl.P_00 : cnt < 5000 ? GeeqServiceImpl.P_05 : GeeqServiceImpl.P_10;
                    break;
                case "fly":
                    // Fly has about 14k protein coding genes
                    scores[i++] = cnt < 2000 ? GeeqServiceImpl.N_10
                            : cnt < 5000 ? GeeqServiceImpl.N_05
                            : cnt < 8000 ? GeeqServiceImpl.P_00 : cnt < 10000 ? GeeqServiceImpl.P_05 : GeeqServiceImpl.P_10;
                    break;
            }

        }

        score = this.getMean( scores );
        gq.setsScoreAvgPlatformSize( score );
    }

    /**
     *
     */
    private void scoreSampleSize( ExpressionExperiment ee, Geeq gq ) {
        double score;

        int cnt = ee.getBioAssays().size();

        // FIXME factor out these magic numbers. Rationale: >500 is "too big"; 5 is "very small" and 20-500 is just fine.
        if ( cnt > 500 ) {
            score = GeeqServiceImpl.N_10;
        } else {
            if ( cnt < 6 ) {
                score = GeeqServiceImpl.N_10;
            } else if ( cnt < 10 ) {
                score = GeeqServiceImpl.N_03;
            } else if ( cnt < 20 ) {
                score = GeeqServiceImpl.P_00;
            } else {
                score = GeeqServiceImpl.P_10;
            }
        }
        gq.setsScoreSampleSize( score );
    }

    private boolean scoreRawData( ExpressionExperiment ee, Geeq gq ) {
        double score;

        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );

        boolean dataReprocessedFromRaw = false;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsRecomputedFromRawData() ) {
                dataReprocessedFromRaw = true;
                break;
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
            hasProcessedVectors = expressionExperimentService.hasProcessedExpressionData( ee );
            hasMissingValues = hasProcessedVectors && expressionDataMatrixService.getProcessedExpressionDataMatrix( ee ).hasMissingValues();
        }

        score = hasRawData || ( !hasMissingValues && hasProcessedVectors ) ? GeeqServiceImpl.P_10 : GeeqServiceImpl.N_10;
        gq.setNoVectors( !hasProcessedVectors );
        gq.addOtherIssues( problems );
        gq.setsScoreMissingValues( score );
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

    private void createGeeqEvent( ExpressionExperiment ee, String note, String details ) {
        auditTrailService.addUpdateEvent( ee, GeeqEvent.class, note, details );
    }

    /**
     * Checks for all combinations of factor values in the experiments bio assays, and counts the amount of
     * their occurrences, then checks what the lowest amount is. The method only combines factor values from
     * first (up to) MAX_EFS_REPLICATE_CHECK categorical experimental factors it encounters, and always disregards
     * values from batch factors.
     *
     * @param  ee an expression experiment to get the count for.
     * @return the lowest number of replicates (ignoring factor value combinations with only one replicate),
     *            or -2 if <em>all</em> factor value combinations were present only once, or -1, if there were no usable
     *            factors
     *            to begin with.
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
                if ( ExperimentalDesignUtils.isBatch( ef )
                        || fv.getCharacteristics().stream().map( Characteristic::getValue ).anyMatch( DE_EXCLUDE::equalsIgnoreCase )
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
            cormat = sampleCoexpressionAnalysisService.loadBestMatrix( ee );
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
