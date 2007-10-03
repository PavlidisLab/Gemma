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

import ubic.gemma.model.common.description.Characteristic;
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
    private ExperimentalFactorService experimentalFactorService = null;
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

        // TODO this should be in the experimentalFactorService, if its too slow we might have to do this with a
        // hibernate query.
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
                Collection<FactorValue> fvs = factor.getFactorValues();
                factor.setFactorValues( new HashSet<FactorValue>() );                
                for ( FactorValue fv : fvs ) {
                    this.factorValueService.delete( fv );
                }
                this.experimentalFactorService.delete( factor );
                continue;
            }
            factorsToKeep.add( factor );

        }

        ed.setExperimentalFactors( factorsToKeep );
        experimentalDesignService.update( ed );

    }

    /**
     * @param a collection of factorValueIDs
     * @param efID id of the experimental factor
     * @param eeID expresion experiment ID
     * 
     * 
     * Deletes the given factorValue.  Removes associations with BioMaterials. Updates the Factor. 
     */
    public void deleteFactorValue( Collection<Long> factorValueIds, EntityDelegator efID, EntityDelegator eeID) {

        ExpressionExperiment ee = this.expressionExperimentService.load( eeID.getId() );
        ExperimentalFactor ef  = this.experimentalFactorService.load( efID.getId() );   

        //Remove assocations of factorvalues with biomaterials.
        //TODO: refactor (check out deleteFactor)
        for ( BioAssay assay : ee.getBioAssays() ) {
            for ( BioMaterial bm : assay.getSamplesUsed() ) {

                Collection<FactorValue> removeFactorValues = new HashSet<FactorValue>();
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( factorValueIds.contains( fv.getId() ) ) removeFactorValues.add( fv );

                }
                bm.getFactorValues().removeAll( removeFactorValues );
                bioMaterialService.update( bm );

            }
        }

        //Remove assocations between factor and factorValue. delete factorValue      
        for ( Long fvId : factorValueIds ) {
            FactorValue fv2Delete = this.factorValueService.load( fvId );
            ef.getFactorValues().remove( fv2Delete); 
            factorValueService.delete( fv2Delete );            
        }
        
        //Update the experimental factor. 
        this.experimentalFactorService.update( ef );
        
       
    }

    /**
     * @param factorID
     * @param factorValues
     * 
     * Creates a new factor value associated with the given factor
     * 
     */
    public void createNewFactorValue( EntityDelegator factorID, Collection<Characteristic> factorValues ) {

        ExperimentalFactor ef = this.experimentalFactorService.load( factorID.getId() );
        FactorValue newFV = FactorValue.Factory.newInstance( ef );

        //This is a hack.  DWR passes back arraylist which hibernate won't persist!
        Collection<Characteristic> fvs = new HashSet<Characteristic>(factorValues); 
        newFV.setCharacteristics( fvs );
        newFV = this.factorValueService.create( newFV );
        ef.getFactorValues().add( newFV );
        this.experimentalFactorService.update( ef );

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
