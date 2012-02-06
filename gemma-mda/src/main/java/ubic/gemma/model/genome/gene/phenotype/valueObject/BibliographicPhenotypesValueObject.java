package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

public class BibliographicPhenotypesValueObject {

    private String geneNCBI = "";
    private String geneName = "";
    private Long evidenceDatabaseID = null;
    private Set<CharacteristicValueObject> phenotypesValues = new HashSet<CharacteristicValueObject>();

    public BibliographicPhenotypesValueObject() {
        super();
    }

    public BibliographicPhenotypesValueObject( String geneName, String geneNCBI,
            Set<CharacteristicValueObject> phenotypesValues ) {
        super();
        this.geneName = geneName;
        this.geneNCBI = geneNCBI;
        this.phenotypesValues = phenotypesValues;
    }

    public BibliographicPhenotypesValueObject( PhenotypeAssociation phenotypeAssociation ) {
        super();
        this.geneNCBI = phenotypeAssociation.getGene().getNcbiGeneId().toString();
        this.geneName = phenotypeAssociation.getGene().getName();
        this.evidenceDatabaseID = phenotypeAssociation.getId();
        for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {
            this.phenotypesValues.add( new CharacteristicValueObject( ( VocabCharacteristic ) cha ) );
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

    public Collection<CharacteristicValueObject> getPhenotypesValues() {
        return this.phenotypesValues;
    }

    public void setPhenotypesValues( Set<CharacteristicValueObject> phenotypesValues ) {
        this.phenotypesValues = phenotypesValues;
    }

    public String getGeneNCBI() {
        return this.geneNCBI;
    }

    public void setGeneNCBI( String geneNCBI ) {
        this.geneNCBI = geneNCBI;
    }

    public Long getEvidenceDatabaseID() {
        return this.evidenceDatabaseID;
    }

    public void setEvidenceDatabaseID( Long evidenceDatabaseID ) {
        this.evidenceDatabaseID = evidenceDatabaseID;
    }

}
