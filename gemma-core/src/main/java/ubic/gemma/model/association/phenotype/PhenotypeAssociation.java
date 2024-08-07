/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association.phenotype;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.genome.Gene;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents abstract evidence for the association of a gene with a phenotype.
 *
 * @author Paul
 */
@Deprecated
public abstract class PhenotypeAssociation extends AbstractAuditable implements Securable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1734685902449428500L;
    private GOEvidenceCode evidenceCode;
    private Boolean isNegativeEvidence = Boolean.FALSE;
    private String score;
    private Double strength;
    private Gene gene;
    private Set<Characteristic> phenotypes = new HashSet<>();
    private Characteristic associationType;
    private DatabaseEntry evidenceSource;
    private QuantitationType scoreType;
    private Set<PhenotypeAssociationPublication> phenotypeAssociationPublications = new HashSet<>();
    private PhenotypeMappingType mappingType;
    private String originalPhenotype;
    private String relationship; // information for a gene-disease relationship
    private Date lastUpdated;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public PhenotypeAssociation() {
    }

    /**
     * @return Describes the nature of the link between (genetic or physiological variation in) the gene and the phenotype, such
     * as "predisposes to" or "causes".
     */
    public Characteristic getAssociationType() {
        return this.associationType;
    }

    public void setAssociationType( Characteristic associationType ) {
        this.associationType = associationType;
    }

    public GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

    public void setEvidenceCode( GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    /**
     * @return An optional external identifiable source for the evidence, if it does not come from with the system. Used to flag
     * evidence that is imported from other phenotype databases, for example.
     */
    public DatabaseEntry getEvidenceSource() {
        return this.evidenceSource;
    }

    public void setEvidenceSource( DatabaseEntry evidenceSource ) {
        this.evidenceSource = evidenceSource;
    }

    public ubic.gemma.model.genome.Gene getGene() {
        return this.gene;
    }

    public void setGene( ubic.gemma.model.genome.Gene gene ) {
        this.gene = gene;
    }

    /**
     * @return If true, this association is a negative one: it indicates the evidence argues against an association between the
     * gene and the phenotype. The default value is false. Use of this field should follow curator guidelines.
     */
    public Boolean getIsNegativeEvidence() {
        return this.isNegativeEvidence;
    }

    public void setIsNegativeEvidence( Boolean isNegativeEvidence ) {
        this.isNegativeEvidence = isNegativeEvidence;
    }

    /**
     * @return The phenotype this association is about. A phenotype is (basically) a term from a controlled vocabulary such as a
     * disease.
     */
    public Set<Characteristic> getPhenotypes() {
        return this.phenotypes;
    }

    public void setPhenotypes( Set<Characteristic> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    /**
     * @return A score, either provided by the system or (often) imported from an external source. If this is populated, the
     * scoreType should be populated.
     */
    public String getScore() {
        return this.score;
    }

    public void setScore( String score ) {
        this.score = score;
    }

    /**
     * @return Describes the score associated with the evidence.
     */
    public QuantitationType getScoreType() {
        return this.scoreType;
    }

    public void setScoreType( QuantitationType scoreType ) {
        this.scoreType = scoreType;
    }

    /**
     * @return The relative strength of the evidence, where higher values are better. This strength may be based on human
     * curation could be from an outside source), automated criteria, or a combination of the two.
     */
    public Double getStrength() {
        return this.strength;
    }

    public void setStrength( Double strength ) {
        this.strength = strength;
    }

    public Set<PhenotypeAssociationPublication> getPhenotypeAssociationPublications() {
        return phenotypeAssociationPublications;
    }

    public void setPhenotypeAssociationPublications(
            Set<PhenotypeAssociationPublication> phenotypeAssociationPublications ) {
        this.phenotypeAssociationPublications = phenotypeAssociationPublications;
    }

    public PhenotypeMappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType( PhenotypeMappingType mappingType ) {
        this.mappingType = mappingType;
    }

    public String getOriginalPhenotype() {
        return originalPhenotype;
    }

    public void setOriginalPhenotype( String originalPhenotype ) {
        this.originalPhenotype = originalPhenotype;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship( String relationship ) {
        this.relationship = relationship;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }
}