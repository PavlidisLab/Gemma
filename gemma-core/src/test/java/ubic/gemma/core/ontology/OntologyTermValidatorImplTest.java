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
 */
package ubic.gemma.core.ontology;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.ontology.model.OntologyProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.ols.OlsTerm;
import ubic.gemma.core.ontology.ols.OlsTermResolver;
import ubic.gemma.core.ontology.ols.OlsUnavailableException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Gene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OntologyTermValidatorImplTest {

    private OntologyService ontologyService;
    private OlsTermResolver olsTermResolver;
    private ubic.gemma.persistence.service.genome.gene.GeneService geneService;
    private ubic.gemma.core.ontology.ncbi.NcbiGeneResolver ncbiGeneResolver;
    private OntologyTermValidatorImpl validator;

    @BeforeEach
    public void setUp() {
        ontologyService = mock( OntologyService.class );
        olsTermResolver = mock( OlsTermResolver.class );
        geneService = mock( ubic.gemma.persistence.service.genome.gene.GeneService.class );
        ncbiGeneResolver = mock( ubic.gemma.core.ontology.ncbi.NcbiGeneResolver.class );
        validator = new OntologyTermValidatorImpl( ontologyService, olsTermResolver, geneService, ncbiGeneResolver );
        ReflectionTestUtils.setField( validator, "timeoutMs", 5000L );
    }

    /** Stub a local ontology hit for a URI. */
    private void localResolves( String uri, String label ) throws Exception {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getLabel() ).thenReturn( label );
        when( ontologyService.getTerm( eq( uri ), anyLong(), any( TimeUnit.class ) ) ).thenReturn( term );
    }

    private Characteristic characteristic( String category, String categoryUri, String value, String valueUri ) {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( category );
        c.setCategoryUri( categoryUri );
        c.setValue( value );
        c.setValueUri( valueUri );
        return c;
    }

    @Test
    public void testExactMatchPasses() throws Exception {
        localResolves( "http://x/asthma", "asthma" );
        Characteristic c = characteristic( null, null, "asthma", "http://x/asthma" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
    }

    @Test
    public void testCaseNearMatchAcceptedAndCanonicalized() throws Exception {
        localResolves( "http://x/asthma", "asthma" );
        Characteristic c = characteristic( null, null, "Asthma", "http://x/asthma" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "asthma", c.getValue() ); // rewritten to canonical
    }

    @Test
    public void testWhitespaceNearMatchAcceptedAndCanonicalized() throws Exception {
        localResolves( "http://x/t", "Homo sapiens" );
        Characteristic c = characteristic( null, null, "Homo  sapiens ", "http://x/t" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "Homo sapiens", c.getValue() );
    }

    @Test
    public void testCaseNearMatchRecordedAsCanonicalization() throws Exception {
        localResolves( "http://x/asthma", "asthma" );
        Characteristic c = characteristic( null, null, "Asthma", "http://x/asthma" );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertTrue( validator.validateAndCanonicalize( c, canons ).isEmpty() );
        assertEquals( 1, canons.size() );
        assertEquals( "value", canons.get( 0 ).getSlot() );
        assertEquals( "Asthma", canons.get( 0 ).getSubmittedLabel() );
        assertEquals( "asthma", canons.get( 0 ).getCanonicalLabel() );
    }

    @Test
    public void testBlankFillRecordedAsCanonicalization() throws Exception {
        localResolves( "http://x/asthma", "asthma" );
        Characteristic c = characteristic( null, null, null, "http://x/asthma" );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertTrue( validator.validateAndCanonicalize( c, canons ).isEmpty() );
        assertEquals( 1, canons.size() );
        assertEquals( "value", canons.get( 0 ).getSlot() );
        assertNull( canons.get( 0 ).getSubmittedLabel() );
        assertEquals( "asthma", canons.get( 0 ).getCanonicalLabel() );
    }

    @Test
    public void testExactMatchRecordsNoCanonicalization() throws Exception {
        localResolves( "http://x/asthma", "asthma" );
        Characteristic c = characteristic( null, null, "asthma", "http://x/asthma" );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertTrue( validator.validateAndCanonicalize( c, canons ).isEmpty() );
        assertTrue( canons.isEmpty() );
    }

    @Test
    public void testWrongBaseTgemoNormalizedResolvesAndCanonicalizesUri() throws Exception {
        // real TGEMO id sent on the OBO PURL base — must ground under the Gemma base, not report as fabricated
        String canonicalUri = "http://gemma.msl.ubc.ca/ont/TGEMO_00166";
        localResolves( canonicalUri, "delivered at dose" );
        Characteristic c = characteristic( null, null, "delivered at dose", "http://purl.obolibrary.org/obo/TGEMO_00166" );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertTrue( validator.validateAndCanonicalize( c, canons ).isEmpty() );
        assertEquals( canonicalUri, c.getValueUri() ); // URI rewritten in place
        assertEquals( 1, canons.size() );
        assertEquals( "http://purl.obolibrary.org/obo/TGEMO_00166", canons.get( 0 ).getSubmittedUri() );
        assertEquals( canonicalUri, canons.get( 0 ).getCanonicalUri() );
        assertEquals( "delivered at dose", canons.get( 0 ).getCanonicalLabel() );
    }

    @Test
    public void testDoubleMangledTgemoNormalized() throws Exception {
        String canonicalUri = "http://gemma.msl.ubc.ca/ont/TGEMO_00166";
        localResolves( canonicalUri, "delivered at dose" );
        Characteristic c = characteristic( null, null, "delivered at dose",
                "http://purl.obolibrary.org/obo/http_//gemma.msl.ubc.ca/ont/TGEMO_00166" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( canonicalUri, c.getValueUri() );
    }

    @Test
    public void testCanonicalTgemoUriUnchanged() throws Exception {
        String canonicalUri = "http://gemma.msl.ubc.ca/ont/TGEMO_00166";
        localResolves( canonicalUri, "delivered at dose" );
        Characteristic c = characteristic( null, null, "delivered at dose", canonicalUri );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertTrue( validator.validateAndCanonicalize( c, canons ).isEmpty() );
        assertEquals( canonicalUri, c.getValueUri() );
        assertTrue( canons.isEmpty() ); // already canonical + exact label → nothing rewritten
    }

    @Test
    public void testFabricatedTgemoStillRejectedAfterNormalization() throws Exception {
        // normalization rescues real ids, not invented ones: TGEMO_99999 resolves nowhere even on the Gemma base
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( anyString() ) ).thenReturn( null );
        Characteristic c = characteristic( null, null, "made up", "http://purl.obolibrary.org/obo/TGEMO_99999" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.URI_UNRESOLVED, v.get( 0 ).getReason() );
    }

    @Test
    public void testMismatchRecordsNoCanonicalization() throws Exception {
        localResolves( "http://x/00166", "delivered at dose" );
        Characteristic c = characteristic( null, null, "has_genotype", "http://x/00166" );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertEquals( 1, validator.validateAndCanonicalize( c, canons ).size() );
        assertTrue( canons.isEmpty() ); // a rejected slot is not a canonicalization
    }

    @Test
    public void testLabelMismatchRejected() throws Exception {
        // the TGEMO_00166 case: real URI, wrong concept
        localResolves( "http://x/00166", "delivered at dose" );
        Characteristic c = characteristic( null, null, "has_genotype", "http://x/00166" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.LABEL_MISMATCH, v.get( 0 ).getReason() );
        assertEquals( "value", v.get( 0 ).getSlot() );
        assertEquals( "delivered at dose", v.get( 0 ).getResolvedLabel() );
        assertEquals( "has_genotype", v.get( 0 ).getSubmittedLabel() );
    }

    @Test
    public void testUnresolvedLocallyThenOlsResolves() throws Exception {
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( "http://x/efo" ) ).thenReturn( new OlsTerm( "http://x/efo", "asthma" ) );
        Characteristic c = characteristic( null, null, "asthma", "http://x/efo" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
    }

    @Test
    public void testUnresolvedEverywhereRejected() throws Exception {
        // the fabricated TGEMO_00003 case
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( anyString() ) ).thenReturn( null );
        Characteristic c = characteristic( null, null, "Heterozygous", "http://purl.obolibrary.org/obo/TGEMO_00003" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.URI_UNRESOLVED, v.get( 0 ).getReason() );
        assertEquals( "Heterozygous", v.get( 0 ).getSubmittedLabel() );
    }

    @Test
    public void testOlsUnavailableYieldsUnverified() throws Exception {
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( anyString() ) ).thenThrow( new OlsUnavailableException( "boom" ) );
        Characteristic c = characteristic( null, null, "asthma", "http://x/efo" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.UNVERIFIED_OLS_UNAVAILABLE, v.get( 0 ).getReason() );
    }

    /**
     * A gene URI is grounded against Gemma's gene table, never against an ontology or OLS: the gene table is
     * the authority for a gene record.
     */
    @Test
    public void testNcbiGeneUriIsCheckedAgainstTheGeneTable() throws Exception {
        when( geneService.findByNCBIId( 22059 ) ).thenReturn( Gene.Factory.newInstance() );
        Characteristic c = characteristic( null, null, "Trp53", Gene.NCBI_URI_PREFIX + "22059" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        verify( geneService ).findByNCBIId( 22059 );
        verify( ontologyService, never() ).getTerm( anyString(), anyLong(), any() );
        verify( olsTermResolver, never() ).resolve( anyString() );
    }

    /**
     * 🛑 The label is not compared. Gemma stores a composed display form in the value slot, so 21,942 of the
     * 23,812 gene-URI characteristics on production carry a value that is not the official symbol (measured
     * 2026-09-12). Comparing them would reject essentially all gene curation.
     */
    @Test
    public void testAGeneLabelThatIsNotTheOfficialSymbolIsNotAViolation() throws Exception {
        Gene g = Gene.Factory.newInstance();
        g.setOfficialSymbol( "Trp53" );
        when( geneService.findByNCBIId( 22059 ) ).thenReturn( g );
        Characteristic c = characteristic( null, null, "Trp53 [mouse] transformation related protein 53",
                Gene.NCBI_URI_PREFIX + "22059" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "Trp53 [mouse] transformation related protein 53", c.getValue(),
                "the display form must survive: rewriting it to the symbol flattens what the UI renders" );
    }

    /**
     * 🛑 An id Gemma does not carry is NOT a fabrication: Gemma imports no QTLs, complexes, pseudogenes or
     * genes of unsupported species, and a mouse study annotating a yeast construct is ordinary curation.
     * All 41 such rows on production are of those kinds. NCBI is asked, and a record it has is accepted.
     */
    @Test
    public void testAGeneIdOnlyNcbiCarriesIsAccepted() throws Exception {
        when( geneService.findByNCBIId( 853271 ) ).thenReturn( null );
        when( ncbiGeneResolver.resolve( 853271 ) ).thenReturn( new ubic.gemma.core.ontology.ncbi.NcbiGeneRecord(
                853271, "SET2", "histone methyltransferase SET2", "Saccharomyces cerevisiae", true, true ) );
        Characteristic c = characteristic( null, null, "SET2 [yeast] histone methyltransferase SET2",
                Gene.NCBI_URI_PREFIX + "853271" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "SET2 [yeast] histone methyltransferase SET2", c.getValue(),
                "the label is left alone off the Gemma path: NCBI serves a scientific name, not the common one" );
        verify( olsTermResolver, never() ).resolve( anyString() );
    }

    /** A record NCBI has retired is its own finding — the id was real, so the fix is the successor. */
    @Test
    public void testAWithdrawnNcbiRecordIsReported() throws Exception {
        when( geneService.findByNCBIId( 918 ) ).thenReturn( null );
        when( ncbiGeneResolver.resolve( 918 ) ).thenReturn( new ubic.gemma.core.ontology.ncbi.NcbiGeneRecord(
                918, "CD3W", "CD3-TCR complex, omega polypeptide", "Homo sapiens", true, false ) );
        Characteristic c = characteristic( null, null, "CD3W [human] CD3-TCR complex, omega polypeptide",
                Gene.NCBI_URI_PREFIX + "918" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.GENE_WITHDRAWN, v.get( 0 ).getReason() );
    }

    /** An id neither Gemma nor NCBI has is a fabrication. */
    @Test
    public void testAGeneIdNobodyHasIsUnresolved() throws Exception {
        when( geneService.findByNCBIId( 999999999 ) ).thenReturn( null );
        when( ncbiGeneResolver.resolve( 999999999 ) )
                .thenReturn( ubic.gemma.core.ontology.ncbi.NcbiGeneRecord.notFound( 999999999 ) );
        Characteristic c = characteristic( null, null, "Nosuchgene", Gene.NCBI_URI_PREFIX + "999999999" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.URI_UNRESOLVED, v.get( 0 ).getReason() );
    }

    /** NCBI down is unverified, not a finding: fail-open is the default, so the write proceeds. */
    @Test
    public void testNcbiUnavailableIsAllowedThroughByDefault() throws Exception {
        when( geneService.findByNCBIId( anyInt() ) ).thenReturn( null );
        when( ncbiGeneResolver.resolve( anyInt() ) )
                .thenThrow( new ubic.gemma.core.ontology.ncbi.NcbiUnavailableException( "boom" ) );
        Characteristic c = characteristic( null, null, "Tabw [mouse] TallyHo associated body weight",
                Gene.NCBI_URI_PREFIX + "117011" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
    }

    /** …and fail-closed reports it with the retryable reason rather than the fabricated one. */
    @Test
    public void testNcbiUnavailableIsReportedWhenFailClosed() throws Exception {
        ReflectionTestUtils.setField( validator, "ncbiFailClosed", true );
        when( geneService.findByNCBIId( anyInt() ) ).thenReturn( null );
        when( ncbiGeneResolver.resolve( anyInt() ) )
                .thenThrow( new ubic.gemma.core.ontology.ncbi.NcbiUnavailableException( "boom" ) );
        Characteristic c = characteristic( null, null, "Tabw [mouse] TallyHo associated body weight",
                Gene.NCBI_URI_PREFIX + "117011" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.UNVERIFIED_NCBI_UNAVAILABLE, v.get( 0 ).getReason() );
    }

    /**
     * 🛑 A stale symbol IS harmonized rather than accepted: 193 rows over 61 ids carry a symbol the gene no
     * longer has (Arntl for Bmal1, MLL for KMT2A, H3F3A for H3-3A), so the label is rewritten to the current
     * display form and echoed, and NCBI is never consulted because Gemma has the gene.
     */
    @Test
    public void testAStaleGeneSymbolIsHarmonizedToTheCurrentDisplayForm() throws Exception {
        ubic.gemma.model.genome.Taxon taxon = ubic.gemma.model.genome.Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        Gene g = Gene.Factory.newInstance();
        g.setOfficialSymbol( "Bmal1" );
        g.setOfficialName( "basic helix-loop-helix ARNT like 1" );
        g.setTaxon( taxon );
        when( geneService.findByNCBIId( 11865 ) ).thenReturn( g );
        Characteristic c = characteristic( null, null,
                "Arntl [mouse] aryl hydrocarbon receptor nuclear translocator-like", Gene.NCBI_URI_PREFIX + "11865" );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertTrue( validator.validateAndCanonicalize( c, canons ).isEmpty() );
        assertEquals( "Bmal1 [mouse] basic helix-loop-helix ARNT like 1", c.getValue() );
        assertEquals( 1, canons.size() );
        assertEquals( "value", canons.get( 0 ).getSlot() );
        verifyNoInteractions( ncbiGeneResolver );
    }

    /** A gene URI whose tail is not a number never reaches the gene table. */
    @Test
    public void testAGeneUriWithANonNumericIdIsUnresolved() throws Exception {
        Characteristic c = characteristic( null, null, "Trp53", Gene.NCBI_URI_PREFIX + "Trp53" );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.URI_UNRESOLVED, v.get( 0 ).getReason() );
        verify( geneService, never() ).findByNCBIId( any() );
    }

    @Test
    public void testFreeTextSkipped() throws Exception {
        Characteristic c = characteristic( null, null, "some free text value", null );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        verify( ontologyService, never() ).getTerm( anyString(), anyLong(), any() );
    }

    @Test
    public void testBlankLabelWithResolvableUriIsFilledIn() throws Exception {
        localResolves( "http://x/asthma", "asthma" );
        Characteristic c = characteristic( null, null, null, "http://x/asthma" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "asthma", c.getValue() );
    }

    @Test
    public void testCategorySlotValidated() throws Exception {
        localResolves( "http://x/badcat", "organism part" );
        Characteristic c = characteristic( "genotype", "http://x/badcat", "free text", null );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( "category", v.get( 0 ).getSlot() );
        assertEquals( TermViolation.Reason.LABEL_MISMATCH, v.get( 0 ).getReason() );
    }

    @Test
    public void testStatementPredicateAndObjectValidated() throws Exception {
        localResolves( "http://x/subj", "Utrn" );
        localResolves( "http://x/00166", "delivered at dose" ); // wrong predicate
        when( ontologyService.getTerm( eq( "http://x/00003" ), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( "http://x/00003" ) ).thenReturn( null ); // fabricated object

        Statement s = Statement.Factory.newInstance();
        s.setSubject( "Utrn" );
        s.setSubjectUri( "http://x/subj" );
        s.setPredicate( "has_genotype" );
        s.setPredicateUri( "http://x/00166" );
        s.setObject( "Heterozygous" );
        s.setObjectUri( "http://x/00003" );

        List<TermViolation> v = validator.validateAndCanonicalize( s );
        assertEquals( 2, v.size() );
        assertTrue( v.stream().anyMatch( tv -> tv.getSlot().equals( "predicate" ) && tv.getReason() == TermViolation.Reason.LABEL_MISMATCH ) );
        assertTrue( v.stream().anyMatch( tv -> tv.getSlot().equals( "object" ) && tv.getReason() == TermViolation.Reason.URI_UNRESOLVED ) );
    }

    /**
     * A resolver that hands back the term's own accession as its label has not resolved anything — it has told
     * us it does not know the term. Treating that as an authoritative label produces a LABEL_MISMATCH against
     * whatever the caller submitted, which turns "this vocabulary is not loaded" into "your label is wrong".
     * <p>
     * Observed on gemma2 with a real payload: RO_0002573 is not loaded (the term endpoint 404s for it), and a
     * design read straight out of Gemma and offered back unchanged was rejected with
     * {@code resolves to "RO_0002573", not the submitted label "has modifier"}. Data Gemma itself stored could
     * not be written back, which also breaks restoring a snapshot of an unmodified dataset.
     */
    @Test
    public void testLabelEqualToTheTermsOwnAccessionIsNotAResolution() throws Exception {
        String uri = "http://purl.obolibrary.org/obo/RO_0002573";
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( uri ) ).thenReturn( new OlsTerm( uri, "RO_0002573" ) );

        Characteristic c = characteristic( null, null, "has modifier", uri );
        List<TermViolation> v = validator.validateAndCanonicalize( c );

        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.URI_UNRESOLVED, v.get( 0 ).getReason(),
                "an unloaded vocabulary is an unresolved URI, not a mislabelled term" );
    }

    /** The same guard for a local hit: a loaded ontology that has no rdfs:label often falls back to the id. */
    @Test
    public void testLocalLabelEqualToTheTermsOwnAccessionIsNotAResolution() throws Exception {
        String uri = "http://purl.obolibrary.org/obo/RO_0002573";
        localResolves( uri, "RO_0002573" );
        when( olsTermResolver.resolve( uri ) ).thenReturn( null );

        Characteristic c = characteristic( null, null, "has modifier", uri );
        List<TermViolation> v = validator.validateAndCanonicalize( c );

        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.URI_UNRESOLVED, v.get( 0 ).getReason() );
    }

    /**
     * A predicate is checked against Gemma's OWN vocabulary, not against whatever ontologies happen to be
     * loaded. {@code Relation.terms.txt} is the rule for that slot -- the relations Gemma ships and sanctions.
     * <p>
     * This is the case that failed on gemma2. {@code RO_0002573} is in that file, labelled exactly
     * {@code "has modifier"}, and Gemma had stored it on GSE11630 — but RO is not among the loadable
     * ontologies, so the generic resolve path found nothing and the commit refused Gemma's own relation.
     */
    @Test
    public void testPredicateResolvesFromGemmasOwnRelationVocabulary() throws Exception {
        String uri = "http://purl.obolibrary.org/obo/RO_0002573";
        // deliberately unknown to both generic sources, exactly as on gemma2
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( uri ) ).thenReturn( null );
        Set<OntologyProperty> rels = relationTerms( uri, "has modifier" );
        when( ontologyService.getRelationTerms() ).thenReturn( rels );

        Statement s = Statement.Factory.newInstance();
        s.setSubject( "astrocyte" );
        s.setPredicate( "has modifier" );
        s.setPredicateUri( uri );

        assertTrue( validator.validateAndCanonicalize( s ).isEmpty(),
                "a sanctioned relation must not need an external lookup" );
    }

    /** The same for the second predicate slot — statements carry two. */
    @Test
    public void testSecondPredicateResolvesFromGemmasOwnRelationVocabulary() throws Exception {
        String uri = "http://purl.obolibrary.org/obo/RO_0000087";
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( uri ) ).thenReturn( null );
        Set<OntologyProperty> rels = relationTerms( uri, "has role" );
        when( ontologyService.getRelationTerms() ).thenReturn( rels );

        Statement s = Statement.Factory.newInstance();
        s.setSubject( "cell" );
        s.setSecondPredicate( "has role" );
        s.setSecondPredicateUri( uri );

        assertTrue( validator.validateAndCanonicalize( s ).isEmpty() );
    }

    /** A predicate URI that is NOT in the sanctioned list still has to ground somewhere. */
    @Test
    public void testPredicateOutsideTheVocabularyStillNeedsToResolve() throws Exception {
        String uri = "http://purl.obolibrary.org/obo/RO_9999999";
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( uri ) ).thenReturn( null );
        Set<OntologyProperty> rels = relationTerms( "http://purl.obolibrary.org/obo/RO_0002573", "has modifier" );
        when( ontologyService.getRelationTerms() ).thenReturn( rels );

        Statement s = Statement.Factory.newInstance();
        s.setSubject( "astrocyte" );
        s.setPredicate( "invented relation" );
        s.setPredicateUri( uri );

        List<TermViolation> v = validator.validateAndCanonicalize( s );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.URI_UNRESOLVED, v.get( 0 ).getReason() );
    }

    /** A category is likewise checked against Gemma's category vocabulary before anything external. */
    @Test
    public void testCategoryResolvesFromGemmasOwnCategoryVocabulary() throws Exception {
        String uri = "http://www.ebi.ac.uk/efo/EFO_0000727";
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( null );
        when( olsTermResolver.resolve( uri ) ).thenReturn( null );
        OntologyTerm cat = mock( OntologyTerm.class );
        when( cat.getUri() ).thenReturn( uri );
        when( cat.getLabel() ).thenReturn( "treatment" );
        Set<OntologyTerm> cats = Collections.singleton( cat );
        when( ontologyService.getCategoryTerms() ).thenReturn( cats );

        Characteristic c = characteristic( "treatment", uri, null, null );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
    }

    /** Gemma's vocabulary is the authority: a wrong label on a sanctioned predicate is still a mismatch. */
    @Test
    public void testWrongLabelOnASanctionedPredicateIsStillRejected() throws Exception {
        String uri = "http://purl.obolibrary.org/obo/RO_0002573";
        Set<OntologyProperty> rels = relationTerms( uri, "has modifier" );
        when( ontologyService.getRelationTerms() ).thenReturn( rels );

        Statement s = Statement.Factory.newInstance();
        s.setSubject( "astrocyte" );
        s.setPredicate( "delivered at dose" ); // a real relation, but not this URI's
        s.setPredicateUri( uri );

        List<TermViolation> v = validator.validateAndCanonicalize( s );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.LABEL_MISMATCH, v.get( 0 ).getReason() );
    }

    private Set<OntologyProperty> relationTerms( String uri, String label ) {
        OntologyProperty p = mock( OntologyProperty.class );
        when( p.getUri() ).thenReturn( uri );
        when( p.getLabel() ).thenReturn( label );
        return Collections.singleton( p );
    }

    /** A genuine label that merely looks id-ish must still resolve — the guard keys on the URI's own local name. */
    @Test
    public void testALabelThatMerelyResemblesAnAccessionStillResolves() throws Exception {
        localResolves( "http://x/CL_0000127", "RO_0002573" ); // different term's id: not this term's local name
        Characteristic c = characteristic( null, null, "RO_0002573", "http://x/CL_0000127" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
    }

    private static final String DISEASE_URI = "http://www.ebi.ac.uk/efo/EFO_0000408";
    private static final String CELL_LINE_URI = "http://www.ebi.ac.uk/efo/EFO_0000322";

    /** Stub Gemma's published category list (what {@code OntologyService#getCategoryTerms} hands back). */
    private void categoryTerms( String uri, String label ) {
        OntologyTerm cat = mock( OntologyTerm.class );
        when( cat.getUri() ).thenReturn( uri );
        when( cat.getLabel() ).thenReturn( label );
        when( ontologyService.getCategoryTerms() ).thenReturn( Collections.singleton( cat ) );
    }

    /**
     * {@code EFO_0000408} is on Gemma's category list as {@code disease}, but EFO has obsoleted it and
     * {@code getCategoryTerms} hands back EFO's live term, labelled {@code obsolete_disease}. Gemma must not
     * refuse the name it uses for its own category (cab, 2026-08-30).
     */
    @Test
    public void testObsoletedCategoryAcceptsTheNameGemmaUses() throws Exception {
        categoryTerms( DISEASE_URI, "obsolete_disease" );
        Characteristic c = characteristic( "disease", DISEASE_URI, "asthma", null );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "disease", c.getCategory() ); // stored as submitted, not as the marked label
    }

    /**
     * {@code EFO_0000322} is NOT on the category list — {@code CLO_0000031} is the canonical cell-line category —
     * so it resolves through the loaded ontologies, and to {@code obsolete_cell line}. Gemma stores it on a large
     * part of the corpus, so a write carrying it has to be accepted too.
     */
    @Test
    public void testObsoletedCategoryOffTheListAcceptsItsPreObsolescenceName() throws Exception {
        localResolves( CELL_LINE_URI, "obsolete_cell line" );
        Characteristic c = characteristic( "cell line", CELL_LINE_URI, "HeLa", null );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "cell line", c.getCategory() );
    }

    /** The carve-out drops a marker, it does not stop checking: a different category's name is still a mismatch. */
    @Test
    public void testWrongLabelOnAnObsoletedCategoryIsStillRejected() throws Exception {
        categoryTerms( DISEASE_URI, "obsolete_disease" );
        Characteristic c = characteristic( "cell type", DISEASE_URI, "asthma", null );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( TermViolation.Reason.LABEL_MISMATCH, v.get( 0 ).getReason() );
        assertEquals( "category", v.get( 0 ).getSlot() );
        assertEquals( "disease", v.get( 0 ).getResolvedLabel() ); // reported unmarked: the name to use
    }

    /** Categories only. A VALUE that resolves to an obsolete term is a different question and is not carved out. */
    @Test
    public void testObsoleteMarkerIsNotStrippedInTheValueSlot() throws Exception {
        localResolves( DISEASE_URI, "obsolete_disease" );
        Characteristic c = characteristic( null, null, "disease", DISEASE_URI );
        List<TermViolation> v = validator.validateAndCanonicalize( c );
        assertEquals( 1, v.size() );
        assertEquals( "value", v.get( 0 ).getSlot() );
        assertEquals( TermViolation.Reason.LABEL_MISMATCH, v.get( 0 ).getReason() );
    }

    /** The ontology's own spelling still validates, and is rewritten to the name Gemma stores. */
    @Test
    public void testCategorySentWithTheOntologysObsoleteSpellingIsCanonicalized() throws Exception {
        categoryTerms( DISEASE_URI, "obsolete_disease" );
        Characteristic c = characteristic( "obsolete_disease", DISEASE_URI, "asthma", null );
        List<TermCanonicalization> canons = new ArrayList<>();
        assertTrue( validator.validateAndCanonicalize( c, canons ).isEmpty() );
        assertEquals( "disease", c.getCategory() );
        assertEquals( 1, canons.size() );
        assertEquals( "category", canons.get( 0 ).getSlot() );
        assertEquals( "obsolete_disease", canons.get( 0 ).getSubmittedLabel() );
        assertEquals( "disease", canons.get( 0 ).getCanonicalLabel() );
    }

    /** A category URI with no label fills in the unmarked name rather than minting {@code obsolete_disease}. */
    @Test
    public void testBlankCategoryLabelFilledWithTheUnmarkedName() throws Exception {
        categoryTerms( DISEASE_URI, "obsolete_disease" );
        Characteristic c = characteristic( null, DISEASE_URI, "asthma", null );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        assertEquals( "disease", c.getCategory() );
    }
}
