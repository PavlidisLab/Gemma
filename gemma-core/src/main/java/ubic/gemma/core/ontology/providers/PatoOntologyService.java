package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.jena.AbstractOntologyMemoryBackedService;
import ubic.gemma.persistence.util.Settings;

public class PatoOntologyService extends AbstractOntologyMemoryBackedService {

    @Override
    protected String getOntologyName() {
        return "patoOntology";
    }

    @Override
    protected String getOntologyUrl() {
        return Settings.getString( "url.patoOntology" );
    }
}
