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

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseFormController;
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

    public Map<Long, Collection<DoubleVectorValueObject>> getDEDV( Collection<Long> eeIds, Collection<Long> geneIds )
            throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();

        // Get and thaw the experiments.
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        for ( ExpressionExperiment ee : ees ) {
            expressionExperimentService.thawLite( ee );
        }

        // Get and thaw gene
        Collection<Gene> genes = geneService.loadMultiple( geneIds );

        if ( genes == null || genes.isEmpty() ) return null;

        // Get dedv's
        Collection<DoubleVectorValueObject> dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvMap.size() + " DEDVs for eeIDs: " + eeIds + " and GeneIds: " + geneIds + " in : "
                + time + " ms." );

        return mapInvert( dedvMap );

    }

    // Private method used for inverting the DEDV map. Having DEDV's as the key is not always useful and DWR does not
    // like non-string key values for the map. During the inverting process Gemma Gene Ids are used instead of the GEne
    // object themselves.
    private Map<Long, Collection<DoubleVectorValueObject>> mapInvert( Collection<DoubleVectorValueObject> dedvMap ) {
        Map<Long, Collection<DoubleVectorValueObject>> convertedMap = new HashMap<Long, Collection<DoubleVectorValueObject>>();

        for ( DoubleVectorValueObject dvvo : dedvMap ) {

            for ( Gene g : dvvo.getGenes() ) {
                if ( convertedMap.containsKey( g.getId() ) )
                    convertedMap.get( g.getId() ).add( dvvo );
                else {// If the gene is not already in the map we need to create the collection to hold the dedv.
                    Collection<DoubleVectorValueObject> dvvos = new HashSet<DoubleVectorValueObject>();
                    dvvos.add( dvvo );
                    convertedMap.put( g.getId(), dvvos );
                }
            }

        }

        return convertedMap;
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

        Map<Long, Collection<DoubleVectorValueObject>> result = getDEDV( eeIds, geneIds );

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
     * @param toConvert
     * @return
     */
    private String format4File( Map<Long, Collection<DoubleVectorValueObject>> toConvert ) {
        StringBuffer converted = new StringBuffer();
        converted.append( "EE \t GENE \t PROBE \t DEDV \n" );
        for ( Long geneId : toConvert.keySet() ) {
            String geneName = geneService.load( geneId ).getOfficialSymbol();

            for ( DoubleVectorValueObject dedv : toConvert.get( geneId ) ) {
                ExpressionExperiment ee = dedv.getExpressionExperiment();
                expressionExperimentService.thawLite( ee );

                converted.append( ee.getShortName() + " \t " );
                converted.append( geneName + " \t " );
                converted.append( dedv.getDesignElement().getId() + "\t" );

                for ( double data : dedv.getData() ) {
                    converted.append( data + "|" );
                }
                converted.deleteCharAt( converted.length() - 1 ); // remove the pipe.
                converted.append( "\n" );
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
