package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

public class BibliographicPhenotypesValueObject {

    private Integer geneNCBI = 0;
    private String geneName = "";
    private Long evidenceId = null;
    private Set<CharacteristicValueObject> phenotypesValues = new HashSet<CharacteristicValueObject>();

    public BibliographicPhenotypesValueObject() {
        super();
    }

    public BibliographicPhenotypesValueObject( String geneName, Integer geneNCBI,
            Set<CharacteristicValueObject> phenotypesValues ) {
        super();
        this.geneName = geneName;
        this.geneNCBI = geneNCBI;
        this.phenotypesValues = phenotypesValues;
    }

    public BibliographicPhenotypesValueObject( PhenotypeAssociation phenotypeAssociation ) {
        super();
        this.geneNCBI = phenotypeAssociation.getGene().getNcbiGeneId();
        this.geneName = phenotypeAssociation.getGene().getName();
        this.evidenceId = phenotypeAssociation.getId();
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

    public Set<CharacteristicValueObject> getPhenotypesValues() {
        return this.phenotypesValues;
    }

    public void setPhenotypesValues( Set<CharacteristicValueObject> phenotypesValues ) {
        this.phenotypesValues = phenotypesValues;
    }

    public Integer getGeneNCBI() {
        return this.geneNCBI;
    }

    public void setGeneNCBI( Integer geneNCBI ) {
        this.geneNCBI = geneNCBI;
    }

    public Long getEvidenceId() {
        return this.evidenceId;
    }

    public void setEvidenceId( Long evidenceId ) {
        this.evidenceId = evidenceId;
    }

    public Collection<Long> getAllIdOfPhenotype() {

        Collection<Long> allPhenotypesID = new HashSet<Long>();

        for ( CharacteristicValueObject phenotype : this.phenotypesValues ) {
            if (phenotype.getId() == null) continue;
            allPhenotypesID.add( phenotype.getId() );
        }
        return allPhenotypesID;
    }
}
