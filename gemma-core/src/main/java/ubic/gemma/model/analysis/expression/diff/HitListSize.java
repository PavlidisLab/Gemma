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

import java.io.Serializable;

/**
 * <p>
 * The number of probes meeting a given q-value threshold in the result set.
 * </p>
 */
public class HitListSize implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6398621486105806034L;
    private Double thresholdQvalue;
    private Integer numberOfProbes;
    private Direction direction;
    private Integer numberOfGenes;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public HitListSize() {
    }

    public Direction getDirection() {
        return this.direction;
    }

    public void setDirection( Direction direction ) {
        this.direction = direction;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
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
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
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
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
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