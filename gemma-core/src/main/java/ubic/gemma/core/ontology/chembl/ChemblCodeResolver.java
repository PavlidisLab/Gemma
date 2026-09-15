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
package ubic.gemma.core.ontology.chembl;

import org.springframework.lang.Nullable;

/**
 * Identifies a drug developmental / trial code against ChEMBL's synonym table.
 *
 * <p>Gemma annotates chemicals in CHEBI, and CHEBI covers the codes it curates well — of 60
 * ungrounded treatment-category codes taken from the corpus, 34 already resolve. This exists for
 * part of the rest, and specifically for the case where <b>the compound IS in CHEBI under a name
 * nobody wrote on the sample</b>: {@code WY-14643} is pirinixic acid, CHEBI_32509, already used 17
 * times in the corpus, and the only missing link was that nothing connected the code to the name.
 * ChEMBL supplies that link, and Gemma's own search does the grounding.</p>
 *
 * <p>🛑 <b>The fuzzy {@code /molecule/search} endpoint must not be used for this.</b> Asked about
 * {@code wy-14643} it returns CHEMBL4303324 — a different molecule — while an exact synonym lookup
 * returns CHEMBL295416, pirinixic acid, correctly. Fuzzy search also returns IDs whose records
 * carry no synonym matching the query at all, which is an identification with no evidence behind
 * it. Adopting it would relocate the near-match fabrication this whole area exists to prevent from
 * our index into ChEMBL's. Only exact synonym equality counts.</p>
 *
 * <p>Coverage is modest and worth stating plainly: of 26 corpus codes CHEBI could not reach, 4 are
 * identifiable this way and 22 are not in ChEMBL at all — NCI {@code NSC} registry numbers, an
 * oncolytic virus, an antibody conjugate. The drug-code gap is mostly not a ChEMBL-shaped
 * problem.</p>
 */
public interface ChemblCodeResolver {

    /**
     * Identify a code against ChEMBL.
     *
     * <p>Never throws for an unreachable or misbehaving ChEMBL: this is advisory enrichment sitting
     * on a search path, and a naming authority being down is not a reason to fail a user's query.
     * Failures are logged and reported as "not identified", and are NOT cached, so the next call
     * retries.</p>
     *
     * @param code the code as the submitter wrote it; separator spelling does not matter
     * @return the identified compound, or {@code null} when ChEMBL has no exact synonym match,
     *         when the resolver is disabled, or when ChEMBL could not be reached
     */
    @Nullable
    ChemblCompound identify( String code );
}
