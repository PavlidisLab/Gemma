package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;

import javax.annotation.Nullable;

class FactorValueOntologyIndividual implements OntologyIndividual {

    private final OntologyTermSimple instanceOf;
    private final String label;
    private final String uri;

    public FactorValueOntologyIndividual( FactorValue factorValue ) {
        if ( factorValue.getExperimentalFactor() != null && factorValue.getExperimentalFactor().getCategory() != null ) {
            String categoryUri = factorValue.getExperimentalFactor().getCategory().getCategoryUri();
            String category = factorValue.getExperimentalFactor().getCategory().getCategory();
            this.instanceOf = new OntologyTermSimple( categoryUri, category );
        } else {
            this.instanceOf = null;
        }
        this.label = FactorValueUtils.getSummaryString( factorValue );
        this.uri = FactorValueOntologyService.factorValueUri( factorValue.getId() );
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
