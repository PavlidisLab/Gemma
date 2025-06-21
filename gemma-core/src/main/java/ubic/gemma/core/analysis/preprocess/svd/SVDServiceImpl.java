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
package ubic.gemma.core.analysis.preprocess.svd;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Distance;
import ubic.basecode.math.KruskalWallis;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

import static ubic.gemma.model.expression.experiment.ExperimentalDesignUtils.measurement2double;

/**
 * Perform SVD on expression data and store the results.
 *
 * @author paul
 * @see    PrincipalComponentAnalysisService
 */
@Service
public class SVDServiceImpl implements SVDService {

    /**
     * How many probe (gene) loadings to store, at most.
     */
    private static final int MAX_LOADINGS_TO_PERSIST = 1000;

    /**
     * How many components we should store probe (gene) loadings for.
     */
    private static final int MAX_NUM_COMPONENTS_TO_PERSIST = 5;

    private static final int MINIMUM_POINTS_TO_COMPARE_TO_EIGEN_GENE = 3;

    private static final int MAX_EIGEN_GENES_TO_TEST = 5;

    private static final Log log = LogFactory.getLog( SVDServiceImpl.class );

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * Retrieve relationships between factors, biomaterials and factor values.
     * <p>
     * Continuous factor values are converted to a {@link Double} if possible, otherwise the {@link FactorValue#getId()}
     * is used. This is the case for categorical measurement.
     * @param bioMaterialFactorMap to be populated, of experimental factor -&gt; biomaterial ID -&gt; factor value
     *                             (double value if possible otherwise ID)
     * @param bm                   to populate for
     */
    public static void populateBMFMap( Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap,
            BioMaterial bm ) {
        for ( FactorValue fv : bm.getAllFactorValues() ) {
            ExperimentalFactor experimentalFactor = fv.getExperimentalFactor();
            Number valueToStore;
            if ( experimentalFactor.getType() == FactorType.CONTINUOUS ) {
                if ( fv.getMeasurement() != null ) { // continuous
                    valueToStore = measurement2double( fv.getMeasurement() );
                } else {
                    log.warn( fv + " is continuous, but lacking a measurement, will use NaN." );
                    valueToStore = Double.NaN;
                }
            } else {
                // for categorical factors, we use the ids as dummy values.
                valueToStore = fv.getId();
            }

            bioMaterialFactorMap
                    .computeIfAbsent( experimentalFactor, k -> new HashMap<>() )
                    .put( bm, valueToStore );
        }
    }

    /**
     * Get the SVD information for experiment with id given.
     *
     * @return value or null if there isn't one.
     */
    @Override
    @Transactional(readOnly = true)
    public SVDResult getSvd( ExpressionExperiment ee ) {
        PrincipalComponentAnalysis pca = this.principalComponentAnalysisService.loadForExperiment( ee );
        if ( pca == null )
            return null;
        // pca.setBioAssayDimension( bioAssayDimensionService.thawRawAndProcessed( pca.getBioAssayDimension() ) );
        try {
            return new SVDResult( pca );
        } catch ( Exception e ) {
            SVDServiceImpl.log.error( e.getLocalizedMessage() );
            return null;
        }
    }

    @Override
    @Transactional
    public SVDResult svd( ExpressionExperiment ee ) throws SVDException {
        assert ee != null;

        Collection<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService
                .getProcessedDataVectorsAndThaw( ee );

        if ( vectors.isEmpty() ) {
            throw new IllegalArgumentException( "Experiment must have processed data already to do SVD" );
        }

        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( vectors );

        SVDServiceImpl.log.info( "Starting SVD" );
        ExpressionDataSVD svd = new ExpressionDataSVD( mat );

        SVDServiceImpl.log.info( "SVD done, postprocessing and storing results." );

        /*
         * Save the results
         */
        DoubleMatrix<Integer, BioMaterial> v = svd.getV();

        BioAssayDimension b = mat.getBioAssayDimension();

        PrincipalComponentAnalysis pca = this.updatePca( ee, svd, v, b );

        return this.getSvdFactorAnalysis( pca );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ProbeLoading, DoubleVectorValueObject> getTopLoadedVectors( ExpressionExperiment ee, int component,
            int count ) {
        ee = expressionExperimentService.thawLite( ee );

        PrincipalComponentAnalysis pca = principalComponentAnalysisService.loadForExperiment( ee );
        Map<ProbeLoading, DoubleVectorValueObject> result = new HashMap<>();
        if ( pca == null ) {
            return result;
        }

        List<ProbeLoading> topLoadedProbes = principalComponentAnalysisService
                .getTopLoadedProbes( ee, component, count );

        if ( topLoadedProbes == null ) {
            SVDServiceImpl.log.warn( "No probes?" );
            return result;
        }

        Map<Long, ProbeLoading> probes = new LinkedHashMap<>();
        Set<CompositeSequence> p = new HashSet<>();
        for ( ProbeLoading probeLoading : topLoadedProbes ) {
            CompositeSequence probe = probeLoading.getProbe();
            probes.put( probe.getId(), probeLoading );
            p.add( probe );
        }

        if ( probes.isEmpty() )
            return result;

        assert probes.size() <= count;

        Collection<DoubleVectorValueObject> dvVos = processedExpressionDataVectorService
                .getProcessedDataArraysByProbe( ee, p );

        if ( dvVos.isEmpty() ) {
            SVDServiceImpl.log.warn( "No vectors came back from the call; check the Gene2CS table?" );
            return result;
        }

        // note that this might have come from a cache.

        /*
         * This is actually expected, because we go through the genes.
         */

        BioAssayDimension bioAssayDimension = pca.getBioAssayDimension();
        assert bioAssayDimension != null;
        assert !bioAssayDimension.getBioAssays().isEmpty();

        for ( DoubleVectorValueObject vct : dvVos ) {
            ProbeLoading probeLoading = probes.get( vct.getDesignElement().getId() );

            if ( probeLoading == null ) {
                /*
                 * This is okay, we will skip this probe. It was another probe for a gene that _was_ highly loaded.
                 */
                continue;
            }

            assert bioAssayDimension.getBioAssays().size() == vct.getData().length;

            // create a copy because we're going to modify its rank
            vct = vct.copy();
            vct.setRank( probeLoading.getLoadingRank().doubleValue() );
            result.put( probeLoading, vct );
        }

        if ( result.isEmpty() ) {
            SVDServiceImpl.log.warn( "No results, something went wrong; there were " + dvVos.size()
                    + " vectors to start but they all got filtered out." );
        }

        return result;

    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPca( ExpressionExperiment ee ) {
        return this.getSvd( ee ) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<ExperimentalFactor> getImportantFactors( ExpressionExperiment ee,
            Collection<ExperimentalFactor> experimentalFactors, Double importanceThreshold ) {
        Set<ExperimentalFactor> importantFactors = new HashSet<>();

        if ( experimentalFactors.isEmpty() ) {
            return importantFactors;
        }
        SVDResult svdFactorAnalysis = this.getSvdFactorAnalysis( ee );
        if ( svdFactorAnalysis == null ) {
            return importantFactors;
        }
        Map<Integer, Map<ExperimentalFactor, Double>> factorPVals = svdFactorAnalysis.getFactorPVals();
        for ( Integer cmp : factorPVals.keySet() ) {
            Map<ExperimentalFactor, Double> factorPv = factorPVals.get( cmp );
            for ( ExperimentalFactor ef : factorPv.keySet() ) {
                Double pvalue = factorPv.get( ef );

                if ( pvalue < importanceThreshold ) {
                    SVDServiceImpl.log
                            .info( ef + " retained at p=" + String.format( "%.2g", pvalue ) + " for PC" + cmp );
                    importantFactors.add( ef );
                } else {
                    SVDServiceImpl.log
                            .debug( ef + " not retained at p=" + String.format( "%.2g", pvalue ) + " for PC" + cmp );
                }
            }
        }
        if ( !importantFactors.isEmpty() ) {
            SVDServiceImpl.log.warn( importantFactors.size() + " factors chosen as important" );
        }
        return importantFactors;
    }

    @Override
    @Transactional(readOnly = true)
    public SVDResult getSvdFactorAnalysis( ExpressionExperiment ee ) {
        PrincipalComponentAnalysis pca = principalComponentAnalysisService.loadForExperiment( ee );
        if ( pca == null ) {
            SVDServiceImpl.log.warn( "PCA not available for this experiment" );
            return null;
        }
        return this.getSvdFactorAnalysis( pca );
    }

    @Override
    @Transactional(readOnly = true)
    public SVDResult getSvdFactorAnalysis( PrincipalComponentAnalysis pca ) {

        BioAssayDimension bad = pca.getBioAssayDimension();
        List<BioAssay> bioAssays = bad.getBioAssays();

        SVDResult svo;
        try {
            svo = new SVDResult( pca );
        } catch ( Exception e ) {
            SVDServiceImpl.log.error( e.getLocalizedMessage() );
            return null;
        }

        Map<BioMaterial, Date> bioMaterialDates = new HashMap<>();
        Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap = new HashMap<>();

        this.prepareForFactorComparisons( svo, bioAssays, bioMaterialDates, bioMaterialFactorMap );

        if ( bioMaterialDates.isEmpty() && bioMaterialFactorMap.isEmpty() ) {
            SVDServiceImpl.log.warn( "No factor or date information to compare to the eigenGenes" );
            return svo;
        }

        List<BioMaterial> svdBioMaterials = svo.getBioMaterials();

        svo.getDateCorrelations().clear();
        svo.getFactorCorrelations().clear();
        svo.getDates().clear();
        svo.getFactors().clear();

        for ( int componentNumber = 0; componentNumber < Math
                .min( svo.getVMatrix().columns(), SVDServiceImpl.MAX_EIGEN_GENES_TO_TEST ); componentNumber++ ) {
            this.analyzeComponent( svo, componentNumber, svo.getVMatrix(), bioMaterialDates, bioMaterialFactorMap,
                    svdBioMaterials );
        }

        return svo;
    }

    /**
     * Do the factor comparisons for one component.
     *
     * @param bioMaterialFactorMap Map of factors to biomaterials to the value we're going to use. Even for
     *                             non-continuous factors the value is a double.
     */
    private void analyzeComponent( SVDResult svo, int componentNumber, DoubleMatrix<BioMaterial, Integer> vMatrix,
            Map<BioMaterial, Date> bioMaterialDates, Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap,
            List<BioMaterial> svdBioMaterials ) {
        DoubleArrayList eigenGene = new DoubleArrayList( vMatrix.getColumn( componentNumber ) );
        // since we use rank correlation/anova, we just use the casted ids (two-groups) or dates as the covariate

        int numWithDates = 0;
        for ( BioMaterial bm : bioMaterialDates.keySet() ) {
            if ( bioMaterialDates.get( bm ) != null ) {
                numWithDates++;
            }
        }

        if ( numWithDates > 2 ) {
            /*
             * Get the dates in order, - no rounding.
             */
            boolean initializingDates = svo.getDates().isEmpty();
            double[] dates = new double[svdBioMaterials.size()];

            /*
             * If dates are all the same, skip.
             */
            Set<Date> uniqueDate = new HashSet<>();

            for ( int j = 0; j < svdBioMaterials.size(); j++ ) {

                Date date = bioMaterialDates.get( svdBioMaterials.get( j ) );
                if ( date == null ) {
                    SVDServiceImpl.log
                            .warn( "Incomplete date information, missing for biomaterial " + svdBioMaterials.get( j ) );
                    dates[j] = Double.NaN;
                } else {
                    Date roundDate = DateUtils.round( date, Calendar.MINUTE );
                    uniqueDate.add( roundDate );
                    dates[j] = roundDate.getTime(); // round to minute; make int, cast to
                    // double
                }

                if ( initializingDates ) {
                    svo.getDates().add( date );
                }
            }

            if ( uniqueDate.size() == 1 ) {
                SVDServiceImpl.log.warn( "All scan dates the same, skipping data analysis" );
                svo.getDates().clear();
            }

            if ( eigenGene.size() != dates.length ) {
                SVDServiceImpl.log
                        .warn( "Could not compute correlation, dates and eigenGene had different lengths." );
                return;
            }

            double dateCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( dates ) );

            svo.setPCDateCorrelation( componentNumber, dateCorrelation );
            svo.setPCDateCorrelationPval( componentNumber,
                    CorrelationStats.spearmanPvalue( dateCorrelation, eigenGene.size() ) );
        }

        /*
         * Compare each factor (including batch information that is somewhat redundant with the dates) to the
         * eigen-genes. Using rank statistics.
         */
        for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {
            Map<BioMaterial, Number> bmToFv = bioMaterialFactorMap.get( ef );

            double[] fvs = new double[svdBioMaterials.size()];
            assert fvs.length > 0;

            int numNotMissing = 0;

            boolean initializing = false;
            if ( !svo.getFactors().containsKey( ef ) ) {
                svo.getFactors().put( ef, new ArrayList<>() );
                initializing = true;
            }

            for ( int j = 0; j < svdBioMaterials.size(); j++ ) {
                fvs[j] = bmToFv.get( svdBioMaterials.get( j ) ).doubleValue();
                if ( !Double.isNaN( fvs[j] ) ) {
                    numNotMissing++;
                }
                // note that this is a double. In the case of categorical factors, it's the Double-fied ID of the factor
                // value.
                if ( initializing ) {
                    if ( SVDServiceImpl.log.isDebugEnabled() )
                        SVDServiceImpl.log
                                .debug( "EF:" + ef.getId() + " fv=" + bmToFv.get( svdBioMaterials.get( j ) ) );
                    svo.getFactors().get( ef ).add( bmToFv.get( svdBioMaterials.get( j ) ) );
                }
            }

            if ( fvs.length != eigenGene.size() ) {
                SVDServiceImpl.log.debug( fvs.length + " factor values (biomaterials) but " + eigenGene.size()
                        + " values in the eigenGene" );
                continue;
            }

            if ( numNotMissing < SVDServiceImpl.MINIMUM_POINTS_TO_COMPARE_TO_EIGEN_GENE ) {
                SVDServiceImpl.log.debug( "Insufficient values to compare " + ef + " to eigenGenes" );
                continue;
            }

            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
                double factorCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( fvs ) );
                svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );
                svo.setPCFactorCorrelationPval( componentNumber, ef,
                        CorrelationStats.spearmanPvalue( factorCorrelation, eigenGene.size() ) );
            } else {

                Collection<Integer> groups = new HashSet<>();
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
                    SVDServiceImpl.log
                            .debug( "Factor had less than two groups: " + ef + ", SVD comparison can't be done." );
                    continue;
                }

                if ( eigenGeneWithoutMissing.size() < SVDServiceImpl.MINIMUM_POINTS_TO_COMPARE_TO_EIGEN_GENE ) {
                    SVDServiceImpl.log
                            .debug( "Too few non-missing values for factor to compare to eigenGenes: " + ef );
                    continue;
                }

                if ( groups.size() == 2 ) {
                    // use the one that still has missing values.
                    double factorCorrelation = Distance
                            .spearmanRankCorrelation( eigenGene, new DoubleArrayList( fvs ) );
                    svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );
                    svo.setPCFactorCorrelationPval( componentNumber, ef,
                            CorrelationStats.spearmanPvalue( factorCorrelation, eigenGeneWithoutMissing.size() ) );
                } else {
                    // one-way ANOVA on ranks. This test is pretty underpowered.
                    double kwPVal = KruskalWallis.test( eigenGeneWithoutMissing, groupings );

                    svo.setPCFactorCorrelationPval( componentNumber, ef, kwPVal );

                    double factorCorrelation = Distance
                            .spearmanRankCorrelation( eigenGene, new DoubleArrayList( fvs ) );
                    double corrPvalue = CorrelationStats
                            .spearmanPvalue( factorCorrelation, eigenGeneWithoutMissing.size() );
                    assert Math.abs( factorCorrelation ) < 1.0 + 1e-2; // sanity.

                    /*
                     * If the regular linear correlation is strong,
                     * then we should just use that -- basically, it means the order we have the groups happens to be a
                     * good one.
                     */
                    if ( corrPvalue <= kwPVal ) {
                        svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );
                        svo.setPCFactorCorrelationPval( componentNumber, ef, corrPvalue );
                    } else {
                        // hack. A bit like turning pvalues into prob it
                        double approxCorr = CorrelationStats
                                .correlationForPvalue( kwPVal, eigenGeneWithoutMissing.size() );
                        svo.setPCFactorCorrelation( componentNumber, ef, approxCorr );
                    }
                }

            }
        }
    }

    /**
     * Fill in NaN for any missing biomaterial factorValues (dates were already done)
     */
    private void fillInMissingValues( Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap,
            List<BioMaterial> svdBioMaterials ) {

        for ( BioMaterial bm : svdBioMaterials ) {
            for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {
                if ( !bioMaterialFactorMap.get( ef ).containsKey( bm ) ) {
                    /*
                     * Missing values in factors, not fatal but not great either.
                     */
                    if ( SVDServiceImpl.log.isDebugEnabled() )
                        SVDServiceImpl.log
                                .debug( "Incomplete factorvalue information for " + ef + " (" + bm
                                        + " missing a value)" );
                    bioMaterialFactorMap.get( ef ).put( bm, Double.NaN );
                }
            }
        }
    }

    private void getFactorsForAnalysis( Collection<BioAssay> bioAssays, Map<BioMaterial, Date> bioMaterialDates,
            Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap ) {
        for ( BioAssay bioAssay : bioAssays ) {
            Date processingDate = bioAssay.getProcessingDate();
            BioMaterial bm = bioAssay.getSampleUsed();
            bioMaterialDates.put( bm, processingDate ); // can be null
            SVDServiceImpl.populateBMFMap( bioMaterialFactorMap, bm );
        }
    }

    /**
     * Gather the information we need for comparing PCs to factors.
     */
    private void prepareForFactorComparisons( SVDResult svo, Collection<BioAssay> bioAssays,
            Map<BioMaterial, Date> bioMaterialDates, Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap ) {
        /*
         * Note that dates or batch information can be missing for some bioassays.
         */

        this.getFactorsForAnalysis( bioAssays, bioMaterialDates, bioMaterialFactorMap );
        List<BioMaterial> svdBioMaterials = svo.getBioMaterials();

        if ( svdBioMaterials == null || svdBioMaterials.isEmpty() ) {
            throw new IllegalStateException( "SVD did not have biomaterial information" );
        }

        this.fillInMissingValues( bioMaterialFactorMap, svdBioMaterials );

    }

    private PrincipalComponentAnalysis updatePca( ExpressionExperiment ee, ExpressionDataSVD svd,
            DoubleMatrix<Integer, BioMaterial> v, BioAssayDimension b ) {
        principalComponentAnalysisService.removeForExperiment( ee );
        PrincipalComponentAnalysis pca = principalComponentAnalysisService
                .create( ee, svd.getU(), svd.getEigenvalues(), v, b, SVDServiceImpl.MAX_NUM_COMPONENTS_TO_PERSIST,
                        SVDServiceImpl.MAX_LOADINGS_TO_PERSIST );

        ee = expressionExperimentService.thawLite( ee ); // I wish this wasn't needed.
        auditTrailService.addUpdateEvent( ee, PCAAnalysisEvent.class, "SVD computation" );
        return pca;
    }
}
