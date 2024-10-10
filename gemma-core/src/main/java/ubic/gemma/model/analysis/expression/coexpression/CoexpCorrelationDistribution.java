/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.coexpression;

/**
 * @author Paul
 */
public class CoexpCorrelationDistribution {

    private double[] binCounts;
    private Long id;
    private Integer numBins;

    public double[] getBinCounts() {
        return binCounts;
    }

    public void setBinCounts( double[] binCounts ) {
        this.binCounts = binCounts;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Integer getNumBins() {
        return numBins;
    }

    public void setNumBins( Integer numBins ) {
        this.numBins = numBins;
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

    /**
     * Returns <code>true</code> if the argument is an PvalueDistribution instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof CoexpCorrelationDistribution ) ) {
            return false;
        }
        final CoexpCorrelationDistribution that = ( CoexpCorrelationDistribution ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    public static final class Factory {
        public static CoexpCorrelationDistribution newInstance() {
            return new CoexpCorrelationDistribution();
        }
    }
}
