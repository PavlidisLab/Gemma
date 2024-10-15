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
package ubic.gemma.model.analysis.expression.diff;

public class PvalueDistribution implements java.io.Serializable {

    private static final long serialVersionUID = -4783507721422402289L;
    private Integer numBins;
    private double[] binCounts;
    private Long id;

    public double[] getBinCounts() {
        return this.binCounts;
    }

    public void setBinCounts( double[] binCounts ) {
        this.binCounts = binCounts;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Integer getNumBins() {
        return this.numBins;
    }

    public void setNumBins( Integer numBins ) {
        this.numBins = numBins;
    }

    /**
     * @return a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof PvalueDistribution ) ) {
            return false;
        }
        final PvalueDistribution that = ( PvalueDistribution ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    public static final class Factory {
        public static PvalueDistribution newInstance() {
            return new PvalueDistribution();
        }
    }

}