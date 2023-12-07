package ubic.gemma.core.ontology.providers;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.search.SearchException;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;

public class DiseaseOntologyTest {

    @Test
    public void test() throws SearchException, OntologySearchException, InterruptedException, IOException {
        DiseaseOntologyService diseaseOntologyService = new DiseaseOntologyService();
        assertEquals( ubic.basecode.ontology.providers.OntologyService.LanguageLevel.FULL, diseaseOntologyService.getLanguageLevel() );
        assertEquals( ubic.basecode.ontology.providers.OntologyService.InferenceMode.TRANSITIVE, diseaseOntologyService.getInferenceMode() );
        diseaseOntologyService.initialize( new ClassPathResource( "/data/loader/ontology/dotest.owl.xml" ).getInputStream(), false );

        Collection<OntologyTerm> name = diseaseOntologyService.findTerm( "diarrhea" );

        assertFalse( name.isEmpty() );

        OntologyTerm term;

        // Actinomadura madurae infectious disease
        term = diseaseOntologyService.getTerm( "http://purl.obolibrary.org/obo/DOID_0050001" );
        assertNotNull( term );
        assertTrue( term.isObsolete() );

        // inflammatory diarrhea, not obsolete as of May 2012.
        term = diseaseOntologyService.getTerm( "http://purl.obolibrary.org/obo/DOID_0050132" );
        assertNotNull( term );
        assertFalse( term.isObsolete() );
    }
}
