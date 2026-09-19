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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import ubic.gemma.core.loader.util.GenBankUtils;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.persister.GenomePersister;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
@Tag("integration")
public class GeneWriteServiceTest extends BaseSpringContextTest5 {

    @Autowired
    private GeneWriteService geneWriteService;

    @Autowired
    private GenomePersister genomePersister;

    @Autowired
    private ChromosomeService chromosomeService;

    @Autowired
    private AnnotationAssociationService annotationAssociationService;

    @Autowired
    private GeneProductService geneProductService;

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

        Gene persisted = this.genomePersister.persistGene( existing );
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

        assertEquals( Integer.valueOf( newNcbiId ), updated.getNcbiGeneId(), "newNcbiGeneId should swap in" );
        assertEquals( String.valueOf( oldNcbiId ), updated.getPreviousNcbiGeneId(),
                "previousNcbiGeneId should record the old id we had" );
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

        Gene persisted = this.genomePersister.persistGene( existing );

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
            assertTrue( expected.getMessage().contains( "previous NCBI id" ),
                    "exception message should mention previous NCBI id" );
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
    @Disabled("Phase 3 Chunk 5.3 cutover test - fixture pending (bicistronic gene-product reattachment)")
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
    public void testGiRotationInPlace() {
        Taxon human = this.getTaxon( "human" );

        String symbol = "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 6 ).toUpperCase();
        String productName = "NM_" + RandomStringUtils.insecure().nextNumeric( 6 );
        String oldGi = RandomStringUtils.insecure().nextNumeric( 9 );
        // distinct GI (lexically guaranteed different; '9...' vs '1...').
        String newGi = "9" + RandomStringUtils.insecure().nextNumeric( 8 );

        Gene existing = Gene.Factory.newInstance();
        existing.setName( symbol );
        existing.setOfficialSymbol( symbol );
        existing.setOfficialName( symbol + " original" );
        existing.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) ) );
        existing.setTaxon( human );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( productName );
        gp.setGene( existing );
        gp.setNcbiGi( oldGi );
        existing.getProducts().add( gp );

        Gene persisted = this.genomePersister.persistGene( existing );
        assertNotNull( persisted.getId() );
        assertEquals( 1, persisted.getProducts().size(), "fixture should have exactly one product before update" );
        assertEquals( oldGi, persisted.getProducts().iterator().next().getNcbiGi() );

        // Incoming "rotated GI" gene info: same name + same NCBI gene id, with one product
        // carrying the SAME name but a different GI. handleGeneProductChangedGIs() should
        // recognize the existing GP by name match and update its NcbiGi in place; no orphan
        // GPs, toRemove should be empty.
        Gene newInfo = Gene.Factory.newInstance();
        newInfo.setName( symbol );
        newInfo.setOfficialSymbol( symbol );
        newInfo.setOfficialName( symbol + " rotated" );
        newInfo.setNcbiGeneId( persisted.getNcbiGeneId() );
        newInfo.setTaxon( human );

        GeneProduct newGp = GeneProduct.Factory.newInstance();
        newGp.setName( productName );
        newGp.setNcbiGi( newGi );
        newGp.setGene( newInfo );
        newInfo.getProducts().add( newGp );

        Gene updated = geneWriteService.updateGene( persisted, newInfo );

        assertNotNull( updated );
        assertEquals( 1, updated.getProducts().size(), "GI rotation must not create or remove products" );
        GeneProduct rotated = updated.getProducts().iterator().next();
        assertEquals( productName, rotated.getName(), "GP name must be unchanged" );
        assertEquals( newGi, rotated.getNcbiGi(), "GP NcbiGi must be rotated to the new value" );
        assertEquals( symbol + " rotated", updated.getOfficialName() );
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
    @Disabled("Phase 3 Chunk 5.3 cutover test - fixture pending (gene-product accession history)")
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

    /**
     * NCBI's gene2accession places a gene on several genomic accessions (for human chromosome 1 in the loader fixture:
     * NC_000001, NC_018912, AC_000133, NT_ and NW_ contigs, CH471 scaffolds), so one chromosome name arrives with
     * several sequences. The chromosome lookup matched the sequence too, so each accession created another chromosome
     * of the same name, and a later lookup without a sequence matched all of them and failed with
     * NonUniqueResultException. Gemma 1.x looked chromosomes up by name and taxon only.
     */
    @Test
    public void testChromosomeIsFoundByNameAndTaxonWhateverItsSequence() {
        Taxon human = this.getTaxon( "human" );
        String chromosomeName = "T" + RandomStringUtils.insecure().nextAlphanumeric( 8 );

        geneWriteService.upsert( geneOn( human, chromosomeName, "NC_" + RandomStringUtils.insecure().nextNumeric( 6 ) ) );
        geneWriteService.upsert( geneOn( human, chromosomeName, "NT_" + RandomStringUtils.insecure().nextNumeric( 6 ) ) );
        geneWriteService.upsert( geneOn( human, chromosomeName, null ) );

        assertEquals( 1, chromosomeService.find( chromosomeName, human ).size() );
    }

    /**
     * A gene product NCBI no longer lists for its gene is deleted with its alignments, which is what takes the probes
     * away from the gene.
     */
    @Test
    public void testUpsertRemovesAProductNoLongerListed() {
        Taxon human = this.getTaxon( "human" );
        Gene gene = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct p1 = gene.getProducts().iterator().next();
        BioSequence sequence = testHelper.getTestPersistentBioSequence( human );
        BlatAssociation alignment = align( sequence, p1, human );
        platformWith( human, sequence );

        geneWriteService.upsert( newInfoWithOnlyANewProduct( gene ) );

        assertFalse( exists( p1 ) );
        assertFalse( exists( alignment ) );
    }

    @Test
    public void testUpsertWithRemovalOffKeepsTheProductAttachedAndReportsIt() {
        Taxon human = this.getTaxon( "human" );
        Gene gene = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct p1 = gene.getProducts().iterator().next();
        BioSequence sequence = testHelper.getTestPersistentBioSequence( human );
        BlatAssociation alignment = align( sequence, p1, human );
        AnnotationAssociation annotation = annotate( sequence, p1 );
        ArrayDesign platform = platformWith( human, sequence );
        Gene newInfo = newInfoWithOnlyANewProduct( gene );
        List<GeneProductChange> changes = new ArrayList<>();

        geneWriteService.upsert( newInfo, false, changes::add );

        assertTrue( exists( p1 ) );
        assertEquals( gene.getId(), geneOf( p1 ), "the product must stay attached to its gene" );
        assertTrue( exists( alignment ) );
        assertTrue( exists( annotation ) );
        assertEquals( 2, countProductsOf( gene ), "the new product is added as usual" );
        assertEquals( 1, changes.size() );
        GeneProductChange change = changes.get( 0 );
        assertEquals( GeneProductChange.Kind.REMOVE, change.kind() );
        assertFalse( change.applied() );
        assertEquals( "human", change.taxon() );
        assertEquals( gene.getNcbiGeneId(), change.geneNcbiId() );
        assertEquals( gene.getOfficialSymbol(), change.geneSymbol() );
        assertEquals( p1.getId(), change.productId() );
        assertEquals( p1.getName(), change.productName() );
        assertEquals( p1.getNcbiGi(), change.productGi() );
        assertEquals( 1, change.blatAssociations() );
        assertEquals( 1, change.annotationAssociations() );
        assertEquals( List.of( platform.getShortName() ), change.platforms() );
    }

    /**
     * A product NCBI now lists for another gene is still moved; the move is reported with the alignments that go with
     * it.
     */
    @Test
    public void testUpsertReportsAProductMovedToAnotherGene() {
        Taxon human = this.getTaxon( "human" );
        Gene from = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct moving = from.getProducts().iterator().next();
        BioSequence sequence = testHelper.getTestPersistentBioSequence( human );
        align( sequence, moving, human );
        ArrayDesign platform = platformWith( human, sequence );
        Gene to = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct staying = to.getProducts().iterator().next();

        Gene newInfo = newInfo( to );
        newInfo.getProducts().add( productInfo( newInfo, staying.getName(), staying.getNcbiGi() ) );
        newInfo.getProducts().add( productInfo( newInfo, moving.getName(), moving.getNcbiGi() ) );
        List<GeneProductChange> changes = new ArrayList<>();

        geneWriteService.upsert( newInfo, false, changes::add );

        assertEquals( to.getId(), geneOf( moving ) );
        assertEquals( 1, changes.size() );
        GeneProductChange change = changes.get( 0 );
        assertEquals( GeneProductChange.Kind.SWITCH, change.kind() );
        assertTrue( change.applied() );
        assertEquals( from.getNcbiGeneId(), change.geneNcbiId() );
        assertEquals( to.getNcbiGeneId(), change.toGeneNcbiId() );
        assertEquals( to.getOfficialSymbol(), change.toGeneSymbol() );
        assertEquals( moving.getId(), change.productId() );
        assertEquals( 1, change.blatAssociations() );
        assertEquals( List.of( platform.getShortName() ), change.platforms() );
    }

    /**
     * A new gene whose product already exists with no gene (an orphan) takes the product over. The move called
     * {@code getProducts()} on the missing previous gene, and the NullPointerException ended the load.
     */
    @Test
    public void testNewGeneTakesOverAnOrphanProduct() {
        Taxon human = this.getTaxon( "human" );
        GeneProduct orphan = GeneProduct.Factory.newInstance();
        orphan.setName( "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        orphan.setNcbiGi( "7" + RandomStringUtils.insecure().nextNumeric( 8 ) );
        orphan = geneProductService.create( orphan );

        Gene newGene = Gene.Factory.newInstance();
        String symbol = "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 6 ).toUpperCase();
        newGene.setName( symbol );
        newGene.setOfficialSymbol( symbol );
        newGene.setOfficialName( symbol );
        newGene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) ) + 40_000_000 );
        newGene.setTaxon( human );
        newGene.getProducts().add( productInfo( newGene, orphan.getName(), orphan.getNcbiGi() ) );

        Gene created = geneWriteService.upsert( newGene );

        assertEquals( created.getId(), geneOf( orphan ) );
    }

    /**
     * A new gene takes over a product Gemma already has, and has another product Gemma does not.
     *
     * <p>Resolving a product's accessions runs a query when the accession's external database is not one the new
     * products carry, and a taken-over product's accessions come from the database, not from NCBI's file. The query
     * flushes the session. While the still-unsaved products were attached to the saved gene before they were
     * resolved, that flush reached a product whose physical location still pointed at the unsaved Chromosome the
     * gene was converted with, and Hibernate threw
     * {@code TransientObjectException: persistent instance references an unsaved transient instance of Chromosome}.
     * In a real run that ends the load; 24 human genes hit it in the 2026-09-19 dry run.</p>
     */
    @Test
    public void testNewGeneTakesOverAProductWhileAnotherProductIsStillUnsaved() {
        Taxon human = this.getTaxon( "human" );

        // The chromosome has to be one Gemma already has. When it is new, the gene's own Chromosome instance is the
        // one saved, so the products that share it are never left pointing at an unsaved instance.
        String chromosomeName = "T" + RandomStringUtils.insecure().nextAlphanumeric( 8 );
        geneWriteService.upsert( geneOn( human, chromosomeName, null ) );

        // create() resolves the products in the order of a HashSet, which for a GeneProduct is its GI's. The failure
        // needs an unresolved product behind the one whose accessions are queried, so pin the order.
        String takenOverGi = giInEarlyBucket();
        String newGi = giInLateBucket();

        // Gemma gives a product it maps from the known-gene track an Ensembl accession
        // (GoldenPathSequenceAnalysis), and NCBI's file only ever gives one from GenBank.
        Gene otherGene = persistGene( human, "NR_" + RandomStringUtils.insecure().nextNumeric( 6 ), takenOverGi,
                externalDatabase( "Ensembl" ) );
        GeneProduct takenOver = otherGene.getProducts().iterator().next();

        Gene newGene = geneOn( human, chromosomeName, null );
        Chromosome converted = newGene.getPhysicalLocation().getChromosome();
        fromNcbi( newGene, takenOver.getName(), takenOverGi, converted );
        fromNcbi( newGene, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ), newGi, converted );
        assertEquals( takenOverGi, new HashSet<>( newGene.getProducts() ).iterator().next().getNcbiGi(),
                "the taken-over product has to be resolved first for this to exercise the flush" );

        Gene created = geneWriteService.upsert( newGene );

        assertEquals( created.getId(), geneOf( takenOver ) );
        assertEquals( 2, countProductsOf( created ) );
    }

    @Test
    public void testReplayWithoutPlatformsDeletesTheProductAndItsAssociations() {
        Taxon human = this.getTaxon( "human" );
        Gene gene = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct p1 = gene.getProducts().iterator().next();
        BioSequence sequence = testHelper.getTestPersistentBioSequence( human );
        BlatAssociation alignment = align( sequence, p1, human );
        AnnotationAssociation annotation = annotate( sequence, p1 );
        ArrayDesign platform = platformWith( human, sequence );
        GeneProductChange removal = keptRemoval( gene );

        GeneProductRemovalOutcome outcome = geneWriteService.replayGeneProductRemoval( removal, null, false );

        assertFalse( outcome.isSkipped() );
        assertTrue( outcome.productDeleted() );
        assertEquals( 1, outcome.blatAssociationsDeleted() );
        assertEquals( 1, outcome.annotationAssociationsDeleted() );
        assertEquals( List.of( platform.getShortName() ), outcome.platforms() );
        assertFalse( exists( p1 ) );
        assertFalse( exists( alignment ) );
        assertFalse( exists( annotation ) );
        assertEquals( 1, countProductsOf( gene ), "only the product NCBI still lists is left" );
    }

    /**
     * Limited to one platform, the associations of the other platform's elements stay, and so does the product, until
     * the replay for that platform removes the last of them.
     */
    @Test
    public void testReplayLimitedToPlatformsKeepsTheProductUntilItsLastAssociationGoes() {
        Taxon human = this.getTaxon( "human" );
        Gene gene = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct p1 = gene.getProducts().iterator().next();
        BioSequence sequence1 = testHelper.getTestPersistentBioSequence( human );
        BioSequence sequence2 = testHelper.getTestPersistentBioSequence( human );
        BlatAssociation alignment1 = align( sequence1, p1, human );
        BlatAssociation alignment2 = align( sequence2, p1, human );
        ArrayDesign platform1 = platformWith( human, sequence1 );
        ArrayDesign platform2 = platformWith( human, sequence2 );
        GeneProductChange removal = keptRemoval( gene );

        GeneProductRemovalOutcome first = geneWriteService.replayGeneProductRemoval( removal, List.of( platform1 ), false );

        assertFalse( first.isSkipped() );
        assertFalse( first.productDeleted() );
        assertEquals( 1, first.blatAssociationsDeleted() );
        assertEquals( List.of( platform1.getShortName() ), first.platforms() );
        assertFalse( exists( alignment1 ) );
        assertTrue( exists( alignment2 ), "the other platform's association must stay" );
        assertTrue( exists( p1 ) );
        assertEquals( gene.getId(), geneOf( p1 ) );

        GeneProductRemovalOutcome second = geneWriteService.replayGeneProductRemoval( removal, List.of( platform2 ), false );

        assertTrue( second.productDeleted() );
        assertEquals( 1, second.blatAssociationsDeleted() );
        assertFalse( exists( alignment2 ) );
        assertFalse( exists( p1 ) );
    }

    @Test
    public void testReplaySkipsARemovalWhoseGiChanged() {
        Taxon human = this.getTaxon( "human" );
        Gene gene = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct p1 = gene.getProducts().iterator().next();
        BioSequence sequence = testHelper.getTestPersistentBioSequence( human );
        BlatAssociation alignment = align( sequence, p1, human );
        GeneProductChange removal = keptRemoval( gene );
        GeneProductChange withOtherGi = new GeneProductChange( removal.kind(), removal.applied(), removal.taxon(),
                removal.geneNcbiId(), removal.geneSymbol(), null, null, removal.productId(), removal.productName(),
                "1" + removal.productGi(), removal.blatAssociations(), removal.annotationAssociations(), removal.platforms() );

        GeneProductRemovalOutcome outcome = geneWriteService.replayGeneProductRemoval( withOtherGi, null, false );

        assertEquals( GeneProductRemovalOutcome.SkipReason.GI_CHANGED, outcome.skipReason() );
        assertTrue( exists( p1 ) );
        assertTrue( exists( alignment ) );
    }

    @Test
    public void testReplayDryRunDeletesNothing() {
        Taxon human = this.getTaxon( "human" );
        Gene gene = persistGene( human, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ) );
        GeneProduct p1 = gene.getProducts().iterator().next();
        BioSequence sequence = testHelper.getTestPersistentBioSequence( human );
        BlatAssociation alignment = align( sequence, p1, human );
        ArrayDesign platform = platformWith( human, sequence );
        GeneProductChange removal = keptRemoval( gene );

        GeneProductRemovalOutcome outcome = geneWriteService.replayGeneProductRemoval( removal, null, true );

        assertTrue( outcome.productDeleted() );
        assertEquals( 1, outcome.blatAssociationsDeleted() );
        assertEquals( List.of( platform.getShortName() ), outcome.platforms() );
        assertTrue( exists( p1 ) );
        assertTrue( exists( alignment ) );
        assertEquals( gene.getId(), geneOf( p1 ) );
    }

    /**
     * Run the gene through an upsert with removal off, listing a new product instead of its only one, and return the
     * removal that was not made.
     */
    private GeneProductChange keptRemoval( Gene gene ) {
        List<GeneProductChange> changes = new ArrayList<>();
        geneWriteService.upsert( newInfoWithOnlyANewProduct( gene ), false, changes::add );
        assertEquals( 1, changes.size() );
        return changes.get( 0 );
    }

    private Gene persistGene( Taxon taxon, String productName ) {
        String symbol = "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 6 ).toUpperCase();
        Gene gene = Gene.Factory.newInstance();
        gene.setName( symbol );
        gene.setOfficialSymbol( symbol );
        gene.setOfficialName( symbol );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) ) + 30_000_000 );
        gene.setTaxon( taxon );
        gene.getProducts().add( productInfo( gene, productName, RandomStringUtils.insecure().nextNumeric( 9 ) ) );
        return genomePersister.persistGene( gene );
    }

    /**
     * A gene with one product carrying an accession from the given database, as a product Gemma already holds would.
     */
    private Gene persistGene( Taxon taxon, String productName, String gi, ExternalDatabase accessionSource ) {
        String symbol = "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 6 ).toUpperCase();
        Gene gene = Gene.Factory.newInstance();
        gene.setName( symbol );
        gene.setOfficialSymbol( symbol );
        gene.setOfficialName( symbol );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) ) + 50_000_000 );
        gene.setTaxon( taxon );
        GeneProduct product = productInfo( gene, productName, gi );
        product.getAccessions().add( accession( productName, accessionSource ) );
        gene.getProducts().add( product );
        return genomePersister.persistGene( gene );
    }

    /**
     * A product as {@link ubic.gemma.core.loader.genome.gene.ncbi.NcbiGeneConverter} builds it: a GenBank accession
     * and a location sharing the gene's own, still-unsaved, chromosome.
     */
    private GeneProduct fromNcbi( Gene gene, String name, String gi, Chromosome chromosome ) {
        GeneProduct product = productInfo( gene, name, gi );
        product.getAccessions().add( accession( name, GenBankUtils.getGenBank() ) );
        product.setPhysicalLocation( PhysicalLocation.Factory.newInstance( chromosome ) );
        gene.getProducts().add( product );
        return product;
    }

    private DatabaseEntry accession( String accession, ExternalDatabase source ) {
        DatabaseEntry entry = DatabaseEntry.Factory.newInstance();
        entry.setAccession( accession );
        entry.setExternalDatabase( source );
        return entry;
    }

    private ExternalDatabase externalDatabase( String name ) {
        ExternalDatabase database = externalDatabaseService.findByName( name );
        if ( database != null ) {
            return database;
        }
        return externalDatabaseService.create( ExternalDatabase.Factory.newInstance( name, DatabaseType.OTHER ) );
    }

    /**
     * {@code create()} collects the gene's products in a HashSet, so it resolves them in the order of
     * {@link GeneProduct#hashCode()}, which is the GI's. These pick a GI for the first and the last of 16 buckets.
     */
    private String giInEarlyBucket() {
        return giInBucket( 0, 3 );
    }

    private String giInLateBucket() {
        return giInBucket( 12, 15 );
    }

    private String giInBucket( int first, int last ) {
        while ( true ) {
            String gi = "9" + RandomStringUtils.insecure().nextNumeric( 8 );
            int bucket = ( gi.hashCode() ^ ( gi.hashCode() >>> 16 ) ) & 15;
            if ( bucket >= first && bucket <= last ) {
                return gi;
            }
        }
    }

    private Gene newInfo( Gene gene ) {
        Gene newInfo = Gene.Factory.newInstance();
        newInfo.setName( gene.getName() );
        newInfo.setOfficialSymbol( gene.getOfficialSymbol() );
        newInfo.setOfficialName( gene.getOfficialName() );
        newInfo.setNcbiGeneId( gene.getNcbiGeneId() );
        newInfo.setTaxon( gene.getTaxon() );
        return newInfo;
    }

    private Gene newInfoWithOnlyANewProduct( Gene gene ) {
        Gene newInfo = newInfo( gene );
        newInfo.getProducts().add( productInfo( newInfo, "NM_" + RandomStringUtils.insecure().nextNumeric( 6 ),
                "8" + RandomStringUtils.insecure().nextNumeric( 8 ) ) );
        return newInfo;
    }

    private GeneProduct productInfo( Gene gene, String name, String gi ) {
        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( name );
        gp.setNcbiGi( gi );
        gp.setGene( gene );
        return gp;
    }

    private BlatAssociation align( BioSequence sequence, GeneProduct product, Taxon taxon ) {
        BlatAssociation alignment = BlatAssociation.Factory.newInstance();
        alignment.setBioSequence( sequence );
        alignment.setGeneProduct( product );
        alignment.setBlatResult( testHelper.getTestPersistentBlatResult( sequence, taxon ) );
        return genomePersister.persistBlatAssociation( alignment );
    }

    private AnnotationAssociation annotate( BioSequence sequence, GeneProduct product ) {
        AnnotationAssociation annotation = AnnotationAssociation.Factory.newInstance();
        annotation.setBioSequence( sequence );
        annotation.setGeneProduct( product );
        return annotationAssociationService.create( annotation );
    }

    /**
     * A platform with one element for each sequence.
     */
    private ArrayDesign platformWith( Taxon taxon, BioSequence... sequences ) {
        ArrayDesign platform = ArrayDesign.Factory.newInstance();
        platform.setShortName( "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        platform.setName( platform.getShortName() );
        platform.setTechnologyType( TechnologyType.ONECOLOR );
        platform.setPrimaryTaxon( taxon );
        for ( BioSequence sequence : sequences ) {
            CompositeSequence element = CompositeSequence.Factory.newInstance();
            element.setName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
            element.setArrayDesign( platform );
            element.setBiologicalCharacteristic( sequence );
            platform.getCompositeSequences().add( element );
        }
        return arrayDesignPersister.persistArrayDesign( platform );
    }

    private boolean exists( GeneProduct product ) {
        return getJdbcTemplate().queryForObject( "select count(*) from CHROMOSOME_FEATURE where ID = ?", Integer.class, product.getId() ) > 0;
    }

    private boolean exists( BioSequence2GeneProduct association ) {
        return getJdbcTemplate().queryForObject( "select count(*) from BIO_SEQUENCE2_GENE_PRODUCT where ID = ?", Integer.class, association.getId() ) > 0;
    }

    @Nullable
    private Long geneOf( GeneProduct product ) {
        return getJdbcTemplate().queryForObject( "select GENE_FK from CHROMOSOME_FEATURE where ID = ?", Long.class, product.getId() );
    }

    private int countProductsOf( Gene gene ) {
        return getJdbcTemplate().queryForObject( "select count(*) from CHROMOSOME_FEATURE where GENE_FK = ?", Integer.class, gene.getId() );
    }

    private Gene geneOn( Taxon taxon, String chromosomeName, @Nullable String chromosomeSequenceName ) {
        String symbol = "TEST_" + RandomStringUtils.insecure().nextAlphabetic( 6 ).toUpperCase();
        Gene gene = Gene.Factory.newInstance();
        gene.setName( symbol );
        gene.setOfficialSymbol( symbol );
        gene.setOfficialName( symbol );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 7 ) ) + 20_000_000 );
        gene.setTaxon( taxon );
        Chromosome chromosome = Chromosome.Factory.newInstance( chromosomeName, taxon );
        if ( chromosomeSequenceName != null ) {
            BioSequence sequence = BioSequence.Factory.newInstance( chromosomeSequenceName, taxon );
            sequence.setType( SequenceType.WHOLE_CHROMOSOME );
            chromosome.setSequence( sequence );
        }
        gene.setPhysicalLocation( PhysicalLocation.Factory.newInstance( chromosome ) );
        return gene;
    }
}
