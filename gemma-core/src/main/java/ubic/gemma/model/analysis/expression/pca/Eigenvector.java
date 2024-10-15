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
package ubic.gemma.model.analysis.expression.pca;

/**
 * A right singular vector (a.k.a. eigengenes)
 */
public class Eigenvector implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5122763307995485698L;
    private Integer componentNumber;
    private double[] vector;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Eigenvector() {
    }

    public Integer getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber( Integer componentNumber ) {
        this.componentNumber = componentNumber;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return Binary representing array of doubles
     */
    public double[] getVector() {
        return this.vector;
    }

    public void setVector( double[] vector ) {
        this.vector = vector;
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

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Eigenvector ) ) {
            return false;
        }
        final Eigenvector that = ( Eigenvector ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    public static final class Factory {

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static Eigenvector newInstance() {
            return new Eigenvector();
        }

        public static Eigenvector newInstance( Integer componentNumber, double[] vector ) {
            final Eigenvector entity = new Eigenvector();
            entity.setComponentNumber( componentNumber );
            entity.setVector( vector );
            return entity;
        }
    }

}