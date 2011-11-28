package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;

public class BibliographicPhenotypesValueObject {

    private String geneName = "";
    private Collection<String> phenotypesValues = new HashSet<String>();
    
    
    public BibliographicPhenotypesValueObject(){
        super();
    }

    public BibliographicPhenotypesValueObject( String geneName, Collection<String> phenotypesValues ) {
        super();
        this.geneName = geneName;
        this.phenotypesValues = phenotypesValues;
    }

    public String getGeneName() {
        return this.geneName;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    public Collection<String> getPhenotypesValues() {
        return this.phenotypesValues;
    }

    public void setPhenotypesValues( Collection<String> phenotypesValues ) {
        this.phenotypesValues = phenotypesValues;
    }

}
