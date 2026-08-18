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
            "http://gemma.msl.ubc.ca/ont/TGEMO_00171",         // induced by
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
            "http://gemma.msl.ubc.ca/ont/TGEMO_00170"          // negative for product of gene
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
        if ( predicateUri == null ) {
            return EXPERIMENT_LEVEL;
        }
        if ( ALWAYS_TERM_LEVEL.contains( predicateUri ) ) {
            return TERM_LEVEL;
        }
        if ( SUBJECT_DEPENDENT.contains( predicateUri ) ) {
            return subjectCategoryUri != null && TERM_LEVEL_SUBJECT_CATEGORIES.contains( subjectCategoryUri )
                    ? TERM_LEVEL
                    : EXPERIMENT_LEVEL;
        }
        return EXPERIMENT_LEVEL;
    }

    private static Set<String> unmodifiable( String... uris ) {
        return Collections.unmodifiableSet( new HashSet<>( Arrays.asList( uris ) ) );
    }
}
