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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.visualization.ExperimentalDesignVisualizationService;
import ubic.gemma.web.controller.visualization.ExpressionProfileDataObject;
import ubic.gemma.web.controller.visualization.VisualizationValueObject;
import ubic.gemma.web.view.TextView;
import cern.colt.list.DoubleArrayList;

/**
 * Exposes methods for accessing underlying Design Element Data Vectors. eg: ajax methods for visualization
 * 
 * @author kelsey
 * @version $Id$
 */
@Controller(value = "dedvController")
@RequestMapping("/dedv")
public class DEDVController {
    protected static Log log = LogFactory.getLog( DEDVController.class.getName() );

    private static final double DEFAULT_THRESHOLD = 0.05;
    private static final int MAX_RESULTS_TO_RETURN = 150;
    private static final int SAMPLE_SIZE = 20; // Number of dedvs to return if no genes given

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ExperimentalDesignVisualizationService experimentalDesignVisualizationService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private GeneDifferentialExpressionService geneDifferentialExpressionService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    /**
     * Given a collection of expression experiment Ids and a geneId returns a map of DEDV value objects to a collection
     * of genes. The EE info is in the value object.
     */
    public Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> getDEDV( Collection<Long> eeIds,
            Collection<Long> geneIds ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvMap;

        if ( geneIds == null || geneIds.isEmpty() ) {
            dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees.iterator().next(), 50, false );
        } else {
            Collection<Gene> genes = geneService.loadMultiple( geneIds );
            dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees, genes );
        }

        // Could be performance problem, and factor info not displayed on front end yet anyway.
        // Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts =
        // experimentalDesignVisualizationService
        // .sortVectorDataByDesign( dedvMap );

        watch.stop();
        Long time = watch.getTime();

        if ( time > 1000 )
            log.info( "Retrieved " + dedvMap.size() + " DEDVs from " + eeIds.size() + " EEs in " + time + " ms." );

        return makeVectorMap( dedvMap );

    }

    /**
     * AJAX exposed method
     * 
     * @param eeIds
     * @param geneIds
     * @return
     */
    public VisualizationValueObject[] getDEDVForCoexpressionVisualization( Collection<Long> eeIds, Long queryGeneId,
            Long coexpressedGeneId ) {

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        Gene queryGene = geneService.load( queryGeneId );
        Gene coexpressedGene = geneService.load( coexpressedGeneId );

        List<Gene> genes = new ArrayList<Gene>();
        genes.add( geneService.thawLite( queryGene ) );
        genes.add( geneService.thawLite( coexpressedGene ) );

        if ( genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes, false );

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;

        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        watch.stop();
        Long time = watch.getTime();

        if ( dedvs.size() == 0 ) {
            log.warn( "No expression profiles (DEDVs) were available for the experiments:  " + eeIds + " and genes(s) "
                    + queryGene.getOfficialSymbol() + ", " + coexpressedGene.getOfficialSymbol() );
            return null;
        }

        if ( time > 1000 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + genes.size()
                    + " genes in " + time + " ms." );
        }

        Map<Long, Collection<Long>> validatedProbes = getProbeLinkValidation( ees, queryGene, coexpressedGene, dedvs );

        return makeVisCollection( dedvs, genes, validatedProbes, layouts );

    }

    /**
     * AJAX
     * 
     * @param eeId
     * @param component
     * @param count
     * @return
     */
    public VisualizationValueObject[] getDEDVForPcaVisualization( Long eeId, int component, int count ) {
        StopWatch watch = new StopWatch();
        watch.start();

        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if ( ee == null ) return null;

        Map<ProbeLoading, DoubleVectorValueObject> topLoadedVectors = this.svdService.getTopLoadedVectors( ee,
                component, count );
        Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;

        Collection<DoubleVectorValueObject> values = topLoadedVectors.values();
        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( values );
        return makeVisCollection( values, null, null, layouts );
    }

    /**
     * AJAX exposed method private DifferentialExpressionAnalysisResultService
     * differentialExpressionAnalysisResultService;
     * 
     * @param eeIds FIXME accomodate ExpressionExperimentSubSets. Currently we pass in the "source experiment" so we
     *        don't get the slice.
     * @param geneIds (could be just one)
     * @param threshold for 'significance'
     * @param factorMap Collection of DiffExpressionSelectedFactorCommand showing which factors to use.
     * @return
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualization( Collection<Long> eeIds, Collection<Long> geneIds,
            Double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( eeIds.isEmpty() || geneIds.isEmpty() ) return null;

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<? extends BioAssaySet> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;
        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes, false );

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;

        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                + " genes in " + time + " ms." );

        watch = new StopWatch();
        watch.start();

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = getProbeDiffExValidation( genes,
                threshold, factorMap );

        watch.stop();
        time = watch.getTime();

        log.info( "Retrieved " + validatedProbes.size() + " valid probes in " + time + " ms." );

        return makeDiffVisCollection( dedvs, new ArrayList<Gene>( genes ), validatedProbes, layouts );

    }

    /**
     * AJAX exposed method Batch factor value analyses are filtered out
     * 
     * @param eeId FIXME accomodate ExpressionExperimentSubSets. Currently we pass in the "source experiment" so we
     *        don't get the slice.
     * @param geneId
     * @param threshold (diff expression threshold)
     * @return
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualizationByExperiment( Long eeId, Long geneId,
            Double threshold ) {

        StopWatch watch = new StopWatch();
        watch.start();
        // TODO this throws an error if the user selects an experiment they don't have permission to see
        // this can happen through the diff expr. table on the gene page
        // (ex: grin1 mouse, dataset="kottmann")
        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) return null;

        if ( threshold == null ) {
            log.warn( "Threshold was null, using default" );
            threshold = DEFAULT_THRESHOLD;
        }

        Collection<DoubleVectorValueObject> dedvs;

        Gene gene = geneService.load( geneId );
        if ( gene == null ) {
            return null;
        }

        Collection<Gene> genes = new ArrayList<Gene>();
        genes.add( gene );
        Collection<BioAssaySet> ees = new ArrayList<BioAssaySet>();
        ees.add( ee );

        dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees, genes, false );

        Long time = watch.getTime();
        watch.reset();
        watch.start();

        if ( time > 100 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + ee.getShortName() + " and "
                    + gene.getOfficialSymbol() + " gene in " + time + " ms (times <100ms not reported)." );
        }
        Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;
        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        time = watch.getTime();
        watch.reset();
        watch.start();
        if ( time > 100 ) {
            log.info( "Ran sortVectorDataByDesign on " + dedvs.size() + " DEDVs for 1 EE" + " in " + time
                    + " ms (times <100ms not reported)." );
        }
        // don't include batch because it isn't biologically relevant

        for ( ExpressionExperiment ee2 : layouts.keySet() ) {
            if ( layouts.get( ee2 ) != null && layouts.get( ee2 ).keySet() != null ) {
                for ( BioAssay bioAssay : layouts.get( ee2 ).keySet() ) {
                    if ( layouts.get( ee2 ).get( bioAssay ) != null
                            && layouts.get( ee2 ).get( bioAssay ).keySet() != null ) {
                        for ( ExperimentalFactor ef : layouts.get( ee2 ).get( bioAssay ).keySet() ) {
                            if ( ExperimentalDesignUtils.isBatch( ef ) ) {
                                layouts.get( ee2 ).get( bioAssay ).remove( ef );
                                break;
                            }
                        }
                    }
                }
            }
        }

        layouts = experimentalDesignVisualizationService.sortLayoutSamplesByFactor( layouts ); // required?

        time = watch.getTime();
        watch.reset();
        watch.start();
        if ( time > 100 ) {
            log.info( "Ran sortLayoutSamplesByFactor on " + layouts.size() + " layouts" + " in " + time
                    + " ms (times <100ms not reported)." );
        }

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = new HashMap<Long, Collection<DifferentialExpressionValueObject>>();
        validatedProbes.put( ee.getId(), geneDifferentialExpressionService.getDifferentialExpression( gene, ees,
                threshold, null ) );

        watch.stop();
        time = watch.getTime();

        log.info( "Retrieved " + validatedProbes.size() + " valid probes in " + time + " ms." );

        return makeDiffVisCollection( dedvs, new ArrayList<Gene>( genes ), validatedProbes, layouts );

    }

    /**
     * AJAX exposed method
     * 
     * @param eeid The experiment we need to visualize DEPRECATED because we don't use it.
     * @param resultSetId The resultset we're specifically interested. Note that this is what is used to choose the
     *        vectors, since it could be a subset of an experiment.
     * @param threshold for 'significance'
     * @return collection of visualization value objects
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualizationByThreshold( Long eeId, Long resultSetId,
            Double givenThreshold ) {

        if ( resultSetId == null ) return null;

        if ( eeId == null ) return null;

        double threshold = DEFAULT_THRESHOLD;

        if ( givenThreshold != null ) {
            threshold = givenThreshold;
            log.debug( "Threshold specified not using default value: " + givenThreshold );
        }

        List<DoubleVectorValueObject> dedvs = getDiffExVectors( resultSetId, threshold );

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;
        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        return makeVisCollection( dedvs, null, null, layouts );

    }

    /**
     * AJAX exposed method
     * 
     * @param eeIds
     * @param geneIds
     * @return
     */
    public VisualizationValueObject[] getDEDVForVisualization( Collection<Long> eeIds, Collection<Long> geneIds ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs;
        Collection<Gene> genes = null;
        if ( geneIds == null || geneIds.isEmpty() ) {
            dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees.iterator().next(), SAMPLE_SIZE,
                    true );
        } else {

            if ( geneIds.size() > MAX_RESULTS_TO_RETURN ) {
                log.warn( geneIds.size() + " genes for visualization. Too many.  Only using first "
                        + MAX_RESULTS_TO_RETURN + " genes. " );
                List<Long> reducedGeneIds = new ArrayList<Long>( geneIds );
                geneIds = reducedGeneIds.subList( 0, MAX_RESULTS_TO_RETURN );
            }

            genes = geneService.loadMultiple( geneIds );
            if ( genes.size() == 0 ) {
                throw new IllegalArgumentException( "No genes found matching the given ids (" + geneIds.size()
                        + ", first one was " + geneIds.iterator().next() + ")" );
            }

            dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees, genes, true );
        }

        // watch.stop();
        Long time = watch.getTime();
        watch.reset();
        watch.start();

        if ( time > 100 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs"
                    + ( geneIds == null ? " sample" : " for " + geneIds.size() + " genes " ) + " in " + time
                    + " ms (times <100ms not reported)." );
        }

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;
        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        time = watch.getTime();
        watch.reset();
        watch.start();
        if ( time > 100 ) {
            log.info( "Ran sortVectorDataByDesign on " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs" + " in "
                    + time + " ms (times <100ms not reported)." );
        }
        layouts = experimentalDesignVisualizationService.sortLayoutSamplesByFactor( layouts ); // required?

        watch.stop();
        time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Ran sortLayoutSamplesByFactor on " + layouts.size() + " layouts" + " in " + time
                    + " ms (times <100ms not reported)." );
        }
        return makeVisCollection( dedvs, genes, null, layouts );

    }

    /**
     * AJAX exposed method
     * 
     * @param eeIds
     * @param geneIds (could be just one)
     * @param threshold for 'significance'
     * @param factorMap Collection of DiffExpressionSelectedFactorCommand showing which factors to use.
     * @return
     */
    public VisualizationValueObject[] getDEDVForVisualizationByProbe( Collection<Long> eeIds, Collection<Long> probeIds ) {

        if ( eeIds.isEmpty() || probeIds.isEmpty() ) return null;

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        Collection<CompositeSequence> probes = this.compositeSequenceService.loadMultiple( probeIds );
        if ( probes == null || probes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArraysByProbe(
                ees, probes, false );

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts = null;
        layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + probeIds.size()
                + " genes in " + time + " ms." );

        return makeVisCollection( dedvs, null, null, layouts );

    }

    /**
     * @param dedvIds
     * @return
     */
    public Collection<ExpressionProfileDataObject> getVectorData( Collection<Long> dedvIds ) {
        List<ExpressionProfileDataObject> result = new ArrayList<ExpressionProfileDataObject>();
        for ( Long id : dedvIds ) {
            DesignElementDataVector vector = this.designElementDataVectorService.load( id );
            try {
                DoubleVectorValueObject dvvo = new DoubleVectorValueObject( vector );
                ExpressionProfileDataObject epdo = new ExpressionProfileDataObject( dvvo );

                DoubleArrayList doubleArrayList = new cern.colt.list.DoubleArrayList( epdo.getData() );
                DescriptiveWithMissing.standardize( doubleArrayList );
                epdo.setData( doubleArrayList.elements() );

                result.add( epdo );
            } catch ( IllegalArgumentException iae ) {
                log.warn( iae );
            }

        }

        // TODO fill in gene; normalize and clip if desired.; watch for invalid ids.

        return result;
    }

    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {

        this.bioAssayDimensionService = bioAssayDimensionService;
    }

    /**
     * Returns a collection of {@link Long} ids from strings.
     * 
     * @param idString
     * @return
     */
    protected Collection<Long> extractIds( String idString ) {
        Collection<Long> ids = new ArrayList<Long>();
        if ( idString != null ) {
            for ( String s : idString.split( "," ) ) {
                try {
                    ids.add( Long.parseLong( s.trim() ) );
                } catch ( NumberFormatException e ) {
                    log.warn( "invalid id " + s );
                }
            }
        }
        return ids;
    }

    /*
     * Handle case of text export of the results.
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse) Called by /Gemma/dedv/downloadDEDV.html
     */
    @RequestMapping("/downloadDEDV.html")
    protected ModelAndView handleRequestInternal( HttpServletRequest request ) throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> geneIds = extractIds( request.getParameter( "g" ) ); // might not be any
        Collection<Long> eeIds = extractIds( request.getParameter( "ee" ) ); // might not be there

        ModelAndView mav = new ModelAndView( new TextView() );
        if ( eeIds == null || eeIds.isEmpty() ) {
            mav.addObject( "text", "Input empty for finding DEDVs: " + geneIds + " and " + eeIds );
            return mav;
        }

        String threshSt = request.getParameter( "thresh" );
        String resultSetIdSt = request.getParameter( "rs" );

        Double thresh = 100.0;
        if ( StringUtils.isNotBlank( threshSt ) ) {
            try {
                thresh = Double.parseDouble( threshSt );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Threshold was not a valid value: " + threshSt );
            }
        }

        Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result = null;

        if ( request.getParameter( "pca" ) != null ) {
            int component = Integer.parseInt( request.getParameter( "component" ) );
            ExpressionExperiment ee = expressionExperimentService.load( eeIds.iterator().next() );
            if ( ee == null ) return null;

            Map<ProbeLoading, DoubleVectorValueObject> topLoadedVectors = this.svdService.getTopLoadedVectors( ee,
                    component, thresh.intValue() );

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

        if ( thresh != null && resultSetId != null ) {

            /*
             * Diff ex case.
             */
            Long eeId = eeIds.iterator().next();

            Collection<DoubleVectorValueObject> diffExVectors = getDiffExVectors( resultSetId, thresh );

            if ( diffExVectors == null || diffExVectors.isEmpty() ) {
                mav.addObject( "text", "No results" );
                return mav;
            }

            /*
             * Organize the vectors in the same way expected by the ee+gene type of request.
             */
            ExpressionExperiment ee = expressionExperimentService.load( eeId );

            result = new HashMap<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>>();
            Map<Gene, Collection<DoubleVectorValueObject>> gmap = new HashMap<Gene, Collection<DoubleVectorValueObject>>();

            for ( DoubleVectorValueObject dv : diffExVectors ) {
                for ( Gene g : dv.getGenes() ) {
                    if ( !gmap.containsKey( g ) ) {
                        gmap.put( g, new HashSet<DoubleVectorValueObject>() );
                    }
                    gmap.get( g ).add( dv );
                }
            }

            result.put( ee, gmap );

        } else {
            result = getDEDV( eeIds, geneIds );
        }

        if ( result == null || result.isEmpty() ) {
            mav.addObject( "text", "No results" );
            return mav;
        }

        mav.addObject( "text", format4File( result ) );
        watch.stop();
        Long time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Retrieved and Formated" + result.keySet().size() + " DEDVs for eeIDs: " + eeIds
                    + " and GeneIds: "

                    + geneIds + " in : " + time + " ms." );
        }
        return mav;

    }

    /**
     * @param vectors
     * @return
     */
    private String format4File( Collection<DoubleVectorValueObject> vectors ) {
        StringBuffer converted = new StringBuffer();
        converted.append( "# Generated by Gemma\n# " + ( new Date() ) + "\n" );
        converted.append( ExpressionDataFileService.DISCLAIMER + "#\n" );
        boolean didHeader = false;
        for ( DoubleVectorValueObject vec : vectors ) {
            if ( !didHeader ) {
                converted.append( makeHeader( vec ) );
                didHeader = true;
            }

            List<String> geneSymbols = new ArrayList<String>();
            List<String> geneNames = new ArrayList<String>();
            for ( Gene g : vec.getGenes() ) {
                geneSymbols.add( g.getOfficialSymbol() );
                geneNames.add( g.getOfficialName() );
            }

            converted.append( StringUtils.join( geneSymbols, "|" ) + "\t" + StringUtils.join( geneNames, "|" ) + "\t" );
            converted.append( vec.getDesignElement().getName() + "\t" );

            if ( vec.getData() != null || vec.getData().length != 0 ) {
                for ( double data : vec.getData() ) {
                    converted.append( String.format( "%.3f", data ) + "\t" );
                }
                converted.deleteCharAt( converted.length() - 1 ); // remove the trailing tab // FIXME just joind
            }
            converted.append( "\n" );
        }

        return converted.toString();
    }

    /**
     * Converts the given map into a tab delimited String
     * 
     * @param result
     * @return
     */
    private String format4File( Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result ) {
        StringBuffer converted = new StringBuffer();
        Map<Long, String> genes = new HashMap<Long, String>(); // Saves us from loading genes unnecessarily
        converted.append( "# Generated by Gemma\n# " + ( new Date() ) + "\n" );
        converted.append( ExpressionDataFileService.DISCLAIMER + "#\n" );
        for ( ExpressionExperiment ee : result.keySet() ) {

            boolean didHeaderForEe = false;

            for ( Gene g : result.get( ee ).keySet() ) {
                Long geneId = g.getId();
                String geneName;
                if ( genes.containsKey( geneId ) ) {
                    geneName = genes.get( geneId );
                } else {
                    geneName = geneService.load( geneId ).getOfficialSymbol();
                    genes.put( geneId, geneName );
                }

                for ( DoubleVectorValueObject dedv : result.get( ee ).get( g ) ) {

                    if ( !didHeaderForEe ) {
                        converted.append( makeHeader( dedv ) );
                        didHeaderForEe = true;
                    }

                    converted.append( geneName + "\t" + g.getOfficialName() + "\t" );
                    converted.append( dedv.getDesignElement().getName() + "\t" );

                    if ( dedv.getData() != null || dedv.getData().length != 0 ) {
                        for ( double data : dedv.getData() ) {
                            converted.append( String.format( "%.3f", data ) + "\t" );
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

    /**
     * @param eeId
     * @param resultSetId
     * @param threshold
     * @return
     */
    private List<DoubleVectorValueObject> getDiffExVectors( Long resultSetId, Double threshold ) {

        StopWatch watch = new StopWatch();
        watch.start();
        ExpressionAnalysisResultSet ar = differentialExpressionResultService.loadAnalysisResult( resultSetId );
        if ( ar == null ) return null;

        Collection<ExpressionAnalysisResultSet> ars = new ArrayList<ExpressionAnalysisResultSet>();
        ars.add( ar );

        differentialExpressionResultService.thawLite( ar );

        BioAssaySet analyzedSet = ar.getAnalysis().getExperimentAnalyzed();

        Collection<BioAssaySet> ees = new ArrayList<BioAssaySet>();
        ees.add( analyzedSet );

        Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> ee2probeResults = differentialExpressionResultService
                .findInResultSets( ars, threshold, MAX_RESULTS_TO_RETURN );

        if ( ee2probeResults == null || ee2probeResults.isEmpty() ) return null;

        Collection<CompositeSequence> probes = new HashSet<CompositeSequence>();
        Map<CompositeSequence, Double> pvalues = new HashMap<CompositeSequence, Double>();
        for ( ProbeAnalysisResult par : ee2probeResults.get( ar ) ) {
            probes.add( par.getProbe() );
            pvalues.put( par.getProbe(), par.getPvalue() );
        }

        List<DoubleVectorValueObject> dedvs = new ArrayList<DoubleVectorValueObject>(
                processedExpressionDataVectorService.getProcessedDataArraysByProbe( ees, probes, false ) );

        /*
         * Resort
         */
        for ( DoubleVectorValueObject v : dedvs ) {
            v.setPvalue( pvalues.get( v.getDesignElement() ) );
        }

        Collections.sort( dedvs, new Comparator<DoubleVectorValueObject>() {
            @Override
            public int compare( DoubleVectorValueObject o1, DoubleVectorValueObject o2 ) {
                if ( o1.getPvalue() == null ) return -1;
                if ( o2.getPvalue() == null ) return 1;
                return o1.getPvalue().compareTo( o2.getPvalue() );
            }
        } );
        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + probes.size() + " genes in " + watch.getTime()
                    + " ms. (result set=" + ar.getId() );
        return dedvs;

    }

    /**
     * @param genes
     * @param threshold
     * @param factorMap
     * @return
     */
    private Map<Long, Collection<DifferentialExpressionValueObject>> getProbeDiffExValidation( Collection<Gene> genes,
            Double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( factorMap == null ) {
            throw new IllegalArgumentException( "Factor information is missing, please make sure factors are selected." );
        }

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = new HashMap<Long, Collection<DifferentialExpressionValueObject>>();

        Collection<Long> wantedFactors = new HashSet<Long>();
        for ( DiffExpressionSelectedFactorCommand factor : factorMap ) {
            wantedFactors.add( factor.getEfId() );
        }

        for ( Gene gene : genes ) {
            Collection<DifferentialExpressionValueObject> differentialExpression = geneDifferentialExpressionService
                    .getDifferentialExpression( gene, threshold, factorMap );

            for ( DifferentialExpressionValueObject diffVo : differentialExpression ) {
                assert diffVo.getP() <= threshold;
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
     * 
     * @param ees
     * @param queryGene
     * @param coexpressedGene
     * @param dedvs
     * @return map of EEID -> collection ProbeIDs which underlie the stored coexpression links.
     */
    private Map<Long, Collection<Long>> getProbeLinkValidation( Collection<ExpressionExperiment> ees, Gene queryGene,
            Gene coexpressedGene, Collection<DoubleVectorValueObject> dedvs ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Map<ExpressionExperiment, Collection<Long>> coexpressedEE2ProbeIds = new HashMap<ExpressionExperiment, Collection<Long>>();
        Map<ExpressionExperiment, Collection<Long>> queryEE2ProbeIds = new HashMap<ExpressionExperiment, Collection<Long>>();

        /*
         * Get the probes for the vectors, organize by ee.
         */
        for ( DoubleVectorValueObject dedv : dedvs ) {
            ExpressionExperiment ee = dedv.getExpressionExperiment();
            if ( dedv.getGenes().contains( queryGene ) ) {
                if ( !queryEE2ProbeIds.containsKey( ee ) ) {
                    queryEE2ProbeIds.put( ee, new HashSet<Long>() );
                }
                queryEE2ProbeIds.get( ee ).add( dedv.getDesignElement().getId() );
            } else if ( dedv.getGenes().contains( coexpressedGene ) ) {
                if ( !coexpressedEE2ProbeIds.containsKey( ee ) ) {
                    coexpressedEE2ProbeIds.put( ee, new HashSet<Long>() );
                }
                coexpressedEE2ProbeIds.get( ee ).add( dedv.getDesignElement().getId() );
            } else {
                log.error( "Dedv doesn't belong to coexpressed or query gene. QueryGene= " + queryGene
                        + "CoexpressedGene= " + coexpressedGene + "DEDV " + dedv.getId() + " has genes: "
                        + dedv.getGenes() );
            }
        }

        Map<Long, Collection<Long>> validatedProbes = new HashMap<Long, Collection<Long>>();
        for ( ExpressionExperiment ee : ees ) {

            Collection<Long> queryProbeIds = queryEE2ProbeIds.get( ee );
            Collection<Long> coexpressedProbeIds = coexpressedEE2ProbeIds.get( ee );

            if ( queryProbeIds == null || queryProbeIds.isEmpty() ) {
                log.warn( "Unexpectedly no probes for " + queryGene + " in " + ee );
                continue;
            }

            if ( coexpressedProbeIds == null || coexpressedProbeIds.isEmpty() ) {
                log.warn( "Unexpectedly no probes for " + coexpressedGene + " in " + ee );
                continue;
            }

            /*
             * Note: this does a probe-level query
             */
            Collection<Long> probesInLinks = this.probe2ProbeCoexpressionService.getCoexpressedProbes( queryProbeIds,
                    coexpressedProbeIds, ee, queryGene.getTaxon().getCommonName() );

            if ( probesInLinks.isEmpty() ) {
                log.warn( "Unexpectedly no probes for link between " + queryGene + " -and- " + coexpressedGene + " in "
                        + ee );
            }

            validatedProbes.put( ee.getId(), probesInLinks );
        }

        watch.stop();
        Long time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Validation of probes for " + ees.size() + " experiments in " + time + "ms." );
        }
        return validatedProbes;
    }

    /**
     * Get the names we'll use for the columns of the vectors.
     * 
     * @param vectors
     * @param vvo
     * @param layouts
     */
    private void getSampleNames( Collection<DoubleVectorValueObject> vectors, VisualizationValueObject vvo,
            Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        // DoubleVectorValueObject vec = vectors.iterator().next(); // just one as an example.

        for ( DoubleVectorValueObject vec : vectors ) {
            List<String> sampleNames = new ArrayList<String>();
            if ( layouts != null && layouts.get( vec.getExpressionExperiment() ) != null ) {
                for ( BioAssay ba : layouts.get( vec.getExpressionExperiment() ).keySet() ) {
                    sampleNames.add( ba.getName() ); // fIXME

                }
                if ( sampleNames.size() > 0 ) {
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

    /**
     * Get the factor values we'll use for grouping the columns of the vectors. Uses factors and factor values from
     * layouts
     * 
     * @param vectors
     * @param vvo
     * @param layouts
     */
    private void getFactorValues( Collection<DoubleVectorValueObject> vectors, VisualizationValueObject vvo,
            Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        List<String> colours = new LinkedList<String>();// TODO this is temp for testing, make function to generate
                                                        // colours

        colours.add( "#d2ff7f" );
        colours.add( "#82a938" );
        colours.add( "#ff9400" );
        colours.add( "#ffc97f" );
        colours.add( "#ff907f" );
        colours.add( "#a94738" );
        colours.add( "#907299" );
        colours.add( "#5292a5" );
        colours.add( "#953158" );
        colours.add( "#194617" );
        colours.add( "#634f9b" );
        colours.add( "#572265" );
        colours.add( "#362367" );
        colours.add( "#884c99" );
        colours.add( "#338c2f" );
        colours.add( "#e0a8bd" );
        colours.add( "#007fa5" );
        colours.add( "#e0709b" );
        colours.add( "#7f759b" );
        colours.add( "#a97a38" );
        colours.add( "#54231c" );
        colours.add( "#6fd369" );
        // Collections.shuffle( colours );
        // add some more later in list
        Random random = new Random();
        for ( int i = 0; i < 50; i++ ) {
            colours.add( "#" + ( Integer.toHexString( random.nextInt( 16 ) ) ) + "0"
                    + ( Integer.toHexString( random.nextInt( 16 ) ) ) + "0"
                    + ( Integer.toHexString( random.nextInt( 16 ) ) ) + "0" );
        }

        /*
         * colours.add( "#F37E79" ); colours.add( "#7998F3" ); colours.add( "#BBF379" ); colours.add( "" ); colours.add(
         * "" ); colours.add( "" ); colours.add( "" ); colours.add( "" ); colours.add( "" ); colours.add( "" );
         * colours.add( "" ); colours.add( "" ); colours.add( "" ); colours.add( "" );
         * 
         * 
         * for(int i = 1; i <= 3; i++){ for(int j = 1; j <= 3; j++){ for(int k = 1; k <= 3; k++){ if(i != 1 || j!= 1 ||
         * k!= 1){ colours.add( "rgb("+255/i+","+255/j+","+255/k+" )"); } } } } Collections.shuffle( colours );
         * System.out.println( colours );
         * 
         * for(int i = 4; i <= 6; i++){ for(int j = 4; j <= 6; j++){ for(int k = 4; k <= 6; k++){ colours.add(
         * "rgb("+255/i+","+255/j+","+255/k+" )"); } } }
         * 
         * 
         * 
         * 
         * /* int colourIndex = 0; String[] colours = new String[] { "FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF",
         * "00FFFF", "000000", "800000", "008000", "000080", "808000", "800080", "008080", "808080", "C00000", "00C000",
         * "0000C0", "C0C000", "C000C0", "00C0C0", "C0C0C0", "400000", "004000", "000040", "404000", "400040", "004040",
         * "404040", "200000", "002000", "000020", "202000", "200020", "002020", "202020", "600000", "006000", "000060",
         * "606000", "600060", "006060", "606060", "A00000", "00A000", "0000A0", "A0A000", "A000A0", "00A0A0", "A0A0A0",
         * "E00000", "00E000", "0000E0", "E0E000", "E000E0", "00E0E0", "E0E0E0", };
         * 
         * 
         * for(int i = 0; i<50; i++){ colours.add( (Integer.toHexString(random.nextInt( 16 ) ))+"0"
         * +(Integer.toHexString(random.nextInt( 16 ) ))+"0" +(Integer.toHexString(random.nextInt( 16 ) ))+"0"); }
         * 
         * // makes 64 colours for(int i = 0; i<16; i+=4){ for(int j = 0; j<16; j+=4){ for(int k = 0; k<16; k+=4){
         * colours.add(Integer.toHexString( i )+Integer.toHexString( i ) +Integer.toHexString( j )+Integer.toHexString(
         * j ) +Integer.toHexString( k )+Integer.toHexString( k )); } } }
         */

        // >1 vector can have same ee, but no need to do the same thing >1
        Collection<ExpressionExperiment> usedEEs = new ArrayList<ExpressionExperiment>();
        // DoubleVectorValueObject vec = vectors.iterator().next(); // just one as an example. TODO
        for ( DoubleVectorValueObject vec : vectors ) {
            ExpressionExperiment ee = vec.getExpressionExperiment();
            if ( usedEEs.contains( ee ) ) {
                continue;
            }
            LinkedHashMap<String, LinkedHashMap<String, String>> factorNames = new LinkedHashMap<String, LinkedHashMap<String, String>>();
            List<List<String>> factorValues = new ArrayList<List<String>>();
            /* list of maps with entries: key = factorName, value=array of factor values*/
            ArrayList<LinkedHashMap<String, String[]>> factorValueMaps = new ArrayList<LinkedHashMap<String, String[]>>(); 

            if ( layouts != null && layouts.get( ee ) != null ) {
                for ( BioAssay ba : layouts.get( ee ).keySet() ) {
                    // double should be the factorValue id, defined in
                    // ubic.gemma.visualization.ExperimentalDesignVisualizationService.getExperimentalDesignLayout(ExpressionExperiment,
                    // BioAssayDimension)
                    LinkedHashMap<ExperimentalFactor, Double> factorMap = layouts.get( ee ).get( ba );
                    LinkedHashMap<String, String[]> factorValuesToNames = new LinkedHashMap<String, String[]>();

                    // for each experimental factor, store the name and value
                    for ( Entry<ExperimentalFactor, Double> pair : factorMap.entrySet() ) {
                        // the double is only a double because it is meant to hold measurements when the factor is
                        // continuous
                        // if the factor is categorical, the double value is set to the value's id
                        // see
                        // ubic.gemma.visualization.ExperimentalDesignVisualizationService.getExperimentalDesignLayout(ExpressionExperiment,
                        // BioAssayDimension)
                        FactorValue facVal = factorValueService.load( new Long( Math.round( pair.getValue() ) ) );
                        StringBuffer facValsStr = new StringBuffer();
                        if ( facVal == null ) {
                            log.warn( "Failed to load factorValue with id = " + pair.getValue()
                                    + ". Load returned null. " );

                            String[] facValAndColour = new String[] { "No value", "#FFFFFF" };
                            factorValuesToNames.put( pair.getKey().getName(), facValAndColour );
                        } else {

                            if ( facVal.getCharacteristics() == null || facVal.getCharacteristics().isEmpty() ) {
                                facValsStr.append( facVal.getValue() + ", " );
                            }
                            for ( Characteristic characteristic : facVal.getCharacteristics() ) {
                                facValsStr.append( characteristic.getValue() + ", " );
                            }
                            if ( facValsStr.length() > 0 ) {
                                facValsStr.delete( facValsStr.length() - 2, facValsStr.length() );
                            }
                            if ( facValsStr.length() == 0 ) {
                                facValsStr.append( "FactorValue id:" + Math.round( pair.getValue() )
                                        + " was not null but no value was found." );
                            }
                            String colourString = "";
                            if ( !factorNames.containsKey( facVal.getExperimentalFactor().getName() ) ) {
                                factorNames.put( facVal.getExperimentalFactor().getName(),
                                        new LinkedHashMap<String, String>() );
                            }
                            if ( !( factorNames.get( facVal.getExperimentalFactor().getName() ) )
                                    .containsKey( facValsStr.toString() ) ) {
                                // assign a colour to this factor value
                                /*
                                 * int rand = random.nextInt(); colourString = "#"+new HsbColour( rand ).toHex();
                                 * colourString = new HsbColour( rand ).toRGBhtmlString(); if(colourIndex <
                                 * colours.length){ colourString = colours[colourIndex++]; }else{ colourString =
                                 * (Integer.toHexString(random.nextInt( 16 ) ))+"0"
                                 * +(Integer.toHexString(random.nextInt( 16 ) ))+"0"
                                 * +(Integer.toHexString(random.nextInt( 16 ) ))+"0"; }
                                 */

                                colourString = colours.remove( 0 ); // for testing
                                colours.add( colourString ); // for testing
                                ( factorNames.get( facVal.getExperimentalFactor().getName() ) ).put( facValsStr
                                        .toString(), colourString );
                            } else {
                                colourString = ( factorNames.get( facVal.getExperimentalFactor().getName() ) )
                                        .get( facValsStr.toString() );
                            }
                            String[] facValAndColour = new String[] { facValsStr.toString(), colourString };
                            factorValuesToNames.put( facVal.getExperimentalFactor().getName(), facValAndColour );
                        }
                    }
                    factorValueMaps.add( factorValuesToNames );
                }
            }
            vvo.setFactorNames( factorNames );
            vvo.setFactorValues( factorValues );
            vvo.setFactorValuesToNames( factorValueMaps );
        }
    }

    /**
     * @param dedv
     * @return
     */
    private List<String> getSampleNames( DoubleVectorValueObject dedv ) {
        List<String> result = new ArrayList<String>();
        BioAssayDimension bioAssayDimension = dedv.getBioAssayDimension();
        if ( bioAssayDimension == null ) {
            return result;
        }
        if ( bioAssayDimension.getId() != null ) {
            bioAssayDimension = bioAssayDimensionService.thaw( bioAssayDimension );
        }
        for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
            result.add( ba.getName() );
        }
        return result;
    }

    /**
     * Takes the DEDVs and put them in point objects and normalize the values. returns a map of eeid to visValueObject.
     * Currently removes multiple hits for same gene. Tries to pick best DEDV. Organizes the experiments from lowest to
     * higest p-value
     * 
     * @param dedvs
     * @param genes
     * @param layouts
     * @return
     */
    private VisualizationValueObject[] makeDiffVisCollection( Collection<DoubleVectorValueObject> dedvs,
            List<Gene> genes, Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes,
            Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Map<Long, Collection<DoubleVectorValueObject>> vvoMap = new HashMap<Long, Collection<DoubleVectorValueObject>>();

        Map<Long, ExpressionExperiment> eeMap = new HashMap<Long, ExpressionExperiment>();

        // Organize by expression experiment
        for ( DoubleVectorValueObject dvvo : dedvs ) {
            ExpressionExperiment ee = dvvo.getExpressionExperiment();
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

        List<EE2PValue> sortedEE = new ArrayList<EE2PValue>();

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

        // Create collection of visualizationValueObject for flotr on js side
        int i = 0;
        for ( EE2PValue ee2P : sortedEE ) {

            VisualizationValueObject vvo = new VisualizationValueObject( vvoMap.get( ee2P.getEEId() ), genes, ee2P
                    .getPValue(), validatedProbes.get( ee2P.getEEId() ) );

            getSampleNames( vvoMap.get( ee2P.getEEId() ), vvo, layouts );
            getFactorValues( vvoMap.get( ee2P.getEEId() ), vvo, layouts );

            /*
             * Set up the experimental designinfo so we can show it above the graph.
             */

            if ( layouts != null ) {
                ExpressionExperiment ee = eeMap.get( ee2P.getEEId() );
                log.debug( "setup experimental design layout profiles for " + ee );
                vvo.setUpFactorProfiles( layouts.get( ee ) );
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
     * @param dedv
     * @return
     */
    private String makeHeader( DoubleVectorValueObject dedv ) {
        StringBuilder buf = new StringBuilder();
        ExpressionExperiment ee = dedv.getExpressionExperiment();
        buf.append( "# " + ee.getShortName() + " : " + ee.getName() + "\n" );

        buf.append( "Gene Symbol\tGene Name\tProbe\t" );

        BioAssayDimension bad = bioAssayDimensionService.thaw( dedv.getBioAssayDimension() );

        if ( bad == null ) {
            // !!! FIXME
            buf.append( "\n" );
            return buf.toString();
        }

        for ( BioAssay ba : bad.getBioAssays() ) {
            buf.append( ba.getName() + "\t" );
        }
        buf.deleteCharAt( buf.length() - 1 );

        buf.append( "\n" );

        return buf.toString();
    }

    /**
     * @param newResults
     * @return
     */
    private Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> makeVectorMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result = new HashMap<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperiment e = v.getExpressionExperiment();
            if ( !result.containsKey( e ) ) {
                result.put( e, new HashMap<Gene, Collection<DoubleVectorValueObject>>() );
            }
            Map<Gene, Collection<DoubleVectorValueObject>> innerMap = result.get( e );

            if ( v.getGenes() == null || v.getGenes().isEmpty() ) continue;

            for ( Gene g : v.getGenes() ) {
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
     * 
     * @param dedvs
     * @param genes
     * @param validatedProbes
     * @param layouts
     * @return
     */
    private VisualizationValueObject[] makeVisCollection( Collection<DoubleVectorValueObject> dedvs,
            Collection<Gene> genes, Map<Long, Collection<Long>> validatedProbes,
            Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        StopWatch timer = new StopWatch();
        timer.start();
        Map<ExpressionExperiment, List<DoubleVectorValueObject>> vvoMap = new HashMap<ExpressionExperiment, List<DoubleVectorValueObject>>();
        // Organize by expression experiment
        if ( dedvs == null || dedvs.isEmpty() ) return new VisualizationValueObject[1];

        for ( DoubleVectorValueObject dvvo : dedvs ) {
            ExpressionExperiment ee = dvvo.getExpressionExperiment();
            if ( !vvoMap.containsKey( ee ) ) {
                vvoMap.put( ee, new ArrayList<DoubleVectorValueObject>() );
            }
            vvoMap.get( ee ).add( dvvo );
        }

        VisualizationValueObject[] result = new VisualizationValueObject[vvoMap.keySet().size()];
        // Create collection of visualizationValueObject for flotr on js side
        int i = 0;
        for ( ExpressionExperiment ee : vvoMap.keySet() ) {

            Collection<Long> validatedProbeList = null;
            if ( validatedProbes != null ) {
                validatedProbeList = validatedProbes.get( ee.getId() );
            }
            Collection<DoubleVectorValueObject> vectors = vvoMap.get( ee );

            List<Gene> geneList = null;

            if ( genes != null ) {
                geneList = new ArrayList<Gene>( genes );
            }

            VisualizationValueObject vvo = new VisualizationValueObject( vectors, geneList, validatedProbeList );

            if ( vectors.size() > 0 ) {
                getSampleNames( vectors, vvo, layouts );
                getFactorValues( vectors, vvo, layouts );
            }

            /*
             * Set up the experimental designinfo so we can show it above the graph.
             */
            if ( layouts != null ) vvo.setUpFactorProfiles( layouts.get( ee ) );

            result[i] = vvo;
            i++;
        }

        long time = timer.getTime();
        if ( time > 1000 ) {
            log.info( "Created vis value objects in: " + time );
        }

        return result;

    }

}
