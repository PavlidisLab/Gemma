package ubic.gemma.model.genome;

import ubic.basecode.ontology.model.OntologyTerm;

@SuppressWarnings("unused") // Getters used by RS serializer
public class GeneOntologyTermValueObject {
    private String goId;
    private String label;
    private String uri;
    private String comment;
    private String localName;
    private String term;
    private boolean isObsolete;

    public GeneOntologyTermValueObject( String goId, OntologyTerm term ) {
        this.goId = goId;
        this.label = term.getLabel();
        this.uri = term.getUri();
        this.comment = term.getComment();
        this.localName = term.getLocalName();
        this.term = term.getTerm();
        this.isObsolete = term.isTermObsolete();
    }

    public String getGoId() {
        return goId;
    }

    public String getLabel() {
        return label;
    }

    public String getUri() {
        return uri;
    }

    public String getComment() {
        return comment;
    }

    public String getLocalName() {
        return localName;
    }

    public String getTerm() {
        return term;
    }

    public boolean isObsolete() {
        return isObsolete;
    }
}
