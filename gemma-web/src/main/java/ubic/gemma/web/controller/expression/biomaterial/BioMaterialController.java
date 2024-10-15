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
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author keshav
 */
@Controller
@RequestMapping("/bioMaterial")
public class BioMaterialController {

    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private FactorValueService factorValueService;

    /**
     * AJAX
     *
     * @param factorValueId given a collection of biomaterial ids, and a factor value id will add that factor value to
     *                      all of the biomaterials in the collection. If the factor is already defined for one of the
     *                      biomaterials
     *                      will remove the previous one and add the new one.
     */
    @SuppressWarnings("unused")
    public void addFactorValueTo( Collection<Long> bmIds, EntityDelegator<FactorValue> factorValueId ) {
        Collection<BioMaterial> bms = bioMaterialService.load( bmIds );
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

    @RequestMapping(value = "/annotate.html", method = RequestMethod.GET)
    public ModelAndView annot( @RequestParam("eeid") Long id ) {
        Collection<BioMaterial> bioMaterials = getBioMaterialsForEE( id );
        ModelAndView mav = new ModelAndView( "bioMaterialAnnotator" );
        mav.addObject( "bioMaterialIdList", bioMaterialService.getBioMaterialIdList( bioMaterials ) );
        Long numBioMaterials = ( long ) bioMaterials.size();
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterials );
        return mav;
    }

    @SuppressWarnings("unused")
    public Collection<AnnotationValueObject> getAnnotation( EntityDelegator<BioMaterial> bm ) throws TimeoutException {
        if ( bm == null || bm.getId() == null )
            return null;
        BioMaterial bioM = bioMaterialService.loadOrFail( bm.getId() );

        Collection<AnnotationValueObject> annotation = new ArrayList<>();

        long timeoutMs = 30000;
        StopWatch timer = StopWatch.createStarted();
        for ( Characteristic c : bioM.getCharacteristics() ) {
            AnnotationValueObject annotationValue = new AnnotationValueObject( c, BioMaterial.class );

            String className = getLabelFromUri( c.getCategoryUri(), Math.max( timeoutMs - timer.getTime(), 0 ) );
            if ( className != null )
                annotationValue.setClassName( className );

            String termName = getLabelFromUri( c.getValueUri(), Math.max( timeoutMs - timer.getTime(), 0 ) );
            if ( termName != null )
                annotationValue.setTermName( termName );

            annotation.add( annotationValue );
        }
        return annotation;
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator<BioMaterial> bm ) {
        if ( bm == null || bm.getId() == null )
            return null;

        BioMaterial bioM = bioMaterialService.loadOrFail( bm.getId() );
        bioM = bioMaterialService.thaw( bioM );
        Collection<FactorValueValueObject> results = new HashSet<>();
        // TODO: include inherited factor values (but the UI is not ready yet for that)
        Collection<FactorValue> factorValues = bioM.getFactorValues();

        for ( FactorValue value : factorValues )
            results.add( new FactorValueValueObject( value ) );

        return results;

    }

    @RequestMapping(value = { "/showBioMaterial.html", "/" }, method = RequestMethod.GET)
    public ModelAndView show( @RequestParam("id") Long id ) {
        BioMaterial bioMaterial = bioMaterialService.loadOrFail( id, EntityNotFoundException::new );
        bioMaterial = bioMaterialService.thaw( bioMaterial );
        return new ModelAndView( "bioMaterial.detail" ).addObject( "bioMaterial", bioMaterial )
                .addObject( "expressionExperiment", bioMaterialService.getExpressionExperiment( id ) );
    }

    private String getLabelFromUri( String uri, long timeoutMs ) throws TimeoutException {
        if ( StringUtils.isBlank( uri ) ) return null;
        OntologyTerm resource = ontologyService.getTerm( uri, timeoutMs, TimeUnit.MILLISECONDS );
        if ( resource != null )
            return resource.getLabel();
        return null;
    }
}

