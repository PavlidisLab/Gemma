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
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.ols.OlsTerm;
import ubic.gemma.core.ontology.ols.OlsTermResolver;
import ubic.gemma.core.ontology.ols.OlsUnavailableException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Gene;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OntologyTermValidatorImplTest {

    private OntologyService ontologyService;
    private OlsTermResolver olsTermResolver;
    private OntologyTermValidatorImpl validator;

    @BeforeEach
    public void setUp() {
        ontologyService = mock( OntologyService.class );
        olsTermResolver = mock( OlsTermResolver.class );
        validator = new OntologyTermValidatorImpl( ontologyService, olsTermResolver );
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

    @Test
    public void testNcbiGeneUriSkipped() throws Exception {
        Characteristic c = characteristic( null, null, "Trp53", Gene.NCBI_URI_PREFIX + "22059" );
        assertTrue( validator.validateAndCanonicalize( c ).isEmpty() );
        verify( ontologyService, never() ).getTerm( anyString(), anyLong(), any() );
        verify( olsTermResolver, never() ).resolve( anyString() );
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
}
