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

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationPublication;
import ubic.gemma.model.association.phenotype.PhenotypeMappingType;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parent class of all evidence value objects
 *
 * @author nicolas
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possibly used in front end
public class EvidenceValueObject<E extends PhenotypeAssociation> extends IdentifiableValueObject<E>
        implements Comparable<EvidenceValueObject<E>>, Serializable {

    private static final long serialVersionUID = -2483508971580975L;

    private String description = "";
    private String evidenceCode = null;
    private boolean isNegativeEvidence = false;
    private String className = "";
    private Set<CharacteristicValueObject> phenotypes = null;
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
    private String relationship = "";
    private boolean isHomologueEvidence = false;
    private boolean containQueryPhenotype = false;
    private String originalPhenotype = "";
    private String phenotypeMapping = "";
    private Set<PhenotypeAssPubValueObject> phenotypeAssPubVO = new HashSet<>();
    private ScoreValueObject scoreValueObject = new ScoreValueObject();

    /**
     * Required when using the class as a spring bean.
     */
    public EvidenceValueObject() {
        super();
    }

    public EvidenceValueObject( Long id ) {
        super( id );
    }

    protected EvidenceValueObject( Long id, Integer geneNCBI, Set<CharacteristicValueObject> phenotypes,
            String description, String evidenceCode, boolean isNegativeEvidence,
            EvidenceSourceValueObject evidenceSource ) {
        super( id );
        this.description = description;
        this.evidenceCode = evidenceCode;
        this.isNegativeEvidence = isNegativeEvidence;
        this.phenotypes = phenotypes;
        this.evidenceSource = evidenceSource;
        this.geneNCBI = geneNCBI;
    }

    /**
     * set fields common to all evidence. Entity to Value Object
     *
     * @param phenotypeAssociation phenotype association
     */
    protected EvidenceValueObject( E phenotypeAssociation ) {
        super( phenotypeAssociation );
        this.className = this.getClass().getSimpleName();
        this.description = phenotypeAssociation.getDescription();
        this.evidenceCode = phenotypeAssociation.getEvidenceCode().name();
        this.isNegativeEvidence = phenotypeAssociation.getIsNegativeEvidence();
        this.taxonCommonName = phenotypeAssociation.getGene().getTaxon().getCommonName();
        this.originalPhenotype = phenotypeAssociation.getOriginalPhenotype();
        this.relationship = phenotypeAssociation.getRelationship();

        if ( phenotypeAssociation.getMappingType() != null ) {
            this.phenotypeMapping = phenotypeAssociation.getMappingType().getValue();
        }

        if ( phenotypeAssociation.getEvidenceSource() != null ) {
            this.evidenceSource = new EvidenceSourceValueObject( phenotypeAssociation.getEvidenceSource() );
        }

        this.phenotypes = new TreeSet<>();

        for ( Characteristic c : phenotypeAssociation.getPhenotypes() ) {

            CharacteristicValueObject characteristicVO = new CharacteristicValueObject( c );
            this.phenotypes.add( characteristicVO );
        }

        for ( PhenotypeAssociationPublication phenotypeAssociationPublication : phenotypeAssociation
                .getPhenotypeAssociationPublications() ) {

            PhenotypeAssPubValueObject phenotypeAss = new PhenotypeAssPubValueObject( phenotypeAssociationPublication );
            this.phenotypeAssPubVO.add( phenotypeAss );
        }

        this.lastUpdated = phenotypeAssociation.getLastUpdated().getTime();
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

    @Override
    public int compareTo( EvidenceValueObject<E> evidenceValueObject ) {
        int comparison = this.comparePropertiesTo( evidenceValueObject );

        if ( comparison == 0 ) {
            // Use id for comparison so that each evidence object is unique.
            comparison = this.getId().compareTo( evidenceValueObject.getId() );
        }

        return comparison;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        @SuppressWarnings("unchecked")
        EvidenceValueObject<E> other = ( EvidenceValueObject<E> ) obj;

        if ( this.phenotypes.size() != other.phenotypes.size() ) {
            return false;
        }

        //noinspection unchecked
        Set<String> otherPhenotypesValueUri = other.getPhenotypesValueUri();

        for ( CharacteristicValueObject characteristicValueObject : this.phenotypes ) {
            if ( !otherPhenotypesValueUri.contains( characteristicValueObject.getValueUri() ) ) {
                return false;
            }
        }

        if ( this.evidenceSource == null ) {
            if ( other.evidenceSource != null )
                return false;
        } else if ( !this.evidenceSource.equals( other.evidenceSource ) )
            return false;

        if ( this.geneNCBI == null ) {
            if ( other.geneNCBI != null )
                return false;
        } else if ( !this.geneNCBI.equals( other.geneNCBI ) )
            return false;

        if ( this.phenotypeAssPubVO.size() != other.phenotypeAssPubVO.size() ) {
            return false;
        }

        for ( PhenotypeAssPubValueObject vo : this.phenotypeAssPubVO ) {
            if ( !other.phenotypeAssPubVO.contains( vo ) ) {
                return false;
            }
        }

        return true;
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

    public String getClassName() {
        return this.className;
    }

    public void setClassName( String className ) {
        this.className = className;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getEvidenceCode() {
        return this.evidenceCode;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public EvidenceSecurityValueObject getEvidenceSecurityValueObject() {
        return this.evidenceSecurityValueObject;
    }

    public void setEvidenceSecurityValueObject( EvidenceSecurityValueObject evidenceSecurityValueObject ) {
        this.evidenceSecurityValueObject = evidenceSecurityValueObject;
    }

    public EvidenceSourceValueObject getEvidenceSource() {
        return this.evidenceSource;
    }

    public void setEvidenceSource( EvidenceSourceValueObject evidenceSource ) {
        this.evidenceSource = evidenceSource;
    }

    public String getExternalUrl() {
        return this.externalUrl;
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

    public String getGeneOfficialName() {
        return this.geneOfficialName;
    }

    public void setGeneOfficialName( String geneOfficialName ) {
        this.geneOfficialName = geneOfficialName;
    }

    public String getGeneOfficialSymbol() {
        return this.geneOfficialSymbol;
    }

    public void setGeneOfficialSymbol( String geneOfficialSymbol ) {
        this.geneOfficialSymbol = geneOfficialSymbol;
    }

    public boolean getIsNegativeEvidence() {
        return this.isNegativeEvidence;
    }

    public void setIsNegativeEvidence( boolean isNegativeEvidence ) {
        this.isNegativeEvidence = isNegativeEvidence;
    }

    public Long getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated( Long lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    public Set<CharacteristicValueObject> getPhenotypes() {
        return this.phenotypes;
    }

    public void setPhenotypes( Set<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public Set<String> getPhenotypesValueUri() {

        Set<String> phenotypesValueUri = new HashSet<>();

        for ( CharacteristicValueObject characteristicValueObject : this.phenotypes ) {
            phenotypesValueUri.add( characteristicValueObject.getValueUri() );
        }

        return phenotypesValueUri;
    }

    public ScoreValueObject getScoreValueObject() {
        return this.scoreValueObject;
    }

    public void setScoreValueObject( ScoreValueObject scoreValueObject ) {
        this.scoreValueObject = scoreValueObject;
    }

    public String getTaxonCommonName() {
        return this.taxonCommonName;
    }

    public void setTaxonCommonName( String taxonCommonName ) {
        this.taxonCommonName = taxonCommonName;
    }

    public String getRelationship() {
        return this.relationship;
    }

    public void setRelationship( String relationship ) {
        this.relationship = relationship;
    }

    public boolean isContainQueryPhenotype() {
        return this.containQueryPhenotype;
    }

    public void setContainQueryPhenotype( boolean containQueryPhenotype ) {
        this.containQueryPhenotype = containQueryPhenotype;
    }

    public boolean isHomologueEvidence() {
        return this.isHomologueEvidence;
    }

    public void setHomologueEvidence( boolean isHomologueEvidence ) {
        this.isHomologueEvidence = isHomologueEvidence;
    }

    public Set<PhenotypeAssPubValueObject> getPhenotypeAssPubVO() {
        return phenotypeAssPubVO;
    }

    public void setPhenotypeAssPubVO( Set<PhenotypeAssPubValueObject> phenotypeAssPubVO ) {
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
        if ( this.phenotypeMapping == null )
            return null;
        if ( phenotypeMapping.equalsIgnoreCase( "Cross Reference" ) ) {
            return PhenotypeMappingType.XREF;
        } else if ( phenotypeMapping.equalsIgnoreCase( "Curated" ) ) {
            return PhenotypeMappingType.CURATED;
        } else if ( phenotypeMapping.equalsIgnoreCase( "Inferred Cross Reference" ) ) {
            return PhenotypeMappingType.INFERRED_XREF;
        } else if ( phenotypeMapping.equalsIgnoreCase( "Inferred Curated" ) ) {
            return PhenotypeMappingType.INFERRED_CURATED;
        } else if ( phenotypeMapping.equalsIgnoreCase( "Direct" ) ) {
            return PhenotypeMappingType.DIRECT;
        }
        return null;
    }

    private int comparePropertiesTo( EvidenceValueObject<E> evidenceValueObject ) {
        if ( this == evidenceValueObject )
            return 0;

        if ( this.containQueryPhenotype && !evidenceValueObject.isContainQueryPhenotype() ) {
            return -1;
        } else if ( !this.containQueryPhenotype && evidenceValueObject.isContainQueryPhenotype() ) {
            return 1;
        }

        if ( !this.isHomologueEvidence && evidenceValueObject.isHomologueEvidence )
            return -1;
        if ( this.isHomologueEvidence && !evidenceValueObject.isHomologueEvidence )
            return 1;

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

        if ( ( this.phenotypes != null && this.phenotypes.size() > 0 ) && ( evidenceValueObject.phenotypes == null
                || evidenceValueObject.phenotypes.size() == 0 ) ) {
            return -1;
        }

        if ( ( this.phenotypes == null || this.phenotypes.size() == 0 ) && ( evidenceValueObject.phenotypes != null
                && evidenceValueObject.phenotypes.size() > 0 ) ) {
            return 1;
        }

        int comparison;

        if ( this.phenotypes != null && evidenceValueObject.phenotypes != null ) {
            Iterator<CharacteristicValueObject> thisIterator = this.phenotypes.iterator();
            //noinspection unchecked
            Iterator<CharacteristicValueObject> otherIterator = evidenceValueObject.phenotypes.iterator();

            while ( true ) {
                boolean thisHasNext = thisIterator.hasNext();
                boolean otherHasNext = otherIterator.hasNext();

                if ( !thisHasNext && otherHasNext )
                    return -1;
                if ( thisHasNext && !otherHasNext )
                    return 1;
                //noinspection ConstantConditions // better readability
                if ( !thisHasNext && !otherHasNext )
                    break;

                comparison = thisIterator.next().compareTo( otherIterator.next() );
                if ( comparison != 0 )
                    return comparison;
            }
        }

        if ( !this.isNegativeEvidence && evidenceValueObject.isNegativeEvidence )
            return -1;
        if ( this.isNegativeEvidence && !evidenceValueObject.isNegativeEvidence )
            return 1;

        comparison = this.className.compareTo( evidenceValueObject.className );
        if ( comparison != 0 )
            return comparison;

        comparison = this.evidenceCode.compareTo( evidenceValueObject.evidenceCode );
        if ( comparison != 0 )
            return comparison;

        comparison = this.evidenceCode.compareTo( evidenceValueObject.evidenceCode );
        if ( comparison != 0 )
            return comparison;

        comparison = this.compareEvidenceSource( evidenceValueObject );
        if ( comparison != 0 )
            return comparison;

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

    private int compareEvidenceSource( EvidenceValueObject<E> evidenceValueObject ) {

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

}