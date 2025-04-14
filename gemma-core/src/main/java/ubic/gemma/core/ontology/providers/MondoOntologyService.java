package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.jena.UrlOntologyService;
import ubic.basecode.ontology.providers.AbstractDelegatingOntologyService;
import ubic.gemma.core.config.Settings;

/**
 * <a href="https://obofoundry.org/ontology/mondo.html">Mondo Disease Ontology</a>
 * @author poirigui
 */
public class MondoOntologyService extends AbstractDelegatingOntologyService {

    public MondoOntologyService() {
        super( new UrlOntologyService( "Mondo Disease Ontology", Settings.getString( "url.mondoOntology" ),
                Settings.getBoolean( "load.mondoOntology" ), "mondoOntology" ) );
    }
}