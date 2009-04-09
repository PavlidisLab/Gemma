/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.genome;

/**
 * @see ubic.gemma.model.genome.ChromosomeService
 */
public class ChromosomeServiceImpl extends ubic.gemma.model.genome.ChromosomeServiceBase {

    public Chromosome find( String name, Taxon taxon ) {
        return this.getChromosomeDao().find( name, taxon );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeService#find(ubic.gemma.model.genome.Chromosome)
     */
    @Override
    protected ubic.gemma.model.genome.Chromosome handleFind( ubic.gemma.model.genome.Chromosome chromosome )
            throws java.lang.Exception {
        return this.getChromosomeDao().find( chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeService#findOrCreate(ubic.gemma.model.genome.Chromosome)
     */
    @Override
    protected ubic.gemma.model.genome.Chromosome handleFindOrCreate( ubic.gemma.model.genome.Chromosome chromosome )
            throws java.lang.Exception {
        return this.getChromosomeDao().findOrCreate( chromosome );

    }

}