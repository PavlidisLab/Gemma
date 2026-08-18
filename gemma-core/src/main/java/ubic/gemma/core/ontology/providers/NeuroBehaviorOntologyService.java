package ubic.gemma.core.ontology.providers;

/**
 * <a href="https://obofoundry.org/ontology/nbo.html">Neuro Behavior Ontology</a> — behavioural
 * processes and the paradigms used to elicit them.
 * <p>
 * Loaded for treatment/experimental-condition annotation, where the conventional sources come up
 * empty: {@code fear conditioning} is NBO:0000209 exactly, and neither EFO nor any disease ontology
 * carries it. Small (~5.6 MB). Disabled by default, like the other optional providers.
 */
public class NeuroBehaviorOntologyService extends AbstractBaseCodeOntologyService {

    public NeuroBehaviorOntologyService() {
        super( "Neuro Behavior Ontology", "neuroBehaviorOntology" );
    }
}
