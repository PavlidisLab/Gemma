package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.lang.Nullable;

/**
 * <a href="https://obofoundry.org/ontology/pato.html">Phenotype And Trait Ontology</a>
 */
public class PatoOntologyService extends AbstractOntologyService {

    @Override
    protected String getOntologyName() {
        return "Phenotype And Trait Ontology";
    }

    @Override
    protected String getOntologyUrl() {
        return Settings.getString( "url.patoOntology" );
    }

    @Override
    protected boolean isOntologyEnabled() {
        return Settings.getBoolean( "load.patoOntology" );
    }

    @Nullable
    @Override
    protected String getCacheName() {
        return "patoOntology";
    }
}
