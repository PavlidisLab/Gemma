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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.web.controller.WebConstants;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author keshav
 */
@SuppressWarnings("unused")
@Controller
@RequestMapping("/bioMaterial")
public class BioMaterialController {

    private static final Log log = LogFactory.getLog( BioMaterialController.class.getName() );

    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private FactorValueService factorValueService;

    private boolean AJAX = true;

    /**
     * AJAX
     *
     * @param factorValueId given a collection of biomaterial ids, and a factor value id will add that factor value to
     *                      all of the biomaterials in the collection. If the factor is already defined for one of the
     *                      biomaterials
     *                      will remove the previous one and add the new one.
     */
    public void addFactorValueTo( Collection<Long> bmIds, EntityDelegator<FactorValue> factorValueId ) {

        Collection<BioMaterial> bms = this.getBioMaterials( bmIds );
        FactorValue factorVToAdd = factorValueService.loadWithExperimentalFactorOrFail( factorValueId.getId() );
        ExperimentalFactor eFactor = factorVToAdd.getExperimentalFactor();

        for ( BioMaterial material : bms ) {
            Collection<FactorValue> oldValues = material.getFactorValues();
            Set<FactorValue> updatedValues = new HashSet<>();

            // Make sure that the BM doesn't have a FactorValue for the Factor
            // we are adding already
            for ( FactorValue value : oldValues ) {
                if ( value.getExperimentalFactor() != eFactor )
                    updatedValues.add( value );
            }

            updatedValues.add( factorVToAdd );
            material.setFactorValues( updatedValues );
            bioMaterialService.update( material );
        }
    }

    @RequestMapping("/annotate.html")
    public ModelAndView annot( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "eeid" ) );

        Long id = Long.parseLong( request.getParameter( "eeid" ) );

        Collection<BioMaterial> bioMaterials = getBioMaterialsForEE( id );

        ModelAndView mav = new ModelAndView( "bioMaterialAnnotator" );
        if ( AJAX ) {
            mav.addObject( "bioMaterialIdList", bioMaterialService.getBioMaterialIdList( bioMaterials ) );
        }

        Long numBioMaterials = ( long ) bioMaterials.size();
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterials );
        return mav;
    }

    public Collection<AnnotationValueObject> getAnnotation( EntityDelegator<BioMaterial> bm ) {
        if ( bm == null || bm.getId() == null )
            return null;
        BioMaterial bioM = bioMaterialService.loadOrFail( bm.getId() );

        Collection<AnnotationValueObject> annotation = new ArrayList<>();

        for ( Characteristic c : bioM.getCharacteristics() ) {
            AnnotationValueObject annotationValue = new AnnotationValueObject( c, BioMaterial.class );

            String className = getLabelFromUri( c.getCategoryUri() );
            if ( className != null )
                annotationValue.setClassName( className );

            String termName = getLabelFromUri( c.getValueUri() );
            if ( termName != null )
                annotationValue.setTermName( termName );

            annotation.add( annotationValue );
        }
        return annotation;
    }

    public Collection<BioMaterial> getBioMaterials( Collection<Long> ids ) {
        return bioMaterialService.load( ids );
    }

    /**
     * @param id of experiment
     */
    public Collection<BioMaterial> getBioMaterialsForEE( Long id ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( "Expression experiment with id=" + id + " not found" );
        }

        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        Collection<BioMaterial> bioMaterials = new ArrayList<>();
        for ( BioAssay assay : bioAssays ) {
            BioMaterial material = assay.getSampleUsed();
            if ( material != null ) {
                bioMaterials.add( material );
            }
        }
        return bioMaterials;
    }

    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator<BioMaterial> bm ) {

        if ( bm == null || bm.getId() == null )
            return null;

        BioMaterial bioM = bioMaterialService.loadOrFail( bm.getId() );
        bioM = bioMaterialService.thaw( bioM );
        Collection<FactorValueValueObject> results = new HashSet<>();
        Collection<FactorValue> factorValues = bioM.getFactorValues();

        for ( FactorValue value : factorValues )
            results.add( new FactorValueValueObject( value ) );

        return results;

    }

    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param factorValueService the factorValueService to set
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

    public void setOntologyService( OntologyService ontologyService ) {
        this.ontologyService = ontologyService;
    }

    @RequestMapping({ "/showBioMaterial.html", "/" })
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        Long id;

        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            String message = "Must provide a numeric biomaterial id";
            return new ModelAndView( WebConstants.HOME_PAGE ).addObject( "message", message );
        }

        BioMaterial bioMaterial = bioMaterialService.load( id );
        if ( bioMaterial == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }
        bioMaterial = bioMaterialService.thaw( bioMaterial );

        request.setAttribute( "id", id ); // / ??

        return new ModelAndView( "bioMaterial.detail" ).addObject( "bioMaterial", bioMaterial )
                .addObject( "expressionExperiment", bioMaterialService.getExpressionExperiment( id ) );
    }

    private String getLabelFromUri( String uri ) {
        if ( StringUtils.isBlank( uri ) ) return null;
        OntologyTerm resource = ontologyService.getTerm( uri );
        if ( resource != null )
            return resource.getLabel();

        return null;
    }

}
