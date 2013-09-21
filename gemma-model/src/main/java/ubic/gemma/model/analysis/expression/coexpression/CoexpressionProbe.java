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
package ubic.gemma.model.analysis.expression.coexpression;

/**
 * <p>
 * Details of a probe included in the analysis.
 * </p>
 */
public abstract class CoexpressionProbe implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe}.
         */
        public static ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe newInstance() {
            return new ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbeImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe},
         * taking all possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe newInstance(
                Integer nodeDegree, Double nodeDegreeRank,
                ubic.gemma.model.expression.designElement.CompositeSequence probe ) {
            final ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe entity = new ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbeImpl();
            entity.setNodeDegree( nodeDegree );
            entity.setNodeDegreeRank( nodeDegreeRank );
            entity.setProbe( probe );
            return entity;
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe},
         * taking all required and/or read-only properties as arguments.
         */
        public static ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe newInstance(
                ubic.gemma.model.expression.designElement.CompositeSequence probe ) {
            final ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe entity = new ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbeImpl();
            entity.setProbe( probe );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5084811170208064072L;
    private Integer nodeDegree;

    private Double nodeDegreeRank;

    private Long id;

    private ubic.gemma.model.expression.designElement.CompositeSequence probe;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public CoexpressionProbe() {
    }

    /**
     * Returns <code>true</code> if the argument is an CoexpressionProbe instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof CoexpressionProbe ) ) {
            return false;
        }
        final CoexpressionProbe that = ( CoexpressionProbe ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * <p>
     * How many other probes this probe was linked to. The filter applied may have removed it from consideration.
     * </p>
     */
    public Integer getNodeDegree() {
        return this.nodeDegree;
    }

    /**
     * <p>
     * The (normalized) ranking of this probe in the node degree distribution for this analysis. 0 = lowest, 1=highest.
     * </p>
     */
    public Double getNodeDegreeRank() {
        return this.nodeDegreeRank;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence getProbe() {
        return this.probe;
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

    public void setId( Long id ) {
        this.id = id;
    }

    public void setNodeDegree( Integer nodeDegree ) {
        this.nodeDegree = nodeDegree;
    }

    public void setNodeDegreeRank( Double nodeDegreeRank ) {
        this.nodeDegreeRank = nodeDegreeRank;
    }

    public void setProbe( ubic.gemma.model.expression.designElement.CompositeSequence probe ) {
        this.probe = probe;
    }

}