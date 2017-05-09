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
public abstract class ChromosomeLocation implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4932607147290671454L;

    private Long id;
    private ubic.gemma.model.genome.Chromosome chromosome;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ChromosomeLocation() {
    }

    public abstract int compareTo( Object o );

    /**
     * Returns <code>true</code> if the argument is an ChromosomeLocation instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof ChromosomeLocation ) ) {
            return false;
        }
        final ChromosomeLocation that = ( ChromosomeLocation ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Chromosome getChromosome() {
        return this.chromosome;
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

    public void setChromosome( ubic.gemma.model.genome.Chromosome chromosome ) {
        this.chromosome = chromosome;
    }

    public void setId( Long id ) {
        this.id = id;
    }

}