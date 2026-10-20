/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.expression.biomaterial;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see BioMaterial
 */
public interface BioMaterialDao extends BaseVoEnabledDao<BioMaterial, BioMaterialValueObject> {

    BioMaterial copy( BioMaterial bioMaterial );

    /**
     * Find all the sub-biomaterials for a given biomaterial related by {@link BioMaterial#getSourceBioMaterial()}.
     * @param direct if true, only direct sub-biomaterials are retained, otherwise the entire hierarchy is visited
     *               recursively.
     */
    List<BioMaterial> findSubBioMaterials( BioMaterial bioMaterial, boolean direct );

    /**
     * Find all the sub-biomaterials for a given biomaterial related by {@link BioMaterial#getSourceBioMaterial()}.
     * @param bioMaterials a collection of biomaterials to visit
     * @param direct       if true, only direct sub-biomaterials are retained, otherwise the entire hierarchy is visited
     *                     recursively.
     */
    List<BioMaterial> findSubBioMaterials( Collection<BioMaterial> bioMaterials, boolean direct );

    Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment );

    Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor );

    /**
     * Obtain all the experiments a biomaterial is used in from its hierarchy.
     * <p>
     * This also includes experiments that are using this via one of their parent?
     */
    Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> getExpressionExperiments( BioMaterial bm );

    /**
     * Batched counterpart to the per-{@link BioMaterial} {@link ubic.gemma.persistence.util.Thaws#thawBioMaterial}
     * pattern: warm the source-{@link BioMaterial} chain plus the per-row lazy associations
     * (sourceTaxon, treatments, factorValues.experimentalFactor) for the {@link BioMaterial}s
     * referenced by every {@link BioAssay} in {@code bas}, using a small constant number of
     * SQL round-trips instead of the per-BA, per-chain-level {@code Hibernate.initialize}
     * pattern.
     * <p>
     * Implementation:
     * <ol>
     *   <li>Seed with the BAs' {@code sampleUsed} IDs (the eager-fetched {@link BioMaterial}
     *       on each BA — no extra query).</li>
     *   <li>Walk the source chain breadth-first via one ID-only HQL per chain level — terminates
     *       when a level yields no new BMs.</li>
     *   <li>Issue one HQL fetch-joining {@code treatments} on the union of chain BM IDs.</li>
     *   <li>Issue one HQL fetch-joining {@code factorValues.experimentalFactor} on the union of
     *       chain BM IDs.</li>
     * </ol>
     * Total query count is {@code 1 + chainDepth} (chain walk) + 2 (collection fetches),
     * independent of {@link BioAssay} count. Replaces the O(N&times;depth) per-row
     * {@link ubic.gemma.persistence.util.Thaws#thawBioMaterial} round-trips that dominated
     * the {@code /datasets/{id}/samples} endpoint (PERF_PROBE_REPORT_ROUND2 finding #2:
     * ~15.8 s projected for a 92-BM EE vs ~150 ms batched).
     * <p>
     * No-op when {@code bas} is empty. Mutates the BioMaterial entities (and their source-chain
     * ancestors) attached to the current session — does not return them; callers continue to
     * access the chain via {@code bioAssay.getSampleUsed()...}.
     */
    void thawBioMaterialsForBioAssays( Collection<BioAssay> bas );
}
