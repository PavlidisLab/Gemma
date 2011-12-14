package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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

    public static Collection<BibliographicPhenotypesValueObject> phenotypeAssociations2BibliographicPhenotypesValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Map<String, BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObject = new HashMap<String, BibliographicPhenotypesValueObject>();

        for ( PhenotypeAssociation phenotypeAssociation : phenotypeAssociations ) {

            String geneName = phenotypeAssociation.getGene().getName();

            Collection<String> phenotypesValues = new HashSet<String>();

            for ( Characteristic characteristic : phenotypeAssociation.getPhenotypes() ) {

                phenotypesValues.add( characteristic.getValue() );
            }

            if ( bibliographicPhenotypesValueObject.get( geneName ) != null ) {

                bibliographicPhenotypesValueObject.get( geneName ).getPhenotypesValues().addAll( phenotypesValues );

            } else {

                BibliographicPhenotypesValueObject bibli = new BibliographicPhenotypesValueObject( geneName,
                        phenotypesValues );

                bibliographicPhenotypesValueObject.put( geneName, bibli );
            }
        }

        return bibliographicPhenotypesValueObject.values();
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

    public void addPhenotypesValues( Collection<String> addedPhenotypesValues ) {
        this.phenotypesValues.addAll( addedPhenotypesValues );
    }

}
