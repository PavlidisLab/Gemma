package ubic.gemma.core.image.aba;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.util.test.category.AllenBrainAtlasTest;
import ubic.gemma.model.genome.Gene;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

@Category(AllenBrainAtlasTest.class)
public class AbaLoaderTest {

    private final AbaLoader abaLoader = new AbaLoader( Paths.get( Settings.getString( "gemma.appdata.home" ) ).resolve( "abaCache" ), 30 * 1000 );

    @Test
    public void testGetGene() {
        assumeThatResourceIsAvailable( "https://api.brain-map.org/api/v2/" );
        Gene gene;
        gene = Gene.Factory.newInstance();
        gene.setName( "glutamate receptor, ionotropic, NMDA1 (zeta 1)" );
        gene.setOfficialSymbol( "grin1" );
        gene.setNcbiGeneId( 14810 );
        assertThat( abaLoader.getAbaGeneXML( gene ) )
                .isNotNull()
                .satisfies( doc -> {
                    assertThat( doc.getDocumentElement().getTagName() )
                            .isEqualTo( "Response" );
                    assertThat( doc.getDocumentElement().getAttributes().getNamedItem( "success" ).getTextContent() )
                            .isEqualTo( "true" );
                } );
    }

    @Test
    public void testGetSagittal() {
        assumeThatResourceIsAvailable( "https://api.brain-map.org/api/v2/" );
        Gene gene;
        gene = Gene.Factory.newInstance();
        gene.setName( "glutamate receptor, ionotropic, NMDA1 (zeta 1)" );
        gene.setOfficialSymbol( "grin1" );
        gene.setNcbiGeneId( 14810 );
        assertThat( abaLoader.getAbaGeneSagittalImages( gene ) )
                .isNotNull()
                .satisfies( doc -> {
                    assertThat( doc.getDocumentElement().getTagName() )
                            .isEqualTo( "Response" );
                    assertThat( doc.getDocumentElement().getAttributes().getNamedItem( "success" ).getTextContent() )
                            .isEqualTo( "true" );
                } );
    }
}