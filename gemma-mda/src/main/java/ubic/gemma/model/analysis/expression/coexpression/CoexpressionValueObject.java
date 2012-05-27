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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * The results for one gene that is coexpressed with a query gene, across multiple expression experiments; possibly with
 * multiple probes per expression experiment.
 * <p>
 * Keeps track of specificity, pValues, Scores, goTerms, GO overlap with the query, stringency value. Information about
 * positive and negative correlations are stored, separately.
 * 
 * @author klc
 * @version $Id$
 */
public class CoexpressionValueObject implements Comparable<CoexpressionValueObject> {

    private static Log log = LogFactory.getLog( CoexpressionValueObject.class.getName() );

    /**
     * Genes that were predicted to cross-hybridize with the target gene
     */
    private Collection<Long> crossHybridizingGenes = new HashSet<Long>();

    private Collection<Long> datasetsTestedIn = new HashSet<Long>();
    // the expression experiments that this coexpression was involved in. The number of these will total the 'support'
    // (pos+neg correlations, minus # of experiments that support both + and -)
    private Map<Long, ExpressionExperimentValueObject> expressionExperimentValueObjects;

    private byte[] datasetsTestedInBytes = null;

    /**
     * ID of the coexpressed gene.
     */
    private Long geneId;

    /**
     * Name of the coexpressed gene
     */
    private String geneName;

    /**
     * Official symbol of the coexpressed gene
     */
    private String geneOfficialName;

    /**
     * Gene type fo the coexpressed gene
     */
    private String geneType = null;

    /**
     * Number of GO terms this gene shares with the query gene.
     */
    private Collection<OntologyTerm> goOverlap;

    private Map<Long, Collection<ProbePair>> links = new HashMap<Long, Collection<ProbePair>>();

    private Map<Long, Map<Long, Double>> negativeScores;

    private Map<Long, Map<Long, Double>> negPvalues;

    /**
     * Expression Experiments whihc have evidence for coexpression of this gene with the query, but the probes are not
     * specific for the target gene.
     */
    private Collection<Long> nonspecificEEs;

    /**
     * Number of GO terms the query gene has. This is the highest possible overlap
     */
    private int numQueryGeneGOTerms;

    /**
     * Maps of Expression Experiment IDs to maps of Probe IDs to scores/pvalues that are in support of this
     * coexpression.
     */
    private Map<Long, Map<Long, Double>> positiveScores;

    private Map<Long, Map<Long, Double>> posPvalues;

    private Gene queryGene;

    private Double queryGeneNodeDegree;

    private Double foundGeneNodeDegree;

    public Double getQueryGeneNodeDegree() {
        return queryGeneNodeDegree;
    }

    public void setQueryGeneNodeDegree( Double queryGeneNodeDegree ) {
        this.queryGeneNodeDegree = queryGeneNodeDegree;
    }

    public Double getFoundGeneNodeDegree() {
        return foundGeneNodeDegree;
    }

    public void setFoundGeneNodeDegree( Double foundGeneNodeDegree ) {
        this.foundGeneNodeDegree = foundGeneNodeDegree;
    }

    /**
     * Map of eeId -> probe IDs for the _query_.
     */
    private Map<Long, Collection<Long>> queryProbeInfo;

    private Long taxonId;

    private String abaGeneUrl;

    public CoexpressionValueObject() {
        geneName = "";
        geneId = null;
        geneOfficialName = null;
        expressionExperimentValueObjects = new HashMap<Long, ExpressionExperimentValueObject>();
        positiveScores = new HashMap<Long, Map<Long, Double>>();
        negativeScores = new HashMap<Long, Map<Long, Double>>();
        posPvalues = new HashMap<Long, Map<Long, Double>>();
        negPvalues = new HashMap<Long, Map<Long, Double>>();
        queryProbeInfo = new HashMap<Long, Collection<Long>>();
        nonspecificEEs = new HashSet<Long>();
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
     * @param eeID
     * @param score
     * @param pvalue
     * @param probeID
     * @param outputProbeId
     */
    public void addScore( Long eeID, Double score, Double pvalue, Long queryProbe, Long coexpressedProbe ) {

        assert !queryProbe.equals( coexpressedProbe );
        if ( !queryProbeInfo.containsKey( eeID ) ) {
            queryProbeInfo.put( eeID, new HashSet<Long>() );
        }
        queryProbeInfo.get( eeID ).add( queryProbe );

        if ( !this.links.containsKey( eeID ) ) {
            this.links.put( eeID, new HashSet<ProbePair>() );
        }

        this.links.get( eeID ).add( new ProbePair( queryProbe, coexpressedProbe, score, pvalue ) );

        if ( score < 0 ) {
            if ( !negativeScores.containsKey( eeID ) ) negativeScores.put( eeID, new HashMap<Long, Double>() );
            if ( !negPvalues.containsKey( eeID ) ) negPvalues.put( eeID, new HashMap<Long, Double>() );
            negPvalues.get( eeID ).put( coexpressedProbe, pvalue );
            negativeScores.get( eeID ).put( coexpressedProbe, score );

        } else {
            if ( !positiveScores.containsKey( eeID ) ) positiveScores.put( eeID, new HashMap<Long, Double>() );
            if ( !posPvalues.containsKey( eeID ) ) posPvalues.put( eeID, new HashMap<Long, Double>() );
            posPvalues.get( eeID ).put( coexpressedProbe, pvalue );
            positiveScores.get( eeID ).put( coexpressedProbe, score );

        }

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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
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
     * FIXME just returning zero for now.
     * <p>
     * Compute a combined pvalue for the scores.
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

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CoexpressionValueObject other = ( CoexpressionValueObject ) obj;
        if ( geneId == null ) {
            if ( other.geneId != null ) return false;
        } else if ( !geneId.equals( other.geneId ) ) return false;
        return true;
    }

    public String getAbaGeneUrl() {
        return abaGeneUrl;
    }

    /**
     * @return IDs of genes that may be crosshybridizing with the target gene for this.
     */
    public Collection<Long> getCrossHybridizingGenes() {
        return crossHybridizingGenes;
    }

    /**
     * Collection of EE IDs in which the link was tested.
     * 
     * @return
     */
    public Collection<Long> getDatasetsTestedIn() {
        return this.datasetsTestedIn;
    }

    public byte[] getDatasetsTestedInBytes() {
        return datasetsTestedInBytes;
    }

    /**
     * @return a collection of EE ids that contributed to this genes negative expression
     */
    public Collection<Long> getEEContributing2NegativeLinks() {
        return negativeScores.keySet();

    }

    /**
     * @return a collection of EEids that contributed to this genes positive expression
     */
    public Collection<Long> getEEContributing2PositiveLinks() {
        return positiveScores.keySet();
    }

    /**
     * @return experiments that are supporting coexpression.
     */
    public Collection<Long> getExpressionExperiments() {
        /*
         * We don't use the expresionexperimentvalueobject keyset because there may be 'cruft' after pruning the
         * results.
         */
        Collection<Long> eeIDs = new HashSet<Long>();
        eeIDs.addAll( this.getNegativeScores().keySet() );
        eeIDs.addAll( this.getPositiveScores().keySet() );
        return eeIDs;
    }

    /**
     * @return the geneId of the coexpressed gene
     */
    public Long getGeneId() {
        return geneId;
    }

    /**
     * @return the geneName of the coexpressed gene
     */
    public String getGeneName() {
        return geneName;
    }

    /**
     * @return the geneOfficialName of the coexpressed gene
     */
    public String getGeneOfficialName() {
        return geneOfficialName;
    }

    /**
     * @return the geneType (known gene, predicted, or probe-aligned region) of the coexpressed gene
     */
    public String getGeneType() {
        return geneType;
    }

    /**
     * @return Gene Ontology similarity of the coexpressed gene with the query gene.
     */
    public Collection<OntologyTerm> getGoOverlap() {
        return goOverlap;
    }

    /**
     * @return the links
     */
    protected Map<Long, Collection<ProbePair>> getLinks() {
        return links;
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

    // /**
    // * @return
    // */
    // public String getImageMapName() {
    // StringBuffer buf = new StringBuffer();
    // buf.append( "map." );
    // buf.append( geneType );
    // buf.append( ".gene" );
    // buf.append( geneId );
    // buf.append( ".taxon" );
    // buf.append( taxonId );
    // return buf.toString();
    // }

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
     * @return the negativeScores, a map of EEID->ProbeID->Correlation score.
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
     * @return the nonspecificEE
     */
    public Collection<Long> getNonspecificEE() {
        return nonspecificEEs;
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
     * @return the positiveScores, a map of EEID->ProbeID->Correlation score.
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
     * @return the query gene
     */
    public Gene getQueryGene() {
        return queryGene;
    }

    /**
     * @return Map of eeId -> probe IDs for the _query_.
     */
    public Map<Long, Collection<Long>> getQueryProbeInfo() {
        return queryProbeInfo;
    }

    /**
     * @return
     */
    public Long getTaxonId() {
        return taxonId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
        return result;
    }

    /**
     * Delete the data for a specific EE-probe combination. This is done during filtering to remove, for example, probes
     * that hybridize with the query gene.
     * 
     * @param probeId
     * @param eeId
     * @return true if there is still evidence of coexpression left in this object, false if not.
     */
    public boolean removeProbeEvidence( Long probeId, Long eeId ) {

        Collection<ProbePair> pairs = this.getLinks().get( eeId );
        for ( Iterator<ProbePair> it = pairs.iterator(); it.hasNext(); ) {
            ProbePair probePair = it.next();
            if ( probePair.getQueryProbeId().equals( probeId ) || probePair.getTargetProbeId().equals( probeId ) ) {
                it.remove();
            }
        }

        if ( this.positiveScores.containsKey( eeId ) ) {
            Map<Long, Double> map = this.positiveScores.get( eeId );
            if ( map.containsKey( probeId ) ) {
                map.remove( probeId );
            }

            /*
             * At this point, we may have removed all evidence for the EE supporting the coexpression. In that case,
             * remove the ee.
             */
            if ( map.size() == 0 ) {
                this.positiveScores.remove( eeId );
            }

            Map<Long, Double> map2 = this.posPvalues.get( eeId );
            if ( map2.containsKey( probeId ) ) {
                map2.remove( probeId );
            }

            if ( map2.size() == 0 ) {
                this.posPvalues.remove( eeId );
            }
        }

        /*
         * Do the same thing for negative correlations.
         */
        if ( this.negativeScores.containsKey( eeId ) ) {
            Map<Long, Double> map = this.negativeScores.get( eeId );
            if ( map.containsKey( probeId ) ) {
                map.remove( probeId );
            }
            if ( map.size() == 0 ) {
                this.negativeScores.remove( eeId );
            }

            Map<Long, Double> map2 = this.negPvalues.get( eeId );
            if ( map2.containsKey( probeId ) ) {
                map2.remove( probeId );
            }

            if ( map2.size() == 0 ) {
                this.negPvalues.remove( eeId );
            }
        }

        if ( this.positiveScores.size() == 0 && this.negativeScores.size() == 0 ) {
            return false;
        }
        return true;

    }

    public void setAbaGeneUrl( String abaGeneUrl ) {
        this.abaGeneUrl = abaGeneUrl;
    }

    public void setDatasetsTestedIn( Collection<Long> datasetsTestedIn ) {
        this.datasetsTestedIn = datasetsTestedIn;
    }

    public void setDatasetsTestedInBytes( byte[] datasetsTestedInBytes ) {
        this.datasetsTestedInBytes = datasetsTestedInBytes;
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
     * A 'non-specific ee' is an expression experiment that lacks specific probes for BOTH the query and target genes.
     * 
     * @param nonspecificEEs the nonspecificEE to set
     */
    public void setNonspecificEEs( Collection<Long> nonspecificEEs ) {
        this.nonspecificEEs = nonspecificEEs;
    }

    /**
     * @param numQueryGeneGOTerms
     */
    public void setNumQueryGeneGOTerms( int numQueryGeneGOTerms ) {
        this.numQueryGeneGOTerms = numQueryGeneGOTerms;
    }

    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }

    /**
     * @param taxonId
     */
    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    @Override
    public String toString() {
        // return StringUtils.isBlank( geneName ) ? "Gene " + geneId : geneName;
        StringBuilder buf = new StringBuilder();
        buf.append( "Coexpression value object: query=" + queryGene + " target=" + geneId + " " + geneName + "\n" );
        buf.append( "Tested in " + datasetsTestedIn.size() + ": " + StringUtils.join( datasetsTestedIn, ',' ) + "\n" );
        if ( positiveScores.size() > 0 ) {
            buf.append( "Positive correlation support=" + positiveScores.size() + "\n" );
            for ( Long eeid : positiveScores.keySet() ) {

                Collection<Long> qprobes = queryProbeInfo.get( eeid );

                for ( Long probe : positiveScores.get( eeid ).keySet() ) {
                    for ( Long qprobe : qprobes ) {
                        buf.append( "EE=" + eeid + " tprobe=" + probe + " qprobe=" + qprobe + " specific="
                                + ( this.nonspecificEEs.contains( eeid ) ? "n" : "y" ) + "\n" );
                    }
                }
            }
        }

        if ( negativeScores.size() > 0 ) {
            buf.append( "Negative correlation support=" + negativeScores.size() + "\n" );
            for ( Long eeid : negativeScores.keySet() ) {
                for ( Long probe : negativeScores.get( eeid ).keySet() ) {
                    buf.append( "EE=" + eeid + " probe=" + probe + " specific="
                            + ( this.nonspecificEEs.contains( eeid ) ? "n" : "y" ) + "\n" );
                }
            }
        }

        return buf.toString();
    }

}