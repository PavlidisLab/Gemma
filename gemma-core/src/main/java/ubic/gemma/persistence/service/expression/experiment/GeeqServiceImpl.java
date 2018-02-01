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

import org.apache.commons.math3.stat.StatUtils;
import org.openjena.atlas.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Geeq;
import ubic.gemma.model.expression.experiment.GeeqValueObject;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.*;

@Service
public class GeeqServiceImpl extends VoEnabledService<Geeq, GeeqValueObject> implements GeeqService {

    //FIXME extract all scoring constants

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
        Geeq gq = ee.getGeeq(); //FIXME might need some super light version of thawing, or just Hibernate.initialize?

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
        Geeq gq = getOrCreateGeeq( ee );

        switch ( mode ) {
            case all: ee = scoreAll( ee, gq );
                break;
            case batchEffect: ee = scoreOnlyBatchEffect( ee, gq );
                break;
            case batchConfound: ee = scoreOnlyBatchConfound( ee, gq );
                break;
        }

//        ee.setGeeq( gq ); // maybe return geeq from the scoring functions instead?
        this.update( gq );
        return ee;
    }

    private ExpressionExperiment scoreAll( ExpressionExperiment ee, Geeq gq ) {
        ee = expressionExperimentService.thaw( ee );
        ee = expressionExperimentService.thawBioAssays( ee );
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
        DoubleMatrix<BioAssay, BioAssay> cormat = outlierDetectionService.getCorrelationMatrix( ee, true );
        double[] cormatLTri = ( cormat == null || cormat.rows() == 0 ) ?
                new double[] {} :
                getLowerTriangle( cormat.getRawMatrix() );

        scoreOutliers( ee, gq, cormat );
        scoreSampleMeanCorrelation( gq, cormatLTri );
        scoreSampleMedianCorrelation( gq, cormatLTri );
        scoreSampleCorrelationVariance( gq, cormatLTri );
        scorePlatformsTech( ads, gq );
        scoreReplicates( ee, gq );
        boolean hasBatchInfo = scoreBatchInfo( ee, gq );
        scoreBatchEffect( ee, gq, hasBatchInfo );
        scoreBatchConfound( ee, gq, hasBatchInfo );

        return ee;
    }

    private ExpressionExperiment scoreOnlyBatchEffect( ExpressionExperiment ee, Geeq gq ) {
        scoreBatchEffect( ee, gq, scoreBatchInfo( ee, gq ) );
        return ee;
    }

    private ExpressionExperiment scoreOnlyBatchConfound( ExpressionExperiment ee, Geeq gq ) {
        scoreBatchConfound( ee, gq, scoreBatchInfo( ee, gq ) );
        return ee;
    }

    private Geeq getOrCreateGeeq( ExpressionExperiment ee ) {
        Geeq gq = ee.getGeeq();
        if ( gq == null ) {
            gq = new Geeq();
            gq = this.create( gq );
            ee.setGeeq( gq );
            expressionExperimentService.update( ee );
        }
        return gq;
    }

    /*
     * Suitability scoring methods
     */

    private void scorePublication( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasBib = true;
        boolean hasDate = true;
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
        cal.set( 2006 + 1900, Calendar.JANUARY, 1 );
        Date d2006 = cal.getTime();
        cal.set( 2009 + 1900, Calendar.JANUARY, 1 );
        Date d2009 = cal.getTime();

        score = !hasBib ? -1.0 : !hasDate ? -0.7 : date.before( d2006 ) ? -0.5 : date.before( d2009 ) ? -0.3 : 0.0;
        gq.setSScorePublication( score );

    }

    private void scorePlatformAmount( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        score = ads.size() > 2 ? -1.0 : ads.size() > 1 ? -0.5 : 0.0;
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

        score = mismatch ? -1.0 : 0.0;
        gq.setSScorePlatformsTechMulti( score );
    }

    private void scoreAvgPlatformPopularity( Collection<ArrayDesign> ads, Geeq gq ) {
        double score;
        double scores[] = new double[ads.size()];

        int i = 0;
        for ( ArrayDesign ad : ads ) {
            int cnt = arrayDesignService.numExperiments( ad );
            scores[i++] = cnt < 10 ? -1.0 : cnt < 20 ? -0.5 : cnt < 50 ? 0.0 : cnt < 100 ? 0.5 : 1.0;
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
            scores[i++] = cnt < 5000 ? -1.0 : cnt < 10000 ? -0.5 : cnt < 15000 ? 0.0 : cnt < 18000 ? 0.5 : 1.0;
        }

        score = getMean( scores );
        gq.setSScoreAvgPlatformSize( score );
    }

    private void scoreSampleSize( ExpressionExperiment ee, Geeq gq ) {
        double score;

        int cnt = ee.getBioAssays().size();

        score = cnt < 20 ? -1.0 : cnt < 50 ? -0.5 : 0.0;
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

        score = !dataReprocessedFromRaw ? -1.0 : 0.0;
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

        score = !hasRawData ? !hasProcessedVectors ? -0.0001 : hasMissingValues ? -1.0 : 0.0 : 0.0;
        gq.setSScoreMissingValues( score );
    }

    /*
     * Quality scoring methods
     */

    private void scoreOutliers( ExpressionExperiment ee, Geeq gq, DoubleMatrix<BioAssay, BioAssay> cormat ) {
        double score;
        boolean hasCorrMat = true;

        if ( cormat == null || cormat.rows() == 0 ) {
            hasCorrMat = false;
        }

        int outliers = outlierDetectionService.identifyOutliersByMedianCorrelation( ee, cormat ).size();
        int samples = ee.getBioAssays().size();
        float percentage = outliers / samples * 100;

        score = !hasCorrMat ? -0.0001 : percentage > 5f ? -1.0 : 0.0;
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

        score = twoColor ? -0.5 : 0.0;
        gq.setSScorePlatformsTechMulti( score );
    }

    private void scoreReplicates( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasDesign = false;
        double replicates = 0;

        if ( ee.getExperimentalDesign() != null ) {
            hasDesign = true;
            replicates = leastReplicates( ee );
        }

        score = !hasDesign ?
                -0.0001 :
                replicates == -1 ?
                        -0.0002 :
                        replicates == 1 ?
                                -0.0003 :
                                replicates == 0 ? -0.0004 : replicates < 4 ? -1.0 : replicates < 10 ? 0.0 : +1.0;

        gq.setQScoreReplicates( score );
    }

    private boolean scoreBatchInfo( ExpressionExperiment ee, Geeq gq ) {
        double score;
        boolean hasInfo = expressionExperimentService.checkHasBatchInfo( ee );

        score = !hasInfo ? -1.0 : 0.0;
        gq.setQScoreBatchInfo( score );
        return hasInfo;
    }

    private void scoreBatchEffect( ExpressionExperiment ee, Geeq gq, boolean infoDetected ) {
        double score;
        boolean hasInfo = true;
        boolean hasStrong = false; //-0.5
        boolean hasNone = false; //+0.5

        if ( infoDetected ) {
            boolean manual = gq.getManualBatchEffectActive();
            if ( !manual ) {
                BatchEffectDetails be = expressionExperimentService.getBatchEffect( ee );
                if ( be == null ) {
                    Log.warn( this.getClass(),
                            "Batch effect scoring in odd state - null batch effect, but batch info should be present."
                                    + "The same problem will be present for batch confound as well." );
                    hasInfo = false;
                } else {
                    hasStrong = be.getPvalue() < 0.0001;
                    hasNone = be.getPvalue() > 0.1;
                }
            } else {
                hasStrong = gq.getManualHasStrongBatchEffect();
                hasNone = gq.getManualHasNoBatchEffect();
            }
        }

        score = !infoDetected || !hasInfo ? 0.0 : hasStrong ? -0.5 : hasNone ? 0.5 : 0.0;
        gq.setQScoreBatchInfo( score );
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

        score = !infoDetected ? 0.0 : hasConfound ? -0.5 : 0.0;
        gq.setQScoreBatchInfo( score );
    }



    /*
     * Support methods and other stuff
     */

    /**
     * Checks for all combinations of factor values in the experiments bio assays, and counts the amount of
     * their occurrences, then checks what the lowest amount is (ignoring singles).
     *
     * @param ee an expression experiment
     * @return the lowest number of replicates (ignoring factor value combinations with only one replicate),
     * or 1, if all factor value combinations were present only once, or -1, if there were no factor values to
     * begin with.
     */
    private int leastReplicates( ExpressionExperiment ee ) {
        HashMap<FactorValue[], Integer> factors = new HashMap<>();
        Collection<BioAssay> bas = ee.getBioAssays();

        for ( BioAssay ba : bas ) {
            Collection<FactorValue> fvs = ba.getSampleUsed().getFactorValues();

            // sort so the keys in the hashmap are consistent
            FactorValue[] arr = ( FactorValue[] ) fvs.toArray();
            Arrays.sort( arr );

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

    private void cormatOps( Geeq gq, double[] cormatLTri, cormatOpsType type ) {
        double score;
        double value = 0;
        boolean hasCorrMat = true;

        if ( cormatLTri == null || cormatLTri.length == 0 ) {
            hasCorrMat = false;
        } else {
            switch ( type ) {
                case mean:
                    value = getMedian( cormatLTri );
                    break;
                case median:
                    value = getMedian( cormatLTri );
                    break;
                case variance:
                    value = getVariance( cormatLTri );
                    break;
            }
        }

        score = !hasCorrMat ? -0.0001 : value;
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

}
