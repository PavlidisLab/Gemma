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

/**
 * Labelled-axis matrix types ported in-tree from baseCode's
 * {@code ubic.basecode.dataStructure.matrix} subsystem as part of the Phase 3
 * baseCode retirement (see {@code BASECODE_MATRIX_RECCE.md} and
 * {@code BASECODE_DEP_AUDIT.md}).
 * <p>
 * The 14 reachable classes (and the {@code PrimitiveMatrix} interface they
 * depend on) are pulled in verbatim with only their package declaration
 * rewritten and the {@code Constants} import rewired to
 * {@link ubic.gemma.core.util.math.Constants}. The 3D variants,
 * {@code CompressedBitMatrix}, {@code RCDoubleMatrix1D}, and
 * {@code SparseRaggedDoubleMatrix} were dropped — no Gemma consumers.
 * <p>
 * These classes are a generic, named-axis veneer over {@code cern.colt}
 * (with one MTJ-backed variant). Colt and MTJ stay as direct Gemma deps;
 * Gemma already uses both directly outside this package.
 *
 * @author pavlidis
 */
package ubic.gemma.core.util.matrix;
