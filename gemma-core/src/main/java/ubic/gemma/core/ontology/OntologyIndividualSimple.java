package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.core.lang.Nullable;

public class OntologyIndividualSimple extends AbstractOntologyResourceSimple implements OntologyIndividual {

    private final OntologyTermSimple instanceOf;

    /**
     * Create a new simple ontology individual.
     * @param uri        a URI for the term, of null for a free-text term
     * @param label      a label for the term
     * @param instanceOf the term this individual is an instance of which must be simple since this class has to be
     *                   {@link java.io.Serializable}.
     */
    public OntologyIndividualSimple( @Nullable String uri, String label, OntologyTermSimple instanceOf ) {
        super( uri, label );
        this.instanceOf = instanceOf;
    }

    @Override
    public OntologyTermSimple getInstanceOf() {
        return instanceOf;
    }
}