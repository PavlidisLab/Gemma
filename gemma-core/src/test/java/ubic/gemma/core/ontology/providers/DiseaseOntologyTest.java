package ubic.gemma.core.ontology.providers;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.providers.DiseaseOntologyService;
import ubic.gemma.core.ontology.basecode.search.OntologySearchException;
import ubic.gemma.core.ontology.basecode.search.OntologySearchResult;
import ubic.gemma.core.search.SearchException;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;

public class DiseaseOntologyTest {

    /**
     * URI-lookup assertions: do not depend on the Lucene indexer, so they survive Phase 2's
     * teardown of the search subsystem. Split out from the original {@code test()} method
     * (which still mixes in a {@code findTerm} call — see below).
     */
    @Test
    public void testGetTermByUri() throws InterruptedException, IOException {
        DiseaseOntologyService diseaseOntologyService = new DiseaseOntologyService();
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.LanguageLevel.FULL, diseaseOntologyService.getLanguageLevel() );
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.InferenceMode.TRANSITIVE, diseaseOntologyService.getInferenceMode() );
        diseaseOntologyService.initialize( new ClassPathResource( "/data/loader/ontology/dotest.owl.xml" ).getInputStream(), true );

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

    @Ignore("Blocked on Phase 3 search-subsystem rebuild: baseCode's renovations branch gutted "
            + "the Lucene 3 ontology indexer (a Lucene 9 / HS 7 / OpenSearch rebuild is its own "
            + "phase). DiseaseOntologyService.findTerm now returns an empty collection. "
            + "Tracked in PHASE3_TEST_TRIAGE.md.")
    @Test
    public void testFindTerm() throws SearchException, OntologySearchException, InterruptedException, IOException {
        DiseaseOntologyService diseaseOntologyService = new DiseaseOntologyService();
        diseaseOntologyService.initialize( new ClassPathResource( "/data/loader/ontology/dotest.owl.xml" ).getInputStream(), true );
        Collection<OntologySearchResult<OntologyTerm>> name = diseaseOntologyService.findTerm( "diarrhea", 100 );
        assertFalse( name.isEmpty() );
    }
}
