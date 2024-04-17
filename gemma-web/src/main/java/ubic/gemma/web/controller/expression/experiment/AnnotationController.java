/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.basecode.ontology.model.OntologyProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.ParseSearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.util.EntityNotFoundException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller for methods involving annotation of experiments (and potentially other things); delegates to
 * OntologyService and the CharacteristicService. Edits to characteristics are handled by
 *
 * @author paul
 * @see ubic.gemma.web.controller.common.CharacteristicBrowserController for related methods.
 */
@SuppressWarnings("unused")
@Controller
public class AnnotationController {

    private static final Log log = LogFactory.getLog( AnnotationController.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private TaxonService taxonService;

    public Collection<OntologyTerm> getCategoryTerms() {
        return ontologyService.getCategoryTerms();
    }

    public Collection<OntologyProperty> getRelationTerms() {
        return ontologyService.getRelationTerms();
    }

    public void createBiomaterialTag( Characteristic vc, Long id ) {
        BioMaterial bm = bioMaterialService.loadOrFail( id,
                EntityNotFoundException::new, "No such BioMaterial with id=" + id );
        bm = bioMaterialService.thaw( bm );
        bioMaterialService.addCharacteristic( bm, vc );
    }

    /**
     * Ajax
     *
     * @param vc . If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param id of the expression experiment
     */
    public void createExperimentTag( Characteristic vc, Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, "No such experiment with id=" + id );
        if ( vc == null ) {
            throw new IllegalArgumentException( "Null characteristic" );
        }
        OntologyTerm term = ontologyService.getTerm( vc.getValueUri() );
        if ( vc.getValueUri() != null && term != null && term.isObsolete() ) {
            throw new IllegalArgumentException( vc + " is an obsolete term! Not saving." );
        }
        expressionExperimentService.addCharacteristic( ee, vc );
    }

    /**
     * AJAX. Find terms for tagging, etc.
     *
     * @param givenQueryString the query string
     * @param taxonId only used for genes, but generally this restriction is problematic for factorValues, which is an
     *                important use case.
     */
    public Collection<CharacteristicValueObject> findTerm( String givenQueryString, Long taxonId ) {
        if ( StringUtils.isBlank( givenQueryString ) ) {
            return new HashSet<>();
        }
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        try {
            Collection<CharacteristicValueObject> sortedResults = ontologyService.findTermsInexact( givenQueryString, taxon );
            /*
             * Populate the definition for the top hits.
             */
            int numfilled = 0;
            int maxfilled = 25; // presuming we don't need to look too far down the list ... just as a start.
            for ( CharacteristicValueObject cvo : sortedResults ) {
                cvo.setValueDefinition( cvo.getValueUri() != null ? ontologyService.getDefinition( cvo.getValueUri() ) : null );
                if ( ++numfilled > maxfilled ) {
                    break;
                }
            }

            return sortedResults;
        } catch ( ParseSearchException e ) {
            throw new IllegalArgumentException( e.getMessage(), e );
        } catch ( SearchException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * AJAX Note that this completely scraps the indices, and runs asynchronously.
     */
    public void reinitializeOntologyIndices() {
        if ( !SecurityUtil.isRunningAsAdmin() ) {
            log.warn( "Attempt to run ontology re-indexing as non-admin." );
            return;
        }
        ontologyService.reinitializeAndReindexAllOntologies();
    }

    public void removeBiomaterialTag( Characteristic vc, Long id ) {
        BioMaterial bm = bioMaterialService.loadOrFail( id, EntityNotFoundException::new, "No such BioMaterial with id=" + id );
        bm = bioMaterialService.thaw( bm );
        bioMaterialService.removeCharacteristic( bm, vc );
    }

    public void removeExperimentTag( Collection<Long> characterIds, Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            return;
        }

        ee = expressionExperimentService.thawLite( ee );

        Set<Characteristic> current = ee.getCharacteristics();

        Collection<Characteristic> found = new HashSet<>();

        for ( Characteristic characteristic : current ) {
            if ( characterIds.contains( characteristic.getId() ) )
                found.add( characteristic );

        }

        for ( Characteristic characteristic : found ) {
            log.info( "Removing characteristic  from " + ee + " : " + characteristic );
        }

        current.removeAll( found );
        ee.setCharacteristics( current );
        expressionExperimentService.update( ee );

        for ( Long id : characterIds ) {
            characteristicService.remove( id );
        }

    }

}
