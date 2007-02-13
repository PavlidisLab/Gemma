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
package ubic.gemma.model.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

public class CoexpressionValueObject {

    private String geneName;
    private Long geneId;
    private String geneOfficialName;

    @Deprecated
    private Double pValue;  
    @Deprecated
    private Double score;
    
    private Map<Double,Long> positiveScores;
    private Map<Double,Long> negativeScores;
    private Map<Double,Long> pValues;
    
    // the expression experiments that this coexpression was involved in
    private Map<Long,ExpressionExperimentValueObject> expressionExperimentValueObjects;

    public CoexpressionValueObject() {
        geneName = "";
        geneId = null;
        geneOfficialName = null;
        expressionExperimentValueObjects = new HashMap<Long, ExpressionExperimentValueObject>();
        positiveScores = new HashMap<Double, Long>();
        negativeScores = new HashMap<Double, Long>();
        pValues = new HashMap<Double, Long>();
        
    }
    
    /**
     * @return the expressionExperiments that actually contained coexpression relationtionships for coexpressed gene 
     */
    public Collection getExpressionExperimentValueObjects() {
        return expressionExperimentValueObjects.values();
    }

    /**
     * @param expressionExperimentValueObjects the expressionExperimentValueObjects to set
     */
    public void addExpressionExperimentValueObject(ExpressionExperimentValueObject expressionExperimentValueObject ) {
        this.expressionExperimentValueObjects.put( new Long(expressionExperimentValueObject.getId()), expressionExperimentValueObject);    
    }
    /**
     * @return the geneId
     */
    public Long getGeneId() {
        return geneId;
    }
    /**
     * @param geneId the geneId to set
     */
    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }
    /**
     * @return the geneName
     */
    public String getGeneName() {
        return geneName;
    }
    /**
     * @param geneName the geneName to set
     */
    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }
    /**
     * @return the geneOfficialName
     */
    public String getGeneOfficialName() {
        return geneOfficialName;
    }
    /**
     * @param geneOfficialName the geneOfficialName to set
     */
    public void setGeneOfficialName( String geneOfficialName ) {
        this.geneOfficialName = geneOfficialName;
    }

    @Deprecated
    public Double getPValue() {
        return pValue;
    }

    @Deprecated
    public void setPValue( Double value ) {
        pValue = value;
    }

    @Deprecated
    public Double getScore() {
        return score;
    }

    @Deprecated
    public void setScore( Double score ) {
        this.score = score;
    }
    
    public void addPValue(Double pValue, Long probeID){
        pValues.put( pValue, probeID );        
    }
    
    public Map<Double,Long> getPValues(){
        return pValues;
        
    }

    /**
     * @return the negativePValues
     */
    public Map<Double, Long> getNegativeScores() {
        return negativeScores;
    }

    /**
     * @return the positivePValues
     */
    public Map<Double, Long> getPositiveScores() {
        return positiveScores;
    }
    
    public void addScore(Double score, long probeID){
        if (score < 0)
            negativeScores.put( score, probeID );
        else
            positiveScores.put( score, probeID );
    }

    
    public double getPositiveScore(){
        
        if (positiveScores.keySet().size() == 0)
            return 0;
        
        double mean = 0;
        for(double score: positiveScores.keySet())            
            mean += score;
        
        return mean/positiveScores.keySet().size();
        
    }

    public double getNegitiveScore(){
        
        if (negativeScores.keySet().size() == 0)
            return 0;
        
        double mean = 0;
        for(double score: negativeScores.keySet())            
            mean += score;
        
        return mean/negativeScores.keySet().size();
        
    }
    
    public double getCollapsedPValue(){
        
        if (pValues.keySet().size() == 0)
            return 0;
        
        double mean = 0;
        for(double pValue: pValues.keySet())            
            mean += pValue;
        
        return mean/pValues.keySet().size();
    }
    
    public int getPositiveLinkCount(){    
       return this.positiveScores.size();
    }
    
    public int getNegativeLinkCount(){
        
        return this.negativeScores.size();
    }
    
    

}
