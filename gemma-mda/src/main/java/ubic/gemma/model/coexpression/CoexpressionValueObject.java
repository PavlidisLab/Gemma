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
    private Integer stringencyFilterValue;
    private Collection<Long> nonspecificEE;

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
        stringencyFilterValue = null;
    }

    /**
     * @return the expressionExperiments that actually contained coexpression relationtionships for coexpressed gene
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperimentValueObjects() {
        return expressionExperimentValueObjects.values();
    }

    /**
     * @param eeID expression experiment ID (long)
     * @return null if the EEid is not part of the ee's that contribute to this genes coexpression returns the
     *         EEValueObject if it does.
     */
    public ExpressionExperimentValueObject getExpressionExperimentValueObject( Long eeID ) {

        if ( expressionExperimentValueObjects.containsKey( eeID ) )
            return expressionExperimentValueObjects.get( eeID );

        return null;
    }

    /**
     * @param expressionExperimentValueObjects the expressionExperimentValueObjects to set
     */
    public void addExpressionExperimentValueObject( ExpressionExperimentValueObject eeVo ) {
        if ( !expressionExperimentValueObjects.containsKey( Long.parseLong( eeVo.getId() ) ) )
            this.expressionExperimentValueObjects.put( new Long( eeVo.getId() ), eeVo );
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

    public double getNegativeScore() {

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

    /**
     * Function to return the positive link counts. If the count equals or exceeds the stringency filter value or the
     * filter value is not set, return the count. If the count is below the filter value, return null.
     * 
     * @return the positive link counts
     */
    public Integer getPositiveLinkCount() {
        Integer count = this.positiveScores.keySet().size();
        if ( stringencyFilterValue == null ) {
            return count;
        } else if ( count >= stringencyFilterValue ) {
            return count;
        } else {
            return null;
        }
    }

    /**
     * @return a collectino of EEids that contributed to this genes positive expression
     */
    public Collection<Long> getEEContributing2PositiveLinks() {
        return positiveScores.keySet();
    }

    /**
     * @return a collection of EE ids that contributed to this genes negative expression
     */
    public Collection<Long> getEEContributing2NegativeLinks() {
        return negativeScores.keySet();

    }

    /**
     * Function to return the negative link counts. If the count equals or exceeds the stringency filter value or the
     * filter value is not set, return the count. If the count is below the filter value, return null.
     * 
     * @return the negative link counts
     */
    public Integer getNegativeLinkCount() {
        Integer count = this.negativeScores.keySet().size();
        if ( stringencyFilterValue == null ) {
            return count;
        } else if ( count >= stringencyFilterValue ) {
            return count;
        } else {
            return null;
        }
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

    /**
     * @return the stringencyFilterValue
     */
    public Integer getStringencyFilterValue() {
        return stringencyFilterValue;
    }

    /**
     * @param stringencyFilterValue the stringencyFilterValue to set
     */
    public void setStringencyFilterValue( Integer stringencyFilterValue ) {
        this.stringencyFilterValue = stringencyFilterValue;
    }

    /**
     * @return the nonspecificEE
     */
    public Collection<Long> getNonspecificEE() {
        return nonspecificEE;
    }

    /**
     * @param nonspecificEE the nonspecificEE to set
     */
    public void setNonspecificEE( Collection<Long> nonspecificEE ) {
        this.nonspecificEE = nonspecificEE;
    }

    /**
     * @return the nonspecificEE
     */
    public Collection<Long> getContributingExpressionExperiments() {
        return expressionExperimentValueObjects.keySet();
    }
}
