package ubic.gemma.model.expression.biomaterial;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.expression.experiment.StatementValueObject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A sample annotation survives the read with whatever was said about it.
 *
 * @author gembro
 */
public class BioMaterialValueObjectStatementTest {

    /**
     * 🛑 A predicated sample annotation used to be flattened to its subject on every read:
     * {@code CharacteristicValueObject} has no predicate or object, and it was the only shape the
     * sample payload carried. A curator could write one through
     * {@code POST /datasets/{id}/samples/{sid}/annotations} — which builds a Statement — and never
     * see it again.
     */
    @Test
    public void aPredicatedSampleAnnotationKeepsItsPredicateAndObject() {
        Statement s = Statement.Factory.newInstance();
        s.setId( 5L );
        s.setCategory( "genotype" );
        s.setSubject( "Trp53" );
        s.setPredicate( "has_genotype" );
        s.setObject( "Homozygous negative" );

        BioMaterial bm = BioMaterial.Factory.newInstance( "sample" );
        bm.setId( 1L );
        bm.getCharacteristics().add( s );

        BioMaterialValueObject vo = new BioMaterialValueObject( bm );

        assertThat( vo.getStatements() )
                .singleElement()
                .satisfies( svo -> {
                    assertThat( svo.getSubject() ).isEqualTo( "Trp53" );
                    assertThat( svo.getPredicate() ).isEqualTo( "has_genotype" );
                    assertThat( svo.getObject() ).isEqualTo( "Homozygous negative" );
                } );
    }

    /**
     * And a bare one is listed too, with nothing said about it — one collection, not two kinds of
     * annotation.
     */
    @Test
    public void aBareSampleAnnotationIsListedWithNoPredicate() {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setId( 6L );
        c.setCategory( "organism part" );
        c.setValue( "chorionic villus" );
        c.setValueUri( "http://purl.obolibrary.org/obo/UBERON_0007106" );

        BioMaterial bm = BioMaterial.Factory.newInstance( "sample" );
        bm.setId( 1L );
        bm.getCharacteristics().add( c );

        BioMaterialValueObject vo = new BioMaterialValueObject( bm );

        assertThat( vo.getCharacteristics() ).hasSize( 1 );
        assertThat( vo.getStatements() )
                .singleElement()
                .satisfies( svo -> {
                    assertThat( svo.getSubject() ).isEqualTo( "chorionic villus" );
                    assertThat( svo.getSubjectUri() ).isEqualTo( "http://purl.obolibrary.org/obo/UBERON_0007106" );
                    assertThat( svo.getPredicate() ).isNull();
                    assertThat( svo.getObject() ).isNull();
                } );
    }
}
