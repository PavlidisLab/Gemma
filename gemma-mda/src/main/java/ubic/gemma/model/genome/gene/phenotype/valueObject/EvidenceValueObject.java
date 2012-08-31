/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

/**
 * Parent class of all evidence value objects
 * 
 * @version $Id$
 * @author nicolas
 */
public class EvidenceValueObject implements Comparable<EvidenceValueObject> {

    /**
     * Convert an collection of evidence entities to their corresponding value objects
     * 
     * @param phenotypeAssociations The List of entities we need to convert to value object
     * @return Collection<EvidenceValueObject> the converted results
     */
    public static Collection<EvidenceValueObject> convert2ValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<EvidenceValueObject> returnEvidenceVO = new HashSet<EvidenceValueObject>();

        if ( phenotypeAssociations != null ) {

            for ( PhenotypeAssociation phe : phenotypeAssociations ) {

                EvidenceValueObject evidence = convert2ValueObjects( phe );

                if ( evidence != null ) {
                    returnEvidenceVO.add( evidence );
                }
            }
        }
        return returnEvidenceVO;
    }

    /**
     * Convert an evidence entity to its corresponding value object
     * 
     * @param phe The phenotype Entity
     * @return Collection<EvidenceValueObject> its corresponding value object
     */
    public static EvidenceValueObject convert2ValueObjects( PhenotypeAssociation phe ) {

        EvidenceValueObject evidence = null;

        if ( phe instanceof ExperimentalEvidence ) {
            evidence = new ExperimentalEvidenceValueObject( ( ExperimentalEvidence ) phe );
        } else if ( phe instanceof GenericEvidence ) {
            evidence = new GenericEvidenceValueObject( ( GenericEvidence ) phe );
        } else if ( phe instanceof LiteratureEvidence ) {
            evidence = new LiteratureEvidenceValueObject( ( LiteratureEvidence ) phe );
        } else if ( phe instanceof DifferentialExpressionEvidence ) {
            // TODO
        }

        return evidence;
    }

    private Long id = null;
    private String description = "";
    private String evidenceCode = null;
    private boolean isNegativeEvidence = false;

    private String className = "";
    private SortedSet<CharacteristicValueObject> phenotypes = null;
    private EvidenceSourceValueObject evidenceSource = null;

    private String externalUrl = "";

    // last modified date of the evidence
    private Long lastUpdated = null;
    // security for the evidence
    private EvidenceSecurityValueObject evidenceSecurityValueObject = null;

    // linked to what gene
    private Long geneId = null;
    private Integer geneNCBI = null;
    private String geneOfficialSymbol = "";
    private String geneOfficialName = "";
    private String taxonCommonName = "";
    private boolean isHomologueEvidence = false;

    private boolean containQueryPhenotype = false;

    private ScoreValueObject scoreValueObject = new ScoreValueObject();

    public EvidenceValueObject() {
        super();
    }

    /** set fields common to all evidence. Entity to Value Object */
    protected EvidenceValueObject( PhenotypeAssociation phenotypeAssociation ) {

        this.className = this.getClass().getSimpleName();
        this.id = phenotypeAssociation.getId();
        this.description = phenotypeAssociation.getDescription();
        this.evidenceCode = phenotypeAssociation.getEvidenceCode().getValue();
        this.isNegativeEvidence = phenotypeAssociation.getIsNegativeEvidence();
        this.taxonCommonName = phenotypeAssociation.getGene().getTaxon().getCommonName();

        if ( phenotypeAssociation.getEvidenceSource() != null ) {
            this.evidenceSource = new EvidenceSourceValueObject( phenotypeAssociation.getEvidenceSource() );
        }

        this.phenotypes = new TreeSet<CharacteristicValueObject>();

        for ( Characteristic c : phenotypeAssociation.getPhenotypes() ) {

            CharacteristicValueObject characteristicVO = new CharacteristicValueObject( ( VocabCharacteristicImpl ) c );
            characteristicVO.setId( c.getId() );
            this.phenotypes.add( characteristicVO );
        }

        this.lastUpdated = phenotypeAssociation.getStatus().getLastUpdateDate().getTime();
        this.geneId = phenotypeAssociation.getGene().getId();
        this.geneNCBI = phenotypeAssociation.getGene().getNcbiGeneId();
        this.geneOfficialSymbol = phenotypeAssociation.getGene().getOfficialSymbol();
        this.geneOfficialName = phenotypeAssociation.getGene().getOfficialName();

        if ( phenotypeAssociation.getScoreType() != null || phenotypeAssociation.getStrength() != null ) {

            String scoreTypeName = "";

            if ( phenotypeAssociation.getScoreType() != null ) {
                scoreTypeName = phenotypeAssociation.getScoreType().getName();
            }

            this.scoreValueObject = new ScoreValueObject( phenotypeAssociation.getStrength(),
                    phenotypeAssociation.getScore(), scoreTypeName );
        }
    }

    protected EvidenceValueObject( Integer geneNCBI, SortedSet<CharacteristicValueObject> phenotypes,
            String description, String evidenceCode, boolean isNegativeEvidence,
            EvidenceSourceValueObject evidenceSource ) {
        super();
        this.description = description;
        this.evidenceCode = evidenceCode;
        this.isNegativeEvidence = isNegativeEvidence;
        this.phenotypes = phenotypes;
        this.evidenceSource = evidenceSource;
        this.geneNCBI = geneNCBI;
    }

    public String getExternalUrl() {
        return this.externalUrl;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getDescription() {
        return this.description;
    }

    public String getEvidenceCode() {
        return this.evidenceCode;
    }

    public boolean getIsNegativeEvidence() {
        return this.isNegativeEvidence;
    }

    public SortedSet<CharacteristicValueObject> getPhenotypes() {
        return this.phenotypes;
    }

    public Set<String> getPhenotypesValueUri() {

        Set<String> phenotypesValueUri = new HashSet<String>();

        for ( CharacteristicValueObject characteristicValueObject : this.phenotypes ) {
            phenotypesValueUri.add( characteristicValueObject.getValueUri() );
        }

        return phenotypesValueUri;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName( String className ) {
        this.className = className;
    }

    public ScoreValueObject getScoreValueObject() {
        return this.scoreValueObject;
    }

    public void setScoreValueObject( ScoreValueObject scoreValueObject ) {
        this.scoreValueObject = scoreValueObject;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public void setIsNegativeEvidence( boolean isNegativeEvidence ) {
        this.isNegativeEvidence = isNegativeEvidence;
    }

    public void setPhenotypes( SortedSet<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public void setEvidenceSource( EvidenceSourceValueObject evidenceSource ) {
        this.evidenceSource = evidenceSource;
    }

    public EvidenceSourceValueObject getEvidenceSource() {
        return this.evidenceSource;
    }

    public Long getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated( Long lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    public EvidenceSecurityValueObject getEvidenceSecurityValueObject() {
        return this.evidenceSecurityValueObject;
    }

    public void setEvidenceSecurityValueObject( EvidenceSecurityValueObject evidenceSecurityValueObject ) {
        this.evidenceSecurityValueObject = evidenceSecurityValueObject;
    }

    public Long getGeneId() {
        return this.geneId;
    }

    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    public Integer getGeneNCBI() {
        return this.geneNCBI;
    }

    public void setGeneNCBI( Integer geneNCBI ) {
        this.geneNCBI = geneNCBI;
    }

    public String getGeneOfficialSymbol() {
        return this.geneOfficialSymbol;
    }

    public void setGeneOfficialSymbol( String geneOfficialSymbol ) {
        this.geneOfficialSymbol = geneOfficialSymbol;
    }

    public String getTaxonCommonName() {
        return this.taxonCommonName;
    }

    public void setTaxonCommonName( String taxonCommonName ) {
        this.taxonCommonName = taxonCommonName;
    }

    public boolean isHomologueEvidence() {
        return this.isHomologueEvidence;
    }

    public void setHomologueEvidence( boolean isHomologueEvidence ) {
        this.isHomologueEvidence = isHomologueEvidence;
    }

    public String getGeneOfficialName() {
        return this.geneOfficialName;
    }

    public void setGeneOfficialName( String geneOfficialName ) {
        this.geneOfficialName = geneOfficialName;
    }

    public boolean isContainQueryPhenotype() {
        return this.containQueryPhenotype;
    }

    public void setContainQueryPhenotype( boolean containQueryPhenotype ) {
        this.containQueryPhenotype = containQueryPhenotype;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        if ( this.phenotypes != null ) {
            for ( CharacteristicValueObject phenotype : this.phenotypes ) {
                result = result + phenotype.hashCode();
            }
        }

        result = result + ( ( this.evidenceSource == null ) ? 0 : this.evidenceSource.hashCode() );
        result = result + ( ( this.geneNCBI == null ) ? 0 : this.geneNCBI.hashCode() );

        return prime * result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        EvidenceValueObject other = ( EvidenceValueObject ) obj;

        if ( this.phenotypes.size() != other.phenotypes.size() ) {
            return false;
        }

        Set<String> otherPhenotypesValueUri = other.getPhenotypesValueUri();

        for ( CharacteristicValueObject characteristicValueObject : this.phenotypes ) {
            if ( !otherPhenotypesValueUri.contains( characteristicValueObject.getValueUri() ) ) {
                return false;
            }
        }

        if ( this.evidenceSource == null ) {
            if ( other.evidenceSource != null ) return false;
        } else if ( !this.evidenceSource.equals( other.evidenceSource ) ) return false;

        if ( this.geneNCBI == null ) {
            if ( other.geneNCBI != null ) return false;
        } else if ( !this.geneNCBI.equals( other.geneNCBI ) ) return false;

        return true;
    }

    protected int comparePropertiesTo( EvidenceValueObject evidenceValueObject ) {
        if ( this == evidenceValueObject ) return 0;

        if ( this.containQueryPhenotype && !evidenceValueObject.isContainQueryPhenotype() ) {
            return -1;
        } else if ( !this.containQueryPhenotype && evidenceValueObject.isContainQueryPhenotype() ) {
            return 1;
        }

        if ( !this.isHomologueEvidence && evidenceValueObject.isHomologueEvidence ) return -1;
        if ( this.isHomologueEvidence && !evidenceValueObject.isHomologueEvidence ) return 1;

        if ( ( this.phenotypes != null && this.phenotypes.size() > 0 )
                && ( evidenceValueObject.phenotypes == null || evidenceValueObject.phenotypes.size() == 0 ) ) {
            return -1;
        }

        if ( ( this.phenotypes == null || this.phenotypes.size() == 0 )
                && ( evidenceValueObject.phenotypes != null && evidenceValueObject.phenotypes.size() > 0 ) ) {
            return 1;
        }

        int comparison = 0;

        if ( this.phenotypes != null && evidenceValueObject.phenotypes != null ) {
            Iterator<CharacteristicValueObject> thisIterator = this.phenotypes.iterator();
            Iterator<CharacteristicValueObject> otherIterator = evidenceValueObject.phenotypes.iterator();

            while ( true ) {
                boolean thisHasNext = thisIterator.hasNext();
                boolean otherHasNext = otherIterator.hasNext();

                if ( !thisHasNext && otherHasNext ) return -1;
                if ( thisHasNext && !otherHasNext ) return 1;
                if ( !thisHasNext && !otherHasNext ) break;

                comparison = thisIterator.next().compareTo( otherIterator.next() );
                if ( comparison != 0 ) return comparison;
            }
        }

        if ( !this.isNegativeEvidence && evidenceValueObject.isNegativeEvidence ) return -1;
        if ( this.isNegativeEvidence && !evidenceValueObject.isNegativeEvidence ) return 1;

        comparison = this.className.compareTo( evidenceValueObject.className );
        if ( comparison != 0 ) return comparison;

        comparison = this.evidenceCode.compareTo( evidenceValueObject.evidenceCode );
        if ( comparison != 0 ) return comparison;

        return 0;
    }

    @Override
    public int compareTo( EvidenceValueObject evidenceValueObject ) {
        int comparison = comparePropertiesTo( evidenceValueObject );

        if ( comparison == 0 ) {
            // Use id for comparison so that each evidence object is unique.
            comparison = this.getId().compareTo( evidenceValueObject.getId() );
        }

        return comparison;
    }
}