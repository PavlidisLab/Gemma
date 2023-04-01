package ubic.gemma.model.genome;

import lombok.Getter;
import lombok.Setter;
import ubic.basecode.ontology.model.OntologyTerm;

import java.io.Serializable;

@Getter
@Setter
public class GeneOntologyTermValueObject implements Serializable {
    private String goId;
    private String label;
    private String uri;
    private String comment;
    private String localName;
    private String term;
    private boolean isObsolete;

    public GeneOntologyTermValueObject() {
        super();
    }

    public GeneOntologyTermValueObject( String goId, OntologyTerm term ) {
        this.goId = goId;
        this.label = term.getLabel();
        this.uri = term.getUri();
        this.comment = term.getComment();
        this.localName = term.getLocalName();
        this.term = term.getLabel();
        this.isObsolete = term.isObsolete();
    }
}
