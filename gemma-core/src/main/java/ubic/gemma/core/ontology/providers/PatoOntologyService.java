package ubic.gemma.core.ontology.providers;

import ubic.gemma.core.ontology.basecode.jena.UrlOntologyService;
import ubic.gemma.core.ontology.basecode.providers.AbstractDelegatingOntologyService;
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
