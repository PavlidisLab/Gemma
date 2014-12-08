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

import org.apache.commons.lang3.reflect.FieldUtils;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Immutable representation of a chromosome
 */
public abstract class Chromosome implements java.io.Serializable {

    /**
     * Constructs new instances of {@link Chromosome}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link Chromosome}.
         */
        public static Chromosome newInstance() {
            return new ChromosomeImpl();
        }

        /**
         * Constructs a new instance of {@link Chromosome}, taking all possible properties (except the identifier(s))as
         * arguments.
         */
        public static Chromosome newInstance( String name, ExternalDatabase assemblyDatabase, BioSequence sequence,
                ubic.gemma.model.genome.Taxon taxon ) {
            final Chromosome entity = new ChromosomeImpl();
            try {
                if ( name != null ) FieldUtils.writeField( entity, "name", name, true );
                if ( assemblyDatabase != null )
                    FieldUtils.writeField( entity, "assemblyDatabase", assemblyDatabase, true );
                if ( sequence != null ) FieldUtils.writeField( entity, "sequence", sequence, true );
                if ( taxon != null ) FieldUtils.writeField( entity, "taxon", taxon, true );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
            return entity;
        }

        /**
         * Constructs a new instance of {@link Chromosome}, taking all required and/or read-only properties as
         * arguments.
         */
        public static Chromosome newInstance( String name, ubic.gemma.model.genome.Taxon taxon ) {
            final Chromosome entity = new ChromosomeImpl();
            try {
                if ( name != null ) FieldUtils.writeField( entity, "name", name, true );
                if ( taxon != null ) FieldUtils.writeField( entity, "taxon", taxon, true );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8353766718193697363L;
    final private String name = null;

    final private Long id = null;

    final private ExternalDatabase assemblyDatabase = null;

    final private BioSequence sequence = null;

    final private Taxon taxon = null;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Chromosome() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Chromosome ) ) {
            return false;
        }
        final Chromosome that = ( Chromosome ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {
            return this.getTaxon().equals( that.getTaxon() ) && this.getName().equals( that.getName() );
        }

        return true;
    }

    /**
     * The database where we have the assesmbly of the chromosome, such as the GoldenPath.
     */
    public ExternalDatabase getAssemblyDatabase() {
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
     * The sequence of the chromosome. This is typically going to be just a reference to the sequence in an external
     * database.
     */
    public BioSequence getSequence() {
        return this.sequence;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        assert this.getName() != null;

        hashCode = 29
                * hashCode
                + ( this.getId() == null ? this.getName().hashCode()
                        + ( this.getTaxon() != null ? this.getTaxon().hashCode() : 0 ) : this.getId().hashCode() );

        return hashCode;
    }

    @Override
    public String toString() {
        return this.getTaxon().getScientificName() + " Chromosome " + this.getName();
    }

}