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
package ubic.gemma.web.controller.visualization;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
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
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.visualization.HttpExpressionDataMatrixVisualizer;
import ubic.gemma.web.controller.BaseFormController;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link viewAll} sets whether or not the entire data set will be viewed (with maximum 50 results displayed), and
 * {@link species} sets the type of species to search. {@link keywords} restrict the search.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentVisualizationFormController"
 * @spring.property name = "commandName" value="expressionExperimentVisualizationCommand"
 * @spring.property name = "commandClass"
 *                  value="ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationCommand"
 * @spring.property name = "formView" value="expressionExperimentVisualizationForm"
 * @spring.property name = "successView" value="showExpressionExperimentVisualization"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class ExpressionExperimentVisualizationFormController extends BaseFormController {
    private static Log log = LogFactory.getLog( ExpressionExperimentVisualizationFormController.class.getName() );

    private ExpressionExperimentService expressionExperimentService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private Map<DesignElement, Collection<Gene>> designElementToGeneMap = null;
    private List<DesignElement> compositeSequences = null;
    private QuantitationType quantitationType = null;
    private boolean viewAll = false;
    private final int MAX_ELEMENTS_TO_VISUALIZE = 50;

    public ExpressionExperimentVisualizationFormController() {
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
        ExpressionExperimentVisualizationCommand eesc = new ExpressionExperimentVisualizationCommand();

        if ( id != null && StringUtils.isNotBlank( id.toString() ) ) {
            ee = expressionExperimentService.findById( id );
        } else {
            ee = ExpressionExperiment.Factory.newInstance();
        }

        eesc.setExpressionExperimentId( ee.getId() );
        eesc.setDescription( ee.getDescription() );
        eesc.setName( ee.getName() );
        eesc.setSearchString( "0_at,1_at,2_at,3_at,4_at,5_at" );
        eesc.setSpecies( "Human" );
        eesc.setStandardQuantitationTypeName( StandardQuantitationType.DERIVEDSIGNAL.getValue() );

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

        ExpressionExperimentVisualizationCommand eesc = ( ( ExpressionExperimentVisualizationCommand ) command );
        Long id = eesc.getExpressionExperimentId();

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Cancelled" );

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

        compositeSequences = new ArrayList<DesignElement>();
        designElementToGeneMap = new HashMap<DesignElement, Collection<Gene>>();

        if ( expressionExperiment == null ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No expression experiment with id " + id
                    + " found" ) );
        }

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expressionExperiment );

        log.debug( "Got " + arrayDesigns.size() + " array designs for the expression experiment with id " + id );

        /* Get the selected standard quantitation type. */
        String standardQuantitationTypeName = eesc.getStandardQuantitationTypeName();
        quantitationType = QuantitationType.Factory.newInstance();
        StandardQuantitationType standardQuantitationType = null;
        if ( StandardQuantitationType.literals().contains( standardQuantitationTypeName ) ) {
            standardQuantitationType = StandardQuantitationType.fromString( standardQuantitationTypeName );
        } else {
            standardQuantitationType = StandardQuantitationType.OTHER;
            log.warn( "Invalid quantitation type.  Using " + standardQuantitationType + " instead." );
        }
        quantitationType.setType( standardQuantitationType );

        /* check to see if 'viewAll' is selected. */
        int size = expressionExperiment.getDesignElementDataVectors().size();
        String[] searchIds = new String[size];
        viewAll = ( ( ExpressionExperimentVisualizationCommand ) command ).isViewAll();
        if ( viewAll ) {/* check size if 'viewAll' is set. */
            if ( size > MAX_ELEMENTS_TO_VISUALIZE ) {
                size = MAX_ELEMENTS_TO_VISUALIZE;
            }
            int i = 0;
            Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();
            Iterator iter = vectors.iterator();
            while ( iter.hasNext() ) {
                DesignElementDataVector vector = ( DesignElementDataVector ) iter.next();
                searchIds[i] = vector.getDesignElement().getName();
                i++;
            }
        } else {/* if viewAll not selected, use search string */
            // more searchString validation - see also validation.xml
            String searchString = eesc.getSearchString();
            log.debug( "Got search string " + searchString );
            searchIds = StringUtils.split( searchString, "," );
            size = searchIds.length;
        }

        /* handle search by design element */
        if ( eesc.getSearchCriteria().equalsIgnoreCase( "probe set id" ) ) {
            Collection<Gene> geneCol = null;
            for ( ArrayDesign design : arrayDesigns ) {

                for ( int i = 0; i < size; i++ ) {
                    String searchId = StringUtils.trim( searchIds[i] );
                    log.debug( "searching for " + searchId );

                    CompositeSequence cs = compositeSequenceService.findByName( design, searchId );

                    if ( cs != null ) {
                        compositeSequences.add( cs );

                        /* get the genes associated with this design element (for display) */
                        geneCol = compositeSequenceService.getAssociatedGenes( cs );

                        log.debug( "geneCol " + geneCol );
                        if ( geneCol != null ) {
                            // FIXME For now, if geneCol is 0 I am still putting in map. Unnecessary and inefficient.
                            designElementToGeneMap.put( cs, geneCol );
                        }
                    }
                }
            }
            log.debug( "number of composite sequences: " + compositeSequences.size() );
            if ( compositeSequences.size() == 0 ) {
                errors.addError( new ObjectError( command.toString(), null, null, "None of the probe sets exist." ) );
            }
        }
        /* handle search by gene */
        if ( ( ( ExpressionExperimentVisualizationCommand ) command ).getSearchCriteria().equalsIgnoreCase(
                "gene symbol" ) ) {
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

        ExpressionExperimentVisualizationCommand eesc = ( ( ExpressionExperimentVisualizationCommand ) command );
        String searchCriteria = eesc.getSearchCriteria();

        // TODO remove this
        File imageFile = File.createTempFile( request.getRemoteUser() + request.getSession( true ).getId()
                + RandomStringUtils.randomAlphabetic( 5 ), ".png", FileTools
                .createDir( "../webapps/ROOT/visualization/" ) );

        // log.debug( "Image to be stored in " + imageFile.getAbsolutePath() );

        log.debug( "Quantitation Type: " + quantitationType.getType().getValue() );

        ExpressionDataMatrix expressionDataMatrix = null;

        HttpExpressionDataMatrixVisualizer httpExpressionDataMatrixVisualizer = null;

        if ( searchCriteria.equalsIgnoreCase( "probe set id" ) ) {
            ExpressionExperiment ee = expressionExperimentService.findById( eesc.getExpressionExperimentId() );

            expressionDataMatrix = new ExpressionDataDoubleMatrix( ee, compositeSequences, quantitationType );

            httpExpressionDataMatrixVisualizer = new HttpExpressionDataMatrixVisualizer( expressionDataMatrix, "http",
                    request.getServerName(), request.getServerPort(), imageFile.getAbsolutePath() );
        } else {
            log.debug( "search by official gene symbol" );
            // call service which produces expression data image based on gene symbol search criteria
        }

        /* deals with the case of probes that match, but not for the given quantitation type. */
        if ( expressionDataMatrix.getRowMap().size() == 0 && expressionDataMatrix.getColumnMap().size() == 0 ) {
            errors
                    .addError( new ObjectError( command.toString(), null, null,
                            "None of the probe sets match the given quantitation type "
                                    + quantitationType.getType().getValue() ) );
            return super.processFormSubmission( request, response, command, errors );
        }
        return new ModelAndView( getSuccessView() ).addObject( "httpExpressionDataMatrixVisualizer",
                httpExpressionDataMatrixVisualizer );
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {

        Map<String, Collection<String>> searchByMap = new HashMap<String, Collection<String>>();

        // add search categories
        Collection<String> searchCategories = new HashSet<String>();
        searchCategories.add( "gene symbol" );
        searchCategories.add( "probe set id" );

        searchByMap.put( "searchCategories", searchCategories );

        // add species
        Collection<String> speciesCategories = new HashSet<String>();
        speciesCategories.add( "Human" );
        speciesCategories.add( "Mouse" );
        speciesCategories.add( "Rat" );

        searchByMap.put( "speciesCategories", speciesCategories );

        // add standard quantitation types to select from
        Collection<String> standardQuantitationTypeNames = StandardQuantitationType.literals();
        for ( String name : standardQuantitationTypeNames ) {
            log.warn( name );
        }

        searchByMap.put( "standardQuantitationTypeNames", standardQuantitationTypeNames );

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