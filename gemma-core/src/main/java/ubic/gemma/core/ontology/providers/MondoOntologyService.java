package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.jena.AbstractOntologyMemoryBackedService;
import ubic.gemma.persistence.util.Settings;

/**
 * MONDO ontology.
 * @author poirigui
 */
public class MondoOntologyService extends AbstractOntologyMemoryBackedService {

    @Override
    protected String getOntologyName() {
        return "mondoOntology";
    }

    @Override
    protected String getOntologyUrl() {
        return Settings.getString( "url.mondoOntology" );
    }
}
