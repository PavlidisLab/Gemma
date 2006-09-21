/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List; 
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.basecode.util.FileTools;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.visualization.HttpExpressionDataMatrixVisualizer;
import ubic.gemma.visualization.MatrixVisualizer;
import ubic.gemma.web.controller.BaseFormController;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link stringency} sets the number of data sets the link must be seen in before it is listed in the results, and
 * {@link species} sets the type of species to search. 
 * {@link keywords} restrict the search.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="coexpressionSearchController"  
 * @spring.property name = "commandName" value="coexpressionSearchCommand"
 * @spring.property name = "commandClass"
 *                  value="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand"
 * @spring.property name = "formView" value="searchCoexpression"
 * @spring.property name = "successView" value="showCoexpressionSearchResults"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class CoexpressionSearchController extends BaseFormController {
    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    private ExpressionExperimentService expressionExperimentService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private GeneService geneService = null;
    private Map<DesignElement, Collection<Gene>> designElementToGeneMap = null;
    private List<DesignElement> compositeSequences = null;

    public CoexpressionSearchController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        CoexpressionSearchCommand csc = new CoexpressionSearchCommand();

        csc.setSearchString( "" );
        csc.setStringency( 1 );
        csc.setSpecies("Human");

        return csc;

    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        CoexpressionSearchCommand csc = ( ( CoexpressionSearchCommand ) command );

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Canceled" );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ));

        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * Mock function - do not use.
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings("unused")
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );
        
        CoexpressionSearchCommand csc = ( ( CoexpressionSearchCommand ) command );
        String searchCriteria = ( (CoexpressionSearchCommand ) command ).getSearchCriteria();
        boolean suppressVisualizations = ( ( CoexpressionSearchCommand ) command ).isSuppressVisualizations();

        File imageFile = File.createTempFile( request.getRemoteUser() + request.getSession( true ).getId()
                + RandomStringUtils.randomAlphabetic( 5 ), ".png", FileTools
                .createDir( "../webapps/ROOT/visualization/" ) );

        log.debug( "Image to be stored in " + imageFile.getAbsolutePath() );
        Collection foundGenes = null;
        ExpressionDataMatrix expressionDataMatrix = null;
        MatrixVisualizer matrixVisualizer = null;
        if ( searchCriteria.equalsIgnoreCase( "probe set id" ) ) {
            ExpressionExperiment ee = expressionExperimentService.findById( Long.decode( "1" ) );
            expressionDataMatrix = new ExpressionDataMatrix( ee, compositeSequences );

            matrixVisualizer = new HttpExpressionDataMatrixVisualizer( expressionDataMatrix, "http", request
                    .getServerName(), request.getServerPort(), imageFile.getAbsolutePath() );
            matrixVisualizer.setSuppressVisualizations( suppressVisualizations );
        } 
        else if (searchCriteria.equalsIgnoreCase( "gene symbol" )) {
            log.debug( "search by official gene symbol" );
            foundGenes = geneService.findByOfficialSymbol (csc.getSearchString() );
            // call service which produces expression data image based on gene symbol search criteria
        }
        else {
            log.debug("Unknown search");
        }
        ModelAndView mav = new ModelAndView(getSuccessView());
        mav.addObject( "matrixVisualizer", matrixVisualizer );
        mav.addObject( "foundGenes", foundGenes );
        mav.addObject( "coexpressionSearchCommand", csc );
        return mav;
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        // add search categories
        Collection<String> searchCategories = new HashSet<String>();
        searchCategories.add( "gene symbol" );
        //searchCategories.add( "probe set id" );

        Map<String, Collection<String>> searchByMap = new HashMap<String, Collection<String>>();
        
        searchByMap.put( "searchCategories", searchCategories );
        
        // add species
        Collection<String> speciesCategories = new HashSet<String>();
        speciesCategories.add( "Human" );
        speciesCategories.add( "Mouse" );
        speciesCategories.add( "Rat" );        
        
        searchByMap.put( "speciesCategories", speciesCategories );        
        
        return searchByMap;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }
    
    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }    
}