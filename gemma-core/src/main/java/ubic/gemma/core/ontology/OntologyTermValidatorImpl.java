/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.core.ontology;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.ols.OlsTerm;
import ubic.gemma.core.ontology.ols.OlsTermResolver;
import ubic.gemma.core.ontology.ols.OlsUnavailableException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Gene;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Default {@link OntologyTermValidator}: resolves each slot's URI against Gemma's loaded ontologies
 * ({@link OntologyService#getTerm}) first, then {@link OlsTermResolver OLS}, and compares labels.
 *
 * @author gemma
 */
@Slf4j
@Component
public class OntologyTermValidatorImpl implements OntologyTermValidator {

    /**
     * URI prefixes that are legitimately not ontology terms and so are never sent for grounding — the same
     * carve-out {@code OntologyServiceImpl}'s label-fixer already applies to NCBI gene URIs.
     */
    private static final String[] NON_ONTOLOGY_URI_PREFIXES = { Gene.NCBI_URI_PREFIX };

    private final OntologyService ontologyService;
    private final OlsTermResolver olsTermResolver;

    @Value("${gemma.ontology.validation.timeout.ms}")
    private long timeoutMs;

    @Autowired
    public OntologyTermValidatorImpl( OntologyService ontologyService, OlsTermResolver olsTermResolver ) {
        this.ontologyService = ontologyService;
        this.olsTermResolver = olsTermResolver;
    }

    @Override
    public List<TermViolation> validateAndCanonicalize( Characteristic c ) {
        List<TermViolation> violations = new ArrayList<>();
        validateSlot( "category", c.getCategory(), c.getCategoryUri(), c::setCategory, violations );
        validateSlot( "value", c.getValue(), c.getValueUri(), c::setValue, violations );
        if ( c instanceof Statement ) {
            Statement s = ( Statement ) c;
            validateSlot( "predicate", s.getPredicate(), s.getPredicateUri(), s::setPredicate, violations );
            validateSlot( "object", s.getObject(), s.getObjectUri(), s::setObject, violations );
            validateSlot( "secondPredicate", s.getSecondPredicate(), s.getSecondPredicateUri(), s::setSecondPredicate, violations );
            validateSlot( "secondObject", s.getSecondObject(), s.getSecondObjectUri(), s::setSecondObject, violations );
        }
        return violations;
    }

    private void validateSlot( String slot, @Nullable String label, @Nullable String uri, Consumer<String> canonicalLabelSetter, List<TermViolation> violations ) {
        if ( StringUtils.isBlank( uri ) ) {
            return; // free text — nothing to ground
        }
        for ( String prefix : NON_ONTOLOGY_URI_PREFIXES ) {
            if ( uri.startsWith( prefix ) ) {
                return; // legitimately not an ontology term
            }
        }

        String resolvedLabel = resolveLabel( uri, slot, label, violations );
        if ( resolvedLabel == null ) {
            return; // resolveLabel already recorded URI_UNRESOLVED / UNVERIFIED, or nothing to compare
        }

        if ( StringUtils.isBlank( label ) ) {
            // a URI with no label supplied — fill in the canonical one rather than reject
            canonicalLabelSetter.accept( resolvedLabel );
            return;
        }
        if ( label.equals( resolvedLabel ) ) {
            return; // exact match
        }
        if ( normalize( label ).equals( normalize( resolvedLabel ) ) ) {
            // case / whitespace difference only — accept, but store the canonical form
            canonicalLabelSetter.accept( resolvedLabel );
            return;
        }
        violations.add( new TermViolation( slot, label, uri, resolvedLabel, TermViolation.Reason.LABEL_MISMATCH ) );
    }

    /**
     * Resolve a URI's canonical label: Gemma's loaded ontologies first, then OLS. Records a violation and
     * returns {@code null} when the URI resolves nowhere or OLS is unreachable.
     */
    @Nullable
    private String resolveLabel( String uri, String slot, @Nullable String submittedLabel, List<TermViolation> violations ) {
        try {
            OntologyTerm local = ontologyService.getTerm( uri, timeoutMs, TimeUnit.MILLISECONDS );
            if ( local != null && local.getLabel() != null ) {
                return local.getLabel();
            }
        } catch ( TimeoutException e ) {
            log.warn( "Timed out resolving " + uri + " against loaded ontologies; falling back to OLS." );
        }
        try {
            OlsTerm ols = olsTermResolver.resolve( uri );
            if ( ols != null && ols.getLabel() != null ) {
                return ols.getLabel();
            }
        } catch ( OlsUnavailableException e ) {
            log.warn( "OLS unavailable while validating " + uri + ": " + e.getMessage() );
            violations.add( new TermViolation( slot, submittedLabel, uri, null, TermViolation.Reason.UNVERIFIED_OLS_UNAVAILABLE ) );
            return null;
        }
        violations.add( new TermViolation( slot, submittedLabel, uri, null, TermViolation.Reason.URI_UNRESOLVED ) );
        return null;
    }

    /**
     * Collapse case and surrounding/internal whitespace so a difference in only those respects counts as a
     * (canonicalizable) match rather than a mismatch.
     */
    private static String normalize( String s ) {
        return StringUtils.normalizeSpace( s ).toLowerCase();
    }
}
