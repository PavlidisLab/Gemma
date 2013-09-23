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

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * Represents abstract evidence for the association of a gene with a phenotype.
 */
public abstract class PhenotypeAssociation extends Auditable implements gemma.gsec.model.Securable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1734685902449428500L;

    private GOEvidenceCode evidenceCode;
    private Boolean isNegativeEvidence = Boolean.valueOf( false );

    private String score;

    private Double strength;

    private ubic.gemma.model.genome.Gene gene;

    private Collection<Characteristic> phenotypes = new java.util.HashSet<Characteristic>();

    private Characteristic associationType;

    private DatabaseEntry evidenceSource;

    private QuantitationType scoreType;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public PhenotypeAssociation() {
    }

    /**
     * Describes the nature of the link between (genetic or physiological variation in) the gene and the phenotype, such
     * as "predisposes to" or "causes".
     */
    public Characteristic getAssociationType() {
        return this.associationType;
    }

    /**
     * 
     */
    public GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

    /**
     * An optional external identifiable source for the evidence, if it does not come from with the system. Used to flag
     * evidence that is imported from other phenotype databases, for example.
     */
    public DatabaseEntry getEvidenceSource() {
        return this.evidenceSource;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene getGene() {
        return this.gene;
    }

    /**
     * <p>
     * If true, this association is a negative one: it indicates the evidence argues against an association between the
     * gene and the phenotype. The default value is false. Use of this field should follow curator guidelines.
     * </p>
     */
    public Boolean getIsNegativeEvidence() {
        return this.isNegativeEvidence;
    }

    /**
     * The phenotype this association is about. A phenotype is (basically) a term from a controlled vocabulary such as a
     * disease.
     */
    public Collection<Characteristic> getPhenotypes() {
        return this.phenotypes;
    }

    /**
     * A score, either provided by the system or (often) imported from an external source. If this is populated, the
     * scoreType should be populated.
     */
    public String getScore() {
        return this.score;
    }

    /**
     * <p>
     * Describes the score associated with the evidence.
     * </p>
     */
    public QuantitationType getScoreType() {
        return this.scoreType;
    }

    /**
     * The relative strength of the evidence, where higher values are better. This strength may be based on human
     * curation could be from an outside source), automated criteria, or a combination of the two.
     */
    public Double getStrength() {
        return this.strength;
    }

    public void setAssociationType( Characteristic associationType ) {
        this.associationType = associationType;
    }

    public void setEvidenceCode( GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public void setEvidenceSource( DatabaseEntry evidenceSource ) {
        this.evidenceSource = evidenceSource;
    }

    public void setGene( ubic.gemma.model.genome.Gene gene ) {
        this.gene = gene;
    }

    public void setIsNegativeEvidence( Boolean isNegativeEvidence ) {
        this.isNegativeEvidence = isNegativeEvidence;
    }

    public void setPhenotypes( Collection<Characteristic> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public void setScore( String score ) {
        this.score = score;
    }

    public void setScoreType( QuantitationType scoreType ) {
        this.scoreType = scoreType;
    }

    public void setStrength( Double strength ) {
        this.strength = strength;
    }

}