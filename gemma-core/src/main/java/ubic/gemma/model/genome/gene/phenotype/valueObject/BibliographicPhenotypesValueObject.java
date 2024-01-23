/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Nicolas
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class BibliographicPhenotypesValueObject implements Comparable<BibliographicPhenotypesValueObject>, Serializable {

    private Integer geneNCBI = 0;
    private String geneName = "";
    private Long evidenceId = null;
    private Set<CharacteristicValueObject> phenotypesValues = new HashSet<>();

    public BibliographicPhenotypesValueObject() {
        super();
    }

    public BibliographicPhenotypesValueObject( PhenotypeAssociation phenotypeAssociation ) {
        super();
        this.geneNCBI = phenotypeAssociation.getGene().getNcbiGeneId();
        this.geneName = phenotypeAssociation.getGene().getName();
        this.evidenceId = phenotypeAssociation.getId();
        for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {
            this.phenotypesValues.add( new CharacteristicValueObject( cha ) );
        }
    }

    public BibliographicPhenotypesValueObject( String geneName, Integer geneNCBI,
            Set<CharacteristicValueObject> phenotypesValues ) {
        super();
        this.geneName = geneName;
        this.geneNCBI = geneNCBI;
        this.phenotypesValues = phenotypesValues;
    }

    public static Collection<BibliographicPhenotypesValueObject> phenotypeAssociations2BibliographicPhenotypesValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = new TreeSet<>();

        for ( PhenotypeAssociation phenotypeAssociation : phenotypeAssociations ) {

            BibliographicPhenotypesValueObject bibli = new BibliographicPhenotypesValueObject( phenotypeAssociation );
            bibliographicPhenotypesValueObjects.add( bibli );
        }

        return bibliographicPhenotypesValueObjects;
    }

    public Collection<Long> getAllIdOfPhenotype() {

        Collection<Long> allPhenotypesID = new HashSet<>();

        for ( CharacteristicValueObject phenotype : this.phenotypesValues ) {
            if ( phenotype.getId() == null )
                continue;
            allPhenotypesID.add( phenotype.getId() );
        }
        return allPhenotypesID;
    }

    public Long getEvidenceId() {
        return this.evidenceId;
    }

    public void setEvidenceId( Long evidenceId ) {
        this.evidenceId = evidenceId;
    }

    public String getGeneName() {
        return this.geneName;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    public Integer getGeneNCBI() {
        return this.geneNCBI;
    }

    public void setGeneNCBI( Integer geneNCBI ) {
        this.geneNCBI = geneNCBI;
    }

    public Set<CharacteristicValueObject> getPhenotypesValues() {
        return this.phenotypesValues;
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
