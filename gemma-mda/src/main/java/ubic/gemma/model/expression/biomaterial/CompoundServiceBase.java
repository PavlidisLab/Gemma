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
package ubic.gemma.model.expression.biomaterial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Spring Service base class for <code>CompoundService</code>, provides access to all services and entities referenced
 * by this service.
 * </p>
 * 
 * @see CompoundService
 */
public abstract class CompoundServiceBase implements CompoundService {

    @Autowired
    private CompoundDao compoundDao;

    /**
     * @see CompoundService#find(Compound)
     */
    @Override
    @Transactional(readOnly = true)
    public Compound find( final Compound compound ) {
        return this.handleFind( compound );

    }

    /**
     * @see CompoundService#findOrCreate(Compound)
     */
    @Override
    @Transactional
    public Compound findOrCreate( final Compound compound ) {
        return this.handleFindOrCreate( compound );

    }

    /**
     * @see CompoundService#remove(Compound)
     */
    @Override
    @Transactional
    public void remove( final Compound compound ) {
        this.handleRemove( compound );

    }

    /**
     * @see CompoundService#update(Compound)
     */
    @Override
    @Transactional
    public void update( final Compound compound ) {
        this.handleUpdate( compound );

    }

    /**
     * Gets the reference to <code>compound</code>'s DAO.
     */
    CompoundDao getCompoundDao() {
        return this.compoundDao;
    }

    /**
     * Performs the core logic for {@link #find(Compound)}
     */
    protected abstract Compound handleFind( Compound compound );

    /**
     * Performs the core logic for {@link #findOrCreate(Compound)}
     */
    protected abstract Compound handleFindOrCreate( Compound compound );

    /**
     * Performs the core logic for {@link #remove(Compound)}
     */
    protected abstract void handleRemove( Compound compound );

    /**
     * Performs the core logic for {@link #update(Compound)}
     */
    protected abstract void handleUpdate( Compound compound );

}