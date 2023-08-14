package ubic.gemma.core.ontology.providers;

import org.junit.Test;
import ubic.basecode.ontology.providers.OntologyService;

import static org.junit.Assert.*;

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
        factory.setEnableInference( false );
        PatoOntologyService ontology = factory.createInstance();
        assertEquals( OntologyService.InferenceMode.NONE, ontology.getInferenceMode() );
        assertTrue( ontology.getProcessImports() );
    }
}