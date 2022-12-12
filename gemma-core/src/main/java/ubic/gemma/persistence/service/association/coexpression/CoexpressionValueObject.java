/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

package ubic.gemma.persistence.service.association.coexpression;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Lightweight/convenient object for manipulating coexpression for a pair of genes. Importantly, this does not
 * necessarily reflect the coexpression data in the database: it may have been filtered in accordance to the query
 * settings in terms of the data sets searched and the maximum number of results.
 * Note that hashCode and equals do not use the ID of the coexpression; they only use the genes (ignoring which is query
 * vs. found) and the sign, since those are in effect unique in the system.
 *
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class CoexpressionValueObject implements Comparable<CoexpressionValueObject>, Serializable {

    private Long coexGeneId;
    private boolean positiveCorrelation;
    private Long queryGeneId;
    private String coexGeneSymbol;
    /**
     * If true, this means the results were trimmed to a subset.
     */
    private boolean eeConstraint = false;
    /**
     * True if these data were pulled from the cache, or if they were put in the cache (so future queries will find
     * them); false otherwise.
     */
    private boolean fromCache = false;
    // is this a link among two query genes. If there were no specific query genes then this doesn't get used.
    private boolean interQueryLink = false;
    /**
     * the max results limit used in the query; 0 means no limit.
     */
    private int maxResults = 0;
    private String queryGeneSymbol;

    /**
     * The stringency used in the query (will be 1 by default)
     */
    private int queryStringency = 1;

    @SuppressWarnings("UnusedAssignment") // Needed for frontend
    private Integer support = 1;

    private Long supportDetailsId;

    /**
     * The data sets which supported the link. If eeConstraint = true, this reflects only data sets which were in the
     * query.
     */
    private Set<Long> supportingDatasets = null;

    /**
     * The data sets in which the link was tested. If eeConstraint = true, this reflects only data sets which were in
     * the query.
     */
    private Set<Long> testedInDatasets = null;

    public CoexpressionValueObject() {
        super();
    }

    /**
     * Construct a value object. The "tested-in" component is not filled in, it must be done later.
     *
     * @param g2g g2g
     */
    public CoexpressionValueObject( Gene2GeneCoexpression g2g ) {
        queryGeneId = g2g.getFirstGene();
        coexGeneId = g2g.getSecondGene();
        positiveCorrelation = g2g.isPositiveCorrelation();

        if ( g2g.getSupportDetails() != null ) {
            this.supportingDatasets = g2g.getSupportDetails().getIdsSet();
            support = this.supportingDatasets.size();
            supportDetailsId = g2g.getSupportDetails().getId();
        } else if ( g2g.getNumDatasetsSupporting() != null ) {
            support = g2g.getNumDatasetsSupporting();
        } else {
            throw new IllegalArgumentException( "Support was not available" );
        }
        // "testedin" is filled in later.
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    protected CoexpressionValueObject( Long queryGeneId, Long coexGeneId, Boolean positiveCorrelation, Integer support,
            Long supportDetailsId, Set<Long> supportingDatasets ) {
        super();
        this.coexGeneId = coexGeneId;
        this.positiveCorrelation = positiveCorrelation;
        this.queryGeneId = queryGeneId;
        this.support = support;
        this.supportDetailsId = supportDetailsId;
        this.supportingDatasets = supportingDatasets;
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    protected CoexpressionValueObject( Long coexGeneId, String coexGeneSymbol, boolean positiveCorrelation,
            Long queryGeneId, String queryGeneSymbol, Integer support, Long supportDetailsId,
            Collection<Long> supportingDatasets, Collection<Long> testedInDatasets ) {
        super();
        this.coexGeneId = coexGeneId;
        this.coexGeneSymbol = coexGeneSymbol;
        this.positiveCorrelation = positiveCorrelation;
        this.queryGeneId = queryGeneId;
        this.queryGeneSymbol = queryGeneSymbol;
        this.support = support;
        this.supportDetailsId = supportDetailsId;
        this.supportingDatasets = ( Set<Long> ) supportingDatasets;
        this.testedInDatasets = ( Set<Long> ) testedInDatasets;
    }

    protected CoexpressionValueObject( Long coexGeneId, String coexGeneSymbol, boolean positiveCorrelation,
            Long queryGeneId, String queryGeneSymbol, Integer support, Long supportDetailsId,
            Set<Long> supportingDatasets, Set<Long> testedInDatasets ) {
        super();
        this.coexGeneId = coexGeneId;
        this.coexGeneSymbol = coexGeneSymbol;
        this.positiveCorrelation = positiveCorrelation;
        this.queryGeneId = queryGeneId;
        this.queryGeneSymbol = queryGeneSymbol;
        this.support = support;
        this.supportDetailsId = supportDetailsId;
        this.supportingDatasets = supportingDatasets;
        this.testedInDatasets = testedInDatasets;
    }

    @Override
    public int compareTo( CoexpressionValueObject o ) {
        return -this.support.compareTo( o.getNumDatasetsSupporting() );
    }

    public Long getCoexGeneId() {
        return coexGeneId;
    }

    public String getCoexGeneSymbol() {
        return coexGeneSymbol;
    }

    public void setCoexGeneSymbol( String coexGeneSymbol ) {
        this.coexGeneSymbol = coexGeneSymbol;
    }

    public int getMaxResults() {
        return maxResults;
    }

    void setMaxResults( int maxResults ) {
        this.maxResults = maxResults;
    }

    /**
     * @return number of data sets this link was found in
     */
    public Integer getNumDatasetsSupporting() {
        return support;
    }

    /**
     * @return number of data sets this link was tested in, of -1 if the information was not retrieved or if the value
     * is zero (which is basically an error).
     */
    public Integer getNumDatasetsTestedIn() {
        if ( testedInDatasets == null || testedInDatasets.isEmpty() ) {
            return -1;
        }
        return this.testedInDatasets.size();
    }

    public Long getQueryGeneId() {
        return queryGeneId;
    }

    public String getQueryGeneSymbol() {
        return queryGeneSymbol;
    }

    public void setQueryGeneSymbol( String queryGeneSymbol ) {
        this.queryGeneSymbol = queryGeneSymbol;
    }

    public int getQueryStringency() {
        return queryStringency;
    }

    void setQueryStringency( int queryStringency ) {
        this.queryStringency = queryStringency;
    }

    public Long getSupportDetailsId() {
        return supportDetailsId;
    }

    /**
     * @return the IDs of the supporting data sets; or an empty collection if the information was not retrieved
     */
    public Set<Long> getSupportingDatasets() {
        return supportingDatasets;
    }

    /**
     * Normally we only set this if we are constraining the query to a subset of the database.
     *
     * @param ids ids
     */
    void setSupportingDatasets( Set<Long> ids ) {
        assert ids != null && !ids.isEmpty();
        this.supportingDatasets = ids;
    }

    public Set<Long> getTestedInDatasets() {
        return testedInDatasets;
    }

    void setTestedInDatasets( Set<Long> ids ) {
        assert ids != null && !ids.isEmpty();
        assert this.testedInDatasets == null || this.testedInDatasets.isEmpty();

        this.testedInDatasets = ids;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( positiveCorrelation ? 1231 : 1237 );

        if ( coexGeneId < queryGeneId ) {
            result = prime * result + coexGeneId.hashCode();
            result = prime * result + queryGeneId.hashCode();
        } else {
            result = prime * result + queryGeneId.hashCode();
            result = prime * result + coexGeneId.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        CoexpressionValueObject other = ( CoexpressionValueObject ) obj;
        if ( positiveCorrelation != other.positiveCorrelation )
            return false;

        // we don't differentiate between the two genes (the "order")
        return coexGeneId.equals( other.coexGeneId ) && queryGeneId.equals( other.queryGeneId )
                || queryGeneId.equals( other.coexGeneId ) && coexGeneId.equals( other.queryGeneId );

    }

    @Override
    public String toString() {
        String[] fields = new String[] { queryGeneId.toString(), queryGeneSymbol, coexGeneId.toString(), coexGeneSymbol,
                support.toString(),
                ( this.testedInDatasets != null ? Integer.toString( this.testedInDatasets.size() ) : "?" ),
                positiveCorrelation ? "+" : "-" };
        return StringUtils.join( fields, "\t" );
    }

    public boolean isEeConstraint() {
        return eeConstraint;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache( boolean fromCache ) {
        this.fromCache = fromCache;
    }

    public boolean isInterQueryLink() {
        return interQueryLink;
    }

    public void setInterQueryLink( boolean interQueryLink ) {
        this.interQueryLink = interQueryLink;
    }

    public boolean isPositiveCorrelation() {
        return positiveCorrelation;
    }

    /**
     * Constrain the results returned to include only the data sets indicated. The support, supportingDatasets,
     * testedInDatasets and eeConstraint are all potentially altered by this call. This means the support might be end
     * up below the stringency, in which case this returns false.
     * eeConstraint will only be changed from its current value if the constraint had any effect (so running this twice
     * is okay).
     *
     * @param bas        bas
     * @param stringency stringency
     * @return true if this still meets the stringency. If it returns false, we assume that means it will be rejected so
     * we don't bother actually trimming.
     */
    boolean trimDatasets( Collection<Long> bas, int stringency ) {

        boolean changed = this.supportingDatasets.retainAll( bas );

        this.support = this.supportingDatasets.size();

        if ( this.testedInDatasets != null ) {
            changed = this.testedInDatasets.retainAll( bas ) || changed;
            assert this.testedInDatasets.size() >= this.supportingDatasets.size();
        }

        this.eeConstraint = this.eeConstraint || changed;

        assert stringency >= this.queryStringency;
        return this.support >= stringency;
    }
}
