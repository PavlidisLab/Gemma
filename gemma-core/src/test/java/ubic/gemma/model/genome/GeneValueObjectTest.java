package ubic.gemma.model.genome;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ubic.gemma.model.genome.gene.GeneValueObject;

import static org.assertj.core.api.Assertions.assertThat;

public class GeneValueObjectTest {

    @Test
    public void testGeneValueObjectWithoutTaxon() {
        Gene gene = new Gene();
        assertThat( gene.getTaxon() ).isNull();
        Assertions.assertThat( new GeneValueObject( gene ) )
                .hasFieldOrPropertyWithValue( "taxonId", null )
                .hasFieldOrPropertyWithValue( "taxonCommonName", null )
                .hasFieldOrPropertyWithValue( "taxonScientificName", null );
    }

}