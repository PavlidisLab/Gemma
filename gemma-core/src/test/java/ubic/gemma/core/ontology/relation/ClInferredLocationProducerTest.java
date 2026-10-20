package ubic.gemma.core.ontology.relation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The entailed CL locations: a reviewed file, read into rows that must be distinguishable from what
 * CL literally asserts.
 *
 * @author gembro
 */
class ClInferredLocationProducerTest {

    private static final String OBO = "http://purl.obolibrary.org/obo/";

    private AnnotationRelationDao dao;
    private ClInferredLocationProducer producer;

    @BeforeEach
    void setUp() throws Exception {
        dao = mock( AnnotationRelationDao.class );
        TransactionTemplate tt = mock( TransactionTemplate.class );
        when( tt.execute( any() ) ).thenAnswer( inv ->
                ( ( TransactionCallback<?> ) inv.getArgument( 0 ) ).doInTransaction( null ) );
        producer = new ClInferredLocationProducer( dao, tt );
    }

    private static InputStream tsv( String... rows ) {
        StringBuilder sb = new StringBuilder( "# a comment\n" )
                .append( "SUBJECT_VALUE\tSUBJECT_VALUE_URI\tPREDICATE\tPREDICATE_URI"
                        + "\tOBJECT_VALUE\tOBJECT_VALUE_URI\tVIA\tN_DATASETS\n" );
        for ( String r : rows ) {
            sb.append( r ).append( '\n' );
        }
        return new ByteArrayInputStream( sb.toString().getBytes( StandardCharsets.UTF_8 ) );
    }

    private List<AnnotationRelation> captureRows() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<AnnotationRelation>> c = ArgumentCaptor.forClass( Collection.class );
        verify( dao ).create( c.capture() );
        return new ArrayList<>( c.getValue() );
    }

    @Test
    void anEntailedRowCarriesItsOwnSourceAndNamesTheAncestorItCameFrom() throws Exception {
        producer.produce( tsv( "Sertoli cell\t" + OBO + "CL_0000216\tpart of\t" + OBO + "BFO_0000050"
                + "\tseminiferous tubule epithelium\t" + OBO + "UBERON_0004labelled\tsupporting cell\t14" ) );

        AnnotationRelation r = captureRows().get( 0 );
        assertThat( r.getSubjectValue() ).isEqualTo( "Sertoli cell" );
        assertThat( r.getSubjectCategory() ).isEqualTo( "cell type" );
        assertThat( r.getObjectValue() ).isEqualTo( "seminiferous tubule epithelium" );
        assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.ONTOLOGY );
        assertThat( r.getEvidenceCode() ).isEqualTo( GOEvidenceCode.IEA );
        // the whole point: a reader can tell this from what CL literally states
        assertThat( r.getSource() ).isEqualTo( "CL_INFERRED" );
        // and can see WHY it exists
        assertThat( r.getEvidence() ).isEqualTo( "entailed from supporting cell" );
    }

    /**
     * 🛑 234 of the 440 real rows are {@code has soma location}, not {@code part of}. The predicate
     * is read from the file rather than assumed, because assuming {@code part of} is wrong on the
     * majority and specifically on every cortical interneuron.
     */
    @Test
    void thePredicateIsReadFromTheFileAndIsNotAlwaysPartOf() throws Exception {
        producer.produce( tsv(
                "oRGC1\t" + OBO + "CL_0020036\thas soma location\t" + OBO + "RO_0002100"
                        + "\tretina\t" + OBO + "UBERON_0000966\tretinal ganglion cell\t0",
                "DN3 thymocyte\t" + OBO + "CL_0000807\tpart of\t" + OBO + "BFO_0000050"
                        + "\tthymus\t" + OBO + "UBERON_0002370\tthymocyte\t15" ) );

        List<AnnotationRelation> rows = captureRows();
        assertThat( rows ).extracting( AnnotationRelation::getPredicateUri )
                .containsExactly( OBO + "RO_0002100", OBO + "BFO_0000050" );
        assertThat( rows ).extracting( AnnotationRelation::getObjectCategory )
                .containsOnly( "organism part" );
    }

    /**
     * 🛑 The delete is narrowed to this SOURCE. It shares BASIS with CL's asserted rows, and a
     * basis-wide delete would take those 1,200-odd with it — rows this job cannot rebuild.
     */
    @Test
    void theDeleteIsNarrowedToItsOwnSourceAndDoesNotTakeClsAssertedRowsWithIt() throws Exception {
        producer.produce( tsv( "DN3 thymocyte\t" + OBO + "CL_0000807\tpart of\t" + OBO + "BFO_0000050"
                + "\tthymus\t" + OBO + "UBERON_0002370\tthymocyte\t15" ) );

        verify( dao ).removeByBasis( eq( AnnotationRelationBasis.ONTOLOGY ), isNull(), eq( "CL_INFERRED" ) );
    }

    /** A target that is another CL class is a cell type, not an anatomical structure. */
    @Test
    void aCellTypeTargetIsNotFiledAsAnOrganismPart() throws Exception {
        producer.produce( tsv( "some cell\t" + OBO + "CL_0000001\tpart of\t" + OBO + "BFO_0000050"
                + "\tglial cell\t" + OBO + "CL_0000125\tsome ancestor\t0" ) );

        assertThat( captureRows().get( 0 ).getObjectCategory() ).isEqualTo( "cell type" );
    }

    /** The shipped file is the artifact; it must parse and be the size the docs claim. */
    @Test
    void theShippedFileParses() throws Exception {
        producer.produce();
        List<AnnotationRelation> rows = captureRows();
        assertThat( rows ).hasSize( 440 );
        assertThat( rows ).allSatisfy( r -> {
            assertThat( r.getSubjectValueUri() ).startsWith( OBO + "CL_" );
            assertThat( r.getObjectValue() ).isNotBlank();
            assertThat( r.getSource() ).isEqualTo( "CL_INFERRED" );
        } );
    }
}
