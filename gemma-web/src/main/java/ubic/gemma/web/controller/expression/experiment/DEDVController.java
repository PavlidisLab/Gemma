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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.visualization.VisualizationValueObject;
import ubic.gemma.web.view.TextView;

/**
 * Exposes methods for accessing underlying Design Element Data Vectors. eg: ajax methods for visualization
 * 
 * @spring.bean id="dedvController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "processedExpressionDataVectorService" ref="processedExpressionDataVectorService"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 * @author kelsey
 * @version $Id$
 */
public class DEDVController extends BaseFormController {

    private static Log log = LogFactory.getLog( DEDVController.class );

    // ------------------------------
    // Service Members
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    private GeneService geneService;
    private ExpressionExperimentService expressionExperimentService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;

    // -----------------------
    // Exposed Ajax Methods

    /**
     * Given a collection of expression experiment Ids and a geneId returns a map of DEDV value objects to a collection
     * of genes. The EE info is in the value object.
     */

    @SuppressWarnings("unchecked")
    public Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> getDEDV( Collection<Long> eeIds,
            Collection<Long> geneIds ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        // Performance note: the above is fast except for the need to security-filter the EEs. This takes 90% of the
        // time.

        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

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

    public VisualizationValueObject[] getDEDVForVisualization( Collection<Long> eeIds, Collection<Long> geneIds ) {

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        // Performance note: the above is fast except for the need to security-filter the EEs. This takes 90% of the
        // time.

        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                + " genes in " + time + " ms." );

        return makeVisCollection( dedvs, new ArrayList<Gene>( genes ), null );

    }

    /**
     * AJAX exposed method
     * 
     * @param eeIds
     * @param geneIds
     * @return
     */

    @SuppressWarnings("unchecked")
    public VisualizationValueObject[] getDEDVForCoexpressionVisualization( Collection<Long> eeIds, Long queryGeneId,
            Long coexpressedGeneId ) {

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        // Performance note: the above is fast except for the need to security-filter the EEs. This takes 90% of the
        // time.
        Gene queryGene = geneService.load( queryGeneId );
        Gene coexpressedGene = geneService.load( coexpressedGeneId );

        List<Gene> genes = new ArrayList<Gene>();
        genes.add( queryGene );
        genes.add( coexpressedGene );
        geneService.thawLite( genes );

        if ( genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

        watch.stop();
        Long time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + genes.size()
                    + " genes in " + time + " ms." );
        }

        watch = new StopWatch();
        watch.start();
        Map<Long, Collection<Long>> coexpressedEE2ProbeIds = new HashMap<Long, Collection<Long>>();
        Map<Long, Collection<Long>> queryEE2ProbeIds = new HashMap<Long, Collection<Long>>();

        for ( DoubleVectorValueObject dedv : dedvs ) {
            if ( dedv.getGenes().contains( queryGene ) ) {
                if ( queryEE2ProbeIds.containsKey( dedv.getExpressionExperiment().getId() ) )
                    queryEE2ProbeIds.get( dedv.getExpressionExperiment().getId() )
                            .add( dedv.getDesignElement().getId() );
                else {
                    Collection qProbeIds = new ArrayList<Long>();
                    qProbeIds.add( dedv.getDesignElement().getId() );
                    queryEE2ProbeIds.put( dedv.getExpressionExperiment().getId(), qProbeIds );
                }
            } else if ( dedv.getGenes().contains( coexpressedGene ) ) {
                if ( coexpressedEE2ProbeIds.containsKey( dedv.getExpressionExperiment().getId() ) )
                    coexpressedEE2ProbeIds.get( dedv.getExpressionExperiment().getId() ).add(
                            dedv.getDesignElement().getId() );
                else {
                    Collection cProbeIds = new ArrayList<Long>();
                    cProbeIds.add( dedv.getDesignElement().getId() );
                    coexpressedEE2ProbeIds.put( dedv.getExpressionExperiment().getId(), cProbeIds );
                }
            } else {
                log.error( "Impossible! Dedv doesn't belong to coexpressed or query gene. QueryGene= "
                        + queryGene.getOfficialSymbol() + "CoexprssedGene= " + coexpressedGene.getOfficialSymbol()
                        + "DEDV " + dedv.getId() + " has genes: " + dedv.getGenes() );
            }
        }

        Map<Long, Collection<Long>> validatedProbes = new HashMap<Long, Collection<Long>>();
        for ( ExpressionExperiment ee : ees ) {
            validatedProbes.put( ee.getId(), this.probe2ProbeCoexpressionService.validateProbesInCoexpression(
                    queryEE2ProbeIds.get( ee.getId() ), coexpressedEE2ProbeIds.get( ee.getId() ), ee, queryGene
                            .getTaxon().getCommonName() ) );

        }

        watch.stop();
        time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Validation of probes for " + ees.size() + " experiments in " + time + " ms." );
        }

        return makeVisCollection( dedvs, genes, validatedProbes );

    }

    /**
     * Takes the DEDVs and put them in point objects and normalize the values. returns a map of eeid to visValueObject.
     * Currently removes multiple hits for same gene. Tries to pick best DEDV.
     * 
     * @param dedvs
     * @param genes
     * @return
     */
    private VisualizationValueObject[] makeVisCollection( Collection<DoubleVectorValueObject> dedvs, List<Gene> genes,
            Map<Long, Collection<Long>> validatedProbes ) {

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

            if ( validatedProbes != null ) validatedProbeList = validatedProbes.get( ee.getId() );
            VisualizationValueObject vvo = new VisualizationValueObject( vvoMap.get( ee ), genes, validatedProbeList );
            result[i] = vvo;
            i++;
        }

        return result;

    }

    /**
     * Takes the DEDVs and put them in point objects and normalize the values. returns a map of eeid to visValueObject.
     * Currently removes multiple hits for same gene. Tries to pick best DEDV.
     * 
     * @param dedvs
     * @param genes
     * @return
     */
    private VisualizationValueObject[] makeVisCollection( Collection<DoubleVectorValueObject> dedvs, List<Gene> genes ) {

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
            VisualizationValueObject vvo = new VisualizationValueObject( vvoMap.get( ee ), genes );
            result[i] = vvo;
            i++;
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

    /*
     * Handle case of text export of the results.
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings( { "unchecked", "unused" })
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

    // --------------------------------
    // Dependency injection setters
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param processedExpressionDataVectorService the processedExpressionDataVectorService to set
     */
    public void setProcessedExpressionDataVectorService(
            ProcessedExpressionDataVectorService processedExpressionDataVectorService ) {
        this.processedExpressionDataVectorService = processedExpressionDataVectorService;
    }

    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

}
