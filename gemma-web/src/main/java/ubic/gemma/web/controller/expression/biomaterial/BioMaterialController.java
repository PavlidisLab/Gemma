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
package ubic.gemma.web.controller.expression.biomaterial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.ontology.OntologyResource;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.controller.expression.experiment.AnnotationValueObject;
import ubic.gemma.web.controller.expression.experiment.BioMaterialValueObject;
import ubic.gemma.web.controller.expression.experiment.FactorValueObject;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="bioMaterialController"
 * @spring.property name = "bioMaterialService" ref="bioMaterialService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="methodNameResolver" ref="bioMaterialActions"
 * @spring.property name="ontologyService" ref="ontologyService"
 * @spring.property name="factorValueService" ref="factorValueService"
 */

public class BioMaterialController extends BaseMultiActionController {

    private static Log log = LogFactory.getLog( BioMaterialController.class.getName() );

    private BioMaterialService bioMaterialService = null;
    private OntologyService ontologyService = null;

    private ExpressionExperimentService expressionExperimentService;
    private FactorValueService factorValueService;

    private boolean AJAX = true;

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param bioMaterialService
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "id" ) );

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a biomaterial id" );
        }

        BioMaterial bioMaterial = bioMaterialService.load( id );
        if ( bioMaterial == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.saveMessage( request, "biomaterial with id " + id + " found" );
        request.setAttribute( "id", id );
        ModelAndView mnv = new ModelAndView( "bioMaterial.detail" ).addObject( "bioMaterial", bioMaterial );

        return mnv;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    public ModelAndView annot( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "eeid" ) );

        Long id = Long.parseLong( request.getParameter( "eeid" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an expression experiment id" );
        }

        Collection<BioMaterial> bioMaterials = getBioMaterialsForEE( id );

        ModelAndView mav = new ModelAndView( "bioMaterialAnnotator" );
        if ( AJAX ) {
            StringBuilder buf = new StringBuilder();
            for ( BioMaterial bm : bioMaterials ) {
                buf.append( bm.getId() );
                buf.append( "," );
            }
            mav.addObject( "bioMaterialIdList", buf.toString().replaceAll( ",$", "" ) );
        }

        Long numBioMaterials = new Long( bioMaterials.size() );
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterials );
        return mav;
    }

    /**
     * @param id of experiment
     * @return
     */
    public Collection<BioMaterial> getBioMaterialsForEE( Long id ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( "Expression experiment with id=" + id + " not found" );
        }

        expressionExperimentService.thawLite( expressionExperiment );
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        Collection<BioMaterial> bioMaterials = new ArrayList<BioMaterial>();
        for ( BioAssay assay : bioAssays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials != null ) {
                bioMaterials.addAll( materials );
            }
        }
        return bioMaterials;
    }

    /**
     * @param eeId
     * @param factorId
     * @return A collection of BioMaterialValueObjects. These value objects are all the biomaterials for the given
     *         Expression Experiment. As a biomaterial can have many factor values for different factors the value
     *         object only contains the factor values for the specified factor
     */
    public Collection<BioMaterialValueObject> getBioMaterialsForEEWithFactor( EntityDelegator eeId,
            EntityDelegator factorId ) {

        ExpressionExperiment expressionExperiment = expressionExperimentService.load( eeId.getId() );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( "Expression experiment with id=" + eeId + " not found" );
        }

        expressionExperimentService.thawLite( expressionExperiment );
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        Collection<BioMaterialValueObject> bioMaterials = new ArrayList<BioMaterialValueObject>();

        for ( BioAssay assay : bioAssays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();

            if ( materials == null ) continue;

            for ( BioMaterial material : materials ) {
                BioMaterialValueObject bmvo = new BioMaterialValueObject( material );

                if ( material.getFactorValues() == null ) continue;

                for ( FactorValue value : material.getFactorValues() ) {
                    // If the factor value isn't the one we are looking for then add BMVO but don't fill in factor info.
                    if ( factorId.getId().compareTo( value.getExperimentalFactor().getId() ) != 0 ){
                        bmvo.setFactorValue( "None" );
                        bioMaterials.add(bmvo);
                        continue;
                    }
                    
                    String factorName = "";
                    if ( value.getCharacteristics().size() > 0 ) {
                        for ( Characteristic c : value.getCharacteristics() ) 
                            factorName += c.getValue();                        

                    } else 
                        factorName += value.getValue();
                    
                    bmvo.setFactorValue(factorName );
                    bioMaterials.add( bmvo );

                }

            }
         }

        return bioMaterials;

    }

    public Collection<BioMaterial> getBioMaterials( Collection<Long> ids ) {
        return bioMaterialService.loadMultiple( ids );
    }

    public Collection<AnnotationValueObject> getAnnotation( EntityDelegator bm ) {
        if ( bm == null || bm.getId() == null ) return null;
        BioMaterial bioM = bioMaterialService.load( bm.getId() );

        Collection<AnnotationValueObject> annotation = new ArrayList<AnnotationValueObject>();

        for ( Characteristic c : bioM.getCharacteristics() ) {
            AnnotationValueObject annotationValue = new AnnotationValueObject();
            annotationValue.setId( c.getId() );
            annotationValue.setClassName( c.getCategory() );
            annotationValue.setTermName( c.getValue() );
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                annotationValue.setClassUri( vc.getCategoryUri() );
                String className = getLabelFromUri( vc.getCategoryUri() );
                if ( className != null ) annotationValue.setClassName( className );
                annotationValue.setTermUri( vc.getValueUri() );
                String termName = getLabelFromUri( vc.getValueUri() );
                if ( termName != null ) annotationValue.setTermName( termName );
            }
            annotation.add( annotationValue );
        }
        return annotation;
    }

    public Collection<FactorValueObject> getFactorValues( EntityDelegator bm ) {

        if ( bm == null || bm.getId() == null ) return null;

        BioMaterial bioM = bioMaterialService.load( bm.getId() );

        Collection<FactorValueObject> results = new HashSet<FactorValueObject>();
        Collection<FactorValue> factorValues = bioM.getFactorValues();

        for ( FactorValue value : factorValues )
            results.add( new FactorValueObject( value ) );

        return results;

    }

    /**
     * @param bmIds
     * @param factorValueId given a collection of biomaterial ids, and a factor value id will add that factor value to
     *        all of the biomaterials in the collection. If the factor is already defined for one of the biomaterials
     *        will remove the previous one and add the new one.
     */
    public void addFactorValueTo( Collection<Long> bmIds, EntityDelegator factorValueId ) {

        Collection<BioMaterial> bms = this.getBioMaterials( bmIds );
        FactorValue factorVToAdd = factorValueService.load( factorValueId.getId() );
        ExperimentalFactor eFactor = factorVToAdd.getExperimentalFactor();

        for ( BioMaterial material : bms ) {
            Collection<FactorValue> oldValues = material.getFactorValues();
            Collection<FactorValue> updatedValues = new HashSet<FactorValue>();

            // Make sure that the BM doesn't have a FactorValue for the Factor we are adding already
            for ( FactorValue value : oldValues ) {
                if ( value.getExperimentalFactor() != eFactor ) updatedValues.add( value );
            }

            updatedValues.add( factorVToAdd );
            material.setFactorValues( updatedValues );
            bioMaterialService.update( material );
        }
    }

    private String getLabelFromUri( String uri ) {
        OntologyResource resource = ontologyService.getResource( uri );
        if ( resource != null )
            return resource.getLabel();
        else
            return null;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "bioMaterials" ).addObject( "bioMaterials", bioMaterialService.loadAll() );
    }

    /**
     * @param searchService the searchService to set
     */
    public void setOntologyService( OntologyService ontologyService ) {
        this.ontologyService = ontologyService;
    }

    /**
     * @param factorValueService the factorValueService to set
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

}
