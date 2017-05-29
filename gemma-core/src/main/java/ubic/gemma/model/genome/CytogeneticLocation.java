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

import org.apache.commons.lang3.builder.CompareToBuilder;

public class CytogeneticLocation extends ChromosomeLocation {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4379807181466604101L;
    private String band;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public CytogeneticLocation() {
    }

    @Override
    public int compareTo( Object object ) {
        CytogeneticLocation other = ( CytogeneticLocation ) object;
        return new CompareToBuilder().append( this.getChromosome().getName(), other.getChromosome().getName() )
                .append( this.getBand(), other.getBand() ).toComparison();
    }

    public String getBand() {
        return this.band;
    }

    public void setBand( String band ) {
        this.band = band;
    }

    /**
     * Constructs new instances of {@link CytogeneticLocation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link CytogeneticLocation}.
         */
        public static CytogeneticLocation newInstance() {
            return new CytogeneticLocation();
        }

        /**
         * Constructs a new instance of {@link CytogeneticLocation}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static CytogeneticLocation newInstance( Chromosome chromosome, String band ) {
            final CytogeneticLocation entity = new CytogeneticLocation();
            entity.setChromosome( chromosome );
            entity.setBand( band );
            return entity;
        }
    }

}