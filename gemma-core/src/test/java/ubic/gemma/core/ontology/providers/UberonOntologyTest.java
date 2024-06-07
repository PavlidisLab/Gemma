package ubic.gemma.core.ontology.providers;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.UberonOntologyService;
import ubic.gemma.core.util.test.category.SlowTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UberonOntologyTest {

    @Test
    @Category(SlowTest.class)
    public void testSubstantiaNigraInUberon() {
        UberonOntologyService uberonOntologyService = new UberonOntologyService();
        assertEquals( ubic.basecode.ontology.providers.OntologyService.LanguageLevel.FULL, uberonOntologyService.getLanguageLevel() );
        assertEquals( ubic.basecode.ontology.providers.OntologyService.InferenceMode.TRANSITIVE, uberonOntologyService.getInferenceMode() );
        uberonOntologyService.initialize( true, false );
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
