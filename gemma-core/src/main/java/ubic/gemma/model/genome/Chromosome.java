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

import java.util.Objects;

/**
 * Immutable representation of a chromosome
 */
public class Chromosome extends AbstractIdentifiable {

    private String name;
    private ExternalDatabase assemblyDatabase;
    private BioSequence sequence;
    private Taxon taxon;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Chromosome() {

    }

    public Chromosome( String name, Taxon taxon ) {
        this.name = name;
        this.taxon = taxon;
    }

    public Chromosome( String name, ExternalDatabase assemblyDatabase, BioSequence sequence, Taxon taxon ) {
        this.name = name;
        this.assemblyDatabase = assemblyDatabase;
        this.sequence = sequence;
        this.taxon = taxon;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @return The database where we have the assesmbly of the chromosome, such as the GoldenPath.
     */
    public ExternalDatabase getAssemblyDatabase() {
        return this.assemblyDatabase;
    }

    /**
     * @return The sequence of the chromosome. This is typically going to be just a reference to the sequence in an external
     * database.
     */
    public BioSequence getSequence() {
        return this.sequence;
    }

    public Taxon getTaxon() {
        return this.taxon;
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
}