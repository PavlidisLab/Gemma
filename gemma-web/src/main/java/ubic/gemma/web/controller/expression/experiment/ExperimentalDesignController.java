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

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="experimentalDesignController"
 * @spring.property name = "experimentalDesignService" ref="experimentalDesignService"
 * @spring.property name="methodNameResolver" ref="experimentalDesignActions"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "bioMaterialService" ref="bioMaterialService"
 * @spring.property name = "experimentalFactorService" ref="experimentalFactorService"
 * @spring.property name = "factorValueService" ref="factorValueService"
 */
public class ExperimentalDesignController extends BaseMultiActionController {

    private ExperimentalDesignService experimentalDesignService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private BioMaterialService bioMaterialService = null;
    private ExperimentalFactorService experimentalFactorService =  null;
    private FactorValueService factorValueService = null;

    private final String messagePrefix = "ExperimenalDesign with id ";
    private final String identifierNotFound = "Must provide a valid ExperimentalDesign identifier";


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

        ExperimentalDesign experimentalDesign = experimentalDesignService.load( id );
        if ( experimentalDesign == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( experimentalDesign );

        request.setAttribute( "id", id );

        ModelAndView mnv = new ModelAndView( "experimentalDesign.detail" );
        mnv.addObject( "experimentalDesign", experimentalDesign );
        mnv.addObject( "expressionExperiment", ee );

        return mnv;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "experimentalDesigns" ).addObject( "experimentalDesigns", experimentalDesignService
                .loadAll() );
    }

    /**
     * Will persist the give vocab characteristic to each expression experiment id supplied in the list
     * 
     * @param newFactor to create
     * @param edId the id of the Experimental design to add the factor to
     */
    public void createNewFactor( FactorValueObject newFactor, EntityDelegator edId ) {

        ExperimentalDesign ed = this.experimentalDesignService.load( edId.getId() );

        ExperimentalFactor createdFactor = ExperimentalFactor.Factory.newInstance();
        createdFactor.setExperimentalDesign( ed );
        createdFactor.setCategory( newFactor.getCategoryCharacteritic() );
        createdFactor.setName( newFactor.getCategoryCharacteritic().getCategory() );
        createdFactor.setDescription( newFactor.getDescription() );

        Collection<ExperimentalFactor> current = ed.getExperimentalFactors();
        if ( current == null ) current = new HashSet<ExperimentalFactor>();

        current.add( createdFactor );

        ed.setExperimentalFactors( current );
        this.experimentalDesignService.update( ed );

    }

    /**
     * @param factorIds
     * @param eeId Removes the selected factors from the expression experiment. also removes the associated factor
     *        values.
     */
    public void deleteFactor( Collection<Long> factorIds, EntityDelegator eeId ) {

        //TODO this should be in the experimentalFactorService, if its too slow we might have to do this with a hibernate query. 
        
        // remove relevent factor values from bio-materials
        ExpressionExperiment ee = this.expressionExperimentService.load( eeId.getId() );

        for ( BioAssay assay : ee.getBioAssays() ) {
            for ( BioMaterial bm : assay.getSamplesUsed() ) {

                Collection<FactorValue> removeFactorValues = new HashSet<FactorValue>();
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( factorIds.contains( fv.getExperimentalFactor().getId() ) ) removeFactorValues.add( fv );

                }
                bm.getFactorValues().removeAll( removeFactorValues );
                bioMaterialService.update( bm );

            }
        }

        // remove factor and factor values from factor
        ExperimentalDesign ed = ee.getExperimentalDesign();
        Collection<ExperimentalFactor> oldExperimentalFactors = ed.getExperimentalFactors();
        ed.setExperimentalFactors( new HashSet<ExperimentalFactor>() );      
        Collection<ExperimentalFactor> factorsToKeep = new HashSet<ExperimentalFactor>();
        
        for ( ExperimentalFactor factor : oldExperimentalFactors ) {
            if ( factorIds.contains( factor.getId() ) ) {
                for (FactorValue fv : factor.getFactorValues()){
                    this.factorValueService.delete( fv );
                }                
                factor.setFactorValues( new HashSet<FactorValue>() );  //necessary?                
                this.experimentalFactorService.delete( factor );
                continue;
            }
            factorsToKeep.add( factor );
            
        }
        
        ed.setExperimentalFactors(factorsToKeep );
        experimentalDesignService.update( ed );
        
        
       

    }

    /**
     * TODO add delete to the model
     * 
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView delete(HttpServletRequest request,
    // HttpServletResponse response) {
    // String name = request.getParameter("name");
    //
    // if (name == null) {
    // // should be a validation error.
    // throw new EntityNotFoundException("Must provide a name");
    // }
    //
    // ExperimentalDesign experimentalDesign = experimentalDesignService
    // .findByName(name);
    // if (experimentalDesign == null) {
    // throw new EntityNotFoundException(experimentalDesign
    // + " not found");
    // }
    //
    // return doDelete(request, experimentalDesign);
    // }
    /**
     * TODO add doDelete to the model
     * 
     * @param request
     * @param experimentalDesign
     * @return
     */
    // private ModelAndView doDelete(HttpServletRequest request,
    // ExperimentalDesign experimentalDesign) {
    // experimentalDesignService.delete(experimentalDesign);
    // log.info("Expression Experiment with name: "
    // + experimentalDesign.getName() + " deleted");
    // addMessage(request, "experimentalDesign.deleted",
    // new Object[] { experimentalDesign.getName() });
    // return new ModelAndView("experimentalDesigns",
    // "experimentalDesign", experimentalDesign);
    // }
    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param experimentalFactorService the experimentalFactorService to set
     */
    public void setExperimentalFactorService( ExperimentalFactorService experimentalFactorService ) {
        this.experimentalFactorService = experimentalFactorService;
    }

    /**
     * @param factorValueService the factorValueService to set
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }
    
    /**
     * @param experimentalDesignService
     */
    public void setExperimentalDesignService( ExperimentalDesignService experimentalDesignService ) {
        this.experimentalDesignService = experimentalDesignService;
    }
    
}
