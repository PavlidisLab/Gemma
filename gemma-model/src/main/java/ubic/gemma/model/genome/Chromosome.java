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
package ubic.gemma.model.genome;

/**
 * 
 */
public abstract class Chromosome implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.Chromosome}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.Chromosome}.
         */
        public static ubic.gemma.model.genome.Chromosome newInstance() {
            return new ubic.gemma.model.genome.ChromosomeImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.Chromosome}, taking all possible properties
         * (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.genome.Chromosome newInstance( String name,
                ubic.gemma.model.common.description.ExternalDatabase assemblyDatabase,
                ubic.gemma.model.genome.biosequence.BioSequence sequence, ubic.gemma.model.genome.Taxon taxon ) {
            final ubic.gemma.model.genome.Chromosome entity = new ubic.gemma.model.genome.ChromosomeImpl();
            entity.setName( name );
            entity.setAssemblyDatabase( assemblyDatabase );
            entity.setSequence( sequence );
            entity.setTaxon( taxon );
            return entity;
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.Chromosome}, taking all required and/or read-only
         * properties as arguments.
         */
        public static ubic.gemma.model.genome.Chromosome newInstance( String name, ubic.gemma.model.genome.Taxon taxon ) {
            final ubic.gemma.model.genome.Chromosome entity = new ubic.gemma.model.genome.ChromosomeImpl();
            entity.setName( name );
            entity.setTaxon( taxon );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8353766718193697363L;
    private String name;

    private Long id;

    private ubic.gemma.model.common.description.ExternalDatabase assemblyDatabase;

    private ubic.gemma.model.genome.biosequence.BioSequence sequence;

    private ubic.gemma.model.genome.Taxon taxon;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Chromosome() {
    }

    /**
     * Returns <code>true</code> if the argument is an Chromosome instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Chromosome ) ) {
            return false;
        }
        final Chromosome that = ( Chromosome ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * <p>
     * The database where we have the assesmbly of the chromosome, such as the GoldenPath.
     * </p>
     * </p>
     */
    public ubic.gemma.model.common.description.ExternalDatabase getAssemblyDatabase() {
        return this.assemblyDatabase;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * <p>
     * The sequence of the chromosome.
     * </p>
     * </p>
     */
    public ubic.gemma.model.genome.biosequence.BioSequence getSequence() {
        return this.sequence;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
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

    public void setAssemblyDatabase( ubic.gemma.model.common.description.ExternalDatabase assemblyDatabase ) {
        this.assemblyDatabase = assemblyDatabase;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setSequence( ubic.gemma.model.genome.biosequence.BioSequence sequence ) {
        this.sequence = sequence;
    }

    public void setTaxon( ubic.gemma.model.genome.Taxon taxon ) {
        this.taxon = taxon;
    }

}