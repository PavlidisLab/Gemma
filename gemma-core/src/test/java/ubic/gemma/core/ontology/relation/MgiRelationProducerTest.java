package ubic.gemma.core.ontology.relation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.ontology.model.OntologyXref;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.common.description.AnnotationRelationStatus;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MGI's genotype-to-disease reports becoming EXTERNAL relations.
 *
 * <p>What is worth testing is not that a TSV can be read, but the three ways this can be quietly
 * wrong: a DOID reaching a stored row, a refutation being stored as though it were support, and a
 * citation silently making no difference to how the claim is qualified.</p>
 */
class MgiRelationProducerTest {

    private static final String MONDO_RETT = "http://purl.obolibrary.org/obo/MONDO_0010726";
    private static final String MONDO_ALS = "http://purl.obolibrary.org/obo/MONDO_0004976";

    private AnnotationRelationDao dao;
    private TransactionTemplate transactionTemplate;
    private TaxonService taxonService;
    private final List<OntologyXref> xrefs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        dao = mock( AnnotationRelationDao.class );
        taxonService = mock( TaxonService.class );
        Taxon mouse = new Taxon();
        mouse.setNcbiId( 10090 );
        when( taxonService.findByNcbiId( anyInt() ) ).thenReturn( mouse );
        transactionTemplate = mock( TransactionTemplate.class );
        when( transactionTemplate.execute( any() ) ).thenAnswer( inv ->
                ( ( TransactionCallback<?> ) inv.getArgument( 0 ) ).doInTransaction( null ) );
        // the label rides on the cross-reference, so the term can be named without the loaded model
        xrefs.add( new OntologyXref( MONDO_RETT, "DOID:1206", OntologyXref.Strength.EXACT, "Rett syndrome" ) );
        xrefs.add( new OntologyXref( MONDO_ALS, "DOID:332", OntologyXref.Strength.EXACT,
                "amyotrophic lateral sclerosis" ) );
    }

    private static String row( String allele, String alleleId, String pubmed, String doid ) {
        return String.join( "\t", allele + "/" + allele, allele, alleleId, "involves: C57BL/6",
                "MP:0002064", pubmed, "MGI:97487", doid, "OMIM:1", "MGI:2166570" );
    }

    private static InputStream report( String... rows ) {
        return new ByteArrayInputStream( String.join( "\n", rows ).getBytes( StandardCharsets.UTF_8 ) );
    }

    private MgiRelationProducer producer() {
        ubic.gemma.core.ontology.providers.OntologyService mondo =
                mock( ubic.gemma.core.ontology.providers.OntologyService.class );
        when( mondo.getIdentifier() ).thenReturn( "mondoOntology" );
        when( mondo.isOntologyLoaded() ).thenReturn( true );
        when( mondo.getCrossReferencesFromSource() ).thenReturn( xrefs );
        return new MgiRelationProducer( Collections.singletonList( mondo ), dao, transactionTemplate,
                taxonService );
    }

    private List<AnnotationRelation> produce( InputStream asserted, InputStream refuted ) throws Exception {
        producer().produce( asserted, refuted );
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<AnnotationRelation>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( dao ).create( captor.capture() );
        return new ArrayList<>( captor.getValue() );
    }

    @Test
    void aGenotypeBecomesAModelOfTheDiseaseItIsAnnotatedAgainst() throws Exception {
        List<AnnotationRelation> rows = produce(
                report( row( "Mecp2<tm1.1Bird>", "MGI:1857444", "11242117", "DOID:1206" ) ), null );

        assertThat( rows ).hasSize( 1 );
        AnnotationRelation r = rows.get( 0 );
        assertThat( r.getSubjectValue() ).isEqualTo( "Mecp2<tm1.1Bird>" );
        assertThat( r.getSubjectValueUri() )
                .isEqualTo( "https://www.informatics.jax.org/allele/MGI:1857444" );
        assertThat( r.getSubjectCategory() ).isEqualTo( "genotype" );
        assertThat( r.getPredicate() ).isEqualTo( "is model of" );
        assertThat( r.getObjectValueUri() ).isEqualTo( MONDO_RETT );
        assertThat( r.getObjectValue() ).isEqualTo( "Rett syndrome" );
        assertThat( r.getObjectCategory() ).isEqualTo( "disease" );
        assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.EXTERNAL );
        assertThat( r.getSource() ).isEqualTo( "MGI" );
        assertThat( r.getStatus() ).isEqualTo( AnnotationRelationStatus.ASSERTED );
        assertThat( r.getTaxon() ).isNotNull();
    }

    /**
     * 🛑 The negative report is the reason STATUS exists. Stored as an ordinary row it would assert
     * precisely what MGI's curators recorded as false.
     */
    @Test
    void theNegativeReportIsStoredAsARefutation() throws Exception {
        List<AnnotationRelation> rows = produce(
                report( row( "Sod1<tm1Cje>", "MGI:1857004", "1", "DOID:332" ) ),
                report( row( "Pax3<Sp-2H>", "MGI:1856293", "", "DOID:1206" ) ) );

        assertThat( rows )
                .extracting( AnnotationRelation::getSubjectValue, AnnotationRelation::getStatus )
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple( "Sod1<tm1Cje>", AnnotationRelationStatus.ASSERTED ),
                        org.assertj.core.api.Assertions.tuple( "Pax3<Sp-2H>", AnnotationRelationStatus.REFUTED ) );
    }

    /**
     * 🛑 Gemma does not annotate in DOID and cannot resolve one. A DOID MONDO does not cross-reference
     * yields no row at all rather than a row nothing downstream can read.
     */
    @Test
    void anUntranslatableDoidIsDroppedRatherThanStoredRaw() throws Exception {
        List<AnnotationRelation> rows = produce(
                report( row( "Mecp2<tm1.1Bird>", "MGI:1857444", "1", "DOID:1206" ),
                        row( "Xyz<tm1>", "MGI:999", "1", "DOID:99999999" ) ), null );

        assertThat( rows ).hasSize( 1 );
        assertThat( rows ).allSatisfy( r ->
                assertThat( r.getObjectValueUri() ).doesNotContain( "DOID" ) );
    }

    /**
     * The citation has nowhere to be stored, so it survives as the difference between a traceable
     * author statement and an import whose own basis is invisible. If that distinction stopped being
     * made, 94% of MGI's statements would silently lose the only trace of their evidence.
     */
    @Test
    void theCitationDecidesTheEvidenceCode() throws Exception {
        List<AnnotationRelation> rows = produce(
                report( row( "Mecp2<tm1.1Bird>", "MGI:1857444", "11242117", "DOID:1206" ),
                        row( "Sod1<tm1Cje>", "MGI:1857004", "", "DOID:332" ) ), null );

        assertThat( rows )
                .extracting( AnnotationRelation::getSubjectValue, AnnotationRelation::getEvidenceCode )
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple( "Mecp2<tm1.1Bird>", GOEvidenceCode.TAS ),
                        org.assertj.core.api.Assertions.tuple( "Sod1<tm1Cje>", GOEvidenceCode.IIA ) );
    }

    /**
     * Rebuild, not upsert, and scoped to this source: a statement MGI has withdrawn must not outlive
     * the report, and no other EXTERNAL source may be touched by an MGI run.
     */
    @Test
    void theRebuildIsScopedToThisSource() throws Exception {
        produce( report( row( "Mecp2<tm1.1Bird>", "MGI:1857444", "1", "DOID:1206" ) ), null );

        verify( dao ).removeByBasis( AnnotationRelationBasis.EXTERNAL, null, "MGI" );
    }
}
