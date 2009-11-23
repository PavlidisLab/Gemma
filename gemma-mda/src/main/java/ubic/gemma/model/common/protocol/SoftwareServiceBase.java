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
package ubic.gemma.model.common.protocol;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.protocol.SoftwareService</code>, provides access to all
 * services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.protocol.SoftwareService
 */
public abstract class SoftwareServiceBase implements ubic.gemma.model.common.protocol.SoftwareService {

    @Autowired
    private ubic.gemma.model.common.protocol.SoftwareDao softwareDao;

    /**
     * @see ubic.gemma.model.common.protocol.SoftwareService#find(ubic.gemma.model.common.protocol.Software)
     */
    public ubic.gemma.model.common.protocol.Software find( final ubic.gemma.model.common.protocol.Software software ) {
        try {
            return this.handleFind( software );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.SoftwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.SoftwareService.find(ubic.gemma.model.common.protocol.Software software)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.SoftwareService#findOrCreate(ubic.gemma.model.common.protocol.Software)
     */
    public ubic.gemma.model.common.protocol.Software findOrCreate(
            final ubic.gemma.model.common.protocol.Software software ) {
        try {
            return this.handleFindOrCreate( software );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.SoftwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.SoftwareService.findOrCreate(ubic.gemma.model.common.protocol.Software software)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.SoftwareService#remove(ubic.gemma.model.common.protocol.Software)
     */
    public void remove( final ubic.gemma.model.common.protocol.Software software ) {
        try {
            this.handleRemove( software );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.SoftwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.SoftwareService.remove(ubic.gemma.model.common.protocol.Software software)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>software</code>'s DAO.
     */
    public void setSoftwareDao( ubic.gemma.model.common.protocol.SoftwareDao softwareDao ) {
        this.softwareDao = softwareDao;
    }

    /**
     * @see ubic.gemma.model.common.protocol.SoftwareService#update(ubic.gemma.model.common.protocol.Software)
     */
    public void update( final ubic.gemma.model.common.protocol.Software software ) {
        try {
            this.handleUpdate( software );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.SoftwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.SoftwareService.update(ubic.gemma.model.common.protocol.Software software)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>software</code>'s DAO.
     */
    protected ubic.gemma.model.common.protocol.SoftwareDao getSoftwareDao() {
        return this.softwareDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.protocol.Software)}
     */
    protected abstract ubic.gemma.model.common.protocol.Software handleFind(
            ubic.gemma.model.common.protocol.Software software ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.protocol.Software)}
     */
    protected abstract ubic.gemma.model.common.protocol.Software handleFindOrCreate(
            ubic.gemma.model.common.protocol.Software software ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.protocol.Software)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.protocol.Software software )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.protocol.Software)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.protocol.Software software )
            throws java.lang.Exception;

}