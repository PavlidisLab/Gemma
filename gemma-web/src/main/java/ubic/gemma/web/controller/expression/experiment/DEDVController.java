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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.Point;
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

    Collection<VisualizationValueObject> getDEDVForVisulization( Collection<Long> eeIds, Collection<Long> geneIds ) {

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

        return makeVisCollection( dedvs );

    }

    /**
     * Takes the DEDVs and put them in point objects. Get the value objects for the experiment and normalize the values.
     * 
     * @param dedvs
     * @return
     */
    private Collection<VisualizationValueObject> makeVisCollection( Collection<DoubleVectorValueObject> dedvs ) {

                
        Collection<VisualizationValueObject> vvos = new ArrayList<VisualizationValueObject>();

        for ( DoubleVectorValueObject dvvo : dedvs ) {
            VisualizationValueObject vvo = new VisualizationValueObject(dvvo);
        }
        
        return vvos;

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
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
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

}
