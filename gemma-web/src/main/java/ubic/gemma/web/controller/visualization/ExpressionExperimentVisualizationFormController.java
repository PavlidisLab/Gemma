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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.genome.CompositeSequenceGeneMapperService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.propertyeditor.QuantitationTypePropertyEditor;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link viewSampling} sets whether or not just some randomly selected vectors will be shown, and {@link species} sets
 * the type of species to search. {@link keywords} restrict the search.
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
 * @spring.property name = "designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name = "compositeSequenceGeneMapperService" ref="compositeSequenceGeneMapperService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class ExpressionExperimentVisualizationFormController extends BaseFormController {

    private static Log log = LogFactory.getLog( ExpressionExperimentVisualizationFormController.class.getName() );

    public static final String SEARCH_BY_PROBE = "probe set id";
    public static final String SEARCH_BY_GENE = "gene symbol";

    private ExpressionExperimentService expressionExperimentService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private DesignElementDataVectorService designElementDataVectorService;
    private CompositeSequenceGeneMapperService compositeSequenceGeneMapperService = null;
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
        eesc.setName( ee.getName() );
        eesc.setSearchString( "probeset_0,probeset_1,probeset_2,probeset_3,probeset_4,probeset_5" );
        return eesc;

    }

    /**
     * 
     */
    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {

        super.initBinder( request, binder );
        binder.registerCustomEditor( QuantitationType.class, new QuantitationTypePropertyEditor(
                getQuantitationTypes( request ) ) );
    }

    /**
     * @param request
     * @return Map
     */
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        log.debug( "entering referenceData" );

        Map<String, List<? extends Object>> searchByMap = new HashMap<String, List<? extends Object>>();
        List<String> searchCategories = new ArrayList<String>();
        searchCategories.add( SEARCH_BY_GENE );
        searchCategories.add( SEARCH_BY_PROBE );
        searchByMap.put( "searchCategories", searchCategories );

        Collection<QuantitationType> types = getQuantitationTypes( request );
        List<QuantitationType> listedTypes = new ArrayList<QuantitationType>();
        listedTypes.addAll( types );

        searchByMap.put( "quantitationTypes", listedTypes );

        return searchByMap;
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

        Map<String, Object> model = new HashMap<String, Object>();
        ExpressionExperimentVisualizationCommand eesc = ( ( ExpressionExperimentVisualizationCommand ) command );
        String searchCriteria = eesc.getSearchCriteria();
        Long id = eesc.getExpressionExperimentId();

        ExpressionExperiment expressionExperiment = this.expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            return processErrors( request, response, command, errors, "No expression experiment with id " + id
                    + " found" );
        }

        QuantitationType quantitationType = eesc.getQuantitationType();
        if ( quantitationType == null ) {
            return processErrors( request, response, command, errors, "Quantitation type must be provided" );
        }

        Collection<DesignElementDataVector> dataVectors = getVectors( command, errors, eesc, expressionExperiment,
                quantitationType );

        if ( errors.hasErrors() ) {
            return processErrors( request, response, command, errors, null );
        }

        designElementDataVectorService.thaw( dataVectors );
        ExpressionDataMatrix expressionDataMatrix = new ExpressionDataDoubleMatrix( dataVectors, quantitationType );
        /* deals with the case where probes don't match for the given quantitation type. */
        if ( expressionDataMatrix.getRowElements().size() == 0 ) {
            String message = "None of the probe sets match the given quantitation type "
                    + quantitationType.getType().getValue();

            return processErrors( request, response, command, errors, message );
        }
        
        ExpressionExperimentVisualizationCommand eevc = ( ExpressionExperimentVisualizationCommand ) command;
        
        ModelAndView mav = new ModelAndView( getSuccessView() );
        mav.addObject( "expressionDataMatrix", expressionDataMatrix );
        // add in information about the query
        mav.addObject( "expressionExperiment", expressionExperiment );
        mav.addObject( "quantitationType", eevc.getQuantitationType().getName() );
        mav.addObject( "searchCriteria", eevc.getSearchCriteria() );
        mav.addObject( "searchCriteriaValue", eevc.getSearchString() );
        return mav;
    }

    /**
     * @param command
     * @param errors
     * @param eesc
     * @param expressionExperiment
     * @param quantitationType
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<DesignElementDataVector> getVectors( Object command, BindException errors,
            ExpressionExperimentVisualizationCommand eesc, ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType ) {

        Collection<DesignElementDataVector> vectors = null;

        String[] searchIds = new String[MAX_ELEMENTS_TO_VISUALIZE];

        Collection<CompositeSequence> compositeSequences = null;

        boolean viewSampling = ( ( ExpressionExperimentVisualizationCommand ) command ).isViewSampling();

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expressionExperiment );

        String searchString = eesc.getSearchString();

        searchIds = StringUtils.split( searchString, "," );

        List searchIdsAsList = Arrays.asList( searchIds );

        /* check size if 'viewSampling' is set. */
        if ( viewSampling ) {
            vectors = expressionExperimentService.getSamplingOfVectors( expressionExperiment, quantitationType,
                    MAX_ELEMENTS_TO_VISUALIZE );
        }

        /* handle search by design element */
        else if ( eesc.getSearchCriteria().equalsIgnoreCase( SEARCH_BY_PROBE ) ) {

            if ( arrayDesigns.size() == 0 ) {
                String message = "No array designs found for " + expressionExperiment;
                log.error( message );
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
                return null;
            }

            compositeSequences = compositeSequenceService.findByNamesInArrayDesigns( searchIdsAsList, arrayDesigns );

            if ( compositeSequences.size() == 0 ) {
                String message = "No probes could be found matching the query.";
                log.error( message );
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
                return null;
            }

            vectors = expressionExperimentService.getDesignElementDataVectors( expressionExperiment,
                    compositeSequences, quantitationType );

        }

        /* handle search by gene */
        else if ( eesc.getSearchCriteria().equalsIgnoreCase( SEARCH_BY_GENE ) ) {

            /* comment me out to add this gene search functionality. */
            // errors.addError( new ObjectError( command.toString(), null, null,
            // "Search by gene symbol unsupported at this time." ) );
            // if ( errors.getErrorCount() > 0 ) return null;
            /* end */

            if ( arrayDesigns.size() == 0 ) {
                String message = "No array designs found for " + expressionExperiment;
                log.error( message );
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
                return null;
            }

            Map<Gene, Collection<CompositeSequence>> compositeSequencesForGene = compositeSequenceGeneMapperService
                    .getCompositeSequencesForGenesByOfficialSymbols( searchIdsAsList );

            Collection<Gene> geneKeySet = compositeSequencesForGene.keySet();

            for ( Gene g : geneKeySet ) {
                compositeSequences = compositeSequencesForGene.get( g );
                log.debug( "gene official symbol: " + g.getOfficialSymbol() + " has " + compositeSequences.size()
                        + " composite sequences associated with it." );
            }

            vectors = expressionExperimentService.getDesignElementDataVectors( expressionExperiment,
                    compositeSequences, quantitationType );

        }
        if ( vectors == null || vectors.size() == 0 ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No data could be found." ) );
        }
        return vectors;
    }

    /**
     * @param request
     * @return Collection<QuantitationType>
     */
    @SuppressWarnings("unchecked")
    private Collection<QuantitationType> getQuantitationTypes( HttpServletRequest request ) {
        Long id = null;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Id was not valid Long integer", e );
        }
        ExpressionExperiment expressionExperiment = this.expressionExperimentService.findById( id );
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( expressionExperiment );
        return types;
    }

    /**
     * New errors are added if <tt>message</tt> is not empty (as per the definition of
     * {@link org.apache.commons.lang.StringUtils#isEmpty}. If empty, a new error will not be added, but existing
     * errors will still be processed.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @param message - The error message to be displayed.
     * @return ModelAndView
     * @throws Exception
     */
    private ModelAndView processErrors( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors, String message ) throws Exception {
        if ( !StringUtils.isEmpty( message ) ) {
            log.error( message );
            errors.addError( new ObjectError( command.toString(), null, null, message ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param designElementDataVectorService
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param compositeSequenceGeneMapperService The compositeSequenceGeneMapperService to set.
     */
    public void setCompositeSequenceGeneMapperService(
            CompositeSequenceGeneMapperService compositeSequenceGeneMapperService ) {
        this.compositeSequenceGeneMapperService = compositeSequenceGeneMapperService;
    }
}