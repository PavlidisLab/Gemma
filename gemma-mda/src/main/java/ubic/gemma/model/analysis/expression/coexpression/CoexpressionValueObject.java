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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.BioAssaySet; 
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.ontology.OntologyTerm;

/**
 * The results for one gene that is coexpressed with a query gene. Keeps track of specificity, pValues, Scores, goTerms,
 * GO overlap with the query, stringency value. Information about positive and negative correlations are stored,
 * separately.
 * 
 * @author klc
 * @version $Id$
 */
public class CoexpressionValueObject implements Comparable<CoexpressionValueObject> {

    private static Log log = LogFactory.getLog( CoexpressionValueObject.class.getName() );

    /*
     * Basic information about the gene.
     */
    private Long geneId;
    private String geneName;
    private String geneOfficialName;
    private String geneType = null;
    private Long taxonId;

    /*
     * Maps of Expression Experiment IDs to maps of Probe IDs to scores/pvalues that are in support of this
     * coexpression.
     */
    private Map<Long, Map<Long, Double>> positiveScores;
    private Map<Long, Map<Long, Double>> negativeScores;
    private Map<Long, Map<Long, Double>> posPvalues;
    private Map<Long, Map<Long, Double>> negPvalues;

    /**
     * Expression Experiments whihc have evidence for coexpression of this gene with the query, but the probes are not
     * specific for the target gene.
     */
    private Collection<Long> nonspecificEE;

    /**
     * Genes that were predicted to cross-hybridize with the target gene
     */
    private Collection<Long> crossHybridizingGenes = new HashSet<Long>();

    /**
     * True if any of the probes for this gene are predicted to cross-hybridize with the query gene. This is an obvious
     * risk of false positives.
     */
    private boolean hybridizesWithQueryGene;

    /**
     * Number of GO terms the query gene has. This is the highest possible overlap
     */
    private int numQueryGeneGOTerms;

    /**
     * Number of GO terms this gene shares with the query gene.
     */
    private Collection<OntologyTerm> goOverlap;

    private List<Long> experimentBitList = new ArrayList<Long>();

    // the expression experiments that this coexpression was involved in
    private Map<Long, ExpressionExperimentValueObject> expressionExperimentValueObjects;

    private Collection<BioAssaySet> datasetsTestedIn = new HashSet<BioAssaySet>();

    public CoexpressionValueObject() {
        geneName = "";
        geneId = null;
        geneOfficialName = null;
        expressionExperimentValueObjects = new HashMap<Long, ExpressionExperimentValueObject>();
        positiveScores = new HashMap<Long, Map<Long, Double>>();
        negativeScores = new HashMap<Long, Map<Long, Double>>();
        posPvalues = new HashMap<Long, Map<Long, Double>>();
        negPvalues = new HashMap<Long, Map<Long, Double>>();

        numQueryGeneGOTerms = 0;
    }

    /**
     * @param geneid of gene that is predicted to cross-hybridize with this gene
     */
    public void addCrossHybridizingGene( Long geneid ) {
        if ( geneid.equals( this.geneId ) ) return;
        this.crossHybridizingGenes.add( geneid );
    }

    /**
     * Add another experiment that supports this coexpression.
     * 
     * @param eeVo
     */
    public void addSupportingExperiment( ExpressionExperimentValueObject eeVo ) {
        if ( expressionExperimentValueObjects.containsKey( eeVo.getId() ) ) {
            // I guess this happens if there are two probes for the same gene.
            if ( log.isDebugEnabled() ) log.debug( "Already have seen this experiment" );
        }
        this.expressionExperimentValueObjects.put( eeVo.getId(), eeVo );
    }

    /**
     * @param eeID
     * @param score
     * @param pvalue
     * @param probeID
     */
    public void addScore( Long eeID, Double score, Double pvalue, long probeID ) {
        if ( score < 0 ) {
            if ( !negativeScores.containsKey( eeID ) ) negativeScores.put( eeID, new HashMap<Long, Double>() );
            if ( !negPvalues.containsKey( eeID ) ) negPvalues.put( eeID, new HashMap<Long, Double>() );
            negPvalues.get( eeID ).put( probeID, pvalue );
            negativeScores.get( eeID ).put( probeID, score );

        } else {
            if ( !positiveScores.containsKey( eeID ) ) positiveScores.put( eeID, new HashMap<Long, Double>() );
            if ( !posPvalues.containsKey( eeID ) ) posPvalues.put( eeID, new HashMap<Long, Double>() );
            posPvalues.get( eeID ).put( probeID, pvalue );
            positiveScores.get( eeID ).put( probeID, score );

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( CoexpressionValueObject o ) {
        int o1Size = this.getMaxLinkCount();
        int o2Size = o.getMaxLinkCount();
        if ( o1Size > o2Size ) {
            return -1;
        } else if ( o1Size < o2Size ) {
            return 1;
        } else {
            return this.getGeneName().compareTo( o.getGeneName() );
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
     * @return IDs of genes that may be crosshybridizing with the target gene for this.
     */
    public Collection<Long> getCrossHybridizingGenes() {
        return crossHybridizingGenes;
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
     * @return String holding a list of values for creating the 'bit image'.
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
        return expressionExperimentValueObjects.get( eeID );
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
     * @return the geneType (known gene, predicted, or probe-aligned region)
     */
    public String getGeneType() {
        return geneType;
    }

    /**
     * @return Gene Ontology similarity with the query gene.
     */
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
     * Function to return the max of the negative and positive link support. This is used for sorting.
     * 
     * @return
     */
    public int getMaxLinkCount() {
        int positiveLinks = this.getPositiveLinkSupport();
        int negativeLinks = this.getNegativeLinkSupport();
        return Math.max( positiveLinks, negativeLinks );
    }

    /**
     * @param eeId
     * @return
     */
    public Collection<Long> getNegativeCorrelationProbes( Long eeId ) {
        if ( !negativeScores.containsKey( eeId ) ) return new HashSet<Long>();
        return negativeScores.get( eeId ).keySet();
    }

    /**
     * @return the negative link counts
     */
    public int getNegativeLinkSupport() {
        if ( negativeScores.size() == 0 ) return 0;
        return this.negativeScores.size();
    }

    /**
     * @return
     */
    public double getNegativeScore() {

        if ( getNegativeLinkSupport() == 0 ) return 0.0;

        double mean = 0;
        int size = 0;

        for ( Map<Long, Double> scores : negativeScores.values() ) {
            for ( Double score : scores.values() ) {
                mean += score;
                size++;
            }
        }

        assert size > 0;

        return mean / size;

    }

    /**
     * @return the negativePValues
     */
    public Map<Long, Map<Long, Double>> getNegativeScores() {
        return negativeScores;
    }

    /**
     * @return geometric mean of the pvalues for the supporting links.
     */
    public double getNegPValue() {
        if ( negPvalues.size() == 0 ) return 0.0;

        Collection<Map<Long, Double>> values = negPvalues.values();
        return computePvalue( values );
    }

    /**
     * FIXME just returning zero for now.
     * 
     * @param mean
     * @param size
     * @param values
     * @return
     */
    private double computePvalue( Collection<Map<Long, Double>> values ) {
        return 0.0;
        // double mean = 0.0;
        // int size = 0;
        // for ( Map<Long, Double> scores : values ) {
        // for ( Double score : scores.values() ) {
        // if ( score.doubleValue() == 0 ) {
        // score = Constants.SMALL;
        // }
        // mean += Math.log( score );
        // size++;
        // }
        // }
        // assert size > 0;
        //
        // return Math.exp( mean / size );
    }

    /**
     * @return the nonspecificEE
     */
    public Collection<Long> getNonspecificEE() {
        return nonspecificEE;
    }

    public int getNumDatasetsTestedIn() {
        if ( datasetsTestedIn == null ) return 0;
        return this.datasetsTestedIn.size();
    }

    /**
     * @param eeId
     * @return
     */
    public Collection<Long> getPositiveCorrelationProbes( Long eeId ) {
        if ( !positiveScores.containsKey( eeId ) ) return new HashSet<Long>();
        return positiveScores.get( eeId ).keySet();
    }

    /**
     * @return the positive link counts
     */
    public int getPositiveLinkSupport() {
        if ( positiveScores == null ) return 0;
        return this.positiveScores.size();
    }

    /**
     * @return
     */
    public double getPositiveScore() {

        if ( positiveScores.size() == 0 ) return 0.0;

        double mean = 0.0;
        int size = 0;

        for ( Map<Long, Double> scores : positiveScores.values() ) {
            for ( Double score : scores.values() ) {
                mean += score;
                size++;
            }
        }
        assert size > 0;

        return mean / size;

    }

    /**
     * @return the positiveScores
     */
    public Map<Long, Map<Long, Double>> getPositiveScores() {
        return positiveScores;
    }

    /**
     * @return geometric mean of the pvalues for the supporting links.
     */
    public double getPosPValue() {
        if ( posPvalues.size() == 0 ) return 0.0;
        return computePvalue( this.posPvalues.values() );
    }

    /**
     * @return
     */
    public int getPossibleOverlap() {
        return numQueryGeneGOTerms;
    }

    /**
     * @param eeId
     * @return
     */
    public Collection<Long> getProbes( Long eeId ) {
        Collection<Long> result = new HashSet<Long>();
        result.addAll( getPositiveCorrelationProbes( eeId ) );
        result.addAll( getNegativeCorrelationProbes( eeId ) );
        return result;
    }

    /**
     * @return
     */
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

    /**
     * @param goOverlap of this gene with the query gene
     */
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
     * @param numQueryGeneGOTerms
     */
    public void setNumQueryGeneGOTerms( int numQueryGeneGOTerms ) {
        this.numQueryGeneGOTerms = numQueryGeneGOTerms;
    }

    /**
     * @param taxonId
     */
    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    @Override
    public String toString() {
        return geneName;
    }

    public Collection<ubic.gemma.model.expression.experiment.BioAssaySet> getDatasetsTestedIn() {
        return this.datasetsTestedIn;
    }

    public void setDatasetsTestedIn( Collection<BioAssaySet> datasetsTestedIn ) {
        this.datasetsTestedIn = datasetsTestedIn;
    }

}