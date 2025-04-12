package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.jena.UrlOntologyService;
import ubic.basecode.ontology.providers.AbstractDelegatingOntologyService;
import ubic.gemma.core.config.Settings;

/**
 * <a href="https://obofoundry.org/ontology/pato.html">Phenotype And Trait Ontology</a>
 */
public class PatoOntologyService extends AbstractDelegatingOntologyService {

    public PatoOntologyService() {
        super( new UrlOntologyService( "Phenotype And Trait Ontology", Settings.getString( "url.patoOntology" ),
                Settings.getBoolean( "load.patoOntology" ), "patoOntology" ) );
    }
}
