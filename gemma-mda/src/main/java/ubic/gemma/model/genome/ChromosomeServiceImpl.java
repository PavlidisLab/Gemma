/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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

import java.util.Collection;

import org.springframework.stereotype.Service;

/**
 * @see ubic.gemma.model.genome.ChromosomeService
 * @version $Id$
 */
@Service
public class ChromosomeServiceImpl extends ubic.gemma.model.genome.ChromosomeServiceBase {

    @Override
    public Collection<Chromosome> find( String name, Taxon taxon ) {
        return this.getChromosomeDao().find( name, taxon );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeService#findOrCreate(ubic.gemma.model.genome.Chromosome)
     */
    @Override
    protected ubic.gemma.model.genome.Chromosome handleFindOrCreate( String name, Taxon taxon ) {
        return this.getChromosomeDao().findOrCreate( name, taxon );

    }

}