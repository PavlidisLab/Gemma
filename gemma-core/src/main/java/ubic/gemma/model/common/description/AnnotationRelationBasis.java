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

/**
 * How an {@link AnnotationRelation} came to be known, ranked best first.
 *
 * <p>The ranking is the substance of the design, not bookkeeping: <b>an assertion beats an
 * attestation</b>. Somebody stating that a genotype belongs to a disease is different in kind from
 * our having noticed the two annotated together, and a reader that cannot tell them apart will treat
 * a coincidence as a claim.</p>
 *
 * <p>{@link #getRank()} exists so precedence is a property of the basis rather than a switch
 * statement each caller writes for itself — the same reason
 * {@link PublicationAssociationSource#getRank()} does.</p>
 *
 * @see AnnotationRelation
 */
public enum AnnotationRelationBasis {

    /**
     * A curator already wrote it, as a {@link ubic.gemma.model.expression.experiment.Statement} on an annotation: 10,040 datasets carry a
     * {@code GENO_0000222 has_genotype} statement, 1,829 an {@code RO_0002573 has modifier}, 469 a
     * {@code TGEMO_00171 induced by}.
     *
     * <p>Nothing about these rows is inferred. The relation was recorded as
     * {@code disease model: left ventricular hypertrophy — induced by — aortic banding} and has
     * always been in {@code CHARACTERISTIC.PREDICATE}/{@code OBJECT}; what was missing is any way to
     * ask the question from the other end, because the only index on it is per-experiment. Harvesting
     * these is the highest-value half of this feature and the cheapest.</p>
     */
    CURATED( 100 ),

    /**
     * A loaded ontology asserts it as an OWL restriction — CLO's {@code derives from patient having
     * disease} and {@code is disease model for}, MONDO's {@code in_taxon}, CL's {@code part_of} into
     * UBERON.
     *
     * <p>Weaker than {@link #CURATED} only because it is a claim about the term in general rather
     * than about anything Gemma holds, and ontologies disagree with each other. It is stronger than
     * anything counted, and it is the basis that can cover terms our corpus has never seen.</p>
     */
    ONTOLOGY( 80 ),

    /**
     * A third-party resource asserts it: MGI's Disease Ontology report, Cellosaurus's
     * {@code disease-list} / {@code derived-from-site-list}.
     *
     * <p>Ranked below {@link #ONTOLOGY} because these are gene-level and unranked where our question
     * is value-level: MGI answers {@code Sod1} with Down syndrome and Parkinson disease rather than
     * ALS, and {@code Trp53} with CHARGE syndrome ahead of any malignancy our corpus attests. Useful
     * as corroboration and as a coverage estimate; never on its own as an input to a curation
     * decision.</p>
     */
    EXTERNAL( 60 ),

    /**
     * Nobody asserts it; our own curation attests it by co-occurrence, and the specificity of that
     * co-occurrence is measurable.
     *
     * <p>🛑 <b>This is a weak link and must be corroborated.</b> Co-occurrence cannot by itself
     * separate {@code Abca4} null → retinal degeneration (which the genotype really does account for)
     * from {@code C57BL/6J} → obesity (diet-induced), {@code CTCF} knockdown → Burkitt lymphoma (the
     * lymphoma belongs to the cell line) or {@code FTY720} → EAE (the drug is what is being tested
     * against an immunization-induced disease). Specificity — the fraction of a value's experiments
     * one disease accounts for — demotes all three without a special case, but demoting is not
     * disproving. A relation resting on this basis alone is reported as uncorroborated.</p>
     *
     * <p>🛑 <b>And it is self-consuming.</b> The co-occurrence exists because we were overtagging: a
     * whole-experiment disease tag written beside a genotype that already implied it. When that
     * redundant tag stops being written — precisely what the curation agents propose to do, on the
     * grounds that it is inferable — the evidence stops accruing. This basis is a snapshot of past
     * curation practice, so it gets weaker every time curation gets better, and nothing should be
     * built to depend on it alone.</p>
     */
    CORPUS( 20 );

    private final int rank;

    AnnotationRelationBasis( int rank ) {
        this.rank = rank;
    }

    /**
     * Higher wins. Absolute values carry no meaning; only the order does, and the gaps leave room for
     * a basis to be slotted in between two existing ones without renumbering.
     */
    public int getRank() {
        return rank;
    }

    /**
     * Whether a relation resting on this basis alone is strong enough to report without
     * corroboration from another.
     *
     * <p>Only {@link #CORPUS} is not: everything else is somebody's claim, while co-occurrence is an
     * observation about how two annotations happen to be distributed.</p>
     */
    public boolean isSelfSufficient() {
        return this != CORPUS;
    }
}
