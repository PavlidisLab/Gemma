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

import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Arrays;
import java.util.Objects;

public class PvalueDistribution extends AbstractIdentifiable {

    private int numBins;
    private double[] binCounts;

    public double[] getBinCounts() {
        return this.binCounts;
    }

    public void setBinCounts( double[] binCounts ) {
        this.binCounts = binCounts;
    }

    public int getNumBins() {
        return this.numBins;
    }

    public void setNumBins( int numBins ) {
        this.numBins = numBins;
    }

    @Override
    public int hashCode() {
        return Objects.hash( numBins );
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
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( numBins, that.numBins )
                    && Arrays.equals( binCounts, that.binCounts );
        }
    }

    public static final class Factory {
        public static PvalueDistribution newInstance( double[] binCounts ) {
            PvalueDistribution pvd = new PvalueDistribution();
            pvd.setNumBins( binCounts.length );
            pvd.setBinCounts( binCounts );
            return pvd;
        }
    }

}