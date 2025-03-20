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

import java.util.Objects;

/**
 * <p>
 * The number of probes meeting a given q-value threshold in the result set.
 * </p>
 */
public class HitListSize extends AbstractIdentifiable {

    private Double thresholdQvalue;
    private Integer numberOfProbes;
    private Direction direction;
    private Integer numberOfGenes;

    public Direction getDirection() {
        return this.direction;
    }

    public void setDirection( Direction direction ) {
        this.direction = direction;
    }

    /**
     * @return Number of genes meeting the threshold.
     */
    public Integer getNumberOfGenes() {
        return this.numberOfGenes;
    }

    public void setNumberOfGenes( Integer numberOfGenes ) {
        this.numberOfGenes = numberOfGenes;
    }

    public Integer getNumberOfProbes() {
        return this.numberOfProbes;
    }

    public void setNumberOfProbes( Integer numberOfProbes ) {
        this.numberOfProbes = numberOfProbes;
    }

    public Double getThresholdQvalue() {
        return this.thresholdQvalue;
    }

    public void setThresholdQvalue( Double thresholdQvalue ) {
        this.thresholdQvalue = thresholdQvalue;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( thresholdQvalue, numberOfGenes, direction, numberOfGenes );
    }

    /**
     * @param object the object to compare with
     * @return <code>true</code> if the argument is an HitListSize instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof HitListSize ) ) {
            return false;
        }
        final HitListSize that = ( HitListSize ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return Objects.equals( this.thresholdQvalue, that.thresholdQvalue )
                    && Objects.equals( this.numberOfProbes, that.numberOfProbes )
                    && Objects.equals( this.direction, that.direction )
                    && Objects.equals( this.numberOfGenes, that.numberOfGenes );
        }
    }

    public static final class Factory {

        public static HitListSize newInstance() {
            return new HitListSize();
        }

        public static HitListSize newInstance( Double thresholdQvalue,
                Integer numberOfProbes, Direction direction, Integer numberOfGenes ) {
            final HitListSize entity = new HitListSize();
            entity.setThresholdQvalue( thresholdQvalue );
            entity.setNumberOfProbes( numberOfProbes );
            entity.setDirection( direction );
            entity.setNumberOfGenes( numberOfGenes );
            return entity;
        }
    }

}