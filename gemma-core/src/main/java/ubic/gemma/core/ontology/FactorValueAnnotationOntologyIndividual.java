package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;

class FactorValueAnnotationOntologyIndividual implements OntologyIndividual {

    private final OntologyTerm instanceOf;
    private final String uri;
    private final String label;

    public FactorValueAnnotationOntologyIndividual( FactorValue factorValue, Long id, String uri, String label ) {
        this.instanceOf = new OntologyTermSimple( uri, label );
        this.uri = FactorValueOntologyService.factorValueAnnotationUri( factorValue.getId(), id );
        this.label = label;
    }

    @Override
    public OntologyTerm getInstanceOf() {
        return instanceOf;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isObsolete() {
        return false;
    }

    @Nullable
    @Override
    public Double getScore() {
        return null;
    }

    @Override
    public int compareTo( OntologyResource ontologyResource ) {
        return uri.compareTo( ontologyResource.getUri() );
    }
}
