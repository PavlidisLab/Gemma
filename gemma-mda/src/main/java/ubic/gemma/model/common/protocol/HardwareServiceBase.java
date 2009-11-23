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
 * Spring Service base class for <code>ubic.gemma.model.common.protocol.HardwareService</code>, provides access to all
 * services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.protocol.HardwareService
 */
public abstract class HardwareServiceBase implements ubic.gemma.model.common.protocol.HardwareService {

    @Autowired
    private ubic.gemma.model.common.protocol.HardwareDao hardwareDao;

    /**
     * @see ubic.gemma.model.common.protocol.HardwareService#find(ubic.gemma.model.common.protocol.Hardware)
     */
    public ubic.gemma.model.common.protocol.Hardware find( final ubic.gemma.model.common.protocol.Hardware hardware ) {
        try {
            return this.handleFind( hardware );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.HardwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.HardwareService.find(ubic.gemma.model.common.protocol.Hardware hardware)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.HardwareService#findOrCreate(ubic.gemma.model.common.protocol.Hardware)
     */
    public ubic.gemma.model.common.protocol.Hardware findOrCreate(
            final ubic.gemma.model.common.protocol.Hardware hardware ) {
        try {
            return this.handleFindOrCreate( hardware );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.HardwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.HardwareService.findOrCreate(ubic.gemma.model.common.protocol.Hardware hardware)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.HardwareService#remove(ubic.gemma.model.common.protocol.Hardware)
     */
    public void remove( final ubic.gemma.model.common.protocol.Hardware hardware ) {
        try {
            this.handleRemove( hardware );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.HardwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.HardwareService.remove(ubic.gemma.model.common.protocol.Hardware hardware)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>hardware</code>'s DAO.
     */
    public void setHardwareDao( ubic.gemma.model.common.protocol.HardwareDao hardwareDao ) {
        this.hardwareDao = hardwareDao;
    }

    /**
     * @see ubic.gemma.model.common.protocol.HardwareService#update(ubic.gemma.model.common.protocol.Hardware)
     */
    public void update( final ubic.gemma.model.common.protocol.Hardware hardware ) {
        try {
            this.handleUpdate( hardware );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.HardwareServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.HardwareService.update(ubic.gemma.model.common.protocol.Hardware hardware)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>hardware</code>'s DAO.
     */
    protected ubic.gemma.model.common.protocol.HardwareDao getHardwareDao() {
        return this.hardwareDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.protocol.Hardware)}
     */
    protected abstract ubic.gemma.model.common.protocol.Hardware handleFind(
            ubic.gemma.model.common.protocol.Hardware hardware ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.protocol.Hardware)}
     */
    protected abstract ubic.gemma.model.common.protocol.Hardware handleFindOrCreate(
            ubic.gemma.model.common.protocol.Hardware hardware ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.protocol.Hardware)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.protocol.Hardware hardware )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.protocol.Hardware)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.protocol.Hardware hardware )
            throws java.lang.Exception;

}