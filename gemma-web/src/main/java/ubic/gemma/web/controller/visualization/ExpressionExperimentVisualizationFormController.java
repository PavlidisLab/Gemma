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
import ubic.gemma.visualization.DefaultExpressionDataMatrixVisualizer;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizer;
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

    private boolean viewSampling = false;
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
        eesc.setSearchString( "probeset_0,probeset_1,probeset_2,probeset_3,probeset_4,probeset_5" );
        eesc.setStandardQuantitationTypeName( StandardQuantitationType.DERIVEDSIGNAL.getValue() );

        return eesc;

    }

    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        ExpressionExperimentVisualizationCommand eesc = ( ( ExpressionExperimentVisualizationCommand ) command );
        Long id = eesc.getExpressionExperimentId();

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Cancelled" );

            if ( id != null ) {
                return new ModelAndView( new RedirectView( "/expressionExperiment/showExpressionExperiment.html?id="
                        + id ) );
            }

            log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView( new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html" ) );

        }

        return super.processFormSubmission( request, response, command, errors );
    }

    @SuppressWarnings("unchecked")
    private QuantitationType getQuantitationType( ExpressionExperimentVisualizationCommand eesc,
            ExpressionExperiment expressionExperiment ) {
        QuantitationType quantitationType = null;
        /* Get the selected standard quantitation type. */
        String standardQuantitationTypeName = eesc.getStandardQuantitationTypeName();
        QuantitationType requestedType = QuantitationType.Factory.newInstance();
        StandardQuantitationType standardQuantitationType = null;
        if ( StandardQuantitationType.literals().contains( standardQuantitationTypeName ) ) {
            standardQuantitationType = StandardQuantitationType.fromString( standardQuantitationTypeName );
        } else {
            standardQuantitationType = StandardQuantitationType.OTHER;
            log.warn( "Invalid quantitation type.  Using " + standardQuantitationType + " instead." );
        }
        requestedType.setType( standardQuantitationType );

        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( expressionExperiment );
        for ( QuantitationType type : types ) {
            if ( type.equals( requestedType ) ) {
                quantitationType = type;
            }
        }
        return quantitationType;
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings( { "unused", "unchecked" })
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        ExpressionExperimentVisualizationCommand eesc = ( ( ExpressionExperimentVisualizationCommand ) command );
        String searchCriteria = eesc.getSearchCriteria();

        Long id = eesc.getExpressionExperimentId();

        ExpressionExperiment expressionExperiment = this.expressionExperimentService.findById( id );
        List<DesignElement> compositeSequences = new ArrayList<DesignElement>();

        if ( expressionExperiment == null ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No expression experiment with id " + id
                    + " found" ) );
        }

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expressionExperiment );

        log.debug( "Got " + arrayDesigns.size() + " array designs for the expression experiment with id " + id );

        QuantitationType quantitationType = getQuantitationType( eesc, expressionExperiment );

        if ( quantitationType == null ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No quantitation type matching "
                    + eesc.getStandardQuantitationTypeName() + " found" ) );
        }

        String[] searchIds = new String[MAX_ELEMENTS_TO_VISUALIZE];
        viewSampling = ( ( ExpressionExperimentVisualizationCommand ) command ).isViewSampling();
        if ( viewSampling ) {/* check size if 'viewAll' is set. */
            int i = 0;
            Collection<DesignElementDataVector> vectors = expressionExperimentService.getSamplingOfVectors(
                    expressionExperiment, quantitationType, MAX_ELEMENTS_TO_VISUALIZE );
            for ( DesignElementDataVector vector : vectors ) {
                searchIds[i] = vector.getDesignElement().getName();
                i++;
            }
        } else {/* if viewAll not selected, use search string */
            // more searchString validation - see also validation.xml
            String searchString = eesc.getSearchString();
            log.debug( "Got search string " + searchString );
            searchIds = StringUtils.split( searchString, "," );
        }

        /* handle search by design element */
        if ( eesc.getSearchCriteria().equalsIgnoreCase( "probe set id" ) ) {
            for ( ArrayDesign design : arrayDesigns ) {
                for ( String searchId : searchIds ) {
                    searchId = StringUtils.trim( searchId );
                    log.debug( "searching for " + searchId );

                    CompositeSequence cs = compositeSequenceService.findByName( design, searchId );

                    if ( cs != null ) {
                        compositeSequences.add( cs );
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

        // TODO remove this
        File imageFile = File.createTempFile( request.getRemoteUser() + request.getSession( true ).getId()
                + RandomStringUtils.randomAlphabetic( 5 ), ".png", FileTools
                .createDir( "../webapps/ROOT/visualization/" ) );

        // log.debug( "Image to be stored in " + imageFile.getAbsolutePath() );

        log.debug( "Quantitation Type: " + quantitationType.getType().getValue() );

        ExpressionDataMatrix expressionDataMatrix = null;

        ExpressionDataMatrixVisualizer expressionDataMatrixVisualizer = null;

        if ( searchCriteria.equalsIgnoreCase( "probe set id" ) ) {
            ExpressionExperiment ee = expressionExperimentService.findById( eesc.getExpressionExperimentId() );

            expressionDataMatrix = new ExpressionDataDoubleMatrix( ee, compositeSequences, quantitationType );

            expressionDataMatrixVisualizer = new DefaultExpressionDataMatrixVisualizer( expressionDataMatrix, imageFile
                    .getAbsolutePath() );
        } else {
            log.debug( "search by official gene symbol" );
            // FIXME call service which produces expression data image based on gene symbol search criteria
            throw new UnsupportedOperationException( "Search by Gene Symbol is not supported yet" );
        }

        /* deals with the case of probes don't match, for the given quantitation type. */
        if ( expressionDataMatrix.getRowMap().size() == 0 && expressionDataMatrix.getColumnMap().size() == 0 ) {
            errors
                    .addError( new ObjectError( command.toString(), null, null,
                            "None of the probe sets match the given quantitation type "
                                    + quantitationType.getType().getValue() ) );
            return super.processFormSubmission( request, response, command, errors );
        }
        return new ModelAndView( getSuccessView() ).addObject( "expressionDataMatrixVisualizer",
                expressionDataMatrixVisualizer );
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
        searchCategories.add( "probe id" );
        searchByMap.put( "searchCategories", searchCategories );

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