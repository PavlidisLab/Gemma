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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "expressionExperimentSubSetService" ref="expressionExperimentSubSetService"
 * @spring.property name="methodNameResolver" ref="expressionExperimentActions"
 */
public class ExpressionExperimentController extends BaseMultiActionController {

    private ExpressionExperimentService expressionExperimentService = null;
    private ExpressionExperimentSubSetService expressionExperimentSubSetService = null;

    private final String messagePrefix = "Expression experiment with id";
    private final String identifierNotFound = "Must provide a valid ExpressionExperiment identifier";

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param expressionExperimentSubSetService
     */
    public void setExpressionExperimentSubSetService(
            ExpressionExperimentSubSetService expressionExperimentSubSetService ) {
        this.expressionExperimentSubSetService = expressionExperimentSubSetService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.addMessage( request, "object.found", new Object[] { messagePrefix, id } );
        request.setAttribute( "id", id );
        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" ).addObject( "expressionExperiment",
                expressionExperiment );

        Set s = expressionExperimentService.getQuantitationTypeCountById( id ).entrySet();
        mav.addObject( "qtCountSet", expressionExperimentService.getQuantitationTypeCountById( id ).entrySet() );

        // add arrayDesigns used, by name
        Collection<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        for ( BioAssay assay : bioAssays ) {
            ArrayDesign design = assay.getArrayDesignUsed();
            if ( !arrayDesigns.contains( design ) ) {
                arrayDesigns.add( design );
            }
        }
        ExpressionExperimentValueObject vo;
        mav.addObject( "arrayDesigns", arrayDesigns );
        long num = expressionExperimentService.getDesignElementDataVectorCountById( id );
        // add count of designElementDataVectors
        mav.addObject( "designElementDataVectorCount", new Long( expressionExperimentService
                .getDesignElementDataVectorCountById( id ) ) );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showBioAssays( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        Map m = expressionExperimentService.getQuantitationTypeCountById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", expressionExperiment.getBioAssays() );
    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showSubSet( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperimentSubSet subset = expressionExperimentSubSetService.load( id );
        if ( subset == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        // request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", subset.getBioAssays() );
    }

    /**
     * Shows a bioassay view of a single expression experiment subset.
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showExpressionExperimentSubSet( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", expressionExperiment.getBioAssays() );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        
        String sId = request.getParameter( "id" );
        Collection<ExpressionExperimentValueObject> expressionExperiments = new HashSet<ExpressionExperimentValueObject>();        
        // if no IDs are specified, then load all expressionExperiments
        if ( sId == null ) {
            Collection<ExpressionExperiment> expressionExperimentCol = expressionExperimentService.loadAll();
            for ( ExpressionExperiment experiment : expressionExperimentCol ) {
                expressionExperiments.add( expressionExperimentService.toExpressionExperimentValueObject( experiment ) );               
            }
        }
       
        // if ids are specified, then display only those expressionExperiments
        else {
            String[] idList = StringUtils.split( sId, ',' );

        for (int i = 0; i < idList.length; i++) {
            Long id = Long.parseLong( idList[i] );
            ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
            if ( expressionExperiment == null ) {
                throw new EntityNotFoundException( id + " not found" );
            }
            expressionExperiments.add( expressionExperimentService.toExpressionExperimentValueObject( expressionExperiment ) );
        }
        }
        return new ModelAndView( "expressionExperiments" ).addObject( "expressionExperiments",
                expressionExperiments );        
        

    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( expressionExperiment + " not found" );
        }

        return doDelete( request, response, expressionExperiment );
    }

    /**
     * @param request
     * @param expressionExperiment
     * @return ModelAndView
     */
    private ModelAndView doDelete( HttpServletRequest request, HttpServletResponse response, ExpressionExperiment expressionExperiment ) {
        addMessage( request, "object.deleted", new Object[] { messagePrefix, expressionExperiment.getId() } );
        expressionExperimentService.delete( expressionExperiment );
        expressionExperiment = null;
        
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ));
    }
    
}