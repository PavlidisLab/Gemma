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
package ubic.gemma.model.genome.gene;

/**
 * 
 */
public abstract class GeneAlias implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.gene.GeneAlias}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.gene.GeneAlias}.
         */
        public static ubic.gemma.model.genome.gene.GeneAlias newInstance() {
            return new ubic.gemma.model.genome.gene.GeneAliasImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.gene.GeneAlias}, taking all possible properties
         * (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.genome.gene.GeneAlias newInstance( String alias ) {
            final ubic.gemma.model.genome.gene.GeneAlias entity = new ubic.gemma.model.genome.gene.GeneAliasImpl();
            entity.setAlias( alias );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4156628260205167700L;
    private String alias;

    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public GeneAlias() {
    }

    /**
     * Returns <code>true</code> if the argument is an GeneAlias instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneAlias ) ) {
            return false;
        }
        final GeneAlias that = ( GeneAlias ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
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

    public void setAlias( String alias ) {
        this.alias = alias;
    }

    public void setId( Long id ) {
        this.id = id;
    }

}