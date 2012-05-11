/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess.svd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Distance;
import ubic.basecode.math.KruskalWallis;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.util.EntityUtils;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

/**
 * Perform SVD on expression data and store the results.
 * 
 * @author paul
 * @version $Id$
 * @see PrincipalComponentAnalysisService
 */
@Component
public class SVDServiceHelperImpl implements SVDServiceHelper {

    /**
     * How many probe (gene) loadings to store, at most.
     */
    private static final int MAX_LOADINGS_TO_PERSIST = 1000;

    /**
     * How many components we should store probe (gene) loadings for.
     */
    private static final int MAX_NUM_COMPONENTS_TO_PERSIST = 5;

    private static final int MINIMUM_POINTS_TO_COMARE_TO_EIGENGENE = 3;

    private static final int MAX_EIGEN_GENES_TO_TEST = 5;

    private static Log log = LogFactory.getLog( SVDServiceHelperImpl.class );

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.preprocess.svd.SVDService#getImportantFactors(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, java.util.Collection, java.lang.Double)
     */
    public Set<ExperimentalFactor> getImportantFactors( ExpressionExperiment ee,
            Collection<ExperimentalFactor> experimentalFactors, Double importanceThreshold ) {
        Set<ExperimentalFactor> importantFactors = new HashSet<ExperimentalFactor>();

        if ( experimentalFactors.isEmpty() ) return importantFactors;
        Map<Long, ExperimentalFactor> factors = EntityUtils.getIdMap( experimentalFactors );
        SVDValueObject svdFactorAnalysis = this.svdFactorAnalysis( ee );
        Map<Integer, Map<Long, Double>> factorPvals = svdFactorAnalysis.getFactorPvals();
        for ( Integer cmp : factorPvals.keySet() ) {
            Map<Long, Double> factorPv = factorPvals.get( cmp );
            for ( Long efId : factorPv.keySet() ) {
                Double pvalue = factorPv.get( efId );
                ExperimentalFactor ef = factors.get( efId );

                if ( pvalue < importanceThreshold ) {
                    assert factors.containsKey( efId );
                    log.info( ef + " retained at p=" + String.format( "%.2g", pvalue ) + " for PC" + cmp );
                    importantFactors.add( ef );
                } else {
                    log.info( ef + " not retained at p=" + String.format( "%.2g", pvalue ) + " for PC" + cmp );
                }
            }
        }
        return importantFactors;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.preprocess.svd.SVDService#getTopLoadedVectors(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, int, int)
     */
    public Map<ProbeLoading, DoubleVectorValueObject> getTopLoadedVectors( ExpressionExperiment ee, int component,
            int count ) {
        PrincipalComponentAnalysis pca = principalComponentAnalysisService.loadForExperiment( ee );
        Map<ProbeLoading, DoubleVectorValueObject> result = new HashMap<ProbeLoading, DoubleVectorValueObject>();
        if ( pca == null ) {
            return result;
        }

        List<ProbeLoading> topLoadedProbes = principalComponentAnalysisService
                .getTopLoadedProbes( ee, component, count );

        if ( topLoadedProbes == null ) {
            log.warn( "No probes?" );
            return result;
        }

        Map<CompositeSequence, ProbeLoading> probes = new LinkedHashMap<CompositeSequence, ProbeLoading>();
        for ( ProbeLoading probeLoading : topLoadedProbes ) {
            CompositeSequence probe = probeLoading.getProbe();
            probes.put( probe, probeLoading );
        }

        if ( probes.isEmpty() ) return result;

        assert probes.size() <= count;

        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        ees.add( ee );
        Collection<DoubleVectorValueObject> vect = processedExpressionDataVectorService.getProcessedDataArraysByProbe(
                ees, probes.keySet(), true );
        // note that this might have come from a cache.

        /*
         * This is actually expected, because we go through the genes.
         */
        // assert vect.size() <= count : vect.size() + " vectors, expected max " + probes.keySet().size();

        BioAssayDimension bioAssayDimension = pca.getBioAssayDimension();
        assert bioAssayDimension != null;
        assert !bioAssayDimension.getBioAssays().isEmpty();

        for ( DoubleVectorValueObject vct : vect ) {
            ProbeLoading probeLoading = probes.get( vct.getDesignElement() );

            if ( probeLoading == null ) {
                /*
                 * This is okay, we will skip this probe. It was another probe for a gene that _was_ highly loaded.
                 */
                continue;
            }

            // FIXME they are not pvalues
            vct.setPvalue( ( double ) probeLoading.getLoadingRank() );
            // vct.setBioAssayDimension( bioAssayDimension );
            vct.setExpressionExperiment( ee );
            result.put( probeLoading, vct );
        }

        return result;

    }

    /**
     * @param ee
     * @return
     */
    public boolean hasPca( ExpressionExperiment ee ) {
        return retrieveSvd( ee ) != null;
    }

    /**
     * Get the SVD information for experiment with id given.
     * 
     * @param id
     * @return value or null if there isn't one.
     */
    public SVDValueObject retrieveSvd( ExpressionExperiment ee ) {
        PrincipalComponentAnalysis pca = this.principalComponentAnalysisService.loadForExperiment( ee );
        if ( pca == null ) return null;
        // pca.setBioAssayDimension( bioAssayDimensionService.thaw( pca.getBioAssayDimension() ) );
        try {
            return new SVDValueObject( pca );
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage() );
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.SVDService#svd(java.util.Collection)
     */
    public void svd( Collection<ExpressionExperiment> ees ) {
        for ( ExpressionExperiment ee : ees ) {
            svd( ee );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.SVDService#svd(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public SVDValueObject svd( ExpressionExperiment ee ) {
        assert ee != null;

        Collection<ProcessedExpressionDataVector> vectos = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( vectos );

        log.info( "Starting SVD" );
        ExpressionDataSVD svd = new ExpressionDataSVD( mat );

        log.info( "SVD done, postprocessing and storing results." );

        /*
         * Save the results
         */
        DoubleMatrix<Integer, BioMaterial> v = svd.getV();

        List<Long> bioMaterialIds = new ArrayList<Long>();
        for ( int i = 0; i < mat.columns(); i++ ) {
            bioMaterialIds.add( mat.getBioMaterialForColumn( i ).getId() );
        }

        principalComponentAnalysisService.removeForExperiment( ee );

        BioAssayDimension b = mat.getBestBioAssayDimension();

        PrincipalComponentAnalysis pca = principalComponentAnalysisService.create( ee, svd.getU(),
                svd.getEigenvalues(), v, b, MAX_NUM_COMPONENTS_TO_PERSIST, MAX_LOADINGS_TO_PERSIST );

        /*
         * Add an audit event.
         */
        auditTrailService.addUpdateEvent( ee, PCAAnalysisEvent.class, "SVD computation", null );

        return svdFactorAnalysis( pca );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.preprocess.svd.SVDService#svdFactorAnalysis(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, ubic.gemma.analysis.preprocess.svd.SVDValueObject)
     */
    public SVDValueObject svdFactorAnalysis( ExpressionExperiment ee ) {
        PrincipalComponentAnalysis pca = principalComponentAnalysisService.loadForExperiment( ee );
        if ( pca == null ) {
            log.warn( "PCA not available for this experiment" );
            return null;
        }
        return svdFactorAnalysis( pca );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.preprocess.svd.SVDService#svdFactorAnalysis(ubic.gemma.model.analysis.expression.
     * PrincipalComponentAnalysis)
     */
    public SVDValueObject svdFactorAnalysis( PrincipalComponentAnalysis pca ) {

        BioAssayDimension bad = pca.getBioAssayDimension();
        List<BioAssay> bioAssays = ( List<BioAssay> ) bad.getBioAssays();

        SVDValueObject svo;
        try {
            svo = new SVDValueObject( pca );
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage() );
            return null;
        }

        Map<Long, Date> bioMaterialDates = new HashMap<Long, Date>();
        Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap = new HashMap<ExperimentalFactor, Map<Long, Double>>();

        prepareForFactorComparisons( svo, bioAssays, bioMaterialDates, bioMaterialFactorMap );

        if ( bioMaterialDates.isEmpty() && bioMaterialFactorMap.isEmpty() ) {
            log.warn( "No factor or date information to compare to the eigengenes" );
            return svo;
        }

        Long[] svdBioMaterials = svo.getBioMaterialIds();

        svo.getDateCorrelations().clear();
        svo.getFactorCorrelations().clear();
        svo.getDates().clear();
        svo.getFactors().clear();

        for ( int componentNumber = 0; componentNumber < Math.min( svo.getvMatrix().columns(), MAX_EIGEN_GENES_TO_TEST ); componentNumber++ ) {
            analyzeComponent( svo, componentNumber, svo.getvMatrix(), bioMaterialDates, bioMaterialFactorMap,
                    svdBioMaterials );
        }

        return svo;
    }

    /**
     * Do the factor comparisons for one component.
     * 
     * @param svo
     * @param componentNumber
     * @param vMatrix
     * @param bioMaterialDates
     * @param bioMaterialFactorMap Map of factors to biomaterials to the value we're going to use. Even for
     *        non-continuous factors the value is a double.
     * @param svdBioMaterials
     */
    private void analyzeComponent( SVDValueObject svo, int componentNumber, DoubleMatrix<Long, Integer> vMatrix,
            Map<Long, Date> bioMaterialDates, Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap,
            Long[] svdBioMaterials ) {
        DoubleArrayList eigenGene = new DoubleArrayList( vMatrix.getColumn( componentNumber ) );
        // since we use rank correlation/anova, we just use the casted ids (two-groups) or dates as the covariate

        int numWithDates = 0;
        for ( Long id : bioMaterialDates.keySet() ) {
            if ( bioMaterialDates.get( id ) != null ) {
                numWithDates++;
            }
        }

        if ( numWithDates > 2 ) {
            /*
             * Get the dates in order, rounded to the nearest hour.
             */
            boolean initializingDates = svo.getDates().isEmpty();
            double[] dates = new double[svdBioMaterials.length];
            for ( int j = 0; j < svdBioMaterials.length; j++ ) {

                Date date = bioMaterialDates.get( svdBioMaterials[j] );
                if ( date == null ) {
                    log.warn( "Incomplete date information, missing for biomaterial " + svdBioMaterials[j] );
                    dates[j] = Double.NaN;
                } else {
                    dates[j] = DateUtils.round( date, Calendar.MINUTE ).getTime(); // make int, cast to double
                }
                if ( initializingDates ) svo.getDates().add( date );
            }
            initializingDates = false;

            if ( eigenGene.size() != dates.length ) {
                log.warn( "Could not compute correlation, dates and eigenGene had different lengths." );
                return;
            }

            double dateCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( dates ) );

            svo.setPCDateCorrelation( componentNumber, dateCorrelation );
            svo.setPCDateCorrelationPval( componentNumber,
                    CorrelationStats.spearmanPvalue( dateCorrelation, eigenGene.size() ) );
        }

        /*
         * Compare each factor (including batch information that is somewhat redundant with the dates) to the
         * eigengenes. Using rank statistics.
         */
        for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {
            Map<Long, Double> bmToFv = bioMaterialFactorMap.get( ef );

            double[] fvs = new double[svdBioMaterials.length];
            assert fvs.length > 0;

            int numNotMissing = 0;

            boolean initializing = false;
            if ( !svo.getFactors().containsKey( ef.getId() ) ) {
                svo.getFactors().put( ef.getId(), new ArrayList<Double>() );
                initializing = true;
            }

            for ( int j = 0; j < svdBioMaterials.length; j++ ) {
                fvs[j] = bmToFv.get( svdBioMaterials[j] ).doubleValue();
                if ( !Double.isNaN( fvs[j] ) ) {
                    numNotMissing++;
                }
                // note that this is a double. In the case of categorical factors, it's the Doubleified ID of the factor
                // value.
                if ( initializing ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "EF:" + ef.getId() + " fv=" + bmToFv.get( svdBioMaterials[j] ) );
                    svo.getFactors().get( ef.getId() ).add( bmToFv.get( svdBioMaterials[j] ) );
                }
            }

            if ( fvs.length != eigenGene.size() ) {
                log.debug( fvs.length + " factor values (biomaterials) but " + eigenGene.size()
                        + " values in the eigengene" );
                continue;
            }

            initializing = false;

            if ( numNotMissing < MINIMUM_POINTS_TO_COMARE_TO_EIGENGENE ) {
                log.debug( "Insufficient values to compare " + ef + " to eigengenes" );
                continue;
            }

            if ( ExperimentalDesignUtils.isContinuous( ef ) ) {
                double factorCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( fvs ) );
                svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );
                // FIXME might need to deal with missing values here too.
                svo.setPCFactorCorrelationPval( componentNumber, ef,
                        CorrelationStats.spearmanPvalue( factorCorrelation, eigenGene.size() ) );
            } else {

                Collection<Integer> groups = new HashSet<Integer>();
                IntArrayList groupings = new IntArrayList( fvs.length );
                int k = 0;
                DoubleArrayList eigenGeneWithoutMissing = new DoubleArrayList();
                for ( double d : fvs ) {
                    if ( Double.isNaN( d ) ) {
                        k++;
                        continue;
                    }
                    groupings.add( ( int ) d );
                    groups.add( ( int ) d );
                    eigenGeneWithoutMissing.add( eigenGene.get( k ) );
                    k++;
                }

                if ( groups.size() < 2 ) {
                    log.debug( "Factor had less than two groups: " + ef + ", SVD comparison can't be done." );
                    continue;
                }

                if ( eigenGeneWithoutMissing.size() < MINIMUM_POINTS_TO_COMARE_TO_EIGENGENE ) {
                    log.debug( "Too few non-missing values for factor to compare to eigengenes: " + ef );
                    continue;
                }

                if ( groups.size() == 2 ) {
                    // use the one that still has missing values.
                    double factorCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( fvs ) );
                    svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );
                    svo.setPCFactorCorrelationPval( componentNumber, ef,
                            CorrelationStats.spearmanPvalue( factorCorrelation, eigenGeneWithoutMissing.size() ) );
                } else {
                    // one-way ANOVA on ranks.
                    double kwpval = KruskalWallis.test( eigenGeneWithoutMissing, groupings );

                    svo.setPCFactorCorrelationPval( componentNumber, ef, kwpval );

                    double factorCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( fvs ) );
                    double corrPvalue = CorrelationStats.spearmanPvalue( factorCorrelation,
                            eigenGeneWithoutMissing.size() );
                    assert Math.abs( factorCorrelation ) < 1.0 + 1e-2; // sanity.
                    /*
                     * Avoid storing a pvalue, as it's hard to compare. If the regular linear correlation is strong,
                     * then we should just use that -- basically, it means the order we have the groups happens to be a
                     * good one. Of course we could just store pvalues, but that's not easy to use either.
                     */
                    if ( corrPvalue <= kwpval ) {
                        svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );
                    } else {
                        // hack. A bit like turning pvalues into probit
                        double approxCorr = CorrelationStats.correlationForPvalue( kwpval,
                                eigenGeneWithoutMissing.size() );
                        svo.setPCFactorCorrelation( componentNumber, ef, approxCorr );

                    }
                }

            }
        }
    }

   
    /**
     * Fill in NaN for any missing biomaterial factorvalues (dates were already done)
     */
    private void fillInMissingValues( Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap,
            Long[] svdBioMaterials ) {

        for ( Long id : svdBioMaterials ) {
            for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {
                if ( !bioMaterialFactorMap.get( ef ).containsKey( id ) ) {
                    /*
                     * Missing values in factors, not fatal but not great either.
                     */
                    if ( log.isDebugEnabled() )
                        log.debug( "Incomplete factorvalue information for " + ef + " (biomaterial id=" + id
                                + " missing a value)" );
                    bioMaterialFactorMap.get( ef ).put( id, Double.NaN );
                }
            }
        }
    }

    /**
     * @param bioAssays
     * @param bioMaterialDates
     * @param bioMaterialFactorMap
     */
    private void getFactorsForAnalysis( Collection<BioAssay> bioAssays, Map<Long, Date> bioMaterialDates,
            Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap ) {
        for ( BioAssay bioAssay : bioAssays ) {
            Date processingDate = bioAssay.getProcessingDate();
            for ( BioMaterial bm : bioAssay.getSamplesUsed() ) {
                bioMaterialDates.put( bm.getId(), processingDate ); // can be null

                for ( FactorValue fv : bm.getFactorValues() ) {

                    ExperimentalFactor experimentalFactor = fv.getExperimentalFactor();

                    if ( !bioMaterialFactorMap.containsKey( experimentalFactor ) ) {
                        bioMaterialFactorMap.put( experimentalFactor, new HashMap<Long, Double>() );
                    }

                    double valueToStore;
                    if ( fv.getMeasurement() != null ) {
                        try {
                            valueToStore = Double.parseDouble( fv.getMeasurement().getValue() );
                        } catch ( NumberFormatException e ) {
                            log.warn( "Measurement wasn't a number for " + fv );
                            valueToStore = Double.NaN;
                        }

                    } else {
                        /*
                         * This is a hack. We're storing the ID but as a double.
                         */
                        valueToStore = fv.getId().doubleValue();
                    }
                    bioMaterialFactorMap.get( experimentalFactor ).put( bm.getId(), valueToStore );
                }

            }
        }
    }

    /**
     * Gather the information we need for comparing PCs to factors.
     * 
     * @param svo
     * @param bioAssays
     * @param bioMaterialDates
     * @param bioMaterialFactorMap
     */
    private void prepareForFactorComparisons( SVDValueObject svo, Collection<BioAssay> bioAssays,
            Map<Long, Date> bioMaterialDates, Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap ) {
        /*
         * Note that dates or batch information can be missing for some bioassays.
         */

        getFactorsForAnalysis( bioAssays, bioMaterialDates, bioMaterialFactorMap );
        Long[] svdBioMaterials = svo.getBioMaterialIds();

        if ( svdBioMaterials == null || svdBioMaterials.length == 0 ) {
            throw new IllegalStateException( "SVD did not have biomaterial information" );
        }

        fillInMissingValues( bioMaterialFactorMap, svdBioMaterials );

    }

}
