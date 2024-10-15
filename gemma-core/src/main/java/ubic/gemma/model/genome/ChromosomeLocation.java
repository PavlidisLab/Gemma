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

import ubic.gemma.model.common.Identifiable;

import java.io.Serializable;

public abstract class ChromosomeLocation implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4932607147290671454L;

    private Long id;
    private Chromosome chromosome;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    @SuppressWarnings("WeakerAccess") // Required by Spring
    public ChromosomeLocation() {
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
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
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    public Chromosome getChromosome() {
        return this.chromosome;
    }

    public void setChromosome( Chromosome chromosome ) {
        this.chromosome = chromosome;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

}