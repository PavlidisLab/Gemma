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
package ubic.gemma.model.association.coexpression;

/**
 * <p>
 * Represents the aggregated node degree for a gene. Note that all node degrees are computed as normalized ranks within
 * an ExpressionExperiment, so a rank of 1.0 means the highest node degree probe in the Experiment. When converting to
 * genes, we compute the median of those values across all Experiments.
 * </p>
 */
public abstract class GeneCoexpressionNodeDegree implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree}.
         */
        public static ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree newInstance() {
            return new ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree},
         * taking all possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree newInstance( Double median,
                Double medianDeviation, String distribution, Integer numTests, Double rank, Double pvalue,
                Integer numLinks, Double rankNumLinks, ubic.gemma.model.genome.Gene gene ) {
            final ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree entity = new ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeImpl();
            entity.setMedian( median );
            entity.setMedianDeviation( medianDeviation );
            entity.setDistribution( distribution );
            entity.setNumTests( numTests );
            entity.setRank( rank );
            entity.setPvalue( pvalue );
            entity.setNumLinks( numLinks );
            entity.setRankNumLinks( rankNumLinks );
            entity.setGene( gene );
            return entity;
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree},
         * taking all required and/or read-only properties as arguments.
         */
        public static ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree newInstance( Double median,
                Double medianDeviation, String distribution, Integer numTests, Double rank, Double pvalue,
                ubic.gemma.model.genome.Gene gene ) {
            final ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree entity = new ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeImpl();
            entity.setMedian( median );
            entity.setMedianDeviation( medianDeviation );
            entity.setDistribution( distribution );
            entity.setNumTests( numTests );
            entity.setRank( rank );
            entity.setPvalue( pvalue );
            entity.setGene( gene );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5947391450315581639L;
    private Double median;

    private Double medianDeviation;

    private String distribution;

    private Integer numTests;

    private Double rank;

    private Double pvalue;

    private Integer numLinks;

    private Double rankNumLinks;

    private Long id;

    private ubic.gemma.model.genome.Gene gene;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public GeneCoexpressionNodeDegree() {
    }

    /**
     * Returns <code>true</code> if the argument is an GeneCoexpressionNodeDegree instance and all identifiers for this
     * entity equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneCoexpressionNodeDegree ) ) {
            return false;
        }
        final GeneCoexpressionNodeDegree that = ( GeneCoexpressionNodeDegree ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * A string representation of the node degree distribution.
     * </p>
     */
    public String getDistribution() {
        return this.distribution;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene getGene() {
        return this.gene;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * <p>
     * The median of the distribution of node degree ranks for this gene across data sets, based on probe-level analysis
     * of each data set.
     * </p>
     */
    public Double getMedian() {
        return this.median;
    }

    /**
     * <p>
     * The median absolute deviation from the median (MAD) of the distribution of node degrees
     * </p>
     */
    public Double getMedianDeviation() {
        return this.medianDeviation;
    }

    /**
     * <p>
     * The number of links we have stored for this gene. This is subject to any filtering/selection criteria reflected
     * in the Gene2GeneCoexpression analysis, such as that the link appear in at least two data sets. This measure is an
     * alternative to the median value based on the per-data set probe-level analysis.
     * </p>
     */
    public Integer getNumLinks() {
        return this.numLinks;
    }

    /**
     * <p>
     * In how many experiments the gene was tested for node degree
     * </p>
     */
    public Integer getNumTests() {
        return this.numTests;
    }

    /**
     * <p>
     * The pvalue for the combined ranks of the node degrees, based on the per-data set probe-level analysis
     * </p>
     */
    public Double getPvalue() {
        return this.pvalue;
    }

    /**
     * <p>
     * The relative rank of the node degree of this gene compared to other genes from the same taxon, based on the
     * per-data set probe-level analysis.
     * </p>
     */
    public Double getRank() {
        return this.rank;
    }

    /**
     * <p>
     * See numLinks. This is the relative ranking corresponding to numLinks, where a value of 0 means "lowest number of
     * links in that taxon" and 1 means "highest number of links in that taxon" (ties are broken using the mean rank
     * approach). This is an alternative to the other rank this stores, which is based on the per-data set probe-level
     * analysis.
     * </p>
     */
    public Double getRankNumLinks() {
        return this.rankNumLinks;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setDistribution( String distribution ) {
        this.distribution = distribution;
    }

    public void setGene( ubic.gemma.model.genome.Gene gene ) {
        this.gene = gene;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setMedian( Double median ) {
        this.median = median;
    }

    public void setMedianDeviation( Double medianDeviation ) {
        this.medianDeviation = medianDeviation;
    }

    public void setNumLinks( Integer numLinks ) {
        this.numLinks = numLinks;
    }

    public void setNumTests( Integer numTests ) {
        this.numTests = numTests;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    public void setRank( Double rank ) {
        this.rank = rank;
    }

    public void setRankNumLinks( Double rankNumLinks ) {
        this.rankNumLinks = rankNumLinks;
    }

}