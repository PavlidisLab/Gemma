package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.providers.UberonOntologyService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UberonOntologyTest {

    @Test
    @Tag("slow")
    public void testSubstantiaNigraInUberon() {
        UberonOntologyService uberonOntologyService = new UberonOntologyService();
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.LanguageLevel.FULL, uberonOntologyService.getLanguageLevel() );
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.InferenceMode.TRANSITIVE, uberonOntologyService.getInferenceMode() );
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
