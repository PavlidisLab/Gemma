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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.biomaterial.CompoundService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.biomaterial.CompoundService
 */
public abstract class CompoundServiceBase implements ubic.gemma.model.expression.biomaterial.CompoundService {

    @Autowired
    private ubic.gemma.model.expression.biomaterial.CompoundDao compoundDao;

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#find(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public ubic.gemma.model.expression.biomaterial.Compound find(
            final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        try {
            return this.handleFind( compound );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.CompoundServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.CompoundService.find(ubic.gemma.model.expression.biomaterial.Compound compound)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#findOrCreate(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public ubic.gemma.model.expression.biomaterial.Compound findOrCreate(
            final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        try {
            return this.handleFindOrCreate( compound );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.CompoundServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.CompoundService.findOrCreate(ubic.gemma.model.expression.biomaterial.Compound compound)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#remove(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public void remove( final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        try {
            this.handleRemove( compound );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.CompoundServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.CompoundService.remove(ubic.gemma.model.expression.biomaterial.Compound compound)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>compound</code>'s DAO.
     */
    public void setCompoundDao( ubic.gemma.model.expression.biomaterial.CompoundDao compoundDao ) {
        this.compoundDao = compoundDao;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#update(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public void update( final ubic.gemma.model.expression.biomaterial.Compound compound ) {
        try {
            this.handleUpdate( compound );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.CompoundServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.CompoundService.update(ubic.gemma.model.expression.biomaterial.Compound compound)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>compound</code>'s DAO.
     */
    protected ubic.gemma.model.expression.biomaterial.CompoundDao getCompoundDao() {
        return this.compoundDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.biomaterial.Compound)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.Compound handleFind(
            ubic.gemma.model.expression.biomaterial.Compound compound ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.biomaterial.Compound)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.Compound handleFindOrCreate(
            ubic.gemma.model.expression.biomaterial.Compound compound ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.biomaterial.Compound)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.biomaterial.Compound compound )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.biomaterial.Compound)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.biomaterial.Compound compound )
            throws java.lang.Exception;

}