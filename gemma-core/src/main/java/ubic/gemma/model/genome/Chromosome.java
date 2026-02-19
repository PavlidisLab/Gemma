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

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.biosequence.BioSequence;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Immutable representation of a chromosome
 */
public class Chromosome extends AbstractIdentifiable {

    private String name;
    @Nullable
    private ExternalDatabase assemblyDatabase;
    private BioSequence sequence;
    private Taxon taxon;

    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return The database where we have the assesmbly of the chromosome, such as the GoldenPath.
     */
    @Nullable
    public ExternalDatabase getAssemblyDatabase() {
        return this.assemblyDatabase;
    }

    public void setAssemblyDatabase( @Nullable ExternalDatabase assemblyDatabase ) {
        this.assemblyDatabase = assemblyDatabase;
    }

    /**
     * @return The sequence of the chromosome. This is typically going to be just a reference to the sequence in an external
     * database.
     */
    public BioSequence getSequence() {
        return this.sequence;
    }

    public void setSequence( BioSequence sequence ) {
        this.sequence = sequence;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getName(), getTaxon() );
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
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return Objects.equals( getName(), that.getName() )
                && Objects.equals( getTaxon(), that.getTaxon() );
    }

    @Override
    public String toString() {
        return this.getTaxon().getScientificName() + " Chromosome " + this.getName();
    }

    public static class Factory {

        public static Chromosome newInstance() {
            return new Chromosome();
        }

        public static Chromosome newInstance( String name, Taxon taxon ) {
            Chromosome chromosome = newInstance();
            chromosome.setName( name );
            chromosome.setTaxon( taxon );
            return chromosome;
        }

        public static Chromosome newInstance( String name, @Nullable ExternalDatabase assemblyDatabase, BioSequence sequence, Taxon taxon ) {
            Chromosome chromosome = newInstance( name, taxon );
            chromosome.setAssemblyDatabase( assemblyDatabase );
            chromosome.setSequence( sequence );
            return chromosome;
        }
    }
}