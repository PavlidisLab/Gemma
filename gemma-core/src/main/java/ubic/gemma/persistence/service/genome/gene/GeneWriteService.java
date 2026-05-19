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
package ubic.gemma.persistence.service.genome.gene;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;

import java.util.Collection;

/**
 * Gene write-side business logic lifted out of
 * {@code GenomePersister} as part of the Phase 3 persisterHelper retirement
 * (Chunk 5.3 PREP). This service owns the NCBI-driven gene-curation logic:
 * NCBI-ID change reconciliation, gene-product GI rotation, bicistronic
 * gene-product reattachment, and gene-product history rotation.
 * <p>
 * <strong>PREP-only status:</strong> the implementation copies the method
 * bodies verbatim from {@code GenomePersister}. Callers (notably
 * {@code NcbiGeneLoader} and {@code ExternalFileGeneLoaderServiceImpl}) are
 * NOT yet rewired; the persister continues to handle production traffic. The
 * cutover and the corresponding deletion of the persister-side methods land
 * in a separate session per the GenomePersister migration plan, after the
 * quirk-pinning tests in this package green up against the new service.
 *
 * @see ubic.gemma.persistence.persister.GenomePersister
 */
public interface GeneWriteService {

    /**
     * Find-or-update entry point for the gene-curation path. If the gene has
     * a persistent identity (id or business-key match), the existing record is
     * updated in place via {@link #updateGene}. Otherwise a new gene is
     * created via {@link #create}.
     * <p>
     * This is the public surface the loaders will eventually call instead of
     * {@code persisterHelper.persistOrUpdate(gene)}.
     *
     * @param gene populated transient or detached gene; gene products,
     *             accessions and physical location are expected to be set
     * @return the persistent gene (existing-updated or newly-created)
     */
    Gene upsert( Gene gene );

    /**
     * Create-path body for a brand-new gene with its products. Handles the
     * orphan-rescue quirk (4.4 in the migration plan): if a transient
     * GeneProduct matches an existing persistent product by business key,
     * that product is reattached to this gene rather than duplicated.
     *
     * @param gene transient gene; will be made persistent
     * @return the persistent gene
     */
    Gene create( Gene gene );

    /**
     * Update-path body for an already-persistent gene. Performs:
     * <ul>
     *   <li>NCBI-ID change check with comma-list previousNcbiGeneId
     *       reconciliation (quirk 4.3)</li>
     *   <li>Official symbol / name / aliases / physical location swap</li>
     *   <li>Per-product update or attach, including bicistronic reattachment
     *       (quirk 4.1)</li>
     *   <li>GI rotation handling via
     *       {@link #handleGeneProductChangedGIs} (quirk 4.2)</li>
     * </ul>
     *
     * @param existingGene persistent gene loaded from the DB
     * @param newGeneInfo  transient gene carrying the new field values
     * @return the persistent gene after update
     */
    Gene updateGene( Gene existingGene, Gene newGeneInfo );

    /**
     * Handle NCBI-GI rotations for the products of an existing gene. Returns
     * the products that should be removed (the caller, currently
     * {@link #updateGene}, finalizes the {@code existingGene.products}
     * collection state and invokes {@link #removeGeneProducts} to clean
     * BLAT/annotation associations).
     * <p>
     * Three sub-cases by current owner of the claimed GI: (a) no current
     * owner — update existing GP's GI; (b) orphan owner — take the GI, delete
     * the orphan; (c) same-gene duplicate — drop the outdated GP; (d)
     * cross-gene owner — reattach to this gene, detach from old.
     *
     * @param existingGene persistent gene whose products are being reconciled
     * @param usedGIs      map of {@code gi -> transient GeneProduct} from
     *                     the incoming update
     * @return products to remove (never null; may be empty)
     */
    Collection<GeneProduct> handleGeneProductChangedGIs( Gene existingGene, java.util.Map<String, GeneProduct> usedGIs );

    /**
     * Update mutable fields on an existing gene product from incoming info.
     * Adds any new accessions ({@link #addAnyNewAccessions}) and refreshes
     * the physical location.
     *
     * @param existingGeneProduct    persistent product to mutate in place
     * @param updatedGeneProductInfo transient product carrying new values
     */
    void updateGeneProduct( GeneProduct existingGeneProduct, GeneProduct updatedGeneProductInfo );

    /**
     * Add accessions from {@code geneProduct} into {@code existing} if not
     * already present (matched by accession string). Implements the
     * gene-product history rotation quirk: old accessions are preserved while
     * new ones accumulate.
     *
     * @param existing    persistent product (will be thawed by the impl)
     * @param geneProduct transient product carrying possibly-new accessions
     */
    void addAnyNewAccessions( GeneProduct existing, GeneProduct geneProduct );

    /**
     * Remove gene products and their associated BLAT + AnnotationAssociation
     * rows. Database entries on the removed products are released only when
     * they are NOT still referenced by a BioSequence (otherwise we would
     * orphan a sequence's accession).
     *
     * @param toRemove products to delete
     */
    void removeGeneProducts( Collection<GeneProduct> toRemove );
}
