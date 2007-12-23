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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.ontology.OntologyTerm;

/**
 * Used for storing the results of each gene that is coexpressed with a query gene. Keeps track of specificity, pValues,
 * Scores, goTerms, GO overlap with the query, stringency value.
 * 
 * @author klc
 * @version $Id$
 */
public class CoexpressionValueObject {

    @Override
    public String toString() {
        return geneName;
    }

    private String geneName;
    private Long geneId;
    private String geneOfficialName;
    private String geneType;
    private Long taxonId;

    private Map<Long, Map<Long, Double>> positiveScores;
    private Map<Long, Map<Long, Double>> negativeScores;
    private Map<Long, Map<Long, Double>> pValues;
    private Integer stringencyFilterValue;
    private Collection<Long> nonspecificEE;
    private int possibleOverlap;
    private Collection<OntologyTerm> goOverlap;
    private List<Long> experimentBitList = new ArrayList<Long>();
    private Collection<String> nonSpecificGenes = new HashSet<String>();
    private boolean hybridizesWithQueryGene;

    // the expression experiments that this coexpression was involved in
    private Map<Long, ExpressionExperimentValueObject> expressionExperimentValueObjects;

    public CoexpressionValueObject() {
        geneName = "";
        geneId = null;
        geneOfficialName = null;
        expressionExperimentValueObjects = Collections
                .synchronizedMap( new HashMap<Long, ExpressionExperimentValueObject>() );
        positiveScores = Collections.synchronizedMap( new HashMap<Long, Map<Long, Double>>() );
        negativeScores = Collections.synchronizedMap( new HashMap<Long, Map<Long, Double>>() );
        pValues = new HashMap<Long, Map<Long, Double>>();
        stringencyFilterValue = null;
        possibleOverlap = 0;
    }

    /**
     * @param expressionExperimentValueObjects the expressionExperimentValueObjects to set
     */
    public void addExpressionExperimentValueObject( ExpressionExperimentValueObject eeVo ) {
        if ( !expressionExperimentValueObjects.containsKey( eeVo.getId() ) )
            this.expressionExperimentValueObjects.put( eeVo.getId(), eeVo );
    }

    public void addNonSpecificGene( String gene ) {
        this.nonSpecificGenes.add( gene );
    }

    public void addPValue( Long eeID, Double pValue, Long probeID ) {

        if ( !pValues.containsKey( eeID ) ) pValues.put( eeID, new HashMap<Long, Double>() );

        pValues.get( eeID ).put( probeID, pValue );

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

    /**
     * Initialize the vector of 'bits'.
     * 
     * @param eeIds
     */
    public void computeExperimentBits( List<Long> eeIds ) {
        experimentBitList.clear();
        Collection<Long> thisUsed = expressionExperimentValueObjects.keySet();
        for ( Long eeId : eeIds ) {
            if ( thisUsed.contains( eeId ) ) {
                experimentBitList.add( eeId );
            } else {
                experimentBitList.add( 0l );
            }
        }
    }

    /**
     * @return
     */
    public double getCollapsedPValue() {

        if ( pValues.keySet().size() == 0 ) return 0;

        double mean = 0;
        int size = 0;

        synchronized ( pValues ) {
            for ( Map<Long, Double> scores : pValues.values() ) {
                for ( Double score : scores.values() ) {
                    mean += score;
                    size++;
                }
            }
        }
        return mean / size;
    }

    /**
     * @return a collection of EE ids that contributed to this genes negative expression
     */
    public Collection<Long> getEEContributing2NegativeLinks() {
        return negativeScores.keySet();

    }

    /**
     * @return a collectino of EEids that contributed to this genes positive expression
     */
    public Collection<Long> getEEContributing2PositiveLinks() {
        return positiveScores.keySet();
    }

    /**
     * @return
     */
    public List<Long> getExperimentBitIds() {
        return experimentBitList;
    }

    /**
     * @return
     */
    public String getExperimentBitList() {
        StringBuffer buf = new StringBuffer();
        for ( Iterator<Long> it = experimentBitList.iterator(); it.hasNext(); ) {
            long i = it.next();
            buf.append( i == 0 ? 0 : 20 );
            if ( it.hasNext() ) buf.append( "," );
        }
        return buf.toString();
    }

    /**
     * @return the nonspecificEE
     */
    public Collection<Long> getExpressionExperiments() {
        return expressionExperimentValueObjects.keySet();
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
     * @return the expressionExperiments that actually contained coexpression relationtionships for coexpressed gene
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperimentValueObjects() {
        return expressionExperimentValueObjects.values();
    }

    /**
     * @return the geneId
     */
    public Long getGeneId() {
        return geneId;
    }

    /**
     * @return the geneName
     */
    public String getGeneName() {
        return geneName;
    }

    /**
     * @return the geneOfficialName
     */
    public String getGeneOfficialName() {
        return geneOfficialName;
    }

    /**
     * @return the geneType
     */
    public String getGeneType() {
        return geneType;
    }

    public Collection<OntologyTerm> getGoOverlap() {
        return goOverlap;
    }

    /**
     * @return
     */
    public String getImageMapName() {
        StringBuffer buf = new StringBuffer();
        buf.append( "map." );
        buf.append( geneType );
        buf.append( ".gene" );
        buf.append( geneId );
        buf.append( ".taxon" );
        buf.append( taxonId );
        return buf.toString();
    }

    /**
     * Function to return the max of negative or positive link count. This is used for sorting.
     * 
     * @return
     */
    public Integer getMaxLinkCount() {
        Integer positiveLinks = this.getPositiveLinkCount();
        Integer negativeLinks = this.getNegativeLinkCount();

        if ( positiveLinks == null && negativeLinks == null ) {
            return 0;
        }
        if ( positiveLinks == null ) {
            return negativeLinks;
        }
        if ( negativeLinks == null ) {
            return positiveLinks;
        }

        if ( positiveLinks >= negativeLinks ) {
            return positiveLinks;
        } else {
            return negativeLinks;
        }
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
     * @return
     */
    public double getNegativeScore() {

        if ( negativeScores.keySet().size() == 0 ) return 0;

        double mean = 0;
        int size = 0;

        synchronized ( negativeScores ) {
            for ( Map<Long, Double> scores : negativeScores.values() ) {
                for ( Double score : scores.values() ) {
                    mean += score;
                    size++;
                }
            }
        }
        return mean / size;

    }

    /**
     * @return the negativePValues
     */
    public Map<Long, Map<Long, Double>> getNegativeScores() {
        return negativeScores;
    }

    /**
     * @return the nonspecificEE
     */
    public Collection<Long> getNonspecificEE() {
        return nonspecificEE;
    }

    /**
     * @return the nonSpecificGenes
     */
    public Collection<String> getNonSpecificGenes() {
        return nonSpecificGenes;
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
     * @return
     */
    public double getPositiveScore() {

        if ( positiveScores.keySet().size() == 0 ) return 0;

        double mean = 0;
        int size = 0;

        synchronized ( positiveScores ) {
            for ( Map<Long, Double> scores : positiveScores.values() ) {
                for ( Double score : scores.values() ) {
                    mean += score;
                    size++;
                }
            }
        }
        return mean / size;

    }

    /**
     * @return the positivePValues
     */
    public Map<Long, Map<Long, Double>> getPositiveScores() {
        return positiveScores;
    }

    public int getPossibleOverlap() {
        return possibleOverlap;
    }

    /**
     * @return
     */
    public Collection<Long> getProbes() {
        Collection<Long> results = new HashSet<Long>();

        for ( Map<Long, Double> obj : pValues.values() ) {
            results.addAll( obj.keySet() );
        }

        return results;

    }

    /**
     * @return
     */
    public Map<Long, Map<Long, Double>> getPValues() {
        return pValues;

    }

    /**
     * @return the stringencyFilterValue
     */
    public Integer getStringencyFilterValue() {
        return stringencyFilterValue;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    /**
     * @return the hybridizesWithQueryGene
     */
    public boolean isHybridizesWithQueryGene() {
        return hybridizesWithQueryGene;
    }

    /**
     * @param geneId the geneId to set
     */
    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    /**
     * @param geneName the geneName to set
     */
    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    /**
     * @param geneOfficialName the geneOfficialName to set
     */
    public void setGeneOfficialName( String geneOfficialName ) {
        this.geneOfficialName = geneOfficialName;
    }

    /**
     * @param geneType the geneType to set
     */
    public void setGeneType( String geneType ) {
        this.geneType = geneType;
    }

    public void setGoOverlap( Collection<OntologyTerm> goOverlap ) {
        this.goOverlap = goOverlap;
    }

    /**
     * @param hybridizesWithQueryGene the hybridizesWithQueryGene to set
     */
    public void setHybridizesWithQueryGene( boolean hybridizesWithQueryGene ) {
        this.hybridizesWithQueryGene = hybridizesWithQueryGene;
    }

    /**
     * @param nonspecificEE the nonspecificEE to set
     */
    public void setNonspecificEE( Collection<Long> nonspecificEE ) {
        this.nonspecificEE = nonspecificEE;
    }

    /**
     * @param nonSpecificGenes the nonSpecificGenes to set
     */
    public void setNonSpecificGenes( Collection<String> nonSpecificGenes ) {
        this.nonSpecificGenes = nonSpecificGenes;
    }

    public void setPossibleOverlap( int possibleOverlap ) {
        this.possibleOverlap = possibleOverlap;
    }

    /**
     * @param stringencyFilterValue the stringencyFilterValue to set
     */
    public void setStringencyFilterValue( Integer stringencyFilterValue ) {
        this.stringencyFilterValue = stringencyFilterValue;
    }

    /**
     * @param taxonId
     */
    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }
}