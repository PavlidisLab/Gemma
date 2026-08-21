/*
 * The gemma-rest project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

/**
 * Pins the taxon backstop on {@code GET /search}.
 * <p>
 * {@link ubic.gemma.model.common.search.SearchSettings#getTaxonConstraint()} is advisory — it is
 * handed to every search source, but the gene sources do not apply it. Before the backstop,
 * {@code ?query=Myc&taxon=mouse} answered with the rat and human orthologs at every limit, and did
 * so silently, so a caller trusting the parameter landed on the wrong species.
 * <p>
 * The subtle half is what the backstop must NOT do: five of the nine supported result types have no
 * taxon concept, and discarding them because their taxon is "unknown" would make a taxon-scoped
 * search quietly stop returning papers, probes and sets.
 */
class SearchTaxonBackstopTest {

    private static final Long MOUSE = 2L;
    private static final Long RAT = 3L;

    private static boolean matches( Object vo, Long wantTaxonId ) {
        return Boolean.TRUE.equals(
                invokeMethod( SearchWebService.class, "matchesTaxon", vo, wantTaxonId ) );
    }

    @Test
    void keepsAGeneOfTheRequestedTaxon() {
        assertThat( matches( gene( MOUSE ), MOUSE ) ).isTrue();
    }

    /** The reported failure: Myc/rat surviving an explicit taxon=mouse. */
    @Test
    void dropsAGeneOfAnotherTaxon() {
        assertThat( matches( gene( RAT ), MOUSE ) ).isFalse();
    }

    /** A type that carries a taxon but has none set cannot be shown to answer the question. */
    @Test
    void dropsATaxonCarryingResultWithNoTaxonSet() {
        assertThat( matches( gene( null ), MOUSE ) ).isFalse();
    }

    @Test
    void appliesToEveryTaxonCarryingResultType() {
        ExpressionExperimentValueObject ee = new ExpressionExperimentValueObject();
        ee.setTaxonObject( new TaxonValueObject( RAT ) );
        assertThat( matches( ee, MOUSE ) ).isFalse();

        ArrayDesignValueObject ad = new ArrayDesignValueObject();
        ad.setTaxonObject( new TaxonValueObject( RAT ) );
        assertThat( matches( ad, MOUSE ) ).isFalse();

        GeneSetValueObject gs = new GeneSetValueObject();
        gs.setTaxon( new TaxonValueObject( RAT ) );
        assertThat( matches( gs, MOUSE ) ).isFalse();
    }

    /**
     * The guard against over-filtering. These types have no taxon at all, so a taxon-constrained
     * search must pass them through rather than treat "no taxon" as "wrong taxon".
     */
    @Test
    void keepsResultTypesThatHaveNoTaxonConcept() {
        assertThat( matches( new BibliographicReferenceValueObject(), MOUSE ) ).isTrue();
        assertThat( matches( new CompositeSequenceValueObject(), MOUSE ) ).isTrue();
        assertThat( matches( new ExpressionExperimentSetValueObject(), MOUSE ) ).isTrue();
    }

    /** Results with a null resultObject (see issue #417) are not the backstop's business. */
    @Test
    void keepsResultsWithNoResultObject() {
        assertThat( matches( null, MOUSE ) ).isTrue();
    }

    private static GeneValueObject gene( Long taxonId ) {
        GeneValueObject g = new GeneValueObject();
        g.setTaxon( taxonId != null ? new TaxonValueObject( taxonId ) : null );
        return g;
    }
}
