package ubic.gemma.core.image.aba;

import org.junit.Test;
import ubic.gemma.core.config.Settings;
import ubic.gemma.model.genome.Gene;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class AbaLoaderTest {

    private final AbaLoader abaLoader = new AbaLoader( Paths.get( Settings.getString( "gemma.appdata.home" ) ).resolve( "abaCache" ), 30 * 1000 );

    @Test
    public void testGetGene() {
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