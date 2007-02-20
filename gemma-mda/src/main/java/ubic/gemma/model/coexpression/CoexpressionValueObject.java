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
    private String geneType;

    @Deprecated
    private Double pValue;
    @Deprecated
    private Double score;

    private Map<Long, Map<Long, Double>> positiveScores;
    private Map<Long, Map<Long, Double>> negativeScores;
    private Map<Long, Map<Long, Double>> pValues;

    // the expression experiments that this coexpression was involved in
    private Map<Long, ExpressionExperimentValueObject> expressionExperimentValueObjects;

    public CoexpressionValueObject() {
        geneName = "";
        geneId = null;
        geneOfficialName = null;
        expressionExperimentValueObjects = new HashMap<Long, ExpressionExperimentValueObject>();
        positiveScores = new HashMap<Long, Map<Long, Double>>();
        negativeScores = new HashMap<Long, Map<Long, Double>>();
        pValues = new HashMap<Long, Map<Long, Double>>();

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
    public void addExpressionExperimentValueObject( ExpressionExperimentValueObject expressionExperimentValueObject ) {
        this.expressionExperimentValueObjects.put( new Long( expressionExperimentValueObject.getId() ),
                expressionExperimentValueObject );
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

    public void addPValue( Long eeID, Double pValue, Long probeID ) {

        if ( !pValues.containsKey( eeID ) ) pValues.put( eeID, new HashMap<Long, Double>() );

        pValues.get( eeID ).put( probeID, pValue );

    }

    public Map<Long, Map<Long, Double>> getPValues() {
        return pValues;

    }

    /**
     * @return the negativePValues
     */
    public Map<Long, Map<Long, Double>> getNegativeScores() {
        return negativeScores;
    }

    /**
     * @return the positivePValues
     */
    public Map<Long, Map<Long, Double>> getPositiveScores() {
        return positiveScores;
    }

    public void addScore( Long eeID, Double score, long probeID ) {
        if ( score < 0 ) {
            if ( !negativeScores.containsKey( eeID ) ) negativeScores.put( eeID, new HashMap<Long, Double>() );

            negativeScores.get( eeID ).put( probeID, score );

        } else {
            if ( !positiveScores.containsKey( eeID ) ) positiveScores.put( eeID, new HashMap<Long, Double>() );

            positiveScores.get( eeID ).put( probeID, score );

        }
    }

    public double getPositiveScore() {

        if ( positiveScores.keySet().size() == 0 ) return 0;

        double mean = 0;
        int size = 0;

        for ( Map<Long, Double> scores : positiveScores.values() ) {
            for ( Double score : scores.values() ) {
                mean += score;
                size++;
            }
        }
        return mean / size;

    }

    public double getNegitiveScore() {

        if ( negativeScores.keySet().size() == 0 ) return 0;

        double mean = 0;
        int size = 0;

        for ( Map<Long, Double> scores : negativeScores.values() ) {
            for ( Double score : scores.values() ) {
                mean += score;
                size++;
            }
        }
        return mean / size;

    }

    public double getCollapsedPValue() {

        if ( pValues.keySet().size() == 0 ) return 0;

        double mean = 0;
        int size = 0;

        for ( Map<Long, Double> scores : pValues.values() ) {
            for ( Double score : scores.values() ) {
                mean += score;
                size++;
            }
        }
        return mean / size;
    }

    public int getPositiveLinkCount() {
        return this.positiveScores.keySet().size();
    }

    public int getNegativeLinkCount() {

        return this.negativeScores.keySet().size();
    }

    /**
     * @return the geneType
     */
    public String getGeneType() {
        return geneType;
    }

    /**
     * @param geneType the geneType to set
     */
    public void setGeneType( String geneType ) {
        this.geneType = geneType;
    }
}
