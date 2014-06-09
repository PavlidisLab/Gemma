/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * A more heavyweight version of Gene2GeneCoexpressionValueObject; has a bit more information about the genes.
 * <p>
 * FIXME consider merging or making this a subclass of the Gene2GeneCoexpressionValueObject. this is very similar to
 * Gene2GeneCoexpressionValueObject; we use the NCBI id sometimes, and the gene name (not just the symbol), the node
 * degree info; and we need the sort key; plus this offers some additional methods
 * <p>
 * Importantly, this does not necessarily reflect the coexpression data in the database: it may have been filtered in
 * accordance to the query settings in terms of the data sets searched and the maximum number of results., this does not
 * necessarily reflect the coexpression data in the database: it may have been filtered in accordance to the query
 * settings in terms of the data sets searched and the maximum number of results.
 * 
 * @author luke
 * @version $Id$
 * @see ubic.gemma.model.association.coexpression.CoexpressionValueObject
 */
public class CoexpressionValueObjectExt implements Comparable<CoexpressionValueObjectExt> {

    private Boolean containsMyData = false;
    private GeneValueObject foundGene;
    private Integer foundGeneNodeDegree = 0;
    private Double foundGeneNodeDegreeRank = 0.0;
    private Integer negSupp = 0;
    private Integer numTestedIn = 0;
    private Integer posSupp = 0;
    private GeneValueObject queryGene = null;
    private Integer queryGeneNodeDegree = 0;
    private Double queryGeneNodeDegreeRank = 0.0;

    /*
     * Used for client-side sorting. See CoexpressionGrid and CoexpressionGridLight.jsF
     */
    private String sortKey = null;

    private Collection<Long> supportingExperiments = null;

    @Override
    public int compareTo( CoexpressionValueObjectExt arg0 ) {
        return this.getSortKey().compareTo( arg0.getSortKey() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CoexpressionValueObjectExt other = ( CoexpressionValueObjectExt ) obj;
        if ( this.sortKey == null ) {
            if ( other.sortKey != null ) return false;
        } else if ( !sortKey.equals( other.sortKey ) ) {
            return false;
        } else if ( !this.queryGene.getOfficialSymbol().equals( other.queryGene.getOfficialSymbol() ) ) {
            return false;
        }
        return true;
    }

    public Boolean getContainsMyData() {
        return containsMyData;
    }

    /**
     * @return the coexpressed gene.
     */
    public GeneValueObject getFoundGene() {
        return foundGene;
    }

    public Integer getFoundGeneNodeDegree() {
        return foundGeneNodeDegree;
    }

    public Double getFoundGeneNodeDegreeRank() {
        return foundGeneNodeDegreeRank;
    }

    public Integer getNegSupp() {
        return negSupp;
    }

    public Integer getNumTestedIn() {
        return numTestedIn;
    }

    public Integer getPosSupp() {
        return posSupp;
    }

    public GeneValueObject getQueryGene() {
        return queryGene;
    }

    public Integer getQueryGeneNodeDegree() {
        return queryGeneNodeDegree;
    }

    public Double getQueryGeneNodeDegreeRank() {
        return queryGeneNodeDegreeRank;
    }

    public String getSortKey() {
        return sortKey;
    }

    /**
     * @return
     */
    public Integer getSupport() {
        return Math.max( posSupp, negSupp );
    }

    public Collection<Long> getSupportingExperiments() {
        return supportingExperiments;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( sortKey == null ) ? 0 : sortKey.hashCode() );
        return result;
    }

    /**
     * @param geneId
     * @return true if this involves the gene provided (either as query or found gene)
     */
    public boolean involves( Long geneId ) {
        return this.foundGene.getId().equals( geneId ) || this.queryGene.getId().equals( geneId );
    }

    /**
     * @param gene ids
     * @return true if this involves any of the genes provided
     */
    public boolean involvesAny( Set<Long> geneIds ) {
        return geneIds.contains( foundGene.getId() ) || geneIds.contains( queryGene.getId() );
    }

    public void setContainsMyData( Boolean containsMyData ) {
        this.containsMyData = containsMyData;
    }

    public void setFoundGene( GeneValueObject foundGene ) {
        this.foundGene = foundGene;
    }

    public void setFoundGeneNodeDegree( Integer foundGeneNodeDegree ) {
        this.foundGeneNodeDegree = foundGeneNodeDegree;
    }

    public void setFoundGeneNodeDegreeRank( Double foundGeneNodeDegreeRank ) {
        this.foundGeneNodeDegreeRank = foundGeneNodeDegreeRank;
    }

    public void setNegSupp( Integer negSupp ) {
        this.negSupp = negSupp;
    }

    /**
     * @param numTestedIn
     */
    public void setNumTestedIn( Integer numTestedIn ) {
        this.numTestedIn = numTestedIn;
    }

    public void setPosSupp( Integer posSupp ) {
        this.posSupp = posSupp;
    }

    public void setQueryGene( GeneValueObject queryGene ) {
        this.queryGene = queryGene;
    }

    public void setQueryGeneNodeDegree( Integer queryGeneNodeDegree ) {
        this.queryGeneNodeDegree = queryGeneNodeDegree;
    }

    public void setQueryGeneNodeDegreeRank( Double queryGeneNodeDegreeRank ) {
        this.queryGeneNodeDegreeRank = queryGeneNodeDegreeRank;
    }

    public void setSortKey() {
        this.sortKey = String.format( "%.3f_%s", 1.0 / this.getSupport(), getFoundGene().getOfficialSymbol() );
    }

    public void setSortKey( String sortKey ) {
        this.sortKey = sortKey;
    }

    public void setSupportingExperiments( Collection<Long> supportingExperiments ) {
        this.supportingExperiments = supportingExperiments;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if ( this.getPosSupp() > 0 ) {
            buf.append( getSupportRow( getPosSupp(), "+" ) );
        }
        if ( getNegSupp() > 0 ) {
            if ( buf.length() > 0 ) buf.append( "\n" );
            buf.append( getSupportRow( getNegSupp(), "-" ) );
        }
        return buf.toString();
    }

    private String getSupportRow( Integer links, String sign ) {
        String[] fields = new String[] { queryGene.getOfficialSymbol(), foundGene.getOfficialSymbol(),
                links.toString(), sign };
        return StringUtils.join( fields, "\t" );
    }

}
