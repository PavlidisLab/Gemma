/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein.string.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import ubic.gemma.loader.protein.StringProteinInteractionEvidenceCodeEnum;

/**
 * Value object that represents the data in a record/line in the string protein interaction file Which follows the
 * structure: protein1 protein2 neighborhood fusion cooccurence coexpression experimental database textmining
 * combined_score. Two objects are equal if they share the same two proteins.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class StringProteinProteinInteraction implements Serializable {
    private static final long serialVersionUID = -859220901359582113L;

    private String protein1 = null;
    private String protein2 = null;
    private Integer ncbiTaxonId = 0;

    /** Combined score of the interacton any value below 04. is considered a low confidence interaction */
    private Double combined_score = 0.0;

    /** Map of the enum evidence value and the score for that particular evidence */
    private Map<StringProteinInteractionEvidenceCodeEnum, Integer> mapEvidenceCodeScores = null;

    /**
     * Evidence vectorbit representing neighborhood, geneFusion, cooccurence, coexpression, experimental, database and
     * textmining A 0 represents no evidence and 1 evidence
     */
    byte[] evidenceVector = new byte[] { 0, 0, 0, 0, 0, 0, 0 };

    /**
     * Constructor these two fields should not be null as they are used to establish equality.
     * 
     * @param protein1
     * @param protein2
     */
    public StringProteinProteinInteraction( String protein1, String protein2 ) {
        this.protein1 = protein1;
        this.protein2 = protein2;
        mapEvidenceCodeScores = new HashMap<StringProteinInteractionEvidenceCodeEnum, Integer>();
    }

    /**
     * For a given evidence add the score
     * 
     * @param evidenceCode What type of evidence there is for this interaction
     * @param score If greater than 0 then evidence for that factor
     */
    public void addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum evidenceCode, Integer score ) {
        mapEvidenceCodeScores.put( evidenceCode, score );
    }

    /**
     * Two StringProteinProteinInteraction are equal if they have the same combination of proteins either one or two can
     * be reversed.
     */
    @Override
    public boolean equals( Object ob ) {
        if ( this == ob ) {
            return true;
        }
        if ( !( ob instanceof StringProteinProteinInteraction ) ) {
            return false;
        }

        StringProteinProteinInteraction otherObj = ( StringProteinProteinInteraction ) ob;
        String proteinOneOtherObj = otherObj.getProtein1();
        String proteinTwoOtherObj = otherObj.getProtein2();

        if ( proteinOneOtherObj == null || proteinTwoOtherObj == null || protein1 == null || protein2 == null ) {
            return false;
        }

        if ( ( protein1.equals( proteinOneOtherObj ) && ( protein2.equals( proteinTwoOtherObj ) ) ) ) {
            return true;
        }
        return false;

    }

    /**
     * The total score for this interaction.
     * 
     * @return the combined_score
     */
    public Double getCombined_score() {
        return combined_score;
    }

    /**
     * Updates the evidenceVector with the particular evidence
     * 
     * @return byte representing the 7 different types of evidence as a 0 or 1 depending on whether they give evidence
     *         for this interaction
     */
    public byte[] getEvidenceVector() {
        // Go throught the map of enums that hold the different types of evidences and get the score
        // if a score is greater than 0 then set the byte array to 1 at the position which records that particular
        // evidence
        for ( StringProteinInteractionEvidenceCodeEnum evidence : mapEvidenceCodeScores.keySet() ) {
            // if the score is greater than 0 then update array with that evidence that is set a flag of 1;
            if ( mapEvidenceCodeScores.get( evidence ) > 0 ) {
                evidenceVector[evidence.getPositionInArray()] = 1;
            }
        }
        return evidenceVector;
    }

    /**
     * @return NCBI id of the taxon in this interaction that is the taxon of the two genes
     */
    public Integer getNcbiTaxonId() {
        return ncbiTaxonId;
    }

    /**
     * @return the protein1
     */
    public String getProtein1() {
        return protein1;
    }

    /**
     * @return the protein2
     */
    public String getProtein2() {
        return protein2;
    }

    /**
     * Create a hash of the two proteins; this method is called when using HashSet as in the parser.
     */
    @Override
    public int hashCode() {
        int hash = 0;

        if ( protein1 == null || protein2 == null ) {
            return hash;
        }
        hash = protein1.concat( protein2 ).hashCode();
        return hash;
    }

    /**
     * Total score of the interaction
     * 
     * @param combined_score the combined_score to set
     */
    public void setCombined_score( Double combined_score ) {
        this.combined_score = combined_score;
    }

    /**
     * @param ncbiTaxonId NCBI id of the taxon in this interaction that is the taxon of the two genes
     */
    public void setNcbiTaxonId( Integer ncbiTaxonId ) {
        this.ncbiTaxonId = ncbiTaxonId;
    }

    /**
     * @param protein1 the protein1 to set
     */
    public void setProtein1( String protein1 ) {
        this.protein1 = protein1;
    }

    /**
     * @param protein2 the protein2 to set
     */
    public void setProtein2( String protein2 ) {
        this.protein2 = protein2;
    }

}
