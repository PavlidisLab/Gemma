/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Rewrites annotations that use an obsolete ontology term to the successor the ontology asserts.
 * <p>
 * Only acts on terms {@link OntologyService#findObsoleteTermsInUse(long, TimeUnit)} marked
 * {@code autoCorrectable} — i.e. where the replacement was DERIVED from {@code IAO:0100001} or a merge record
 * rather than decided by anyone. A term whose ontology offers only {@code oboInOwl:consider} candidates is never
 * touched here; choosing between them is curation and belongs to a curator.
 *
 * @author phase 3 ontology maintenance
 */
public interface ObsoleteTermCorrectionService {

    /**
     * Terms that are mechanically correctable but that Paul has deferred (2026-08-19). They are skipped unless a
     * caller names one explicitly in {@code uris}, so a blanket run can never sweep them up.
     * <p>
     * {@code EFO_0000408} is the "disease" CATEGORY on ~7,600 experiments, so correcting it rewrites the shape of
     * the annotation rather than one of its terms. {@code OBI_0003109} is single-NUCLEUS RNA-seq, which OBI merged
     * into the general single-CELL term — mechanically correct and semantically lossy, since it discards a
     * distinction Gemma can currently make on 321 experiments.
     */
    Set<String> DEFERRED_URIS = Collections.unmodifiableSet( new java.util.LinkedHashSet<>( java.util.Arrays.asList(
            "http://www.ebi.ac.uk/efo/EFO_0000408",
            "http://purl.obolibrary.org/obo/OBI_0003109" ) ) );

    /**
     * Apply the corrections.
     *
     * @param uris    restrict to these obsolete term URIs; empty or null means every auto-correctable term except
     *                {@link #DEFERRED_URIS}. Naming a deferred term here does act on it — that is the deliberate
     *                override.
     * @param dryRun  when true, nothing is written and nothing is resynced, but the returned counts are the ones a
     *                live run would produce.
     * @param timeout budget for resolving terms against the loaded ontologies
     */
    ObsoleteTermCorrectionResult apply( Collection<String> uris, boolean dryRun, long timeout, TimeUnit timeUnit )
            throws TimeoutException;
}
