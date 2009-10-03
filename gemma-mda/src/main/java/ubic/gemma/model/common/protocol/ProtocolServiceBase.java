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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.protocol.ProtocolService</code>, provides access to all
 * services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.protocol.ProtocolService
 */
public abstract class ProtocolServiceBase implements ubic.gemma.model.common.protocol.ProtocolService {

    private ubic.gemma.model.common.protocol.ProtocolDao protocolDao;

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#find(ubic.gemma.model.common.protocol.Protocol)
     */
    public ubic.gemma.model.common.protocol.Protocol find( final ubic.gemma.model.common.protocol.Protocol protocol ) {
        try {
            return this.handleFind( protocol );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.find(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#findOrCreate(ubic.gemma.model.common.protocol.Protocol)
     */
    public ubic.gemma.model.common.protocol.Protocol findOrCreate(
            final ubic.gemma.model.common.protocol.Protocol protocol ) {
        try {
            return this.handleFindOrCreate( protocol );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.findOrCreate(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#remove(ubic.gemma.model.common.protocol.Protocol)
     */
    public void remove( final ubic.gemma.model.common.protocol.Protocol protocol ) {
        try {
            this.handleRemove( protocol );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.remove(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>protocol</code>'s DAO.
     */
    public void setProtocolDao( ubic.gemma.model.common.protocol.ProtocolDao protocolDao ) {
        this.protocolDao = protocolDao;
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#update(ubic.gemma.model.common.protocol.Protocol)
     */
    public void update( final ubic.gemma.model.common.protocol.Protocol protocol ) {
        try {
            this.handleUpdate( protocol );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.update(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>protocol</code>'s DAO.
     */
    protected ubic.gemma.model.common.protocol.ProtocolDao getProtocolDao() {
        return this.protocolDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.protocol.Protocol)}
     */
    protected abstract ubic.gemma.model.common.protocol.Protocol handleFind(
            ubic.gemma.model.common.protocol.Protocol protocol ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.protocol.Protocol)}
     */
    protected abstract ubic.gemma.model.common.protocol.Protocol handleFindOrCreate(
            ubic.gemma.model.common.protocol.Protocol protocol ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.protocol.Protocol)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.protocol.Protocol protocol )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.protocol.Protocol)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.protocol.Protocol protocol )
            throws java.lang.Exception;

}