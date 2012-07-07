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

/**
 * The results for one gene that is coexpressed with a query gene, across multiple expression experiments; possibly with
 * multiple probes per expression experiment.
 * <p>
 * Keeps track of specificity, pValues, Scores, stringency value. Information about positive and negative correlations
 * are stored, separately.
 * 
 * @author klc
 * @version $Id$
 */
public class CoexpressedGenePairValueObject implements Comparable<CoexpressedGenePairValueObject> {

    /**
     * Genes that were predicted to cross-hybridize with the target gene
     */
    private Collection<Long> crossHybridizingGenes = new HashSet<Long>();

    /**
     * Datasets in which this link was tested.
     */
    private Collection<Long> datasetsTestedIn = new HashSet<Long>();

    private byte[] datasetsTestedInBytes = null;

    /**
     * ID of the coexpressed gene.
     */
    private Long coexpressedGene;

    private Map<Long, Collection<ProbePair>> links = new HashMap<Long, Collection<ProbePair>>();

    /**
     * Maps of Expression Experiment IDs to maps of Probe IDs to scores that are in support of this coexpression, with
     * negative correlations.
     */
    private Map<Long, Map<Long, Double>> negativeScores;

    /**
     * Expression Experiments which have evidence for coexpression of this gene with the query, but the probes are not
     * specific for the target gene.
     */
    private Collection<Long> nonspecificEEs;

    /**
     * Maps of Expression Experiment IDs to maps of Probe IDs to scores that are in support of this coexpression.
     */
    private Map<Long, Map<Long, Double>> positiveScores;

    private Long queryGene;

    private Double queryGeneNodeDegree;

    private Double foundGeneNodeDegree;

    /**
     * Map of eeId -> probe IDs for the _query_. Each experiment added is a supporting experiment.
     */
    private Map<Long, Collection<Long>> queryProbeInfo;

    /**
     * Note that most of the fields of this are not populated at construction time.
     * 
     * @param queryGene
     * @param coexpressedGene
     */
    public CoexpressedGenePairValueObject( Long queryGene, Long coexpressedGene ) {
        assert queryGene != null;
        assert coexpressedGene != null;
        positiveScores = new HashMap<Long, Map<Long, Double>>();
        negativeScores = new HashMap<Long, Map<Long, Double>>();
        queryProbeInfo = new HashMap<Long, Collection<Long>>();
        nonspecificEEs = new HashSet<Long>();
        this.queryGene = queryGene;
        this.coexpressedGene = coexpressedGene;
    }

    /**
     * @param geneid of gene that is predicted to cross-hybridize with this gene
     */
    public void addCrossHybridizingGene( Long geneid ) {
        if ( geneid.equals( this.coexpressedGene ) ) return;
        this.crossHybridizingGenes.add( geneid );
    }

    /**
     * @param eeID
     * @param score - all we care about is the sign.
     * @param queryProbe
     * @param coexpressedProbe
     */
    public void addScore( Long eeID, Double score, Long queryProbe, Long coexpressedProbe ) {
        assert queryProbe != null;
        assert !queryProbe.equals( coexpressedProbe );
        assert queryProbeInfo != null;
        if ( !queryProbeInfo.containsKey( eeID ) ) {
            queryProbeInfo.put( eeID, new HashSet<Long>() );
        }
        queryProbeInfo.get( eeID ).add( queryProbe );

        if ( !this.links.containsKey( eeID ) ) {
            this.links.put( eeID, new HashSet<ProbePair>() );
        }

        this.links.get( eeID ).add( new ProbePair( queryProbe, coexpressedProbe ) );

        if ( score < 0 ) {
            if ( !negativeScores.containsKey( eeID ) ) negativeScores.put( eeID, new HashMap<Long, Double>() );
            negativeScores.get( eeID ).put( coexpressedProbe, score );
        } else {
            if ( !positiveScores.containsKey( eeID ) ) positiveScores.put( eeID, new HashMap<Long, Double>() );
            positiveScores.get( eeID ).put( coexpressedProbe, score );

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( CoexpressedGenePairValueObject o ) {
        int o1Size = this.getMaxLinkCount();
        int o2Size = o.getMaxLinkCount();
        if ( o1Size > o2Size ) {
            return -1;
        } else if ( o1Size < o2Size ) {
            return 1;
        } else {
            return this.coexpressedGene.compareTo( o.getCoexpressedGeneId() );
        }
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CoexpressedGenePairValueObject other = ( CoexpressedGenePairValueObject ) obj;
        if ( this.queryGene != null && other.queryGene != null && !this.queryGene.equals( other.queryGene ) ) {
            return false;
        }
        if ( coexpressedGene == null ) {
            if ( other.coexpressedGene != null ) return false;
        } else if ( !coexpressedGene.equals( other.coexpressedGene ) ) return false;
        return true;
    }

    @Override
    public void finalize() {
        this.datasetsTestedIn.clear();
        this.crossHybridizingGenes.clear();
        this.datasetsTestedInBytes = null;
        this.negativeScores.clear();
        this.positiveScores.clear();
        for ( Long l : links.keySet() ) {
            links.get( l ).clear();
        }
        links.clear();
    }

    /**
     * @return the geneId of the coexpressed gene
     */
    public Long getCoexpressedGeneId() {
        return coexpressedGene;
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

    public Double getFoundGeneNodeDegree() {
        return foundGeneNodeDegree;
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
    public Long getQueryGene() {
        return queryGene;
    }

    public Double getQueryGeneNodeDegree() {
        return queryGeneNodeDegree;
    }

    /**
     * @return Map of eeId -> probe IDs for the _query_.
     */
    public Map<Long, Collection<Long>> getQueryProbeInfo() {
        return queryProbeInfo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( queryGene == null ) ? 0 : queryGene.hashCode() );
        result = prime * result + ( ( coexpressedGene == null ) ? 0 : coexpressedGene.hashCode() );
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

        }

        if ( this.positiveScores.size() == 0 && this.negativeScores.size() == 0 ) {
            return false;
        }
        return true;

    }

    /**
     * @param geneId the geneId to set
     */
    public void setCoexpressedGene( Long geneId ) {
        this.coexpressedGene = geneId;
    }

    public void setDatasetsTestedIn( Collection<Long> datasetsTestedIn ) {
        this.datasetsTestedIn = datasetsTestedIn;
    }

    public void setDatasetsTestedInBytes( byte[] datasetsTestedInBytes ) {
        this.datasetsTestedInBytes = datasetsTestedInBytes;
    }

    public void setFoundGeneNodeDegree( Double foundGeneNodeDegree ) {
        this.foundGeneNodeDegree = foundGeneNodeDegree;
    }

    /**
     * A 'non-specific ee' is an expression experiment that lacks specific probes for BOTH the query and target genes.
     * 
     * @param nonspecificEEs the nonspecificEE to set
     */
    public void setNonspecificEEs( Collection<Long> nonspecificEEs ) {
        this.nonspecificEEs = nonspecificEEs;
    }

    public void setQueryGene( Long queryGene ) {
        this.queryGene = queryGene;
    }

    public void setQueryGeneNodeDegree( Double queryGeneNodeDegree ) {
        this.queryGeneNodeDegree = queryGeneNodeDegree;
    }

    @Override
    public String toString() {
        // return StringUtils.isBlank( geneName ) ? "Gene " + geneId : geneName;
        StringBuilder buf = new StringBuilder();
        buf.append( "Coexpression value object: query=" + queryGene + " target=" + coexpressedGene + "\n" );
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

    /**
     * @return the links
     */
    protected Map<Long, Collection<ProbePair>> getLinks() {
        return links;
    }

}