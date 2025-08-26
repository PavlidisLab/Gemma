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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.reflect.FieldUtils;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.association.Gene2GeneIdAssociation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Objects;

/**
 * Represents coexpression of a pair of genes.
 *
 * @author Paul
 */
@CommonsLog
public abstract class Gene2GeneCoexpression extends Gene2GeneIdAssociation
        implements Comparable<Gene2GeneCoexpression> {

    /**
     * If true, this represents a positive correlation; false indicates it is negative (sorry, 0 doesn't exist, I guess
     * we could use null).
     */
    final private Boolean positiveCorrelation = null;
    public SupportDetails supportDetails;
    // we assume 1 in case we don't yet have it populated directly from the db - it has to be at least 1...
    private Integer numDataSetsSupporting = 1;

    static void tryWriteFields( Double effect, Long firstGene, Long secondGene, Object entity ) {
        try {
            FieldUtils.writeField( entity, "firstGene", firstGene, true );
            FieldUtils.writeField( entity, "secondGene", secondGene, true );
            FieldUtils.writeField( entity, "positiveCorrelation", effect > 0, true );
            FieldUtils.writeField( entity, "numDataSetsSupporting", 1, true );
        } catch ( IllegalAccessException e ) {
            log.error( e.getMessage(), e );
        }
    }

    @Override
    public int compareTo( Gene2GeneCoexpression o ) {
        if ( numDataSetsSupporting != null && o.getNumDatasetsSupporting() != null && !Objects
                .equals( numDataSetsSupporting, o.getNumDatasetsSupporting() ) ) {
            return -this.numDataSetsSupporting.compareTo( o.getNumDatasetsSupporting() );
        }

        if ( !this.getFirstGene().equals( o.getFirstGene() ) )
            return this.getFirstGene().compareTo( o.getFirstGene() );

        if ( !this.getSecondGene().equals( o.getSecondGene() ) )
            return this.getSecondGene().compareTo( o.getSecondGene() );

        return 0;
    }

    @SuppressWarnings("unused") // Possible external use
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
            this.updateNumDatasetsSupporting();
        }
        this.numDataSetsSupporting = numDataSets;
    }

    public SupportDetails getSupportDetails() {
        return supportDetails;
    }

    public void setSupportDetails( SupportDetails supportDetails ) {
        this.supportDetails = supportDetails;
    }

    public Boolean isPositiveCorrelation() {
        return positiveCorrelation;
    }

    public boolean isSupportedBy( ExpressionExperiment ee ) {
        assert this.supportDetails != null;
        return this.supportDetails.isIncluded( ee.getId() );
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null )
            return this.getId().hashCode();

        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this.isPositiveCorrelation() != null ) ?
                0 :
                this.isPositiveCorrelation().hashCode() );
        result = prime * result + ( ( this.getFirstGene() == null ) ? 0 : this.getFirstGene().hashCode() );
        result = prime * result + ( ( this.getSecondGene() == null ) ? 0 : this.getSecondGene().hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        return super.equals( obj ) && this.isPositiveCorrelation()
                .equals( ( ( Gene2GeneCoexpression ) obj ).isPositiveCorrelation() );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + this.getId() + ", firstGene=" + this.getFirstGene()
                + ", secondGene=" + this.getSecondGene() + ", pos=" + positiveCorrelation + "; support=" + this
                .getNumDatasetsSupporting() + "]";
    }

    /**
     * Refresh the value of numDataSetsSupporting after updating supportDetails (use during modification of the data...)
     */
    public void updateNumDatasetsSupporting() {
        assert this.supportDetails != null;
        this.numDataSetsSupporting = this.supportDetails.getNumIds();
    }

}