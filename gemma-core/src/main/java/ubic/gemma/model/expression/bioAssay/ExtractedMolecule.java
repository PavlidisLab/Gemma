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
package ubic.gemma.model.expression.bioAssay;

/**
 * What was extracted from the sample and assayed — GEO's {@code !Sample_molecule_chN}.
 *
 * <h2>Why this is on the BioAssay and not the BioMaterial</h2>
 *
 * <p>Paul, 2026-09-05: <em>"the biomaterial is the cells/tissue we got the RNA from, not the RNA"</em>,
 * and <em>"the assay is 'we took that sample and did something to it to get expression
 * measurements'"</em>. The extraction is part of that doing, so it belongs to the assay. Recording it
 * as a characteristic OF the biomaterial describes the extract as though it were the sample.</p>
 *
 * <p>It is structural rather than tidy: one biomaterial can yield two different molecules. In CITE-seq
 * the same cells give both the RNA readout and the protein one, and on prod that is not hypothetical —
 * 181 {@code genomic_DNA} rows over 9 experiments and 80 {@code protein} rows over 3, all multimodal
 * designs (cab, 2026-09-05). On the biomaterial, expressing that needs a duplicated biomaterial, which
 * is a lie about the sample. On the BioAssay it is two assays over one material, which is what
 * happened.</p>
 *
 * <h2>🛑 It is not the whole story, and is not a substitute for library selection</h2>
 *
 * <p>Paul, 2026-08-31: <em>"total RNA … is potentially misleading because there's often still a poly-A
 * selection step"</em>. GEO's {@code molecule} says what went in;
 * {@link BioAssay#getLibrarySelection()} says how it was selected, and the two disagree routinely. Read
 * both before concluding anything about a library.</p>
 *
 * <h2>What it is good for</h2>
 *
 * <p>It is currently the ONLY thing in the database that separates single-nucleus from single-cell
 * RNA-seq — {@code isSingleCell} is true for both. Before this field that distinction existed solely as
 * one {@code molecular entity} characteristic among a sample's several, with no typed way to query it.</p>
 *
 * <p>Mirrors GEO's own vocabulary rather than inventing one, so an unmapped submitter value is
 * {@link #other} instead of a guess.</p>
 */
public enum ExtractedMolecule {
    totalRNA,
    polyARNA,
    cytoplasmicRNA,
    nuclearRNA,
    genomicDNA,
    protein,
    other
}
