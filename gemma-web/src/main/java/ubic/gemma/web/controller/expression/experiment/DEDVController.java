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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.visualization.ExperimentalDesignVisualizationService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.visualization.ExpressionProfileDataObject;
import ubic.gemma.web.controller.visualization.VisualizationValueObject;
import ubic.gemma.web.view.TextView;
import cern.colt.list.DoubleArrayList;

/**
 * Exposes methods for accessing underlying Design Element Data Vectors. eg: ajax methods for visualization
 * 
 * @spring.bean id="dedvController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "processedExpressionDataVectorService" ref="processedExpressionDataVectorService"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="geneDifferentialExpressionService" ref="geneDifferentialExpressionService"
 * @spring.property name="experimentalDesignVisualizationService" ref="experimentalDesignVisualizationService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name="differentialExpressionAnalysisResultService" ref="differentialExpressionAnalysisResultService"

 * 
 * @author kelsey
 * @version $Id$
 */
public class DEDVController extends BaseFormController {

    private static final int SAMPLE_SIZE = 20;  //Number of dedvs to return if no genes given 
    private static final double DEFAULT_THRESHOLD = 0.05;
    private static final int MAX_RESULTS_TO_RETURN = 100;
    
    
    private DesignElementDataVectorService designElementDataVectorService;
    private ExperimentalDesignVisualizationService experimentalDesignVisualizationService;
    private ExpressionExperimentService expressionExperimentService;
    private GeneDifferentialExpressionService geneDifferentialExpressionService;
    private GeneService geneService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    private CompositeSequenceService compositeSequenceService;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService;
    
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

        
        Collection<DoubleVectorValueObject>  dedvMap;
        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) {
            dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees.iterator().next(),50, false );
        }
        else{
            dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees, genes );
        }
        //FIXME: Commented out for performance and factor info not displayed on front end yet anyway. 
       // experimentalDesignVisualizationService.sortVectorDataByDesign( dedvMap );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvMap.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                + " genes in " + time + " ms." );

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
        genes.add( queryGene );
        genes.add( coexpressedGene );
        geneService.thawLite( genes );

        if ( genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes,false );

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts = null;

        //FIXME: Commented out for performance and factor info not displayed on front end yet anyway. 
        //layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

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
     * AJAX exposed method
     *  private DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService;
     * @param eeIds
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
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;
        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes, false );

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts = null;
        //FIXME: Commented out for performance and factor info not displayed on front end yet anyway. 
        //layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                + " genes in " + time + " ms." );

        watch = new StopWatch();
        watch.start();

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = getProbeDiffExValidation( ees,
                genes, threshold, factorMap );

        watch.stop();
        time = watch.getTime();

        log.info( "Retrieved " + validatedProbes.size() + " valid probes in " + time + " ms." );

        return makeDiffVisCollection( dedvs, new ArrayList<Gene>( genes ), validatedProbes, layouts );

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
    public VisualizationValueObject[] getDEDVForVisualizationByProbe( Collection<Long> eeIds, Collection<Long> probeIds) {

        if ( eeIds.isEmpty() || probeIds.isEmpty() ) return null;

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;
        
        Collection<CompositeSequence> probes = this.compositeSequenceService.loadMultiple( probeIds );
        if ( probes == null || probes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArraysByProbe( ees, probes, false );

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts = null;
        //FIXME: Commented out for performance and factor info not displayed on front end yet anyway. 
        //layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + probeIds.size()
                + " genes in " + time + " ms." );


        return makeVisCollection( dedvs, null, null, null );

    }


    /**
     * AJAX exposed method
     * 
     * @param resultSetIds
     * @param threshold for 'significance'
     * @return collection of visualization value objects
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualizationByThreshold( Long eeId, Long resultSetId, Double givenThreshold){

        if (resultSetId == null)  return null;
        
        if (eeId == null) return null;
        
        
        double threshold = DEFAULT_THRESHOLD;
        
        if (givenThreshold != null){
            threshold = givenThreshold;
            log.warn( "Threshold specified not using default value: " + givenThreshold );
            
        }
        
        
        StopWatch watch = new StopWatch();
        watch.start();
        
        AnalysisResultSet ar = differentialExpressionAnalysisResultService.loadAnalysisResult( resultSetId );
        if ( ar == null) return null;
          
        Collection<ExpressionAnalysisResultSet> ars = new ArrayList<ExpressionAnalysisResultSet>();
         ars.add( (ExpressionAnalysisResultSet) ar );
 
         ExpressionExperiment ee = expressionExperimentService.load( eeId );
         if (ee == null) return null;
        Collection<ExpressionExperiment> ees = new ArrayList<ExpressionExperiment>();
        ees.add( ee );
        
        Map<ExpressionAnalysisResultSet,Collection<ProbeAnalysisResult>> ee2probeResults = differentialExpressionAnalysisService.findGenesInResultSetsThatMetThreshold(ars, threshold);
        
        if(ee2probeResults == null || ee2probeResults.isEmpty()) return null;
        
        
        Collection<CompositeSequence> probes = new HashSet<CompositeSequence>();
        //TODO: put a limit on the results in the DAO        
        for (ProbeAnalysisResult par : ee2probeResults.get(ar )){            
            probes.add(par.getProbe());
                if (probes.size() > MAX_RESULTS_TO_RETURN) break;
        }
        
        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArraysByProbe(ees , probes, false );

        //Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts = null;
        //FIXME: Commented out for performance and factor info not displayed on front end yet anyway. 
        //layouts = experimentalDesignVisualizationService.sortVectorDataByDesign( dedvs );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + ar.getId() + " ResultSetId and " + probes.size()
                + " genes in " + time + " ms." );


        return makeVisCollection( dedvs, null, null, null );

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
        
        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) {
            dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees.iterator().next(),SAMPLE_SIZE, false );
        }
        else{
            dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees, genes, false );
        }

        watch.stop();
        Long time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                    + " genes in " + time + " ms (times <100ms not reported)." );
        }

        return makeVisCollection( dedvs, new ArrayList<Gene>( genes ), null, null );

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
            }catch(IllegalArgumentException iae){
                log.warn(iae );
            }
     
        }

        // TODO fill in gene; normalize and clip if desired.; watch for invalid ids.

        return result;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * @param experimentalDesignVisualizationService the experimentalDesignVisualizationService to set
     */
    public void setExperimentalDesignVisualizationService(
            ExperimentalDesignVisualizationService experimentalDesignVisualizationService ) {
        this.experimentalDesignVisualizationService = experimentalDesignVisualizationService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setGeneDifferentialExpressionService(
            GeneDifferentialExpressionService geneDifferentialExpressionService ) {
        this.geneDifferentialExpressionService = geneDifferentialExpressionService;
    }

    // --------------------------------
    // Dependency injection setters
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    /**
     * @param processedExpressionDataVectorService the processedExpressionDataVectorService to set
     */
    public void setProcessedExpressionDataVectorService(
            ProcessedExpressionDataVectorService processedExpressionDataVectorService ) {
        this.processedExpressionDataVectorService = processedExpressionDataVectorService;
    }

    /*
     * Handle case of text export of the results.
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
        Collection<Long> eeIds = extractIds( request.getParameter( "ee" ) );

        ModelAndView mav = new ModelAndView( new TextView() );

        if ( geneIds == null || geneIds.isEmpty() || eeIds == null || eeIds.isEmpty() ) {
            mav.addObject( "text", "Input empty for finding DEDVs: " + geneIds + " and " + eeIds );
            return mav;

        }

        Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result = getDEDV( eeIds, geneIds );

        if ( result == null || result.isEmpty() ) {
            mav.addObject( "text", " No DEDV results for genes: " + geneIds + " and datasets: " + eeIds );
            return mav;
        }

        mav.addObject( "text", format4File( result ) );
        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved and Formated" + result.keySet().size() + " DEDVs for eeIDs: " + eeIds + " and GeneIds: "
                + geneIds + " in : " + time + " ms." );

        return mav;

    }

    /**
     * Converts the given map into a tab delimited String
     * 
     * @param result
     * @return
     */
    private String format4File( Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result ) {
        StringBuffer converted = new StringBuffer();
        converted.append( "Experiment\tGene\tProbe\tData\n" );
        Map<Long, String> genes = new HashMap<Long, String>();
        for ( ExpressionExperiment ee : result.keySet() ) {

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
                    ee = dedv.getExpressionExperiment();

                    converted.append( ee.getShortName() + " \t " );
                    converted.append( ee.getName() + " \t " );
                    converted.append( geneName + " \t " + g.getOfficialName() + "\t" );
                    converted.append( dedv.getDesignElement().getName() + "\t" );

                    for ( double data : dedv.getData() ) {
                        converted.append( String.format( "%.3f", data ) + "|" );
                    }
                    converted.deleteCharAt( converted.length() - 1 ); // remove the pipe.
                    converted.append( "\n" );
                }
            }
        }
        converted.append( "\r\n" );
        return converted.toString();
    }

    /**
     * @param ees
     * @param genes
     * @param threshold
     * @param factorMap
     * @return
     */
    private Map<Long, Collection<DifferentialExpressionValueObject>> getProbeDiffExValidation(
            Collection<ExpressionExperiment> ees, Collection<Gene> genes, Double threshold,
            Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

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
                    .getDifferentialExpression( ees, gene, threshold, factorMap );

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
     * @return map of EEID -> collection ProbeIDs
     */
    private Map<Long, Collection<Long>> getProbeLinkValidation( Collection<ExpressionExperiment> ees, Gene queryGene,
            Gene coexpressedGene, Collection<DoubleVectorValueObject> dedvs ) {
        StopWatch watch;
        Long time;
        watch = new StopWatch();
        watch.start();
        Map<Long, Collection<Long>> coexpressedEE2ProbeIds = new HashMap<Long, Collection<Long>>();
        Map<Long, Collection<Long>> queryEE2ProbeIds = new HashMap<Long, Collection<Long>>();

        for ( DoubleVectorValueObject dedv : dedvs ) {
            Long eeid = dedv.getExpressionExperiment().getId();
            if ( dedv.getGenes().contains( queryGene ) ) {
                if ( !queryEE2ProbeIds.containsKey( eeid ) ) {
                    queryEE2ProbeIds.put( eeid, new HashSet<Long>() );
                }
                queryEE2ProbeIds.get( eeid ).add( dedv.getDesignElement().getId() );
            } else if ( dedv.getGenes().contains( coexpressedGene ) ) {
                if ( !coexpressedEE2ProbeIds.containsKey( eeid ) ) {
                    coexpressedEE2ProbeIds.put( eeid, new HashSet<Long>() );
                }
                coexpressedEE2ProbeIds.get( eeid ).add( dedv.getDesignElement().getId() );
            } else {
                log.error( "Impossible! Dedv doesn't belong to coexpressed or query gene. QueryGene= "
                        + queryGene.getOfficialSymbol() + "CoexprssedGene= " + coexpressedGene.getOfficialSymbol()
                        + "DEDV " + dedv.getId() + " has genes: " + dedv.getGenes() );
            }
        }

        Map<Long, Collection<Long>> validatedProbes = new HashMap<Long, Collection<Long>>();
        for ( ExpressionExperiment ee : ees ) {
            Collection<Long> queryProbeIds = queryEE2ProbeIds.get( ee.getId() );
            Collection<Long> coexpressedProbeIds = coexpressedEE2ProbeIds.get( ee.getId() );

            if ( queryProbeIds == null || queryProbeIds.size() == 0 ) {
                log.warn( "Unexpectedly no probes for query in " + ee );
                continue;
            }

            if ( coexpressedProbeIds == null || coexpressedProbeIds.size() == 0 ) {
                log.warn( "Unexpectedly no probes for coexpressed gene in " + ee );
                continue;
            }

            validatedProbes.put( ee.getId(), this.probe2ProbeCoexpressionService.validateProbesInCoexpression(
                    queryProbeIds, coexpressedProbeIds, ee, queryGene.getTaxon().getCommonName() ) );
        }

        watch.stop();
        time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Validation of probes for " + ees.size() + " experiments in " + time + " ms." );
        }
        return validatedProbes;
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
            Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts ) {

        Long time;
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

        watch.stop();
        time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Making vis collection - Organizing DEDVS by Expression experimen took: " + time );
        }

        watch = new StopWatch();
        watch.start();

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

        watch.stop();
        time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Making vis collection - Sorted DEDVS by lowest p value for Expression experiment took: " + time );
        }

        watch = new StopWatch();
        watch.start();

        VisualizationValueObject[] result = new VisualizationValueObject[vvoMap.keySet().size()];

        // Create collection of visualizationValueObject for flotr on js side
        int i = 0;
        for ( EE2PValue ee2P : sortedEE ) {
    
            VisualizationValueObject vvo = new VisualizationValueObject( vvoMap.get( ee2P.getEEId() ), genes,
                     ee2P.getPValue(),validatedProbes.get( ee2P.getEEId() )  );

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

        watch.stop();
        time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Making vis collection - Created vis value objects in: " + time );
        }
        return result;

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
     * @param layouts
     * @return
     */
    private VisualizationValueObject[] makeVisCollection( Collection<DoubleVectorValueObject> dedvs, List<Gene> genes,
            Map<Long, Collection<Long>> validatedProbes,
            Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts ) {

        Map<ExpressionExperiment, Collection<DoubleVectorValueObject>> vvoMap = new HashMap<ExpressionExperiment, Collection<DoubleVectorValueObject>>();
        // Organize by expression experiment
        for ( DoubleVectorValueObject dvvo : dedvs ) {
            ExpressionExperiment ee = dvvo.getExpressionExperiment();
            if ( !vvoMap.containsKey( ee ) ) {
                vvoMap.put( ee, new HashSet<DoubleVectorValueObject>() );
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
            VisualizationValueObject vvo = new VisualizationValueObject( vectors, genes, validatedProbeList);

            /*
             * Set up the experimental designinfo so we can show it above the graph.
             */
            if ( layouts != null ) vvo.setUpFactorProfiles( layouts.get( ee ) );

            result[i] = vvo;
            i++;
        }

        return result;

    }



    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }
    
    public void setDifferentialExpressionAnalysisService(DifferentialExpressionAnalysisService differentialExpressionAnalysisService){
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }



    public void setDifferentialExpressionAnalysisResultService(
            DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService ) {
        this.differentialExpressionAnalysisResultService = differentialExpressionAnalysisResultService;
    }

}
