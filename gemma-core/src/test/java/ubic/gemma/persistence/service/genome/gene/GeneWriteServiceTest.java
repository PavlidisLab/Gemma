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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.IntegrationTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Quirk-pinning tests for {@link GeneWriteService}. Each test pins one of the
 * four hardened branches that {@link ubic.gemma.persistence.persister.GenomePersister}
 * is known to handle and that the Chunk 5.3 cutover must preserve:
 *
 * <ol>
 *   <li>Drosophila bicistronic reattachment — 2 GeneProducts sharing one
 *       accession (Lcp65Ab1 / Lcp65Ab2 in real data).</li>
 *   <li>NCBI GI rotation — same Gene, GeneProduct's GI rotated to a new value.</li>
 *   <li>NCBI ID merge — comma-list previousNcbiGeneId (MTUS2-AS1 in real data).</li>
 *   <li>Gene-product history rotation — GP gains a new accession while keeping
 *       old one in history.</li>
 * </ol>
 *
 * <p>This is PREP work: the service is wired but not yet invoked by production
 * callers. Tests intentionally call {@link GeneWriteService#updateGene} directly
 * to pin the contract.</p>
 *
 * <p>Tests that need a fixture richer than the lightweight in-memory shape are
 * stubbed with {@code @Ignore} and a documented hand-off note for the cutover
 * agent; the test scaffold (autowires, taxon setup, naming convention) is in
 * place so re-enabling them is mechanical.</p>
 */
@Category(IntegrationTest.class)
public class GeneWriteServiceTest extends BaseSpringContextTest {

    @Autowired
    private GeneWriteService geneWriteService;

    /**
     * Quirk 4.3 (migration plan): NCBI ID merge with comma-separated
     * previousNcbiGeneId. NCBI sometimes merges two formerly distinct gene
     * records and the new record's previousNcbiGeneId field carries BOTH
     * old IDs as a comma-list. The service must accept the update when any
     * comma-separated entry matches the existing record's NCBI ID, and
     * swap the IDs (old -> previousNcbiGeneId, new -> ncbiGeneId).
     *
     * Real-world driver: MTUS2-AS1 (human) — merged from LOC728437 +
     * LOC731614.
     */
    @Test
    public void testNcbiIdMergeWithCommaListPreviousId() {
        Taxon human = this.getTaxon( "human" );

        // Existing gene with the OLD NCBI id (one of two pre-merge ids).
        int oldNcbiId = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) );
        int otherOldNcbiId = oldNcbiId + 1; // the "other" pre-merge id, not in Gemma
        int newNcbiId = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) ) + 10_000_000;
        String symbol = "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 6 ).toUpperCase();

        Gene existing = Gene.Factory.newInstance();
        existing.setName( symbol );
        existing.setOfficialSymbol( symbol );
        existing.setOfficialName( symbol + " original" );
        existing.setNcbiGeneId( oldNcbiId );
        existing.setTaxon( human );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( symbol + "_gp1" );
        gp.setGene( existing );
        gp.setNcbiGi( RandomStringUtils.insecure().nextNumeric( 8 ) );
        existing.getProducts().add( gp );

        Gene persisted = ( Gene ) this.persisterHelper.persist( existing );
        assertNotNull( persisted.getId() );
        assertEquals( Integer.valueOf( oldNcbiId ), persisted.getNcbiGeneId() );

        // Incoming "merged" gene info: new NCBI id, previousNcbiGeneId =
        // "<oldNcbiId>,<otherOldNcbiId>" — a comma-list that includes our
        // current id as one of two entries.
        Gene newInfo = Gene.Factory.newInstance();
        newInfo.setName( symbol );
        newInfo.setOfficialSymbol( symbol );
        newInfo.setOfficialName( symbol + " merged" );
        newInfo.setNcbiGeneId( newNcbiId );
        newInfo.setPreviousNcbiGeneId( oldNcbiId + "," + otherOldNcbiId );
        newInfo.setTaxon( human );
        // Keep the same product so the rest of updateGene() is a no-op.
        GeneProduct newGp = GeneProduct.Factory.newInstance();
        newGp.setName( gp.getName() );
        newGp.setNcbiGi( gp.getNcbiGi() );
        newGp.setGene( newInfo );
        newInfo.getProducts().add( newGp );

        Gene updated = geneWriteService.updateGene( persisted, newInfo );

        assertEquals( "newNcbiGeneId should swap in", Integer.valueOf( newNcbiId ), updated.getNcbiGeneId() );
        assertEquals( "previousNcbiGeneId should record the old id we had",
                String.valueOf( oldNcbiId ), updated.getPreviousNcbiGeneId() );
        assertEquals( symbol + " merged", updated.getOfficialName() );
    }

    /**
     * Quirk 4.3 negative case: if previousNcbiGeneId does NOT contain our
     * existing id, the service must refuse the update with
     * IllegalStateException — we never silently overwrite an NCBI ID we
     * didn't expect.
     */
    @Test
    public void testNcbiIdMergeRefusedWhenPreviousIdDoesNotMatch() {
        Taxon human = this.getTaxon( "human" );

        int oldNcbiId = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) );
        int unrelatedId = oldNcbiId + 999_999;
        int newNcbiId = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) ) + 10_000_000;
        String symbol = "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 6 ).toUpperCase();

        Gene existing = Gene.Factory.newInstance();
        existing.setName( symbol );
        existing.setOfficialSymbol( symbol );
        existing.setNcbiGeneId( oldNcbiId );
        existing.setTaxon( human );
        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( symbol + "_gp1" );
        gp.setGene( existing );
        gp.setNcbiGi( RandomStringUtils.insecure().nextNumeric( 8 ) );
        existing.getProducts().add( gp );

        Gene persisted = ( Gene ) this.persisterHelper.persist( existing );

        Gene newInfo = Gene.Factory.newInstance();
        newInfo.setName( symbol );
        newInfo.setOfficialSymbol( symbol );
        newInfo.setNcbiGeneId( newNcbiId );
        // previousNcbiGeneId does NOT include our current id.
        newInfo.setPreviousNcbiGeneId( String.valueOf( unrelatedId ) );
        newInfo.setTaxon( human );
        newInfo.setProducts( new HashSet<>() );

        try {
            geneWriteService.updateGene( persisted, newInfo );
            fail( "Expected IllegalStateException when previousNcbiGeneId does not list our existing id" );
        } catch ( IllegalStateException expected ) {
            assertTrue( "exception message should mention previous NCBI id",
                    expected.getMessage().contains( "previous NCBI id" ) );
        }
    }

    /**
     * Quirk 4.1 (migration plan): drosophila bicistronic — a GeneProduct
     * found-by-BK to belong to gene A is reattached to gene B during a
     * gene update. The service must remove the product from gene A's
     * products collection and add it to gene B's. If gene A ends up
     * empty, a warn is logged (no exception).
     *
     * Cutover hand-off note: this needs a populated fixture with TWO
     * pre-existing genes sharing a GeneProduct accession in Gemma's DB
     * shape. The current PersistentDummyObjectHelper.getTestPersistentGene()
     * doesn't expose accession control. Populate by hand or extend the
     * helper. Real-world driver: GenBank BT099970 / GI 1108657489 across
     * Lcp65Ab1 + Lcp65Ab2 in drosophila.
     */
    @Test
    @Ignore("Phase 3 Chunk 5.3 cutover test - fixture pending (bicistronic gene-product reattachment)")
    public void testDrosophilaBicistronicReattachment() {
        // Setup expected:
        //   - taxon: drosophila
        //   - gene A (Lcp65Ab1) with GP1 (name='BT099970', GI='289666832')
        //   - gene B (Lcp65Ab2) — exists, no products yet (or with unrelated products)
        //   - persist both
        // Action:
        //   - construct newInfo for gene B with a transient GP carrying
        //     name='BT099970', GI='1108657489'
        //   - geneProductDao.find(newGp) will resolve to GP1 (on gene A)
        //   - geneWriteService.updateGene(geneB_persistent, newInfo) should
        //     reattach GP1 from A->B and log a warn
        // Assert:
        //   - reload gene A: GP1 not in products
        //   - reload gene B: GP1 in products, with NcbiGi updated to '1108657489'
        fail( "fixture not yet written - see javadoc" );
    }

    /**
     * Quirk 4.2 (migration plan): GI rotation — same Gene, same product
     * name, GeneProduct's GI rotates to a new value because NCBI bumped
     * the sequence version. This is the COMMON case (no cross-gene
     * conflict, no orphan): handleGeneProductChangedGIs() should
     * recognize the existing GP by name match and update its NcbiGi in
     * place, returning an EMPTY toRemove collection.
     *
     * Cutover hand-off note: the service-direct test path requires
     * loading a persistent gene then constructing an updated `Gene`
     * carrying a single product with the same name but a different GI.
     * The straightforward fixture is to use getTestPersistentGene(taxon)
     * which already gives us a single product, then read back its name
     * and build the updater. Stubbed until the cutover session because
     * verifying the toRemove-collection state requires either exposing
     * handleGeneProductChangedGIs return value directly (already done in
     * the interface) or carefully observing existingGene.products
     * post-state across the transaction boundary.
     */
    @Test
    @Ignore("Phase 3 Chunk 5.3 cutover test - fixture pending (GI rotation in-place)")
    public void testGiRotationInPlace() {
        // Setup:
        //   - persist gene with one GP (name='NM_001234', GI='100000001')
        // Action:
        //   - construct newInfo with same gene id and one GP
        //     (name='NM_001234', GI='100000002')
        //   - updateGene(persisted, newInfo)
        // Assert:
        //   - reload gene: single GP, name='NM_001234', GI now '100000002'
        //   - no GP rows orphaned
        fail( "fixture not yet written - see javadoc" );
    }

    /**
     * Gene-product history rotation: when a GeneProduct already in the DB
     * receives an updated DatabaseEntry accession, the new one must be
     * APPENDED to the accessions collection, not replace the old one.
     * This preserves the cross-version history that downstream callers
     * (gemma-cli array-design probe re-mappings) depend on.
     *
     * Cutover hand-off note: requires constructing a GP with one or more
     * DatabaseEntry accessions (and the matching ExternalDatabase), then
     * a newInfo GP carrying an ADDITIONAL accession on top. The service's
     * addAnyNewAccessions() should result in BOTH accessions surviving.
     * Stubbed because building a populated DatabaseEntry/ExternalDatabase
     * pair through the persisterHelper requires either the
     * BioSequenceFactory or directly poking the DAO — see
     * BioSequencePersistTest for the closest existing pattern.
     */
    @Test
    @Ignore("Phase 3 Chunk 5.3 cutover test - fixture pending (gene-product accession history)")
    public void testGeneProductAccessionHistoryRotation() {
        // Setup:
        //   - persist gene with GP carrying accession A1 (db=GenBank)
        // Action:
        //   - construct newInfo with GP carrying accessions {A1, A2}
        //   - updateGene(persisted, newInfo) -> updateGeneProduct path
        //     -> addAnyNewAccessions
        // Assert:
        //   - GP.accessions size == 2
        //   - both A1 and A2 present
        fail( "fixture not yet written - see javadoc" );
    }
}
