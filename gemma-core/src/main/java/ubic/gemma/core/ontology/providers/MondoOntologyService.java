package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.lang.Nullable;

/**
 * <a href="https://obofoundry.org/ontology/mondo.html">Mondo Disease Ontology</a>
 * @author poirigui
 */
public class MondoOntologyService extends AbstractOntologyService {

    @Override
    protected String getOntologyName() {
        return "Mondo Disease Ontology";
    }

    @Override
    protected String getOntologyUrl() {
        return Settings.getString( "url.mondoOntology" );
    }

    @Override
    protected boolean isOntologyEnabled() {
        return Settings.getBoolean( "load.mondoOntology" );
    }

    @Nullable
    @Override
    protected String getCacheName() {
        return "mondoOntology";
    }
}
