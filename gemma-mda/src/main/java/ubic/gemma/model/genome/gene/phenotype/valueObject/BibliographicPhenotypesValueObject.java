package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;

public class BibliographicPhenotypesValueObject {

    private String geneName = "";
    private Collection<String> phenotypesValues = new HashSet<String>();

    public BibliographicPhenotypesValueObject() {
        super();
    }

    public BibliographicPhenotypesValueObject( String geneName, Collection<String> phenotypesValues ) {
        super();
        this.geneName = geneName;
        this.phenotypesValues = phenotypesValues;
    }

    public BibliographicPhenotypesValueObject( PhenotypeAssociation phenotypeAssociation ) {
        super();
        this.geneName = phenotypeAssociation.getName();
        for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {
            this.phenotypesValues.add( cha.getValue() );
        }
    }

    public static Collection<BibliographicPhenotypesValueObject> phenotypeAssociations2BibliographicPhenotypesValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = new HashSet<BibliographicPhenotypesValueObject>();

        for ( PhenotypeAssociation phenotypeAssociation : phenotypeAssociations ) {

            BibliographicPhenotypesValueObject bibli = new BibliographicPhenotypesValueObject( phenotypeAssociation );
            bibliographicPhenotypesValueObjects.add( bibli );
        }

        return bibliographicPhenotypesValueObjects;
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
