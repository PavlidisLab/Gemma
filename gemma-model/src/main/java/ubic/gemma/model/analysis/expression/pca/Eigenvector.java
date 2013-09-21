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
public abstract class Eigenvector implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.analysis.expression.pca.Eigenvector}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.pca.Eigenvector}.
         */
        public static ubic.gemma.model.analysis.expression.pca.Eigenvector newInstance() {
            return new ubic.gemma.model.analysis.expression.pca.EigenvectorImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.pca.Eigenvector}, taking all
         * possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.analysis.expression.pca.Eigenvector newInstance( Integer componentNumber,
                byte[] vector ) {
            final ubic.gemma.model.analysis.expression.pca.Eigenvector entity = new ubic.gemma.model.analysis.expression.pca.EigenvectorImpl();
            entity.setComponentNumber( componentNumber );
            entity.setVector( vector );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5122763307995485698L;
    private Integer componentNumber;

    private byte[] vector;

    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Eigenvector() {
    }

    /**
     * Returns <code>true</code> if the argument is an Eigenvector instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Eigenvector ) ) {
            return false;
        }
        final Eigenvector that = ( Eigenvector ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public Integer getComponentNumber() {
        return this.componentNumber;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Binary representing array of doubles
     */
    public byte[] getVector() {
        return this.vector;
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

    public void setComponentNumber( Integer componentNumber ) {
        this.componentNumber = componentNumber;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setVector( byte[] vector ) {
        this.vector = vector;
    }

}