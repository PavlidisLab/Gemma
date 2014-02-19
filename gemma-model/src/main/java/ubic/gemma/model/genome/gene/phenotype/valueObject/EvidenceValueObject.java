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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.gemma.model.association.phenotype.PhenotypeAssociationPublication;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeMappingType;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

/**
 * Parent class of all evidence value objects
 * 
 * @version $Id$
 * @author nicolas
 */
public class EvidenceValueObject implements Comparable<EvidenceValueObject> {

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

    private String originalPhenotype = "";
    private String phenotypeMapping = "";

    private SortedSet<PhenotypeAssPubValueObject> phenotypeAssPubVO = new TreeSet<PhenotypeAssPubValueObject>();

    private ScoreValueObject scoreValueObject = new ScoreValueObject();

    public EvidenceValueObject() {
        super();
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

    /** set fields common to all evidence. Entity to Value Object */
    protected EvidenceValueObject( PhenotypeAssociation phenotypeAssociation ) {

        this.className = this.getClass().getSimpleName();
        this.id = phenotypeAssociation.getId();
        this.description = phenotypeAssociation.getDescription();
        this.evidenceCode = phenotypeAssociation.getEvidenceCode().getValue();
        this.isNegativeEvidence = phenotypeAssociation.getIsNegativeEvidence();
        this.taxonCommonName = phenotypeAssociation.getGene().getTaxon().getCommonName();
        this.originalPhenotype = phenotypeAssociation.getOriginalPhenotype();

        if ( phenotypeAssociation.getMappingType() != null ) {
            this.phenotypeMapping = phenotypeAssociation.getMappingType().getValue();
        }

        if ( phenotypeAssociation.getEvidenceSource() != null ) {
            this.evidenceSource = new EvidenceSourceValueObject( phenotypeAssociation.getEvidenceSource() );
        }

        this.phenotypes = new TreeSet<CharacteristicValueObject>();

        for ( Characteristic c : phenotypeAssociation.getPhenotypes() ) {

            CharacteristicValueObject characteristicVO = new CharacteristicValueObject( ( VocabCharacteristicImpl ) c );
            characteristicVO.setId( c.getId() );
            this.phenotypes.add( characteristicVO );
        }

        for ( PhenotypeAssociationPublication phenotypeAssociationPublication : phenotypeAssociation
                .getPhenotypeAssociationPublications() ) {

            PhenotypeAssPubValueObject phenotypeAss = new PhenotypeAssPubValueObject( phenotypeAssociationPublication );
            this.phenotypeAssPubVO.add( phenotypeAss );
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

    public int compareEvidenceSource( EvidenceValueObject evidenceValueObject ) {

        if ( this.evidenceSource != null && evidenceValueObject.getEvidenceSource() != null ) {

            if ( !this.evidenceSource.equals( evidenceValueObject.getEvidenceSource() ) ) {
                return -1;
            }
        } else if ( this.evidenceSource == null && evidenceValueObject.getEvidenceSource() != null ) {
            return -1;
        } else if ( this.evidenceSource != null && evidenceValueObject.getEvidenceSource() == null ) {
            return -1;
        }
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

        if ( this.phenotypeAssPubVO.size() != other.phenotypeAssPubVO.size() ) {
            return false;
        }

        for ( PhenotypeAssPubValueObject phenotypeAssPubValueObject : other.phenotypeAssPubVO ) {
            if ( !this.phenotypeAssPubVO.contains( phenotypeAssPubValueObject ) ) {
                return false;
            }
        }

        return true;
    }

    public String getClassName() {
        return this.className;
    }

    public String getDescription() {
        return this.description;
    }

    public String getEvidenceCode() {
        return this.evidenceCode;
    }

    public EvidenceSecurityValueObject getEvidenceSecurityValueObject() {
        return this.evidenceSecurityValueObject;
    }

    public EvidenceSourceValueObject getEvidenceSource() {
        return this.evidenceSource;
    }

    public String getExternalUrl() {
        return this.externalUrl;
    }

    public Long getGeneId() {
        return this.geneId;
    }

    public Integer getGeneNCBI() {
        return this.geneNCBI;
    }

    public String getGeneOfficialName() {
        return this.geneOfficialName;
    }

    public String getGeneOfficialSymbol() {
        return this.geneOfficialSymbol;
    }

    public Long getId() {
        return this.id;
    }

    public boolean getIsNegativeEvidence() {
        return this.isNegativeEvidence;
    }

    public Long getLastUpdated() {
        return this.lastUpdated;
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

    public ScoreValueObject getScoreValueObject() {
        return this.scoreValueObject;
    }

    public String getTaxonCommonName() {
        return this.taxonCommonName;
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

    public boolean isContainQueryPhenotype() {
        return this.containQueryPhenotype;
    }

    public boolean isHomologueEvidence() {
        return this.isHomologueEvidence;
    }

    public void setClassName( String className ) {
        this.className = className;
    }

    public void setContainQueryPhenotype( boolean containQueryPhenotype ) {
        this.containQueryPhenotype = containQueryPhenotype;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public void setEvidenceSecurityValueObject( EvidenceSecurityValueObject evidenceSecurityValueObject ) {
        this.evidenceSecurityValueObject = evidenceSecurityValueObject;
    }

    public void setEvidenceSource( EvidenceSourceValueObject evidenceSource ) {
        this.evidenceSource = evidenceSource;
    }

    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    public void setGeneNCBI( Integer geneNCBI ) {
        this.geneNCBI = geneNCBI;
    }

    public void setGeneOfficialName( String geneOfficialName ) {
        this.geneOfficialName = geneOfficialName;
    }

    public void setGeneOfficialSymbol( String geneOfficialSymbol ) {
        this.geneOfficialSymbol = geneOfficialSymbol;
    }

    public void setHomologueEvidence( boolean isHomologueEvidence ) {
        this.isHomologueEvidence = isHomologueEvidence;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setIsNegativeEvidence( boolean isNegativeEvidence ) {
        this.isNegativeEvidence = isNegativeEvidence;
    }

    public void setLastUpdated( Long lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    public void setPhenotypes( SortedSet<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public void setScoreValueObject( ScoreValueObject scoreValueObject ) {
        this.scoreValueObject = scoreValueObject;
    }

    public void setTaxonCommonName( String taxonCommonName ) {
        this.taxonCommonName = taxonCommonName;
    }

    @Override
    public String toString() {
        return "EvidenceValueObject [id=" + id + ", description=" + description + ", evidenceCode=" + evidenceCode
                + ", isNegativeEvidence=" + isNegativeEvidence + ", className=" + className + ", phenotypes="
                + phenotypes + ", evidenceSource=" + evidenceSource + ", externalUrl=" + externalUrl + ", lastUpdated="
                + lastUpdated + ", evidenceSecurityValueObject=" + evidenceSecurityValueObject + ", geneId=" + geneId
                + ", geneNCBI=" + geneNCBI + ", geneOfficialSymbol=" + geneOfficialSymbol + ", geneOfficialName="
                + geneOfficialName + ", taxonCommonName=" + taxonCommonName + ", isHomologueEvidence="
                + isHomologueEvidence + ", containQueryPhenotype=" + containQueryPhenotype + ", scoreValueObject="
                + scoreValueObject + "]";
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

        // sort them using the score server side

        if ( this.getScoreValueObject().getStrength() != null
                && evidenceValueObject.getScoreValueObject().getStrength() != null ) {

            if ( this.getScoreValueObject().getStrength() != null
                    && evidenceValueObject.getScoreValueObject().getStrength() == null ) {
                return -1;
            } else if ( this.getScoreValueObject().getStrength() == null
                    && evidenceValueObject.getScoreValueObject().getStrength() != null ) {
                return 1;
            } else if ( this.getScoreValueObject().getStrength() > evidenceValueObject.getScoreValueObject()
                    .getStrength() ) {
                return -1;
            } else if ( this.getScoreValueObject().getStrength() < evidenceValueObject.getScoreValueObject()
                    .getStrength() ) {
                return 1;
            }
        }

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

        comparison = this.evidenceCode.compareTo( evidenceValueObject.evidenceCode );
        if ( comparison != 0 ) return comparison;

        comparison = compareEvidenceSource( evidenceValueObject );
        if ( comparison != 0 ) return comparison;

        // compare their pubmeds

        if ( this.phenotypeAssPubVO.size() != evidenceValueObject.phenotypeAssPubVO.size() ) {
            return -1;
        }

        for ( PhenotypeAssPubValueObject phenotypeAssPubValueObject : this.phenotypeAssPubVO ) {
            if ( !evidenceValueObject.phenotypeAssPubVO.contains( phenotypeAssPubValueObject ) ) {
                return -1;
            }
        }

        return 0;
    }

    public SortedSet<PhenotypeAssPubValueObject> getPhenotypeAssPubVO() {
        return phenotypeAssPubVO;
    }

    public void setPhenotypeAssPubVO( SortedSet<PhenotypeAssPubValueObject> phenotypeAssPubVO ) {
        this.phenotypeAssPubVO = phenotypeAssPubVO;
    }

    public String getOriginalPhenotype() {
        return originalPhenotype;
    }

    public void setOriginalPhenotype( String originalPhenotype ) {
        this.originalPhenotype = originalPhenotype;
    }

    public String getPhenotypeMapping() {
        return phenotypeMapping;
    }

    public void setPhenotypeMapping( String phenotypeMapping ) {
        this.phenotypeMapping = phenotypeMapping;
    }

    public PhenotypeMappingType findPhenotypeMappingAsEnum() {

        if ( phenotypeMapping.equalsIgnoreCase( "Cross Reference" ) ) {
            return PhenotypeMappingType.XREF;
        } else if ( phenotypeMapping.equalsIgnoreCase( "Curated" ) ) {
            return PhenotypeMappingType.CURATED;
        } else if ( phenotypeMapping.equalsIgnoreCase( "Inferred Cross Reference" ) ) {
            return PhenotypeMappingType.INFERRED_XREF;
        } else if ( phenotypeMapping.equalsIgnoreCase( "Inferred Curated" ) ) {
            return PhenotypeMappingType.INFERRED_CURATED;
        }

        return null;
    }

}