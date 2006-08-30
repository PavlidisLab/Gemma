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
package ubic.gemma.web.controller.expression.experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.basecode.util.FileTools;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.visualization.HttpExpressionDataMatrixVisualizer;
import ubic.gemma.visualization.MatrixVisualizer;
import ubic.gemma.web.controller.BaseFormController;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link stringency} sets the number of data sets the link must be seen in before it is listed in the results, and
 * {@link species} sets the type of species to search. {@link keywords} restrict the search.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentSearchController" name="/expressionExperiment/searchExpressionExperiment.html"
 * @spring.property name = "commandName" value="expressionExperimentSearchCommand"
 * @spring.property name = "commandClass"
 *                  value="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSearchCommand"
 * @spring.property name = "formView" value="searchExpressionExperimentForm"
 * @spring.property name = "successView" value="showExpressionExperimentSearchResults"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class ExpressionExperimentSearchController extends BaseFormController {
    private static Log log = LogFactory.getLog( ExpressionExperimentSearchController.class.getName() );

    private ExpressionExperimentService expressionExperimentService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private Map<DesignElement, Collection<Gene>> designElementToGeneMap = null;
    private List<DesignElement> compositeSequences = null;

    public ExpressionExperimentSearchController() {
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

        Long id = null;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Id was not valid Long integer", e );
        }
        log.debug( id );

        ExpressionExperiment ee = null;
        ExpressionExperimentSearchCommand eesc = new ExpressionExperimentSearchCommand();

        if ( id != null && StringUtils.isNotBlank( id.toString() ) ) {
            ee = expressionExperimentService.findById( id );
        } else {
            ee = ExpressionExperiment.Factory.newInstance();
        }

        eesc.setExpressionExperimentId( ee.getId() );
        eesc.setDescription( ee.getDescription() );
        eesc.setName( ee.getName() );
        eesc.setSearchString( "probe_0, probe_1, probe_2, probe_3, probe_4" );
        eesc.setStringency( 1 );

        return eesc;

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

        ExpressionExperimentSearchCommand eesc = ( ( ExpressionExperimentSearchCommand ) command );
        Long id = eesc.getExpressionExperimentId();

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Canceled" );

            if ( id != null ) {
                return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":"
                        + request.getServerPort() + request.getContextPath()
                        + "/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
            }

            log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":"
                    + request.getServerPort() + request.getContextPath()
                    + "/expressionExperiment/showAllExpressionExperiments.html" ) );

        }

        ExpressionExperiment expressionExperiment = this.expressionExperimentService.findById( id );

        if ( expressionExperiment == null ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No expression experiment with id " + id
                    + " found" ) );
        }

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expressionExperiment );

        log.debug( "Got " + arrayDesigns.size() + " array designs for the expression experiment with id " + id );

        // more searchString validation - see also validation.xml
        String searchString = ( ( ExpressionExperimentSearchCommand ) command ).getSearchString();
        log.debug( "Got search string " + searchString );
        String[] searchIds = StringUtils.split( searchString, "," );

        /* handle search by design element */
        if ( ( ( ExpressionExperimentSearchCommand ) command ).getSearchCriteria().equalsIgnoreCase( "probe set id" ) ) {
            compositeSequences = new ArrayList<DesignElement>();
            Collection<Gene> geneCol = null;
            for ( ArrayDesign design : arrayDesigns ) {

                for ( int i = 0; i < searchIds.length; i++ ) {
                    String searchId = StringUtils.trim( searchIds[i] );
                    log.debug( "searching for " + searchId );

                    CompositeSequence cs = compositeSequenceService.findByName( design, searchId );

                    if ( cs != null ) {
                        compositeSequences.add( cs );

                        /* get the genes associated with this design element */
                        geneCol = compositeSequenceService.getAssociatedGenes( cs );

                        log.debug( "geneCol " + geneCol );
                        if ( geneCol != null ) {
                            // FIXME For now, if geneCol is 0 I am still putting in map. Unnecessary and inefficient.
                            designElementToGeneMap.put( cs, geneCol );
                        }
                    }
                }
            }
            log.debug( compositeSequences.size() );
            if ( compositeSequences.size() == 0 ) {
                errors.addError( new ObjectError( command.toString(), null, null, "None of the probe sets exist." ) );
            }
        }
        /* handle search by gene */
        if ( ( ( ExpressionExperimentSearchCommand ) command ).getSearchCriteria().equalsIgnoreCase( "gene symbol" ) ) {
            // TODO add search by gene symbol
            errors.addError( new ObjectError( command.toString(), null, null,
                    "Search by gene symbol unsupported at this time." ) );
        }
        return super.processFormSubmission( request, response, command, errors );
    }

    /**
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
        ExpressionExperimentSearchCommand eesc = ( ( ExpressionExperimentSearchCommand ) command );
        String searchCriteria = ( ( ExpressionExperimentSearchCommand ) command ).getSearchCriteria();
        boolean suppressVisualizations = ( ( ExpressionExperimentSearchCommand ) command ).isSuppressVisualizations();

        File imageFile = File.createTempFile( request.getRemoteUser() + request.getSession( true ).getId()
                + RandomStringUtils.randomAlphabetic( 5 ), ".png", FileTools
                .createDir( "../webapps/ROOT/visualization/" ) );

        log.debug( "Image to be stored in " + imageFile.getAbsolutePath() );

        ExpressionDataMatrix expressionDataMatrix = null;
        MatrixVisualizer matrixVisualizer = null;
        if ( searchCriteria.equalsIgnoreCase( "probe set id" ) ) {
            ExpressionExperiment ee = expressionExperimentService.findById( eesc.getExpressionExperimentId() );
            expressionDataMatrix = new ExpressionDataMatrix( ee, compositeSequences );

            matrixVisualizer = new HttpExpressionDataMatrixVisualizer( expressionDataMatrix, "http", request
                    .getServerName(), request.getServerPort(), imageFile.getAbsolutePath() );
            matrixVisualizer.setSuppressVisualizations( suppressVisualizations );
        } else {
            log.debug( "search by official gene symbol" );
            // call service which produces expression data image based on gene symbol search criteria
        }

        log.debug( "here" );
        return new ModelAndView( getSuccessView() ).addObject( "matrixVisualizer", matrixVisualizer );
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        Collection<String> searchCategories = new HashSet<String>();
        searchCategories.add( "gene symbol" );
        searchCategories.add( "probe set id" );

        Map<String, Collection<String>> searchByMap = new HashMap<String, Collection<String>>();

        searchByMap.put( "searchCategories", searchCategories );
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
}
