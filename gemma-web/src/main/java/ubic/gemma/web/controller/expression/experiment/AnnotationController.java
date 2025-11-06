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
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.basecode.ontology.model.OntologyProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.ParseSearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.controller.util.EntityNotFoundException;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Controller for methods involving annotation of experiments (and potentially other things); delegates to
 * OntologyService and the CharacteristicService. Edits to characteristics are handled by
 *
 * @author paul
 * @see ubic.gemma.web.controller.common.description.CharacteristicBrowserController for related methods.
 */
@Controller
public class AnnotationController {

    private static final Log log = LogFactory.getLog( AnnotationController.class.getName() );

    private static final long FIND_TERM_TIMEOUT_MS = 30000L;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private TaxonService taxonService;

    /**
     * Obtain all category terms that can be used as characteristic categories.
     * <p>
     * AJAX
     */
    @SuppressWarnings("unused")
    public Collection<OntologyTerm> getCategoryTerms() {
        return ontologyService.getCategoryTerms();
    }

    /**
     * Obtain all relation terms that can be used as predicate.
     * <p>
     * AJAX
     */
    @SuppressWarnings("unused")
    public Collection<OntologyProperty> getRelationTerms() {
        return ontologyService.getRelationTerms();
    }

    /**
     * Find terms for tagging, etc.
     * <p>
     * AJAX
     *
     * @param query   the query string
     * @param taxonId only used for genes, but generally this restriction is problematic for factorValues, which is an
     *                important use case, ignored if null.
     */
    @SuppressWarnings("unused")
    public Collection<CharacteristicValueObject> findTerm( String query, @Nullable Long taxonId ) {
        StopWatch timer = StopWatch.createStarted();
        if ( StringUtils.isBlank( query ) ) {
            return Collections.emptySet();
        }
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.loadOrFail( taxonId, EntityNotFoundException::new );
        }
        try {
            Collection<CharacteristicValueObject> sortedResults = ontologyService.findTermsInexact( query, 5000, taxon, Math.max( FIND_TERM_TIMEOUT_MS - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
            /*
             * Populate the definition for the top hits.
             */
            int numfilled = 0;
            int maxfilled = 25; // presuming we don't need to look too far down the list ... just as a start.
            for ( CharacteristicValueObject cvo : sortedResults ) {
                cvo.setValueDefinition( cvo.getValueUri() != null ? ontologyService.getDefinition( cvo.getValueUri(), Math.max( FIND_TERM_TIMEOUT_MS - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ) : null );
                if ( ++numfilled > maxfilled ) {
                    break;
                }
            }

            return sortedResults;
        } catch ( TimeoutException e ) {
            log.error( "Search for " + query + ( taxon != null ? " in " + taxon : "" ) + " timed out, no results will be returned.", e );
            return Collections.emptySet();
        } catch ( ParseSearchException e ) {
            throw new IllegalArgumentException( e.getMessage(), e );
        } catch ( SearchException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * AJAX Note that this completely scraps the indices, and runs asynchronously.
     */
    @SuppressWarnings("unused")
    public void reinitializeOntologyIndices() {
        if ( !SecurityUtil.isRunningAsAdmin() ) {
            log.warn( "Attempt to run ontology re-indexing as non-admin." );
            return;
        }
        ontologyService.reinitializeAndReindexAllOntologies();
    }
}
