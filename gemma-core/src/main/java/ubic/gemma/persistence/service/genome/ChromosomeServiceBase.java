/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.genome;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Taxon;

/**
 * <p>
 * Spring Service base class for <code>ChromosomeService</code>, provides access to all services
 * and entities referenced by this service.
 * </p>
 * 
 * @see ChromosomeService
 */
public abstract class ChromosomeServiceBase implements ChromosomeService {

    @Autowired
    private ChromosomeDao chromosomeDao;

    /**
     * @see ChromosomeService#findOrCreate(ubic.gemma.model.genome.Chromosome)
     */
    @Override
    @Transactional
    public ubic.gemma.model.genome.Chromosome findOrCreate( final String name, final Taxon taxon ) {

        return this.handleFindOrCreate( name, taxon );

    }

    /**
     * Sets the reference to <code>chromosome</code>'s DAO.
     */
    public void setChromosomeDao( ChromosomeDao chromosomeDao ) {
        this.chromosomeDao = chromosomeDao;
    }

    /**
     * Gets the reference to <code>chromosome</code>'s DAO.
     */
    protected ChromosomeDao getChromosomeDao() {
        return this.chromosomeDao;
    }

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.genome.Chromosome)}
     */
    protected abstract ubic.gemma.model.genome.Chromosome handleFindOrCreate( String name, Taxon taxon );

}