/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.controller.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.core.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.visualization.ExperimentalDesignVisualizationService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.web.controller.ControllerUtils;
import ubic.gemma.web.controller.visualization.VisualizationValueObject;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.view.TextView;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.Map.Entry;

import static ubic.gemma.core.util.Constants.GEMMA_CITATION_NOTICE;
import static ubic.gemma.core.util.Constants.GEMMA_LICENSE_NOTICE;
import static ubic.gemma.core.util.TsvUtils.format;

/**
 * Exposes methods for accessing underlying Design Element Data Vectors. eg: ajax methods for visualization
 *
 * @author kelsey
 */
@SuppressWarnings("unused")
@Controller
@RequestMapping("/dedv")
public class DEDVController {
    private static final double DEFAULT_THRESHOLD = 0.05;
    private static final int MAX_RESULTS_TO_RETURN = 150;
    private static final int SAMPLE_SIZE = 20; // Number of dedvs to return if no genes given
    private static final Log log = LogFactory.getLog( DEDVController.class.getName() );
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    private ExperimentalDesignVisualizationService experimentalDesignVisualizationService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    @Autowired
    private GeneDifferentialExpressionService geneDifferentialExpressionService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private ExperimentalFactorService experimentalFactorService;
    @Autowired
    private BuildInfo buildInfo;

    /**
     * Assign colour lists (queues actually) to factors. The idea is that every factor value will get a colour assigned
     * from its factor's list.
     */
    private static Map<ExperimentalFactor, Queue<String>> createFactorNameToColoursMap(
            Collection<ExperimentalFactor> factors ) {

        String[] continuousGreen = { "#f7fcfd", "#e5f5f9", "#ccece6", "#99d8c9", "#66c2a4", "#41ae76", "#238b45",
                "#006d2c", "#00441b" };

        String[] continuousBlue = { "#f7fbff", "#deebf7", "#c6dbef", "#9ecae1", "#6baed6", "#4292c6", "#2171b5",
                "#08519c", "#08306b" };
        String[][] colourContinuousArrs = { continuousGreen, continuousBlue };
        // colours for conditions/factor values bar chart
        Map<ExperimentalFactor, Queue<String>> factorColoursMap = new HashMap<>();
        String[] blues = { "#85c6ff", "#6b90ff", "#105bfe", "#005589", "#0090e9", "#0400fe", "#008998", "#3e3c90",
                "#020090", "#105bfe" };
        String[] purples = { "#d19bff", "#a30064", "#7d00ea", "#893984", "#f05eb8", "#9c00d0", "#b66ccf", "#e7008f",
                "#670089", "#bf00b2", "#890080", "#8865a6", "#3f0076" };
        String[] redYellows = { "#ffd78d", "#d85d00", "#b40101", "#944343", "#ff6d48", "#d36b62", "#ff8001", "#c74f34",
                "#d89561", "#f8bc2e" };
        String[] greens = { "#98da95", "#82b998", "#257e21", "#36b52f", "#38b990", "#a9da5f", "#4cfe42", "#73c000",
                "#0fa345", "#99fe01", "#508500" };

        String[][] colourArrs = { blues, greens, purples, redYellows };

        int j = 0;
        for ( ExperimentalFactor factor : factors ) {

            if ( !factorColoursMap.containsKey( factor ) ) {
                factorColoursMap.put( factor, new LinkedList<String>() );
            }

            if ( factor.getType().equals( FactorType.CONTINUOUS ) ) {

                int numValues = factor.getFactorValues().size();

                int map = j % colourContinuousArrs.length;

                int numReps = 1;
                if ( numValues > colourContinuousArrs[map].length ) {
                    numReps = ( int ) Math.ceil( numValues / ( double ) colourContinuousArrs[map].length );
                }

                int ind = 0;
                int nr = 0;
                for ( int i = 0; i < numValues; i++ ) {
                    factorColoursMap.get( factor ).add( colourContinuousArrs[map][ind] ); // array to queue
                    if ( numReps > 1 ) {
                        nr++;
                    }
                    if ( nr > numReps ) {
                        ind++;
                        nr = 0;
                    }
                }
            } else {
                int map = j % colourArrs.length;
                for ( int i = 0; i < colourArrs[map].length; i++ ) {
                    factorColoursMap.get( factor ).add( colourArrs[map][i] ); // array to queue
                }
            }
            j++;

        }

        return factorColoursMap;
    }

    /**
     * Given a collection of expression experiment Ids and a geneId returns a map of DEDV value objects to a collection
     * of genes. The EE info is in the value object. FIXME handle subsets.
     */
    public Map<BioAssaySetValueObject, Map<Long, Collection<DoubleVectorValueObject>>> getDEDV(
            Collection<Long> eeIds, Collection<Long> geneIds ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.load( eeIds );
        if ( ees == null || ees.isEmpty() )
            return null;

        Collection<DoubleVectorValueObject> dedvMap;

        if ( geneIds == null || geneIds.isEmpty() ) {
            dedvMap = processedExpressionDataVectorService.getRandomProcessedDataArrays( ees.iterator().next(), 50 );
        } else {
            dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees, geneIds );
        }

        /*
         * Don't reorganize them -- the headings will be wrong.
         */
        Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;
        //
        // layouts= experimentalDesignVisualizationService .sortVectorDataByDesign( dedvMap );

        watch.stop();
        Long time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Retrieved " + dedvMap.size() + " DEDVs from " + eeIds.size() + " EEs in " + time + " ms." );
        }

        return makeVectorMap( dedvMap, layouts );

    }

    /**
     * AJAX exposed method
     */
    public VisualizationValueObject[] getDEDVForCoexpressionVisualization( Collection<Long> eeIds, Long queryGeneId,
            Long coexpressedGeneId ) {

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.load( eeIds );
        if ( ees == null || ees.isEmpty() )
            return new VisualizationValueObject[0];

        Gene queryGene = geneService.loadOrFail( queryGeneId );
        Gene coexpressedGene = geneService.loadOrFail( coexpressedGeneId );

        List<Long> genes = new ArrayList<>();
        genes.add( queryGeneId );
        genes.add( coexpressedGeneId );

        if ( genes.isEmpty() )
            return new VisualizationValueObject[0];

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService
                .getProcessedDataArrays( ees, genes );

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts;

        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs, null );

        // layouts = experimentalDesignVisualizationService.sortLayoutSamplesByFactor( layouts );
        watch.stop();
        Long time = watch.getTime();

        if ( dedvs.size() == 0 ) {
            log.warn( "No expression profiles (DEDVs) were available for the experiments:  " + eeIds + " and genes(s) "
                    + queryGene.getOfficialSymbol() + ", " + coexpressedGene.getOfficialSymbol() );
            return new VisualizationValueObject[0];
        }

        if ( time > 1000 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + genes.size()
                    + " genes in " + time + " ms." );
        }

        Map<Long, Collection<Long>> validatedProbes = getProbeLinkValidation( ees, queryGene, coexpressedGene, dedvs );

        return makeVisCollection( dedvs, genes, validatedProbes, layouts );

    }

    /**
     * AJAX exposed method - for ProbeLevelDiffExGrid, VisualizationDifferentialWindow,
     * DifferentialExpressionAnalysesSummaryTree
     *
     * @param eeIds     FIXME accommodate ExpressionExperimentSubSets. Currently we pass in the "source experiment" so we
     *                  don't get the slice.
     * @param geneIds   (could be just one)
     * @param threshold for 'significance'
     * @param factorMap Collection of DiffExpressionSelectedFactorCommand showing which factors to use.
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualization( Collection<Long> eeIds, Collection<Long> geneIds,
            Double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( eeIds.isEmpty() || geneIds.isEmpty() )
            return null;

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.load( eeIds );
        if ( ees == null || ees.isEmpty() )
            return null;
        Collection<Gene> genes = geneService.load( geneIds );
        if ( genes == null || genes.isEmpty() )
            return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService
                .getProcessedDataArrays( ees, geneIds );

        watch.stop();
        Long time = watch.getTime();

        log.info(
                "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size() + " genes in "
                        + time + " ms." );

        watch = new StopWatch();
        watch.start();

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts;
        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs, null );

        time = watch.getTime();
        if ( time > 100 ) {
            log.info( "Ran sortVectorDataByDesign on " + dedvs.size() + " DEDVs for 1 EE" + " in " + time
                    + " ms (times <100ms not reported)." );
        }

        // layouts = experimentalDesignVisualizationService.sortLayoutSamplesByFactor( layouts ); // required? yes, see
        // GSE11859

        time = watch.getTime();
        if ( time > 100 ) {
            log.info( "Ran sortLayoutSamplesByFactor on " + layouts.size() + " layouts" + " in " + time
                    + " ms (times <100ms not reported)." );
        }

        watch = new StopWatch();
        watch.start();
        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = getProbeDiffExValidation( genes,
                threshold, factorMap );

        watch.stop();
        time = watch.getTime();
        log.info( "Retrieved " + validatedProbes.size() + " valid probes in " + time + " ms." );

        return makeDiffVisCollection( dedvs, new ArrayList<>( geneIds ), validatedProbes, layouts );

    }

    /**
     * AJAX exposed method Batch factor value analyses are filtered out; for
     * ProbeLevelDiffExGrid:VisualizationDifferentialWindow.
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualizationByExperiment( Long eeId, Long geneId,
            Double threshold, Boolean isSubset ) {

        if ( geneId == null ) {
            throw new IllegalArgumentException( "Gene ID cannot be null" );
        }

        StopWatch watch = new StopWatch();
        watch.start();
        BioAssaySet ee;
        if ( isSubset ) {
            ee = expressionExperimentSubSetService.load( eeId );
        } else {
            ee = expressionExperimentService.load( eeId );
        }

        if ( ee == null )
            return new VisualizationValueObject[] {}; // access denied, etc.

        if ( threshold == null ) {
            log.warn( "Threshold was null, using default" );
            threshold = DEFAULT_THRESHOLD;
        }

        Collection<DoubleVectorValueObject> dedvs;

        Gene g = geneService.load( geneId );
        if ( g == null )
            return null;

        Collection<Long> genes = new ArrayList<>();
        genes.add( geneId );

        dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ee, genes );

        Long time = watch.getTime();
        watch.reset();
        watch.start();

        if ( time > 100 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + ee.getId() + " and " + "one gene in " + time
                    + " ms (times <100ms not reported)." );
        }
        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts;
        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs, null );

        time = watch.getTime();
        watch.reset();
        watch.start();
        if ( time > 100 ) {
            log.info( "Ran sortVectorDataByDesign on " + dedvs.size() + " DEDVs for 1 EE" + " in " + time
                    + " ms (times <100ms not reported)." );
        }

        time = watch.getTime();
        watch.reset();
        watch.start();
        if ( time > 100 ) {
            log.info( "Ran sortLayoutSamplesByFactor on " + layouts.size() + " layouts" + " in " + time
                    + " ms (times <100ms not reported)." );
        }

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = new HashMap<>();
        validatedProbes.put( ee.getId(),
                geneDifferentialExpressionService.getDifferentialExpression( g, ee, threshold, -1 ) );

        watch.stop();
        time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Retrieved " + validatedProbes.size() + " valid probes in " + time + " ms." );
        }

        return makeDiffVisCollection( dedvs, new ArrayList<>( genes ), validatedProbes, layouts );

    }

    /**
     * AJAX exposed method
     *
     * @param resultSetId The resultset we're specifically interested. Note that this is what is used to choose the
     *                    vectors, since it could be a subset of an experiment.
     * @param givenThreshold   If non-null, a P-value threshold for retrieving associated vectors
     * @param primaryFactorID  If non-null, the factor to use for sorting the samples before other factors are considered
     * @return collection of visualization value objects
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualizationByThreshold( Long resultSetId,
            Double givenThreshold, @Nullable Long primaryFactorID ) {

        if ( resultSetId == null ) {
            throw new IllegalArgumentException( "The resultSetId parameter cannot be null" );
        }

        double threshold = DEFAULT_THRESHOLD;

        if ( givenThreshold != null ) {
            threshold = givenThreshold;
            log.debug( "Threshold specified not using default value: " + givenThreshold );
        }

        List<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService
                .getDiffExVectors( resultSetId, threshold, MAX_RESULTS_TO_RETURN );
        // at this point the dedvs are reduced down to the subset we need to visualize

        ExperimentalFactor primaryFactor = this.experimentalFactorService.load( primaryFactorID );

        if ( primaryFactor == null ) {
            throw new IllegalArgumentException( "No factor found for id=" + primaryFactorID );
        }

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts = experimentalDesignVisualizationService
                .sortVectorDataByDesign( dedvs, primaryFactor );

        return makeVisCollection( dedvs, null, null, layouts );
    }

    /**
     * AJAX
     */
    public VisualizationValueObject[] getDEDVForPcaVisualization( Long eeId, int component, int count ) {
        StopWatch watch = new StopWatch();
        watch.start();

        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId, EntityNotFoundException::new );
        Map<ProbeLoading, DoubleVectorValueObject> topLoadedVectors = this.svdService
                .getTopLoadedVectors( ee, component, count );

        if ( topLoadedVectors == null )
            return null;

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts;

        Collection<DoubleVectorValueObject> values = topLoadedVectors.values();

        if ( values.isEmpty() ) {
            return null;
        }

        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( values, null );
        return makeVisCollection( values, null, null, layouts );
    }

    /**
     * AJAX exposed method
     */
    public VisualizationValueObject[] getDEDVForVisualization( Collection<Long> eeIds, Collection<Long> geneIds ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<ExpressionExperiment> ees = expressionExperimentService.load( eeIds );
        if ( ees == null || ees.isEmpty() )
            return null;

        Collection<DoubleVectorValueObject> dedvs;
        if ( geneIds == null || geneIds.isEmpty() ) {
            ExpressionExperiment ee = ees.iterator().next();
            ee = expressionExperimentService.thawLite( ee );
            dedvs = processedExpressionDataVectorService.getRandomProcessedDataArrays( ee, SAMPLE_SIZE );
            if ( dedvs.size() > SAMPLE_SIZE ) {
                dedvs = new ArrayList<>( dedvs ).subList( 0, SAMPLE_SIZE );
            }
        } else {
            if ( geneIds.size() > MAX_RESULTS_TO_RETURN ) {
                log.warn( geneIds.size() + " genes for visualization. Too many.  Only using first "
                        + MAX_RESULTS_TO_RETURN + " genes. " );
                List<Long> reducedGeneIds = new ArrayList<>( geneIds );
                geneIds = reducedGeneIds.subList( 0, MAX_RESULTS_TO_RETURN );
            }

            dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees, geneIds );
        }

        if ( dedvs.isEmpty() ) {
            return null;
        }

        Long time = watch.getTime();
        watch.reset();
        watch.start();

        if ( time > 100 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs" + ( geneIds == null ?
                    " sample" :
                    " for " + geneIds.size() + " genes " ) + " in " + time + " ms (times <100ms not reported)." );
        }

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs, null );

        time = watch.getTime();
        watch.reset();
        watch.start();
        if ( time > 500 ) {
            log.info( "Ran sortVectorDataByDesign on " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs" + " in "
                    + time + " ms (times <500ms not reported)." );
        }

        watch.stop();
        time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Ran sortLayoutSamplesByFactor on " + layouts.size() + " layouts" + " in " + time
                    + " ms (times <100ms not reported)." );
        }
        return makeVisCollection( dedvs, geneIds, null, layouts );

    }

    /**
     * AJAX exposed method
     */
    public VisualizationValueObject[] getDEDVForVisualizationByProbe( Collection<Long> eeIds,
            Collection<Long> probeIds ) {

        if ( eeIds.isEmpty() || probeIds.isEmpty() )
            return null;

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.load( eeIds );
        if ( ees == null || ees.isEmpty() )
            return null;

        Collection<CompositeSequence> probes = this.compositeSequenceService.load( probeIds );
        if ( probes == null || probes.isEmpty() )
            return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService
                .getProcessedDataArraysByProbe( ees, probes );

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs, null );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + probeIds.size()
                + " genes in " + time + " ms." );

        return makeVisCollection( dedvs, null, null, layouts );

    }

    /**
     * Handle case of text export of the results.
     */
    @RequestMapping(value = "/downloadDEDV.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView downloadDEDV( HttpServletRequest request ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> geneIds = ControllerUtils.extractIds( request.getParameter( "g" ) ); // might not be any
        Collection<Long> eeIds = ControllerUtils.extractIds( request.getParameter( "ee" ) ); // might not be there

        ModelAndView mav = new ModelAndView( new TextView( "tab-separated-values" ) );
        if ( eeIds.isEmpty() ) {
            mav.addObject( "text", "Input empty for finding DEDVs: " + geneIds + " and " + eeIds );
            return mav;
        }

        String threshSt = request.getParameter( "thresh" );
        String resultSetIdSt = request.getParameter( "rs" );

        double thresh = 100.0;
        if ( StringUtils.isNotBlank( threshSt ) ) {
            try {
                thresh = Double.parseDouble( threshSt );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Threshold was not a valid value: " + threshSt );
            }
        }

        Map<BioAssaySetValueObject, Map<Long, Collection<DoubleVectorValueObject>>> result;

        if ( request.getParameter( "pca" ) != null ) {
            int component = Integer.parseInt( request.getParameter( "component" ) );
            Long eeId = eeIds.iterator().next();

            ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId, EntityNotFoundException::new );
            Map<ProbeLoading, DoubleVectorValueObject> topLoadedVectors = this.svdService
                    .getTopLoadedVectors( ee, component, ( int ) thresh );

            if ( topLoadedVectors == null )
                return null;

            mav.addObject( "text", format4File( topLoadedVectors.values() ) );
            return mav;
        }

        /*
         * The following should be set if we're viewing diff. ex results.
         */

        Long resultSetId = null;
        if ( StringUtils.isNumeric( resultSetIdSt ) ) {
            resultSetId = Long.parseLong( resultSetIdSt );
        }

        if ( resultSetId != null ) {

            /*
             * Diff ex case.
             */
            Long eeId = eeIds.iterator().next();

            Collection<DoubleVectorValueObject> diffExVectors = processedExpressionDataVectorService
                    .getDiffExVectors( resultSetId, thresh, MAX_RESULTS_TO_RETURN );

            if ( diffExVectors == null || diffExVectors.isEmpty() ) {
                mav.addObject( "text", "No results" );
                return mav;
            }

            /*
             * Organize the vectors in the same way expected by the ee+gene type of request.
             */
            ExpressionExperimentValueObject ee = expressionExperimentService
                    .loadValueObjectById( eeId );

            result = new HashMap<>();
            Map<Long, Collection<DoubleVectorValueObject>> gmap = new HashMap<>();

            for ( DoubleVectorValueObject dv : diffExVectors ) {
                for ( Long g : dv.getGenes() ) {
                    if ( !gmap.containsKey( g ) ) {
                        gmap.put( g, new HashSet<DoubleVectorValueObject>() );
                    }
                    gmap.get( g ).add( dv );
                }
            }

            result.put( ee, gmap );

        } else {
            // Generic listing.
            result = getDEDV( eeIds, geneIds );
        }

        if ( result == null || result.isEmpty() ) {
            mav.addObject( "text", "No results" );
            return mav;
        }

        mav.addObject( "text", format4File( result ) );
        watch.stop();
        long time = watch.getTime();

        if ( time > 100 ) {
            log.info(
                    "Retrieved and Formated" + result.keySet().size() + " DEDVs for eeIDs: " + eeIds + " and GeneIds: "

                            + geneIds + " in : " + time + " ms." );
        }
        return mav;

    }

    private String format4File( Collection<DoubleVectorValueObject> vectors ) {
        StringBuilder converted = new StringBuilder();
        converted.append( "# Generated by Gemma " ).append( buildInfo.getVersion() ).append( " on " ).append( format( new Date() ) ).append( "\n" );
        for ( String line : GEMMA_CITATION_NOTICE ) {
            converted.append( "# " ).append( line ).append( "\n" );
        }
        converted.append( "#\n" );
        converted.append( "# " ).append( GEMMA_LICENSE_NOTICE ).append( "\n" );
        boolean didHeader = false;

        Map<Long, GeneValueObject> gmap = getGeneValueObjectsUsed( vectors );

        for ( DoubleVectorValueObject vec : vectors ) {
            if ( !didHeader ) {
                converted.append( makeHeader( vec ) );
                didHeader = true;
            }

            List<String> geneSymbols = new ArrayList<>();
            List<String> geneNames = new ArrayList<>();

            for ( Long g : vec.getGenes() ) {
                GeneValueObject gene = gmap.get( g );
                assert gene != null;
                geneSymbols.add( gene.getOfficialSymbol() );
                geneNames.add( gene.getOfficialName() );
            }

            converted.append( StringUtils.join( geneSymbols, "|" ) ).append( "\t" )
                    .append( StringUtils.join( geneNames, "|" ) ).append( "\t" );
            converted.append( vec.getDesignElement().getName() ).append( "\t" );

            if ( vec.getData() != null || vec.getData().length != 0 ) {
                for ( double data : vec.getData() ) {
                    converted.append( String.format( "%.3f", data ) ).append( "\t" );
                }
                converted.deleteCharAt( converted.length() - 1 ); // remove the trailing tab // FIXME just joind
            }
            converted.append( "\n" );
        }

        return converted.toString();
    }

    /**
     * Converts the given map into a tab delimited String
     */
    private String format4File(
            Map<BioAssaySetValueObject, Map<Long, Collection<DoubleVectorValueObject>>> result ) {
        StringBuilder converted = new StringBuilder();
        Map<Long, GeneValueObject> genes = new HashMap<>(); // Saves us from loading genes
        // unnecessarily
        converted.append( "# Generated by Gemma " ).append( buildInfo.getVersion() ).append( " on " ).append( format( new Date() ) ).append( "\n" );
        converted.append( "#\n" );
        for ( String line : GEMMA_CITATION_NOTICE ) {
            converted.append( "# " ).append( line ).append( "\n" );
        }
        converted.append( "#\n" );
        for ( BioAssaySetValueObject ee : result.keySet() ) {

            boolean didHeaderForEe = false;

            Collection<Long> geneIds = result.get( ee ).keySet();
            for ( Long geneId : geneIds ) {
                GeneValueObject gene;
                if ( genes.containsKey( geneId ) ) {
                    gene = genes.get( geneId );
                } else {
                    gene = geneService.loadValueObjectById( geneId );
                    if ( gene == null ) {
                        log.warn( String.format( "Failed to convert gene with ID %d to VO.", geneId ) );
                        continue;
                    }
                    genes.put( geneId, gene );
                }
                String geneName = gene.getOfficialSymbol();

                Collection<DoubleVectorValueObject> vecs = result.get( ee ).get( geneId );

                for ( DoubleVectorValueObject dedv : vecs ) {

                    if ( !didHeaderForEe ) {
                        converted.append( makeHeader( dedv ) );
                        didHeaderForEe = true;
                    }

                    converted.append( geneName ).append( "\t" ).append( gene.getOfficialName() ).append( "\t" );
                    converted.append( dedv.getDesignElement().getName() ).append( "\t" );

                    if ( dedv.getData() != null || dedv.getData().length != 0 ) {
                        for ( double data : dedv.getData() ) {
                            converted.append( String.format( "%.3f", data ) ).append( "\t" );
                        }
                        converted.deleteCharAt( converted.length() - 1 ); // remove the trailing tab
                    }
                    converted.append( "\n" );
                }
            }
            converted.append( "\n" );

        }
        converted.append( "\r\n" );
        return converted.toString();
    }

    private LinkedHashSet<ExperimentalFactor> getFactorNames(
            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> eeLayouts ) {
        LinkedHashSet<ExperimentalFactor> factorNames = new LinkedHashSet<>(); // need uniqueness & order
        for ( BioAssayValueObject ba : eeLayouts.keySet() ) {
            LinkedHashMap<ExperimentalFactor, Double> factorMap = eeLayouts.get( ba );
            for ( Entry<ExperimentalFactor, Double> pair : factorMap.entrySet() ) {
                if ( pair.getKey() != null ) {
                    factorNames.add( pair.getKey() );
                }
            }
        }
        return factorNames;
    }

    private List<GeneValueObject> getGeneValueObjectList( List<Long> genes ) {
        Collection<GeneValueObject> geneValueObjects = geneService.loadValueObjectsByIds( genes );
        Map<Long, GeneValueObject> m = IdentifiableUtils.getIdMap( geneValueObjects );
        List<GeneValueObject> geneValueObjectList = new ArrayList<>();
        for ( Long id : genes ) {
            if ( !m.containsKey( id ) ) {
                continue;
            }
            geneValueObjectList.add( m.get( id ) );
        }
        return geneValueObjectList;
    }

    private Map<Long, GeneValueObject> getGeneValueObjectsUsed( Collection<DoubleVectorValueObject> vectors ) {
        Set<Long> usedGeneIds = new HashSet<>();
        for ( DoubleVectorValueObject vec : vectors ) {
            if ( vec == null || vec.getGenes() == null )
                continue;
            usedGeneIds.addAll( vec.getGenes() );
        }
        return IdentifiableUtils.getIdMap( geneService.loadValueObjectsByIds( usedGeneIds ) );
    }

    /**
     * This is probably no longer being really used?
     */
    private Map<Long, Collection<DifferentialExpressionValueObject>> getProbeDiffExValidation( Collection<Gene> genes,
            Double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( factorMap == null )
            throw new IllegalArgumentException(
                    "Factor information is missing, please make sure factors are selected." );

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = new HashMap<>();

        Collection<Long> wantedFactors = new HashSet<>();
        for ( DiffExpressionSelectedFactorCommand factor : factorMap ) {
            wantedFactors.add( factor.getEfId() );
        }

        for ( Gene gene : genes ) {
            Collection<DifferentialExpressionValueObject> differentialExpression = geneDifferentialExpressionService
                    .getDifferentialExpression( gene, threshold, factorMap );

            for ( DifferentialExpressionValueObject diffVo : differentialExpression ) {
                assert diffVo.getCorrP() <= threshold;
                Long eeId = diffVo.getExpressionExperiment().getId();

                if ( !validatedProbes.containsKey( eeId ) ) {
                    validatedProbes.put( eeId, new HashSet<DifferentialExpressionValueObject>() );
                }

                Collection<ExperimentalFactorValueObject> factors = diffVo.getExperimentalFactors();

                for ( ExperimentalFactorValueObject fac : factors ) {
                    if ( wantedFactors.contains( fac.getId() ) ) {
                        validatedProbes.get( eeId ).add( diffVo );
                    }
                }
            }
        }
        return validatedProbes;
    }

    /**
     * Identify which probes were 'responsible' for the coexpression links.
     * FIXME change this to actually compute the correlations.
     *
     * @return map of EEID -> collection ProbeIDs which underlie the stored coexpression links.
     */
    private Map<Long, Collection<Long>> getProbeLinkValidation( Collection<ExpressionExperiment> ees, Gene queryGene,
            Gene coexpressedGene, Collection<DoubleVectorValueObject> dedvs ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Map<Long, Collection<Long>> coexpressedEE2ProbeIds = new HashMap<>();
        Map<Long, Collection<Long>> queryEE2ProbeIds = new HashMap<>();

        /*
         * Get the probes for the vectors, organize by ee.
         */
        for ( DoubleVectorValueObject dedv : dedvs ) {
            BioAssaySetValueObject ee = dedv.getExpressionExperiment();
            if ( dedv.getGenes().contains( queryGene.getId() ) ) {
                if ( !queryEE2ProbeIds.containsKey( ee.getId() ) ) {
                    queryEE2ProbeIds.put( ee.getId(), new HashSet<Long>() );
                }
                queryEE2ProbeIds.get( ee.getId() ).add( dedv.getDesignElement().getId() );
            } else if ( dedv.getGenes().contains( coexpressedGene.getId() ) ) {
                if ( !coexpressedEE2ProbeIds.containsKey( ee.getId() ) ) {
                    coexpressedEE2ProbeIds.put( ee.getId(), new HashSet<Long>() );
                }
                coexpressedEE2ProbeIds.get( ee.getId() ).add( dedv.getDesignElement().getId() );
            } else {
                log.error( "Dedv doesn't belong to coexpressed or query gene. QueryGene= " + queryGene
                        + "CoexpressedGene= " + coexpressedGene + "DEDV " + dedv.getId() + " has genes: " + dedv
                        .getGenes() );
            }
        }

        Map<Long, Collection<Long>> validatedProbes = new HashMap<>();
        for ( ExpressionExperiment ee : ees ) {

            Collection<Long> queryProbeIds = queryEE2ProbeIds.get( ee.getId() );
            Collection<Long> coexpressedProbeIds = coexpressedEE2ProbeIds.get( ee.getId() );

            if ( queryProbeIds == null || queryProbeIds.isEmpty() ) {
                log.warn( "Unexpectedly no probes for " + queryGene + " in " + ee );
                continue;
            }

            if ( coexpressedProbeIds == null || coexpressedProbeIds.isEmpty() ) {
                log.warn( "Unexpectedly no probes for " + coexpressedGene + " in " + ee );
            }

            /*
             * Note: this does a probe-level query FIXME if we don't store data at probe-level we can't do this.
             */
            // Collection<Long> probesInLinks = this.geneCoexpressionService.getCoexpressedProbes( queryProbeIds,
            // coexpressedProbeIds, ee, queryGene.getTaxon().getCommonName() );

            // if ( probesInLinks.isEmpty() ) {
            // log.warn( "Unexpectedly no probes for link between " + queryGene + " -and- " + coexpressedGene + " in "
            // + ee );
            // }
            //
            // validatedProbes.put( ee.getId(), probesInLinks );
            // FIXME FIXME
        }

        watch.stop();
        Long time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Validation of probes for " + ees.size() + " experiments in " + time + "ms." );
        }
        return validatedProbes;
    }

    private String getRandomColour( Random random ) {
        String colourString;
        colourString =
                "#" + Integer.toHexString( random.nextInt( 16 ) ) + "0" + Integer.toHexString( random.nextInt( 16 ) )
                        + "0" + Integer.toHexString( random.nextInt( 16 ) ) + "0";
        return colourString;
    }

    /**
     * Get the names we'll use for the columns of the vectors.
     */
    private void getSampleNames( Collection<DoubleVectorValueObject> vectors, VisualizationValueObject vvo,
            Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        // FIXME this is inefficient - we don't need to check all the vectors if they all have the same samples...though
        // we do need to set it for all of them?

        for ( DoubleVectorValueObject vec : vectors ) {
            List<String> sampleNames = new ArrayList<>();
            if ( layouts != null && layouts.get( vec.getExpressionExperiment().getId() ) != null ) {
                Collection<BioMaterialValueObject> seenSamples = new HashSet<>(); // if same sample was run more than
                // once on diff platforms.

                for ( BioAssayValueObject ba : layouts.get( vec.getExpressionExperiment().getId() ).keySet() ) {
                    if ( seenSamples.contains( ba.getSample() ) ) {
                        continue;
                    }
                    seenSamples.add( ba.getSample() );
                    sampleNames.add( ba.getName() );
                }
                if ( sampleNames.size() > 0 ) {

                    assert sampleNames.size() == vec.getData().length;
                    log.debug( sampleNames.size() + " sample names!" );
                    vvo.setSampleNames( sampleNames );
                }
            } else {
                sampleNames = getSampleNames( vec );
                if ( sampleNames.size() > 0 ) {
                    log.debug( sampleNames.size() + " sample names!" );
                    vvo.setSampleNames( sampleNames );
                }
            }
        }

    }

    private List<String> getSampleNames( DoubleVectorValueObject dedv ) {
        List<String> result = new ArrayList<>();

        for ( BioAssayValueObject ba : dedv.getBioAssays() ) {
            result.add( ba.getName() );
        }

        assert result.size() == dedv.getData().length;

        return result;
    }

    /**
     * Ensure the names are unique in the colour bars
     */
    private String getUniqueFactorName( ExperimentalFactor factor ) {
        return factor.getName() + " [ID=" + factor.getId() + "]";
    }

    /**
     * Takes the DEDVs and put them in point objects and normalize the values. returns a map of eeid to visValueObject.
     * Currently removes multiple hits for same gene. Tries to pick best DEDV. Organizes the experiments from lowest to
     * higest p-value
     *
     * @param validatedProbes (bad name)
     */
    private VisualizationValueObject[] makeDiffVisCollection( Collection<DoubleVectorValueObject> dedvs,
            List<Long> genes, Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes,
            Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Map<Long, Collection<DoubleVectorValueObject>> vvoMap = new HashMap<>();

        Map<Long, BioAssaySetValueObject> eeMap = new HashMap<>();

        // Organize by expression experiment
        for ( DoubleVectorValueObject dvvo : dedvs ) {
            BioAssaySetValueObject ee = dvvo.getExpressionExperiment();
            eeMap.put( ee.getId(), ee );
            if ( !vvoMap.containsKey( ee.getId() ) ) {
                vvoMap.put( ee.getId(), new HashSet<DoubleVectorValueObject>() );
            }
            vvoMap.get( ee.getId() ).add( dvvo );
        }

        class EE2PValue implements Comparable<EE2PValue> {
            Long EEId;
            double pValue;

            public EE2PValue() {
                super();
            }

            public EE2PValue( Long eeid, double pValue ) {
                this();
                this.EEId = eeid;
                this.pValue = pValue;
            }

            @Override
            public int compareTo( EE2PValue o ) {
                if ( this.pValue > o.getPValue() )
                    return 1;
                else if ( this.pValue > o.getPValue() )
                    return -1;
                else
                    return 0;
            }

            public Long getEEId() {
                return EEId;
            }

            public double getPValue() {
                return pValue;
            }

        }

        List<EE2PValue> sortedEE = new ArrayList<>();

        // Need to sort the expression experiments by lowest p-value
        for ( Long eeId : vvoMap.keySet() ) {
            Collection<DifferentialExpressionValueObject> devos = validatedProbes.get( eeId );
            double minP = 1;

            if ( devos != null && !devos.isEmpty() ) {
                for ( DifferentialExpressionValueObject devo : devos ) {
                    if ( minP > devo.getP() ) {
                        minP = devo.getP();
                    }
                }
            }
            sortedEE.add( new EE2PValue( eeId, minP ) );
        }

        Collections.sort( sortedEE );

        VisualizationValueObject[] result = new VisualizationValueObject[vvoMap.keySet().size()];

        List<GeneValueObject> geneValueObjects = getGeneValueObjectList( genes );

        // Create collection of visualizationValueObject for flotr on js side
        int i = 0;
        for ( EE2PValue ee2P : sortedEE ) {

            VisualizationValueObject vvo = new VisualizationValueObject( vvoMap.get( ee2P.getEEId() ), geneValueObjects,
                    ee2P.getPValue(), validatedProbes.get( ee2P.getEEId() ) );

            getSampleNames( vvoMap.get( ee2P.getEEId() ), vvo, layouts );

            if ( layouts != null && !layouts.isEmpty() && layouts.containsKey( ee2P.getEEId() ) ) {
                LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = layouts
                        .get( ee2P.getEEId() );
                this.prepareFactorsForFrontEndDisplay( vvo, layout );
            }

            /*
             * Set up the experimental designinfo so we can show it above the graph.
             */

            if ( layouts != null ) {
                BioAssaySetValueObject ee = eeMap.get( ee2P.getEEId() );
                log.debug( "setup experimental design layout profiles for " + ee );
                vvo.setUpFactorProfiles( layouts.get( ee.getId() ) );
            }

            result[i] = vvo;
            i++;
        }

        Long time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Created vis value objects in: " + time );
        }
        return result;

    }

    /**
     * @param dedv exemplar to use for forming the heading
     */
    private String makeHeader( DoubleVectorValueObject dedv ) {

        String firstThreeColumnHeadings = "GeneSymbol\tGeneName\tElement";

        StringBuilder buf = new StringBuilder();
        BioAssaySetValueObject ee = dedv.getExpressionExperiment();
        buf.append( "# " );
        if ( ee instanceof ExpressionExperimentValueObject ) {
            buf.append( ( ( ExpressionExperimentValueObject ) ee ).getShortName() ).append( " : " );
        }
        buf.append( ee.getName() ).append( "\n" );
        buf.append( firstThreeColumnHeadings );
        for ( BioAssayValueObject ba : dedv.getBioAssays() ) {
            buf.append( "\t" ).append( ba.getName() );
        }

        buf.append( "\n" );

        return buf.toString();
    }

    private Map<BioAssaySetValueObject, Map<Long, Collection<DoubleVectorValueObject>>> makeVectorMap(
            Collection<DoubleVectorValueObject> newResults,
            Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        // FIXME use the layouts.

        Map<BioAssaySetValueObject, Map<Long, Collection<DoubleVectorValueObject>>> result = new HashMap<>();

        for ( DoubleVectorValueObject v : newResults ) {
            BioAssaySetValueObject e = v.getExpressionExperiment();
            if ( !result.containsKey( e ) ) {
                result.put( e, new HashMap<>() );
            }
            Map<Long, Collection<DoubleVectorValueObject>> innerMap = result.get( e );

            if ( v.getGenes() == null || v.getGenes().isEmpty() ) {
                continue;
            }

            for ( Long g : v.getGenes() ) {
                if ( !innerMap.containsKey( g ) ) {
                    innerMap.put( g, new HashSet<DoubleVectorValueObject>() );
                }
                innerMap.get( g ).add( v );
            }
        }

        return result;

    }

    /**
     * Takes the DEDVs and put them in point objects and normalize the values. returns a map of eeid to visValueObject.
     * Currently removes multiple hits for same gene. Tries to pick best DEDV.
     */
    private VisualizationValueObject[] makeVisCollection( Collection<DoubleVectorValueObject> dedvs,
            Collection<Long> genes, Map<Long, Collection<Long>> validatedProbes,
            Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        Map<Long, List<DoubleVectorValueObject>> vvoMap = new HashMap<>();
        // Organize by expression experiment
        if ( dedvs == null || dedvs.isEmpty() )
            return new VisualizationValueObject[1];

        for ( DoubleVectorValueObject dvvo : dedvs ) {
            // FIXME: we can probably use this information, and do away with carrying so much Layout information around?
            // assert dvvo.isReorganized() && dvvo.getBioAssayDimension().isReordered(); // not always true!!
            BioAssaySetValueObject ee = dvvo.getExpressionExperiment();
            if ( !vvoMap.containsKey( ee.getId() ) ) {
                vvoMap.put( ee.getId(), new ArrayList<>() );
            }
            vvoMap.get( ee.getId() ).add( dvvo );
        }

        List<GeneValueObject> geneValueObjects;
        if ( genes == null || genes.isEmpty() ) {
            geneValueObjects = new ArrayList<>( getGeneValueObjectsUsed( dedvs ).values() );
        } else {
            geneValueObjects = getGeneValueObjectList( new ArrayList<>( genes ) );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        VisualizationValueObject[] result = new VisualizationValueObject[vvoMap.keySet().size()];
        // Create collection of visualizationValueObject for flotr on js side
        int i = 0;
        for ( Long ee : vvoMap.keySet() ) {

            Collection<Long> validatedProbeList = null;
            if ( validatedProbes != null ) {
                validatedProbeList = validatedProbes.get( ee );
            }
            Collection<DoubleVectorValueObject> vectors = vvoMap.get( ee );

            VisualizationValueObject vvo = new VisualizationValueObject( vectors, geneValueObjects,
                    validatedProbeList );

            if ( vectors.size() > 0 ) {
                getSampleNames( vectors, vvo, layouts );
                if ( vectors.size() > 0 && layouts != null && !layouts.isEmpty() && layouts.containsKey( ee ) ) {
                    // Set up the experimental designinfo so we can show it above the graph.
                    LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = layouts
                            .get( ee );
                    this.prepareFactorsForFrontEndDisplay( vvo, layout );
                }
            }

            /*
             * Set up the experimental design info so we can show it above the graph.
             */
            if ( layouts != null && layouts.get( ee ) != null ) {
                vvo.setUpFactorProfiles( layouts.get( ee ) );
            }

            result[i] = vvo;
            i++;
        }

        long time = timer.getTime();
        if ( time > 1000 ) {
            log.info( "Created " + result.length + " vis value objects in: " + time );
        }

        return result;

    }

    /**
     * Prepare vvo for display on front end. Uses factors and factor values from layouts
     *
     * @param vvo Note: This will be modified! It will be updated with the factorNames and factorValuesToNames
     */
    private void prepareFactorsForFrontEndDisplay( VisualizationValueObject vvo,
            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> eeLayouts ) {

        if ( eeLayouts == null || eeLayouts.isEmpty() ) {
            log.warn( "No layouts, bail" );
            vvo.setFactorNames( null );
            vvo.setFactorValuesToNames( null );
            return;
        }

        LinkedHashSet<ExperimentalFactor> factorNames = getFactorNames( eeLayouts );

        // colours for conditions/factor values bar chart FIXME make continuous maps different.
        Map<ExperimentalFactor, Queue<String>> factorColoursMap = createFactorNameToColoursMap( factorNames );
        String missingValueColour = "#DCDCDC";

        Random random = new Random();

        LinkedHashMap<String, LinkedHashMap<String, String>> factorToValueNames = new LinkedHashMap<>();
        // list of maps with entries: key = factorName, value=array of factor values
        // 1 entry per sample
        List<LinkedHashMap<String, String[]>> factorValueMaps = new ArrayList<>();

        Collection<String> factorsMissingValues = new HashSet<>();

        Collection<BioMaterialValueObject> seenSamples = new HashSet<>(); // if same sample was run more than once on
        // diff platforms.
        Map<Long, FactorValue> fvs = new HashMap<>(); // avoid loading repeatedly.
        Collection<ExperimentalFactor> seenFactors = new HashSet<>();

        for ( BioAssayValueObject ba : eeLayouts.keySet() ) {

            if ( seenSamples.contains( ba.getSample() ) ) {
                continue;
            }
            seenSamples.add( ba.getSample() );

            // double should be the factorValue id, defined in
            // ubic.gemma.core.visualization.ExperimentalDesignVisualizationService.getExperimentalDesignLayout(ExpressionExperiment,
            // BioAssayDimension)
            LinkedHashMap<ExperimentalFactor, Double> factorMap = eeLayouts.get( ba );
            LinkedHashMap<String, String[]> factorNamesToValueColourPairs = new LinkedHashMap<>( factorNames.size() );

            // this is defensive, should only come into play when there's something messed up with the data.
            // for every factor, add a missing-value entry (guards against missing data messing up the layout)
            for ( ExperimentalFactor factor : factorNames ) {
                String[] facValAndColour = new String[] { "No value", missingValueColour };

                factorNamesToValueColourPairs.put( getUniqueFactorName( factor ), facValAndColour );
            }

            // for each experimental factor, store the name and value
            for ( Entry<ExperimentalFactor, Double> pair : factorMap.entrySet() ) {
                ExperimentalFactor factor = pair.getKey();
                Double valueOrId = pair.getValue();

                /*
                 * the double is only a double because it is meant to hold measurements when the factor is continuous if
                 * the factor is categorical, the double value is set to the value's id see
                 * ubic.gemma.core.visualization.ExperimentalDesignVisualizationService.getExperimentalDesignLayout(
                 * ExpressionExperiment, BioAssayDimension)
                 */
                if ( valueOrId == null || factor.getType() == null || (
                        factor.getType().equals( FactorType.CATEGORICAL ) && factor.getFactorValues().isEmpty() ) ) {
                    factorsMissingValues.add( getUniqueFactorName( factor ) );
                    continue;
                }

                if ( !seenFactors.contains( factor ) && factor.getType().equals( FactorType.CATEGORICAL ) ) {
                    for ( FactorValue fv : factor.getFactorValues() ) {
                        fvs.put( fv.getId(), fv );
                    }
                }

                String facValsStr = getFacValsStr( fvs, factor, valueOrId );

                if ( !factorToValueNames.containsKey( getUniqueFactorName( factor ) ) ) {
                    factorToValueNames.put( getUniqueFactorName( factor ), new LinkedHashMap<String, String>() );
                }
                // assign colour if unassigned or fetch it if already assigned
                String colourString = "";
                if ( !factorToValueNames.get( getUniqueFactorName( factor ) ).containsKey( facValsStr ) ) {
                    if ( factorColoursMap.containsKey( factor ) ) {
                        colourString = factorColoursMap.get( factor ).poll();
                    }
                    if ( colourString == null || Objects.equals( colourString, "" ) ) { // ran out of predefined colours
                        colourString = getRandomColour( random );
                    }
                    factorToValueNames.get( getUniqueFactorName( factor ) ).put( facValsStr, colourString );
                } else {
                    colourString = factorToValueNames.get( getUniqueFactorName( factor ) ).get( facValsStr );
                }
                String[] facValAndColour = new String[] { facValsStr, colourString };

                factorNamesToValueColourPairs.put( getUniqueFactorName( factor ), facValAndColour );

            }
            factorValueMaps.add( factorNamesToValueColourPairs );
        }

        // add missing value entries here so they show up at the end of the legend's value lists
        if ( !factorsMissingValues.isEmpty() ) {
            for ( String factorName : factorsMissingValues ) {
                if ( !factorToValueNames.containsKey( factorName ) ) {
                    factorToValueNames.put( factorName, new LinkedHashMap<String, String>() );
                }
                factorToValueNames.get( factorName ).put( "No value", missingValueColour );
            }
        }
        vvo.setFactorNames( factorToValueNames ); // this is summary of values & colours by factor, used for legend
        vvo.setFactorValuesToNames( factorValueMaps ); // this is list of maps for each sample
    }

    private String getFacValsStr( Map<Long, FactorValue> fvs, ExperimentalFactor factor, Double valueOrId ) {
        String facValsStr;
        if ( factor.getType().equals( FactorType.CONTINUOUS ) ) {
            /*
             * FIXME continuous factors need a different color scheme.
             */
            log.debug( "Experiment has continuous factor." );
            facValsStr = valueOrId.toString();
        } else if ( ExperimentalDesignUtils.isBatchFactor( factor ) ) {
            /*
             * FIXME for batch, also treat like they are continuous. There can be many so we tend to run out of
             * colors.
             */
            facValsStr = composeFacvalStr( fvs, valueOrId );
        } else {
            facValsStr = composeFacvalStr( fvs, valueOrId );
        }
        return facValsStr;
    }

    private String composeFacvalStr( Map<Long, FactorValue> fvs, Double valueOrId ) {
        Long id = Math.round( valueOrId );
        FactorValue facVal = fvs.get( id );
        assert facVal != null;
        fvs.put( facVal.getId(), facVal );
        return FactorValueUtils.getSummaryString( facVal );
    }
}