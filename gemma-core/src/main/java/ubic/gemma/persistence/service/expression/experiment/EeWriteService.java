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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.ArrayDesignsForExperimentCache;

import javax.annotation.Nullable;

/**
 * Write service for {@link ExpressionExperiment} graphs.
 * <p>
 * This is the strangler-fig replacement for the EE-related portions of
 * the former {@code ExpressionPersister} (deleted in Persister-shrink S1;
 * the remaining polymorphic EE dispatch arms used only by test fixtures
 * have been folded into {@code PersisterHelperImpl.doPersist}).
 *
 * @author pavlidis
 */
public interface EeWriteService {

    /**
     * Persist an {@link ExpressionExperiment} graph.
     * <p>
     * The {@code cache} should be obtained from a prior, separate transaction
     * via {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentPrePersistService#prepare}.
     *
     * @param ee    the expression experiment to persist
     * @param cache cache of array designs already persisted in a prior transaction
     * @return the persisted entity
     */
    ExpressionExperiment create( ExpressionExperiment ee, @Nullable ArrayDesignsForExperimentCache cache );

    /**
     * Persist an {@link ExpressionExperiment} graph, synthesising the
     * {@link ArrayDesignsForExperimentCache} via
     * {@link ExpressionExperimentPrePersistService#prepare} in the same
     * transaction.
     * <p>
     * Prefer the two-argument {@link #create(ExpressionExperiment, ArrayDesignsForExperimentCache)}
     * overload when a cache is available from a prior, separate transaction
     * (recommended for large platforms). This single-argument overload exists
     * for the few callers (CellXGene, SplitExperiment, etc.) that build the
     * EE graph in one transaction and accept the in-transaction prepare.
     *
     * @param ee the expression experiment to persist
     * @return the persisted entity
     */
    ExpressionExperiment create( ExpressionExperiment ee );

    /**
     * Persist a standalone {@link BioAssay}.
     * <p>
     * Persister-shrink S4c: public typed entry point added so test fixtures
     * (notably {@code PersistentDummyObjectHelper}) can stop routing through
     * the polymorphic {@code PersisterHelper.persist(BioAssay)} dispatch. The
     * caller's {@link BioAssay} must reference an already-persistent
     * {@link ubic.gemma.model.expression.arrayDesign.ArrayDesign}; this entry
     * point allocates fresh per-call caches (xdb / taxon) and a {@code null}
     * {@link ArrayDesignsForExperimentCache} — matching the prior
     * {@code persisterHelper.persist(BioAssay)} semantics for stand-alone BA
     * fixtures.
     *
     * @param bioAssay the bioassay to persist
     * @return the persisted entity
     */
    BioAssay persistBioAssay( BioAssay bioAssay );

    /**
     * Persist a standalone {@link BioMaterial}.
     * <p>
     * Persister-shrink S4c: public typed entry point added so test fixtures
     * (notably {@code PersistentDummyObjectHelper}) can stop routing through
     * the polymorphic {@code PersisterHelper.persist(BioMaterial)} dispatch.
     * Allocates fresh per-call caches (xdb / taxon) — matches the prior
     * {@code persisterHelper.persist(BioMaterial)} semantics.
     *
     * @param bioMaterial the biomaterial to persist
     * @return the persisted entity
     */
    BioMaterial persistBioMaterial( BioMaterial bioMaterial );
}
