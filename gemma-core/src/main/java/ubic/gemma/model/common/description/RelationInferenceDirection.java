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
 * Which end of a relation implies the other.
 *
 * <p>🛑 <b>A relation is readable from both ends; it is not INFERABLE from both ends.</b> This is the
 * distinction the whole gate depends on and it is easy to lose, because the store is deliberately
 * symmetric — {@code findRelations} answers from either side, and that is right for browsing. Applying
 * a relation as an inference is not symmetric:</p>
 *
 * <pre>
 * stored:  Alzheimer disease  -- has_genotype -->  APP/PS1
 *
 *   APP/PS1  =>  an Alzheimer disease model     ✅  the specific implies the general
 *   Alzheimer disease  =>  APP/PS1              🛑  NOT all Alzheimer models are APP/PS1
 * </pre>
 *
 * <p>A gate that followed the second direction would suppress a perfectly correct
 * {@code genotype: APP/PS1} tag because the dataset also said {@code disease: Alzheimer} — deleting
 * curation on the strength of an inference nobody made.</p>
 *
 * <p><b>And the valid direction is not the same for every predicate</b>, because Gemma's curation
 * does not put the specific end on the same side each time:</p>
 *
 * <pre>
 * disease model: Alzheimer  -- has_genotype -->  APP/PS1        specific is the OBJECT
 * MCF7 cell  -- derives from patient having disease -->  DOID   specific is the SUBJECT
 * </pre>
 *
 * <p>So this is a property of the predicate, read off which end carries the narrower thing. It is not
 * a property of the basis, the category, or the direction a caller happened to query from.</p>
 */
public enum RelationInferenceDirection {

    /**
     * The subject is the specific end: knowing the subject tells you the object. A cell line implies
     * the disease it derives from; a compound implies the role it plays.
     */
    SUBJECT_IMPLIES_OBJECT,

    /**
     * The object is the specific end: knowing the object tells you the subject. A genotype implies the
     * disease it models, because Gemma writes the disease as the subject of the statement.
     */
    OBJECT_IMPLIES_SUBJECT,

    /**
     * Neither end implies the other. Per-experiment parameters live here — a dose implies nothing
     * about a disease and a disease implies no dose — as does any predicate not classified.
     */
    NEITHER;

    /**
     * Predicates where the SUBJECT is the narrower thing.
     *
     * <p>These are the cell-line and compound provenance relations: the subject is a specific line or
     * substance and the object is the general class it belongs to or came from. {@code MCF7} implies
     * adenocarcinoma; adenocarcinoma implies nothing about MCF7.</p>
     */
    private static final Set<String> SUBJECT_SIDE = unmodifiable(
            "http://purl.obolibrary.org/obo/CLO_0000015",      // derives from patient having disease
            "http://purl.obolibrary.org/obo/CLO_0000179",      // is disease model for
            "http://purl.obolibrary.org/obo/CLO_0037207",      // derives from organism
            "http://purl.obolibrary.org/obo/CLO_0037208",      // derives from anatomic part
            "http://purl.obolibrary.org/obo/CLO_0037209",      // derived from cell
            "http://purl.obolibrary.org/obo/CLO_0037210",      // derived from cell line
            "http://purl.obolibrary.org/obo/CLO_0037227",
            "http://purl.obolibrary.org/obo/CLO_0037229",
            "http://purl.obolibrary.org/obo/ENVO_01003004",    // derives from part of
            "http://purl.obolibrary.org/obo/RO_0000087",       // has role -- imatinib implies antineoplastic
            "http://purl.obolibrary.org/obo/RO_0003301",       // is model of
            "http://purl.obolibrary.org/obo/RO_0016002",       // has disease -- SNCA implies Parkinson
            "http://gemma.msl.ubc.ca/ont/TGEMO_00201"          // has child with disease
    );

    /**
     * Predicates where the OBJECT is the narrower thing.
     *
     * <p>Gemma's curated disease-model statements put the disease in the SUBJECT and the genotype or
     * the inducer in the OBJECT, so the implication runs backwards along the arrow. This is the set
     * the motivating case lives in: {@code APP/PS1} implies an Alzheimer model, and
     * {@code MPTP} implies a Parkinson model.</p>
     */
    private static final Set<String> OBJECT_SIDE = unmodifiable(
            "http://purl.obolibrary.org/obo/GENO_0000222",     // has_genotype
            "http://purl.obolibrary.org/obo/GENO_0000413",     // has_allele
            "http://gemma.msl.ubc.ca/ont/TGEMO_00171",         // induced by -- MPTP => Parkinson model
            "http://gemma.msl.ubc.ca/ont/TGEMO_00169",         // positive for product of gene
            "http://gemma.msl.ubc.ca/ont/TGEMO_00170"          // negative for product of gene
    );

    /**
     * Which way the implication runs for this predicate, or {@link #NEITHER}.
     *
     * <p>Unclassified predicates are {@link #NEITHER}, closed by default: a predicate nobody has
     * reasoned about must not silently license a suppression. {@code RO_0001000 derives from} is
     * deliberately absent — it covers both {@code amplified total RNA -> total RNA} and
     * {@code cell line -> donor}, and one URI cannot carry two directions.</p>
     */
    public static RelationInferenceDirection of( @Nullable String predicateUri ) {
        if ( predicateUri == null ) {
            return NEITHER;
        }
        if ( SUBJECT_SIDE.contains( predicateUri ) ) {
            return SUBJECT_IMPLIES_OBJECT;
        }
        if ( OBJECT_SIDE.contains( predicateUri ) ) {
            return OBJECT_IMPLIES_SUBJECT;
        }
        return NEITHER;
    }

    /**
     * Whether a term sitting on the given end may be used to infer the other end.
     *
     * @param seedIsSubject true when the caller holds the subject and wants the object
     */
    public boolean licenses( boolean seedIsSubject ) {
        return seedIsSubject ? this == SUBJECT_IMPLIES_OBJECT : this == OBJECT_IMPLIES_SUBJECT;
    }

    private static Set<String> unmodifiable( String... uris ) {
        return Collections.unmodifiableSet( new HashSet<>( Arrays.asList( uris ) ) );
    }
}
