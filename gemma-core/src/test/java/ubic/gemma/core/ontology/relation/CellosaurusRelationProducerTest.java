package ubic.gemma.core.ontology.relation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.model.OntologyXref;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cellosaurus becoming EXTERNAL relations.
 *
 * <p>The fixture is a real MCF-7 record trimmed to the lines that matter, including the two shapes
 * that make this a parser: the disease arrives as an NCIt cross-reference and the anatomic site is
 * buried in a free-text comment that also carries doubling times and anecdotes.</p>
 */
class CellosaurusRelationProducerTest {

    private static final String MONDO_BREAST_CARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0004988";

    private AnnotationRelationDao dao;
    private TransactionTemplate transactionTemplate;
    private TaxonService taxonService;
    private final List<OntologyXref> xrefs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        dao = mock( AnnotationRelationDao.class );
        taxonService = mock( TaxonService.class );
        Taxon human = new Taxon();
        human.setNcbiId( 9606 );
        when( taxonService.findByNcbiId( anyInt() ) ).thenReturn( human );
        transactionTemplate = mock( TransactionTemplate.class );
        when( transactionTemplate.execute( any() ) ).thenAnswer( inv ->
                ( ( TransactionCallback<?> ) inv.getArgument( 0 ) ).doInTransaction( null ) );
        xrefs.add( new OntologyXref( MONDO_BREAST_CARCINOMA, "NCIT:C4194", OntologyXref.Strength.EXACT,
                "breast adenocarcinoma" ) );
    }

    private static final String MCF7 = String.join( "\n",
            "id: CVCL_0031",
            // 🛑 no space after this colon, unlike every other field
            "name:MCF-7",
            "synonym: \"MCF 7\" RELATED []",
            "xref: CLO:CLO_0007606",
            "xref: EFO:EFO_0001203",
            "xref: PubMed:31978347",
            "xref: NCIt:C4194 ! Invasive breast carcinoma of no special type",
            "xref: NCBI_TaxID:9606 ! Homo sapiens (Human)",
            "comment: \"Doubling time: 1.8 days. Anecdotal: flown in space. Derived from site: Metastatic;"
                    + " Pleural effusion; UBERON=UBERON_0000175.\"",
            "" );

    /** A zebrafish line, complete with a disease and a site, that must not be stored. */
    private static final String ZEBRAFISH = String.join( "\n",
            "id: CVCL_9999",
            "name:ZF4",
            "xref: NCIt:C4194 ! something",
            "xref: NCBI_TaxID:7955 ! Danio rerio (Zebrafish)",
            "comment: \"Derived from site: In situ; Fin; UBERON=UBERON_0008897.\"",
            "" );

    private List<AnnotationRelation> produce( String obo ) throws Exception {
        return produce( obo, true );
    }

    /**
     * @param uberonLoaded whether UBERON is available to name the anatomic-part objects; false
     *                     exercises the fallback, which is a real deployment state (UBERON is
     *                     off by default) and not a hypothetical
     */
    private List<AnnotationRelation> produce( String obo, boolean uberonLoaded ) throws Exception {
        ubic.gemma.core.ontology.providers.OntologyService mondo =
                mock( ubic.gemma.core.ontology.providers.OntologyService.class );
        when( mondo.getIdentifier() ).thenReturn( "mondoOntology" );
        when( mondo.isOntologyLoaded() ).thenReturn( true );
        when( mondo.getCrossReferencesFromSource() ).thenReturn( xrefs );
        ubic.gemma.core.ontology.providers.OntologyService uberon =
                mock( ubic.gemma.core.ontology.providers.OntologyService.class );
        when( uberon.getIdentifier() ).thenReturn( "uberonOntology" );
        when( uberon.isOntologyLoaded() ).thenReturn( uberonLoaded );
        if ( uberonLoaded ) {
            OntologyTerm pleuralEffusion = mock( OntologyTerm.class );
            when( pleuralEffusion.getLabel() ).thenReturn( "pleural effusion" );
            when( uberon.getTerm( "http://purl.obolibrary.org/obo/UBERON_0000175" ) )
                    .thenReturn( pleuralEffusion );
        }
        new CellosaurusRelationProducer( Arrays.asList( mondo, uberon ), dao, transactionTemplate,
                taxonService )
                .produce( new ByteArrayInputStream( obo.getBytes( StandardCharsets.UTF_8 ) ) );
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<AnnotationRelation>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( dao ).create( captor.capture() );
        return new ArrayList<>( captor.getValue() );
    }

    /**
     * 🛑 Both keys, for one fact. Dropping the aliases makes it unreachable from the vocabulary most
     * existing annotations use; dropping the accession discards the lines that are nowhere else, which
     * is the entire reason Cellosaurus is loaded.
     */
    @Test
    void oneFactIsStoredUnderTheAccessionAndUnderEveryAlias() throws Exception {
        List<AnnotationRelation> rows = produce( MCF7 );

        assertThat( rows )
                .filteredOn( r -> r.getPredicateUri().endsWith( "CLO_0000015" ) )
                .extracting( AnnotationRelation::getSubjectValueUri )
                .containsExactlyInAnyOrder(
                        "https://www.cellosaurus.org/CVCL_0031",
                        "http://purl.obolibrary.org/obo/CLO_0007606",
                        "http://www.ebi.ac.uk/efo/EFO_0001203" );
        assertThat( rows ).allSatisfy( r -> {
            assertThat( r.getSubjectValue() ).isEqualTo( "MCF-7" );
            assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.EXTERNAL );
            assertThat( r.getSource() ).isEqualTo( "CELLOSAURUS" );
        } );
    }

    /** The NCIt disease translates to MONDO; an NCIt must never reach a stored row. */
    @Test
    void theDonorDiseaseIsTranslatedOutOfNcit() throws Exception {
        assertThat( produce( MCF7 ) )
                .filteredOn( r -> r.getPredicateUri().endsWith( "CLO_0000015" ) )
                .allSatisfy( r -> {
                    assertThat( r.getObjectValueUri() ).isEqualTo( MONDO_BREAST_CARCINOMA );
                    assertThat( r.getObjectValue() ).isEqualTo( "breast adenocarcinoma" );
                    assertThat( r.getObjectCategory() ).isEqualTo( "disease" );
                    // what Cellosaurus called it, so a curator can see what was translated
                    assertThat( r.getEvidence() ).contains( "NCIT:C4194" );
                } )
                .isNotEmpty();
    }

    /**
     * 🛑 The site is inside a free-text comment that also carries doubling times and anecdotes. It is
     * found by locating UBERON= within a "Derived from site:" clause, because the comment has no
     * grammar to parse.
     */
    @Test
    void theAnatomicSiteIsReadOutOfTheFreeTextComment() throws Exception {
        assertThat( produce( MCF7 ) )
                .filteredOn( r -> r.getPredicateUri().endsWith( "CLO_0037208" ) )
                .isNotEmpty()
                .allSatisfy( r -> {
                    assertThat( r.getObjectValueUri() )
                            .isEqualTo( "http://purl.obolibrary.org/obo/UBERON_0000175" );
                    assertThat( r.getObjectCategory() ).isEqualTo( "organism part" );
                } );
    }

    /**
     * The object is what a curator reads as the term's name, so it has to be the term's name. It used
     * to be Cellosaurus's raw site field — {@code Metastatic; Pleural effusion} — which is a sentence
     * no ontology contains, while {@code evidence} held a verbatim copy of the same string.
     */
    @Test
    void theSiteObjectIsTheTermsLabelAndTheRawFieldIsTheEvidence() throws Exception {
        assertThat( produce( MCF7 ) )
                .filteredOn( r -> r.getPredicateUri().endsWith( "CLO_0037208" ) )
                .isNotEmpty()
                .allSatisfy( r -> {
                    assertThat( r.getObjectValue() ).isEqualTo( "pleural effusion" );
                    // Not dropped: primary-vs-metastatic survives only in the source's sentence.
                    assertThat( r.getEvidence() ).isEqualTo( "Metastatic; Pleural effusion" );
                    assertThat( r.getEvidence() ).isNotEqualTo( r.getObjectValue() );
                } );
    }

    /**
     * UBERON is off by default, so the fallback is a state Gemma actually runs in. A sentence in the
     * object field is worse than the term's name and better than an empty one.
     */
    @Test
    void theRawSiteFieldIsKeptWhenUberonCannotNameTheTerm() throws Exception {
        assertThat( produce( MCF7, false ) )
                .filteredOn( r -> r.getPredicateUri().endsWith( "CLO_0037208" ) )
                .isNotEmpty()
                .allSatisfy( r -> {
                    assertThat( r.getObjectValue() ).isEqualTo( "Metastatic; Pleural effusion" );
                    assertThat( r.getObjectValueUri() )
                            .isEqualTo( "http://purl.obolibrary.org/obo/UBERON_0000175" );
                } );
    }

    /**
     * Species is a correctness filter, not a volume control — it removes 4.6% of the corpus. A
     * zebrafish line with a perfectly good disease and site is still not something Gemma curates.
     */
    @Test
    void aLineFromAnUncuratedSpeciesIsSkippedEntirely() throws Exception {
        List<AnnotationRelation> rows = produce( MCF7 + ZEBRAFISH );

        assertThat( rows )
                .extracting( AnnotationRelation::getSubjectValue )
                .containsOnly( "MCF-7" );
    }

    @Test
    void theRebuildIsScopedToThisSource() throws Exception {
        produce( MCF7 );
        verify( dao ).removeByBasis( AnnotationRelationBasis.EXTERNAL, null, "CELLOSAURUS" );
    }

    /** Same predicates CLO uses, so the two sources corroborate instead of sitting side by side. */
    @Test
    void thePredicatesMatchTheOnesCloAsserts() throws Exception {
        assertThat( produce( MCF7 ) )
                .extracting( AnnotationRelation::getPredicate, AnnotationRelation::getPredicateUri )
                .contains(
                        tuple( "derives from patient having disease",
                                "http://purl.obolibrary.org/obo/CLO_0000015" ),
                        tuple( "derives from anatomic part",
                                "http://purl.obolibrary.org/obo/CLO_0037208" ) );
    }
}
