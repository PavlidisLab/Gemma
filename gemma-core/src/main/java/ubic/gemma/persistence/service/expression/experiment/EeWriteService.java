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
 *
 */
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Phase 3 ExpressionPersister retirement: the eventual home for the
 * EE persistence path. This skeleton exposes the "trivial find-or-create"
 * primitives that {@link ubic.gemma.persistence.persister.ExpressionPersister}
 * used to inline. The persister still owns orchestration (taxon / accession /
 * association fill-in); each of its {@code persistXxx} methods now delegates
 * the lookup-or-insert step here.
 * <p>
 * Strangler-fig staging: callers continue to go through the persister chain
 * unchanged. As more of the persister body moves into the impl (Chunks E3+),
 * this interface will grow a top-level {@code create(ExpressionExperiment, ...)}
 * method and the persister itself will be deleted.
 *
 * @author pavlidis
 */
public interface EeWriteService {

    /**
     * Find an existing Compound by business key (name) or create a new one.
     * Wraps {@code CompoundDao.findOrCreate} which delegates to
     * {@code BusinessKey}-equivalent name lookup.
     */
    Compound findOrCreate( Compound compound );

    /**
     * Find an existing BioMaterial by business key (see
     * {@code BusinessKey.find(Session, BioMaterial)}) or create one.
     * <p>
     * Pre-conditions enforced by the caller in the current persister chain:
     * the source taxon must already be persistent, and any external accession
     * must have its {@code ExternalDatabase} resolved.
     */
    BioMaterial findOrCreate( BioMaterial bioMaterial );

    /**
     * Find an existing FactorValue by business key (see
     * {@code BusinessKey.find(Session, FactorValue)}) or create one.
     * <p>
     * Pre-condition: the parent {@code ExperimentalFactor} must already be
     * persistent (id != null). The persister chain ensures this via
     * {@code fillInFactorValueAssociations}.
     */
    FactorValue findOrCreate( FactorValue factorValue );

    /**
     * Persist a new ExperimentalFactor. {@code ExperimentalFactor} is
     * composition-owned by its parent {@code ExperimentalDesign} so we do
     * <em>not</em> find-or-create: each call creates a fresh row. Kept on
     * the service interface as {@code create} (not {@code findOrCreate}) to
     * preserve that semantic.
     */
    ExperimentalFactor create( ExperimentalFactor experimentalFactor );

    /**
     * Find or create an ExpressionExperimentSubSet by business key (see
     * {@code BusinessKey.find(Session, ExpressionExperimentSubSet)}).
     * <p>
     * Pre-conditions enforced by the caller: the subset must have at least
     * one bioassay, and its source experiment must already be persistent.
     */
    ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet subSet );
}
