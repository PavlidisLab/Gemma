package ubic.gemma.core.ontology.providers;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.util.test.category.SlowTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GemmaOntologyServiceTest {

    @Test
    @Category(SlowTest.class)
    public void testInferenceInGemma() throws InterruptedException {
        GemmaOntologyService gemmaOntology = new GemmaOntologyService();
        gemmaOntology.setProcessImports( false );
        assertEquals( ubic.basecode.ontology.providers.OntologyService.LanguageLevel.FULL, gemmaOntology.getLanguageLevel() );
        assertEquals( ubic.basecode.ontology.providers.OntologyService.InferenceMode.TRANSITIVE, gemmaOntology.getInferenceMode() );
        gemmaOntology.initialize( true, false );
        OntologyTerm overexpression = gemmaOntology.getTerm( "http://gemma.msl.ubc.ca/ont/TGEMO_00004" );
        assertNotNull( overexpression );
        assertThat( gemmaOntology.getParents( Collections.singleton( overexpression ), false, false ) )
                .extracting( OntologyTerm::getUri )
                .containsExactlyInAnyOrder(
                        "http://purl.obolibrary.org/obo/BFO_0000001",
                        "http://purl.obolibrary.org/obo/BFO_0000003",
                        "http://purl.obolibrary.org/obo/BFO_0000015",
                        "http://www.ebi.ac.uk/efo/EFO_0000001",
                        "http://www.ebi.ac.uk/efo/EFO_0000510"
                );
    }
}
