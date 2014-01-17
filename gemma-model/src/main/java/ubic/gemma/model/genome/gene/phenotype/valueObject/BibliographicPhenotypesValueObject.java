package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

public class BibliographicPhenotypesValueObject implements Comparable<BibliographicPhenotypesValueObject> {

    public static Collection<BibliographicPhenotypesValueObject> phenotypeAssociations2BibliographicPhenotypesValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = new TreeSet<BibliographicPhenotypesValueObject>();

        for ( PhenotypeAssociation phenotypeAssociation : phenotypeAssociations ) {

            BibliographicPhenotypesValueObject bibli = new BibliographicPhenotypesValueObject( phenotypeAssociation );
            bibliographicPhenotypesValueObjects.add( bibli );
        }

        return bibliographicPhenotypesValueObjects;
    }

    private Integer geneNCBI = 0;
    private String geneName = "";
    private Long evidenceId = null;

    private Set<CharacteristicValueObject> phenotypesValues = new HashSet<CharacteristicValueObject>();

    public BibliographicPhenotypesValueObject() {
        super();
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

    public BibliographicPhenotypesValueObject( String geneName, Integer geneNCBI,
            Set<CharacteristicValueObject> phenotypesValues ) {
        super();
        this.geneName = geneName;
        this.geneNCBI = geneNCBI;
        this.phenotypesValues = phenotypesValues;
    }

    public Collection<Long> getAllIdOfPhenotype() {

        Collection<Long> allPhenotypesID = new HashSet<Long>();

        for ( CharacteristicValueObject phenotype : this.phenotypesValues ) {
            if ( phenotype.getId() == null ) continue;
            allPhenotypesID.add( phenotype.getId() );
        }
        return allPhenotypesID;
    }

    public Long getEvidenceId() {
        return this.evidenceId;
    }

    public String getGeneName() {
        return this.geneName;
    }

    public Integer getGeneNCBI() {
        return this.geneNCBI;
    }

    public Set<CharacteristicValueObject> getPhenotypesValues() {
        return this.phenotypesValues;
    }

    public void setEvidenceId( Long evidenceId ) {
        this.evidenceId = evidenceId;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    public void setGeneNCBI( Integer geneNCBI ) {
        this.geneNCBI = geneNCBI;
    }

    public void setPhenotypesValues( Set<CharacteristicValueObject> phenotypesValues ) {
        this.phenotypesValues = phenotypesValues;
    }

    @Override
    public int compareTo( BibliographicPhenotypesValueObject bibliographicPhenotypesValueObject ) {

        if ( this.geneName.compareTo( bibliographicPhenotypesValueObject.getGeneName() ) == 0
                && this.evidenceId.compareTo( bibliographicPhenotypesValueObject.getEvidenceId() ) == 0 ) {
            return 0;
        } else if ( this.geneName.compareTo( bibliographicPhenotypesValueObject.getGeneName() ) != 0 ) {
            return this.geneName.compareTo( bibliographicPhenotypesValueObject.getGeneName() );
        } else if ( this.evidenceId.compareTo( bibliographicPhenotypesValueObject.getEvidenceId() ) != 0 ) {
            return this.evidenceId.compareTo( bibliographicPhenotypesValueObject.getEvidenceId() );
        }

        return -1;

    }

}
