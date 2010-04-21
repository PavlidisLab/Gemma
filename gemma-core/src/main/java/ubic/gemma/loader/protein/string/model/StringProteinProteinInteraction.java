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

/**
 * Value object that represents the data in a record/line in the string protein interaction file
 * Which follows the structure:
 * protein1 protein2 neighborhood fusion cooccurence coexpression experimental database textmining combined_score.
 * 
 * Two objects are equal if they share the same two proteins.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class StringProteinProteinInteraction implements Serializable{
    private static final long serialVersionUID = -859220901359582113L;
    
    private  String protein1 =null;
    private  String protein2 = null;
    private Integer ncbiTaxonId =0;   
    private Integer neighborhood =0;
    private Integer fusion =0;
    private Integer cooccurence =0;
    private Integer coexpression =0;
    private Integer experimental =0;
    private Integer database =0;
    private Integer textmining  =0;
    private Integer combined_score =0;
    
    
    /**
     * Constructor these two fields should not be null as they are used to establish equality.
     * 
     * @param protein1
     * @param protein2
     */
    public StringProteinProteinInteraction(String protein1, String protein2){
        this.protein1 =protein1;
        this.protein2 =protein2;        
    }
    
    
    /**
     * Two StringProteinProteinInteraction are equal if they have the same combination of proteins either 
     * one or two can be reversed.
     */
    @Override
    public boolean equals(Object ob){
        if(this ==ob){
            return true;
        }
        if (!(ob instanceof StringProteinProteinInteraction)){
            return false;
        }
                
        StringProteinProteinInteraction otherObj = (StringProteinProteinInteraction) ob ;
        String proteinOneOtherObj = otherObj.getProtein1();
        String proteinTwoOtherObj = otherObj.getProtein2();
                
        if(proteinOneOtherObj ==null || proteinTwoOtherObj ==null ||  protein1==null || protein2 == null){
            return false;
        }
        
        if((protein1.equals(proteinOneOtherObj) && (protein2.equals(proteinTwoOtherObj)))){
            return true;
        }
        //account scenario where they have been flipped
       // if((protein2.equals(proteinOneOtherObj) && (protein1.equals(proteinTwoOtherObj)))){
          //  return true; 
       // }        
        else{
            return false;
        }        
    }
    
    /**
     * Create a hash of the two proteins
     * 
     * This method is called when using HashSet as in the parser
     */
    @Override
    public int hashCode(){
        int hash =0;
             
        if(protein1==null || protein2 ==null){
            return hash;
        }    
        hash =  protein1.concat( protein2 ).hashCode();    
        return hash;        
    }
    
    /**
     * @return the protein1
     */
    public String getProtein1() {
        return protein1;
    }
    /**
     * @param protein1 the protein1 to set
     */
    public void setProtein1( String protein1 ) {
        this.protein1 = protein1;
    }
    /**
     * @return the protein2
     */
    public String getProtein2() {
        return protein2;
    }
    /**
     * @param protein2 the protein2 to set
     */
    public void setProtein2( String protein2 ) {
        this.protein2 = protein2;
    }
    /**
     * @return the neighborhood
     */
    public Integer getNeighborhood() {
        return neighborhood;
    }
    /**
     * @param neighborhood the neighborhood to set
     */
    public void setNeighborhood( Integer neighborhood ) {
        this.neighborhood = neighborhood;
    }
    /**
     * @return the fusion
     */
    public Integer getFusion() {
        return fusion;
    }
    /**
     * @param fusion the fusion to set
     */
    public void setFusion( Integer fusion ) {
        this.fusion = fusion;
    }
    /**
     * @return the cooccurence
     */
    public Integer getCooccurence() {
        return cooccurence;
    }
    /**
     * @param cooccurence the cooccurence to set
     */
    public void setCooccurence( Integer cooccurence ) {
        this.cooccurence = cooccurence;
    }
    /**
     * @return the coexpression
     */
    public Integer getCoexpression() {
        return coexpression;
    }
    /**
     * @param coexpression the coexpression to set
     */
    public void setCoexpression( Integer coexpression ) {
        this.coexpression = coexpression;
    }
    /**
     * @return the experimental
     */
    public Integer getExperimental() {
        return experimental;
    }
    /**
     * @param experimental the experimental to set
     */
    public void setExperimental( Integer experimental ) {
        this.experimental = experimental;
    }
    /**
     * @return the database
     */
    public Integer getDatabase() {
        return database;
    }
    /**
     * @param database the database to set
     */
    public void setDatabase( Integer database ) {
        this.database = database;
    }
    /**
     * @return the textmining
     */
    public Integer getTextmining() {
        return textmining;
    }
    /**
     * @param textmining the textmining to set
     */
    public void setTextmining( Integer textmining ) {
        this.textmining = textmining;
    }
    /**
     * @return the combined_score
     */
    public Integer getCombined_score() {
        return combined_score;
    }
    /**
     * @param combined_score the combined_score to set
     */
    public void setCombined_score( Integer combined_score ) {
        this.combined_score = combined_score;
    }
    
    /**
     * @return the ncbiTaxonId
     */
    public Integer getNcbiTaxonId() {
        return ncbiTaxonId;
    }
    /**
     * @param ncbiTaxonId the ncbiTaxonId to set
     */
    public void setNcbiTaxonId( Integer ncbiTaxonId ) {
        this.ncbiTaxonId = ncbiTaxonId;
    }

}
