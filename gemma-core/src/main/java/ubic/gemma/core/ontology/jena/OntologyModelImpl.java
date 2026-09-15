package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntModel;
import ubic.gemma.core.ontology.model.OntologyModel;

class OntologyModelImpl implements OntologyModel {

    private final OntModel ontModel;

    public OntologyModelImpl( OntModel ontModel ) {
        this.ontModel = ontModel;
    }

    public OntModel getOntModel() {
        return ontModel;
    }
}
