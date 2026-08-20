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
 */
package ubic.gemma.model.common.description;

import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Whether a relation says something about the TERM, or records a parameter of an EXPERIMENT.
 *
 * <p>Every row in {@link AnnotationRelation} is a statement a curator or an ontology actually made,
 * so none of them is wrong. But they answer two different questions, and only one of them is what
 * somebody looking at a term wants:</p>
 *
 * <pre>
 * disease model: Alzheimer disease  -- has_genotype -->  APP/PS1      what this disease IS modelled by
 * female                            -- has_genotype -->  XX           what one experiment's samples WERE
 * </pre>
 *
 * <p>Measured by uib on the term card for {@code female} ({@code PATO_0000383}): six rows, taller than
 * the definition above them, not one of them actionable. Across ten datasets the split is roughly four
 * rows of experiment bookkeeping for every row of knowledge — {@code delivered for duration} 375 and
 * {@code has developmental stage} 297, against {@code is disease model for} 61 and {@code has disease}
 * 31.</p>
 *
 * <p>🛑 <b>{@code objectBreadth} does not catch this and cannot.</b> The junk rows on {@code female}
 * carry breadth 1&ndash;3 — maximally specific. Breadth separates a topic from a dose; it says nothing
 * about whether a relation is <i>about the term you are looking at</i>. Two orthogonal filters.</p>
 *
 * <p><b>Classified per ROW, not per predicate</b>, because two predicates genuinely do both jobs:</p>
 *
 * <ul>
 * <li>{@code GENO_0000222 has_genotype} is knowledge when the subject is a disease, disease model,
 * cell line, genotype or strain, and a sample's sex when the subject is {@code female}. Same
 * predicate; the subject decides.</li>
 * <li>{@code RO_0001000 derives from} covers {@code amplified total RNA -> total RNA} (bookkeeping)
 * and {@code cell line -> female donor} (provenance). A predicate allow-list has to drop both to be
 * safe, which loses the second.</li>
 * <li>{@code TGEMO_00171 induced by} is a disease model when the subject is a disease and a
 * differentiation protocol when the subject is a cell type — {@code Parkinson disease --induced by-->
 * MPTP} versus {@code lower motor neuron --induced by--> iPSC line}. Same predicate, opposite
 * meanings; the subject decides.</li>
 * </ul>
 *
 * <p><b>Nothing is dropped from the store.</b> This is a read-time reading of two columns the row
 * already carries, so the table stays the general thing it was built as and a caller can always ask
 * for everything. It is computed rather than stored precisely because the rule is new: changing it is
 * a deploy, not a migration and a re-harvest.</p>
 */
public enum RelationTopicality {

    /**
     * The relation says what the subject term IS or where it came from: a disease it models, the
     * patient it derives from, the tissue or organism it came from, the role a compound plays.
     */
    TERM_LEVEL,

    /**
     * The relation records how one experiment was run: a dose, a duration, a developmental stage, a
     * sample's sex. Real curation, correctly stored, and a fact about the experiment rather than about
     * the term — so it does not belong on a term card and cannot imply an annotation.
     */
    EXPERIMENT_LEVEL;

    /** Predicates that state what a term is or where it came from, whatever the subject. */
    private static final Set<String> ALWAYS_TERM_LEVEL = unmodifiable(
            "http://purl.obolibrary.org/obo/RO_0016002",       // has disease
            "http://purl.obolibrary.org/obo/CLO_0000179",      // is disease model for
            "http://purl.obolibrary.org/obo/CLO_0000015",      // derives from patient having disease
            "http://purl.obolibrary.org/obo/CLO_0037207",      // derives from organism
            "http://purl.obolibrary.org/obo/CLO_0037208",      // derives from anatomic part
            "http://purl.obolibrary.org/obo/CLO_0037209",      // derived from cell
            "http://purl.obolibrary.org/obo/CLO_0037210",      // derived from cell line
            "http://purl.obolibrary.org/obo/CLO_0037227",      // cell line cell derived from anatomical part
            "http://purl.obolibrary.org/obo/CLO_0037229",      // cell line cell derived from organism
            "http://purl.obolibrary.org/obo/ENVO_01003004",    // derives from part of -- cell line -> brain
            "http://purl.obolibrary.org/obo/RO_0000087",       // has role -- CHEBI's drug -> role
            "http://purl.obolibrary.org/obo/RO_0003301",       // is model of
            "http://gemma.msl.ubc.ca/ont/TGEMO_00201"          // has child with disease
    );

    /**
     * Predicates whose job depends on what the subject is. Term-level only when the subject carries
     * one of {@link #TERM_LEVEL_SUBJECT_CATEGORIES}.
     *
     * <p>{@code positive/negative for product of gene} and {@code has_allele} are here rather than in
     * the always-list for the same reason as {@code has_genotype}: on a disease or a cell line they
     * describe the thing, on a sample-descriptor subject they describe one experiment's samples.</p>
     */
    private static final Set<String> SUBJECT_DEPENDENT = unmodifiable(
            "http://purl.obolibrary.org/obo/GENO_0000222",     // has_genotype
            "http://purl.obolibrary.org/obo/GENO_0000413",     // has_allele
            "http://purl.obolibrary.org/obo/RO_0001000",       // derives from -- two jobs, see class javadoc
            "http://gemma.msl.ubc.ca/ont/TGEMO_00169",         // positive for product of gene
            "http://gemma.msl.ubc.ca/ont/TGEMO_00170",         // negative for product of gene
            // 🛑 induced by was in the always-list and had to leave it. It carries two senses and the
            // corpus uses both: `Parkinson disease --induced by--> MPTP` is a disease model, and
            // `lower motor neuron --induced by--> iPSC line` is a stem-cell differentiation protocol,
            // one of the commonest things curated here. Reported by uib 2026-08-18, who were shown
            // `iPSC line has disease neuron` on a curator's term card. Over the 541 curated rows:
            // 301 Disease model, 56 disease, 46 genotype and 1 cell line are the disease sense;
            // 106 treatment, 20 cell type, 6 phenotype and 3 collection of material are not.
            "http://gemma.msl.ubc.ca/ont/TGEMO_00171"          // induced by
    );

    /**
     * Subject categories that make a {@link #SUBJECT_DEPENDENT} predicate knowledge rather than
     * bookkeeping. uib's list, measured: applying it took {@code female} from 12 rows to 0 (the
     * section disappears, which is correct), Alzheimer from 58 to 39, Parkinson from 73 to 14.
     *
     * <p>🛑 A row with NO subject category does not qualify. That is the {@code female} case again,
     * and defaulting an unknown to "show it" is how the noise got in.</p>
     */
    private static final Set<String> TERM_LEVEL_SUBJECT_CATEGORIES = unmodifiable(
            "http://www.ebi.ac.uk/efo/EFO_0000408",            // disease
            "http://gemma.msl.ubc.ca/ont/TGEMO_00101",         // disease model
            "http://purl.obolibrary.org/obo/CLO_0000031",      // cell line
            "http://www.ebi.ac.uk/efo/EFO_0000513",            // genotype
            "http://www.ebi.ac.uk/efo/EFO_0005135"             // strain
    );

    /**
     * Predicates whose OBJECT is a quantity, not a concept.
     *
     * <p>🛑 <b>These have no business in a relation store at all</b>, which is different from being
     * {@link #EXPERIMENT_LEVEL}. A developmental stage is a concept and a poor topic; {@code 10 uM} is
     * not a concept. Measured on the corpus 2026-08-18, the objects of {@code delivered at dose} are
     * {@code 10 uM} (497), {@code 1 uM} (311), {@code 100 nM} (142), {@code 10 ng/ml} (102),
     * {@code 10 mg/kg} (67) — measurements all the way down. Nobody will ever ask what is delivered at
     * 10 uM, so the row cannot be read from the object end, cannot corroborate another row, and cannot
     * license an inference. It was 9,606 of the 36,073 curated rows: a quarter of the table that every
     * reader had to filter and no reader could use.</p>
     *
     * <p>{@code Relation.terms.txt} already states the rule — it opens "Terms usable for relations
     * among <b>concepts</b>" and then lists all three of these. This is that header, enforced.</p>
     *
     * <p><b>A deny-list, where {@link #of} is an allow-list, and deliberately so.</b> The harvest is
     * predicate-agnostic on purpose: a relational predicate added tomorrow should be harvested without
     * anyone editing this file. Only measurement predicates need naming, and there are few — so the
     * closed default belongs on the read side, where an unvetted predicate merely stays off a term
     * card, and the open default belongs in the harvest, where it decides whether data exists at
     * all.</p>
     */
    private static final Set<String> QUANTITY_VALUED_URIS = unmodifiable(
            "http://gemma.msl.ubc.ca/ont/TGEMO_00166",         // delivered at dose
            "http://gemma.msl.ubc.ca/ont/TGEMO_00167",         // delivered for duration
            "http://gemma.msl.ubc.ca/ont/TGEMO_00202"          // sampled after
    );

    /**
     * The same predicates by label, for the rows that carry no URI.
     *
     * <p>{@code timepoint} is here and in no vocabulary file: two curated rows use it as a bare label.
     * Matching on the label as well as the URI is what keeps the rule from being defeated by a
     * predicate nobody grounded.</p>
     */
    private static final Set<String> QUANTITY_VALUED_LABELS = unmodifiable(
            "delivered at dose", "delivered for duration", "sampled after", "timepoint"
    );

    /**
     * @see #QUANTITY_VALUED_URIS
     */
    public static Set<String> getQuantityValuedPredicateUris() {
        return QUANTITY_VALUED_URIS;
    }

    /**
     * @see #QUANTITY_VALUED_LABELS
     */
    public static Set<String> getQuantityValuedPredicateLabels() {
        return QUANTITY_VALUED_LABELS;
    }

    /**
     * Whether a relation with this predicate relates two concepts at all.
     *
     * <p>Either end identifies it: a row carrying the URI is caught by the URI, and a row carrying
     * only a label is caught by the label.</p>
     */
    public static boolean isQuantityValued( @Nullable String predicateUri, @Nullable String predicate ) {
        return ( predicateUri != null && QUANTITY_VALUED_URIS.contains( predicateUri ) )
                || ( predicate != null && QUANTITY_VALUED_LABELS.contains( predicate.trim() ) );
    }

    /**
     * Classify one row.
     *
     * <p>Unknown predicates are {@link #EXPERIMENT_LEVEL}. That is deliberate and is the opposite of
     * how the harvest works: the harvest is predicate-agnostic so nothing is lost from the store, and
     * this default is closed so nothing unvetted reaches a term card. A predicate that deserves
     * promotion gets named here, which is one edit in one place rather than in each client.</p>
     *
     * <p>{@code RO_0002200 has phenotype} is deliberately NOT term-level despite reading like it —
     * uib sampled it and found {@code precursor cell -> astrocyte} and {@code 17 d -> neuron},
     * which is differentiation bookkeeping. It is the sort of predicate a reader would allow-list on
     * the name alone.</p>
     */
    public static RelationTopicality of( @Nullable String predicateUri, @Nullable String subjectCategoryUri ) {
        return of( predicateUri, subjectCategoryUri, null );
    }

    /**
     * @param subjectValueUri the subject term's own URI, which names what it IS when the curated
     *                        category disagrees — see {@link #denotesADiseaseOrPhenotype}
     */
    public static RelationTopicality of( @Nullable String predicateUri, @Nullable String subjectCategoryUri,
            @Nullable String subjectValueUri ) {
        if ( predicateUri == null ) {
            return EXPERIMENT_LEVEL;
        }
        if ( ALWAYS_TERM_LEVEL.contains( predicateUri ) ) {
            return TERM_LEVEL;
        }
        if ( SUBJECT_DEPENDENT.contains( predicateUri ) ) {
            boolean byCategory = subjectCategoryUri != null
                    && TERM_LEVEL_SUBJECT_CATEGORIES.contains( subjectCategoryUri );
            return byCategory || denotesADiseaseOrPhenotype( subjectValueUri )
                    ? TERM_LEVEL
                    : EXPERIMENT_LEVEL;
        }
        return EXPERIMENT_LEVEL;
    }

    /**
     * Identifier spaces whose terms ARE a disease or phenotype, whatever a curator filed them under.
     *
     * <p>🛑 <b>The curated category is the unreliable half of the row and the subject's vocabulary is
     * the reliable half.</b> uib measured the same fact twice in one corpus, 2026-08-18:
     * {@code seizures MP_0002064 --induced by--> kainic acid} appears once categorised
     * {@code Disease model} and once {@code treatment}, and only the category differs. Licensing on
     * the category alone made one of them a disease model and the other nothing — the same fact
     * wearing two spellings of its metadata. {@code tauopathy MONDO_0005574} is filed under
     * {@code treatment} too, and is plainly a disease.</p>
     *
     * <p>🛑 <b>EFO is deliberately NOT here</b>, even though it carries plenty of diseases. It also
     * carries {@code EFO_0000579 growth condition}, which is what {@code media --induced by-->
     * lipopolysaccharide} is — a real treatment condition and not a disease model. So EFO diseases are
     * admitted by their CATEGORY, which is right on those rows, and the namespaces here admit the ones
     * whose category is wrong. Either signal suffices; neither is required.</p>
     */
    private static final Set<String> DISEASE_LOCAL_NAME_PREFIXES = unmodifiable(
            "MONDO_", "DOID_", "MP_", "HP_" );

    /**
     * Whether a term URI names a disease or phenotype by virtue of the vocabulary it belongs to.
     *
     * @see #DISEASE_LOCAL_NAME_PREFIXES
     */
    public static boolean denotesADiseaseOrPhenotype( @Nullable String termUri ) {
        if ( termUri == null ) {
            return false;
        }
        int cut = Math.max( termUri.lastIndexOf( '/' ), termUri.lastIndexOf( '#' ) );
        String localName = cut >= 0 ? termUri.substring( cut + 1 ) : termUri;
        for ( String prefix : DISEASE_LOCAL_NAME_PREFIXES ) {
            if ( localName.startsWith( prefix ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Categories that make the subject a disease in its own right, for a predicate whose implied claim
     * is about disease. Narrower than {@link #TERM_LEVEL_SUBJECT_CATEGORIES}, which also admits cell
     * line, genotype and strain: those are facts worth showing on a term card and are NOT things that
     * bear a disease.
     */
    private static final Set<String> DISEASE_SUBJECT_CATEGORIES = unmodifiable(
            "http://www.ebi.ac.uk/efo/EFO_0000408",            // disease
            "http://gemma.msl.ubc.ca/ont/TGEMO_00101"          // disease model
    );

    /**
     * Whether this row's subject is a disease, by either signal.
     *
     * @see RelationInferenceDirection
     */
    public static boolean subjectIsADisease( @Nullable String subjectCategoryUri,
            @Nullable String subjectValueUri ) {
        return ( subjectCategoryUri != null && DISEASE_SUBJECT_CATEGORIES.contains( subjectCategoryUri ) )
                || denotesADiseaseOrPhenotype( subjectValueUri );
    }

    private static Set<String> unmodifiable( String... uris ) {
        return Collections.unmodifiableSet( new HashSet<>( Arrays.asList( uris ) ) );
    }
}
