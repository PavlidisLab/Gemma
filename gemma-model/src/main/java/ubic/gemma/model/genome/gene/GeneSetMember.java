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
public abstract class GeneSetMember implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.gene.GeneSetMember}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.gene.GeneSetMember}.
         */
        public static ubic.gemma.model.genome.gene.GeneSetMember newInstance() {
            return new ubic.gemma.model.genome.gene.GeneSetMemberImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.gene.GeneSetMember}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.genome.gene.GeneSetMember newInstance( Double score,
                ubic.gemma.model.genome.Gene gene ) {
            final ubic.gemma.model.genome.gene.GeneSetMember entity = new ubic.gemma.model.genome.gene.GeneSetMemberImpl();
            entity.setScore( score );
            entity.setGene( gene );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -926690781193097196L;
    private Double score;

    private Long id;

    private ubic.gemma.model.genome.Gene gene;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public GeneSetMember() {
    }

    /**
     * Returns <code>true</code> if the argument is an GeneSetMember instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneSetMember ) ) {
            return false;
        }
        final GeneSetMember that = ( GeneSetMember ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene getGene() {
        return this.gene;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * <p>
     * A generic value that can be used to provide additional information about group membership, to provide rankings of
     * group members for example.
     * </p>
     */
    public Double getScore() {
        return this.score;
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

    public void setGene( ubic.gemma.model.genome.Gene gene ) {
        this.gene = gene;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

}