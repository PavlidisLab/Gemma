package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.basecode.providers.OntologyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OntologyServiceFactoryTest {

    @Test
    public void test() throws Exception {
        OntologyServiceFactory<PatoOntologyService> factory = new OntologyServiceFactory<>( PatoOntologyService.class );
        PatoOntologyService ontology = factory.createInstance();
        assertEquals( OntologyService.InferenceMode.TRANSITIVE, ontology.getInferenceMode() );
        assertTrue( ontology.getProcessImports() );
    }

    @Test
    public void testDisableInference() throws Exception {
        OntologyServiceFactory<PatoOntologyService> factory = new OntologyServiceFactory<>( PatoOntologyService.class );
        factory.setInferenceMode( OntologyService.InferenceMode.NONE );
        PatoOntologyService ontology = factory.createInstance();
        assertEquals( OntologyService.InferenceMode.NONE, ontology.getInferenceMode() );
        assertTrue( ontology.getProcessImports() );
    }
}