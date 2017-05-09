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

/**
 * <p>
 * The number of probes meeting a given q-value threshold in the result set.
 * </p>
 */
public abstract class HitListSize implements java.io.Serializable {

    /**
     * Constructs new instances of {@link HitListSize}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link HitListSize}.
         */
        public static HitListSize newInstance() {
            return new HitListSizeImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.diff.HitListSize}, taking all
         * possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.analysis.expression.diff.HitListSize newInstance( Double thresholdQvalue,
                Integer numberOfProbes, Direction direction, Integer numberOfGenes ) {
            final ubic.gemma.model.analysis.expression.diff.HitListSize entity = new ubic.gemma.model.analysis.expression.diff.HitListSizeImpl();
            entity.setThresholdQvalue( thresholdQvalue );
            entity.setNumberOfProbes( numberOfProbes );
            entity.setDirection( direction );
            entity.setNumberOfGenes( numberOfGenes );
            return entity;
        }
    }

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

    /**
     * Returns <code>true</code> if the argument is an HitListSize instance and all identifiers for this entity equal
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
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public Direction getDirection() {
        return this.direction;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * <p>
     * Number of genes meeting the threshold.
     * </p>
     */
    public Integer getNumberOfGenes() {
        return this.numberOfGenes;
    }

    /**
     * 
     */
    public Integer getNumberOfProbes() {
        return this.numberOfProbes;
    }

    /**
     * 
     */
    public Double getThresholdQvalue() {
        return this.thresholdQvalue;
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

    public void setDirection( Direction direction ) {
        this.direction = direction;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setNumberOfGenes( Integer numberOfGenes ) {
        this.numberOfGenes = numberOfGenes;
    }

    public void setNumberOfProbes( Integer numberOfProbes ) {
        this.numberOfProbes = numberOfProbes;
    }

    public void setThresholdQvalue( Double thresholdQvalue ) {
        this.thresholdQvalue = thresholdQvalue;
    }

}