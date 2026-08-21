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
import ubic.gemma.core.ontology.model.OntologyProperty;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * A Gemma-owned ontology id (currently just TGEMO) captured from anywhere in a URI, so a term written on a
     * foreign base can be normalized onto {@link OntologyUtils#BASE_GEMMA_ONTOLOGY_URI}. Extend the alternation
     * if another Gemma-owned vocabulary starts showing up on the wrong base.
     */
    private static final Pattern GEMMA_ONTOLOGY_ID = Pattern.compile( "(TGEMO_\\d+)" );

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
    public List<TermViolation> validateAndCanonicalize( Characteristic c, List<TermCanonicalization> canonicalizations ) {
        List<TermViolation> violations = new ArrayList<>();
        validateSlot( "category", c.getCategory(), c.getCategoryUri(), c::setCategory, c::setCategoryUri, violations, canonicalizations );
        validateSlot( "value", c.getValue(), c.getValueUri(), c::setValue, c::setValueUri, violations, canonicalizations );
        if ( c instanceof Statement ) {
            Statement s = ( Statement ) c;
            validateSlot( "predicate", s.getPredicate(), s.getPredicateUri(), s::setPredicate, s::setPredicateUri, violations, canonicalizations );
            validateSlot( "object", s.getObject(), s.getObjectUri(), s::setObject, s::setObjectUri, violations, canonicalizations );
            validateSlot( "secondPredicate", s.getSecondPredicate(), s.getSecondPredicateUri(), s::setSecondPredicate, s::setSecondPredicateUri, violations, canonicalizations );
            validateSlot( "secondObject", s.getSecondObject(), s.getSecondObjectUri(), s::setSecondObject, s::setSecondObjectUri, violations, canonicalizations );
        }
        return violations;
    }

    private void validateSlot( String slot, @Nullable String label, @Nullable String uri, Consumer<String> canonicalLabelSetter, Consumer<String> canonicalUriSetter, List<TermViolation> violations, List<TermCanonicalization> canonicalizations ) {
        if ( StringUtils.isBlank( uri ) ) {
            return; // free text — nothing to ground
        }
        for ( String prefix : NON_ONTOLOGY_URI_PREFIXES ) {
            if ( uri.startsWith( prefix ) ) {
                return; // legitimately not an ontology term
            }
        }

        // Normalize a known Gemma-ontology term (TGEMO) that arrived on a foreign base back to the Gemma base
        // BEFORE resolution, so a real term written as e.g. purl.obolibrary.org/obo/TGEMO_00166 grounds instead
        // of being mis-reported as fabricated. A fabricated id still won't resolve under the canonical base.
        String submittedUri = uri;
        String canonicalUri = canonicalizeGemmaOntologyUri( uri );
        boolean uriRewritten = !canonicalUri.equals( uri );
        if ( uriRewritten ) {
            canonicalUriSetter.accept( canonicalUri );
            uri = canonicalUri;
        }

        String resolvedLabel = resolveLabel( uri, slot, label, violations );
        if ( resolvedLabel == null ) {
            return; // resolveLabel already recorded URI_UNRESOLVED / UNVERIFIED, or nothing to compare
        }

        boolean labelRewritten;
        if ( StringUtils.isBlank( label ) ) {
            // a URI with no label supplied — fill in the canonical one rather than reject
            canonicalLabelSetter.accept( resolvedLabel );
            labelRewritten = true;
        } else if ( label.equals( resolvedLabel ) ) {
            labelRewritten = false; // exact match
        } else if ( normalize( label ).equals( normalize( resolvedLabel ) ) ) {
            // case / whitespace difference only — accept, but store the canonical form
            canonicalLabelSetter.accept( resolvedLabel );
            labelRewritten = true;
        } else {
            violations.add( new TermViolation( slot, label, uri, resolvedLabel, TermViolation.Reason.LABEL_MISMATCH ) );
            return;
        }

        if ( uriRewritten || labelRewritten ) {
            canonicalizations.add( new TermCanonicalization( slot, label, resolvedLabel, submittedUri, canonicalUri ) );
        }
    }

    /**
     * Whether a resolver actually told us what a term is called.
     * <p>
     * A resolver that hands back the term's own accession as its label ({@code RO_0002573} for
     * {@code .../obo/RO_0002573}) has not resolved anything — it has said "I don't know this term" in the one
     * form that looks like an answer. Both sources do it: a loaded ontology with no {@code rdfs:label} falls
     * back to the local name, and OLS echoes the id for terms outside the ontologies it serves.
     * <p>
     * Treating that as authoritative is worse than useless, because the caller then gets a LABEL_MISMATCH:
     * "this vocabulary is not loaded" is reported as "your label is wrong". On gemma2 that rejected a design
     * read straight out of Gemma and offered back unchanged — the RO predicates on GSE11630 are not loaded, so
     * Gemma refused to accept its own stored data, which also made a snapshot of an unmodified dataset
     * impossible to restore.
     * <p>
     * Falling through instead yields URI_UNRESOLVED, which is both true and actionable: load the vocabulary,
     * or drop the URI.
     */
    private static boolean isRealLabel( String uri, @Nullable String label ) {
        if ( StringUtils.isBlank( label ) ) {
            return false;
        }
        String localName = localName( uri );
        // Both conditions are needed. "equals the local name" alone is too broad: plenty of URIs end in the
        // word they mean (.../obo/asthma labelled "asthma"), and calling those unresolved would reject real
        // terms. The degenerate case is specifically an ACCESSION echoed back as prose -- RO_0002573, GO:0008150
        // -- which no ontology uses as a human label.
        return !( label.equals( localName ) && ACCESSION_SHAPED.matcher( localName ).matches() );
    }

    /** An OBO-style accession: a short alphabetic prefix, a separator, and digits. Never a real label. */
    private static final Pattern ACCESSION_SHAPED = Pattern.compile( "[A-Za-z]{2,}[_:]\\d+" );

    /** The trailing id of a term URI — everything after the last {@code /} or {@code #}. */
    private static String localName( String uri ) {
        int cut = Math.max( uri.lastIndexOf( '/' ), uri.lastIndexOf( '#' ) );
        return cut >= 0 && cut < uri.length() - 1 ? uri.substring( cut + 1 ) : uri;
    }

    /**
     * Rewrite a URI carrying a Gemma-owned ontology id ({@code TGEMO_<n>}) onto Gemma's canonical ontology base
     * regardless of the base it arrived on (the OBO PURL base, or a double-mangled
     * {@code .../obo/http_//gemma…/TGEMO_…} form) — the server-side mirror of the curation-UI's {@code curieToUrl}.
     * A stopgap until every writer emits the canonical base; returns the URI unchanged when it carries no such id
     * or is already canonical.
     */
    private static String canonicalizeGemmaOntologyUri( String uri ) {
        Matcher m = GEMMA_ONTOLOGY_ID.matcher( uri );
        if ( m.find() ) {
            String canonical = OntologyUtils.BASE_GEMMA_ONTOLOGY_URI + m.group( 1 );
            if ( !canonical.equals( uri ) ) {
                return canonical;
            }
        }
        return uri;
    }

    /**
     * Resolve a URI's canonical label: Gemma's loaded ontologies first, then OLS. Records a violation and
     * returns {@code null} when the URI resolves nowhere or OLS is unreachable.
     */
    /**
     * Gemma's own answer for the two slots where Gemma has rules, or {@code null} for the slots where it does not.
     * <p>
     * Predicates and categories are constrained vocabularies Gemma ships and owns — {@code Relation.terms.txt}
     * (29 relations) and the category terms — so for those slots the local list is the authority and there is
     * nothing to ask an ontology or OLS about. Subjects, objects and values are deliberately unconstrained:
     * curators legitimately use terms from ontologies Gemma has never loaded, and blocking those would be wrong.
     * <p>
     * Consulting the loaded ontologies instead of this list is what rejected {@code RO_0002573} — a predicate
     * that IS in {@code Relation.terms.txt}, labelled exactly {@code "has modifier"}, and that Gemma had itself
     * stored on GSE11630. RO is not among the loadable ontologies, so the generic path found nothing and the
     * commit refused Gemma's own sanctioned relation.
     */
    @Nullable
    private String resolveFromGemmaVocabulary( String slot, String uri ) {
        if ( "predicate".equals( slot ) || "secondPredicate".equals( slot ) ) {
            for ( OntologyProperty p : ontologyService.getRelationTerms() ) {
                if ( uri.equals( p.getUri() ) ) {
                    return p.getLabel();
                }
            }
            return null;
        }
        if ( "category".equals( slot ) ) {
            for ( OntologyTerm t : ontologyService.getCategoryTerms() ) {
                if ( uri.equals( t.getUri() ) ) {
                    return t.getLabel();
                }
            }
            return null;
        }
        return null;
    }

    @Nullable
    private String resolveLabel( String uri, String slot, @Nullable String submittedLabel, List<TermViolation> violations ) {
        // Gemma's own vocabularies first: for a predicate or a category they are definitive, offline, and cannot
        // be second-guessed by whatever an external service happens to know.
        String own = resolveFromGemmaVocabulary( slot, uri );
        if ( own != null ) {
            return own;
        }
        try {
            OntologyTerm local = ontologyService.getTerm( uri, timeoutMs, TimeUnit.MILLISECONDS );
            if ( local != null && isRealLabel( uri, local.getLabel() ) ) {
                return local.getLabel();
            }
        } catch ( TimeoutException e ) {
            log.warn( "Timed out resolving " + uri + " against loaded ontologies; falling back to OLS." );
        }
        try {
            OlsTerm ols = olsTermResolver.resolve( uri );
            if ( ols != null && isRealLabel( uri, ols.getLabel() ) ) {
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
