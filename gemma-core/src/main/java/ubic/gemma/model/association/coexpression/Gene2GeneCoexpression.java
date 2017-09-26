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

import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.association.Gene2GeneIdAssociation;
import ubic.gemma.model.expression.experiment.BioAssaySet;

import java.util.Collection;

/**
 * Represents coexpression of a pair of genes.
 *
 * @author Paul
 */
public abstract class Gene2GeneCoexpression extends Gene2GeneIdAssociation
        implements Comparable<Gene2GeneCoexpression> {

    private static final long serialVersionUID = 6088623734153830393L;
    /**
     * If true, this represents a positive correlation; false indicates it is negative (sorry, 0 doesn't exist, I guess
     * we could use null).
     */
    final private Boolean positiveCorrelation = null;
    public SupportDetails supportDetails;
    // we assume 1 in case we don't yet have it populated directly from the db - it has to be at least 1...
    private Integer numDataSetsSupporting = 1;

    @Override
    public int compareTo( Gene2GeneCoexpression o ) {
        if ( numDataSetsSupporting != null && o.getNumDatasetsSupporting() != null && numDataSetsSupporting != o
                .getNumDatasetsSupporting() ) {
            return -this.numDataSetsSupporting.compareTo( o.getNumDatasetsSupporting() );
        }

        if ( !this.getFirstGene().equals( o.getFirstGene() ) )
            return this.getFirstGene().compareTo( o.getFirstGene() );

        if ( !this.getSecondGene().equals( o.getSecondGene() ) )
            return this.getSecondGene().compareTo( o.getSecondGene() );

        return 0;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( !super.equals( obj ) )
            return false;

        return this.isPositiveCorrelation().equals( ( ( Gene2GeneCoexpression ) obj ).isPositiveCorrelation() );
    }

    public Collection<Long> getDataSetsSupporting() {
        /*
         * We may be making this available more than one way.
         */
        assert this.supportDetails != null;
        return supportDetails.getIds();
    }

    /**
     * @return The value returned comes from the supportDetails if it is non-null
     */
    public Integer getNumDatasetsSupporting() {
        if ( this.supportDetails != null ) {
            return this.supportDetails.getNumIds();
        }
        return numDataSetsSupporting;
    }

    /**
     * Warning: wouldn't normally use this. This is just to make serializing easier.
     *
     * @param numDataSets Only used if the supportDetails are null. Otherwise this just calls
     *                    updateNumDatasetsSupporting().
     */
    public void setNumDatasetsSupporting( Integer numDataSets ) {
        if ( this.supportDetails != null ) {
            updateNumDatasetsSupporting();
        }
        this.numDataSetsSupporting = numDataSets;
    }

    public SupportDetails getSupportDetails() {
        return supportDetails;
    }

    public void setSupportDetails( SupportDetails supportDetails ) {
        this.supportDetails = supportDetails;
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null )
            return this.getId().hashCode();

        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( isPositiveCorrelation() != null ) ? 0 : isPositiveCorrelation().hashCode() );
        result = prime * result + ( ( getFirstGene() == null ) ? 0 : getFirstGene().hashCode() );
        result = prime * result + ( ( getSecondGene() == null ) ? 0 : getSecondGene().hashCode() );
        return result;
    }

    public Boolean isPositiveCorrelation() {
        return positiveCorrelation;
    }

    /*
     * TODO optimize.
     *

     */
    public boolean isSupportedBy( BioAssaySet bioAssaySet ) {
        assert this.supportDetails != null;
        return this.supportDetails.isIncluded( bioAssaySet.getId() );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + getId() + ", firstGene=" + getFirstGene() + ", secondGene="
                + getSecondGene() + ", pos=" + positiveCorrelation + "; support=" + getNumDatasetsSupporting() + "]";
    }

    /**
     * Refresh the value of numDataSetsSupporting after updating supportDetails (use during modification of the data...)
     */
    public void updateNumDatasetsSupporting() {
        assert this.supportDetails != null;
        this.numDataSetsSupporting = this.supportDetails.getNumIds();
    }

}