package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.providers.UberonOntologyService;

import java.io.InputStream;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Loads a ROBOT STAR-extracted module of Uberon (substantia-nigra subhierarchy)
 * from a classpath fixture so the test runs in the default surefire pass. The
 * fixture preserves the brain → substantia nigra → substantia nigra pars compacta
 * SubClassOf chain that the assertions below depend on. Regeneration recipe is in
 * src/test/resources/data/loader/ontology/README.
 */
public class UberonOntologyTest {

    @Test
    public void testSubstantiaNigraInUberon() throws Exception {
        UberonOntologyService uberonOntologyService = new UberonOntologyService();
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.LanguageLevel.FULL, uberonOntologyService.getLanguageLevel() );
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.InferenceMode.TRANSITIVE, uberonOntologyService.getInferenceMode() );
        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/ontology/uberon.test.owl.gz" ).getInputStream() ) ) {
            uberonOntologyService.initialize( is, false );
        }
        OntologyTerm brain = uberonOntologyService.getTerm( "http://purl.obolibrary.org/obo/UBERON_0000955" );
        assertNotNull( brain );
        OntologyTerm substantiaNigra = uberonOntologyService.getTerm( "http://purl.obolibrary.org/obo/UBERON_0002038" );
        assertNotNull( substantiaNigra );
        OntologyTerm substantiaNigraParsCompacta = uberonOntologyService.getTerm( "http://purl.obolibrary.org/obo/UBERON_0001965" );
        assertNotNull( substantiaNigraParsCompacta );
        assertThat( uberonOntologyService.getChildren( Collections.singleton( brain ), false, true ) )
                .contains( substantiaNigra, substantiaNigraParsCompacta );
        assertThat( uberonOntologyService.getChildren( Collections.singleton( substantiaNigra ), false, true ) )
                .contains( substantiaNigraParsCompacta );
    }
}
