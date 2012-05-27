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
package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.auditAndSecurity.ContactService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.ContactService
 */
public abstract class ContactServiceBase implements ubic.gemma.model.common.auditAndSecurity.ContactService {

    @Autowired
    private ubic.gemma.model.common.auditAndSecurity.ContactDao contactDao;

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#find(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    public ubic.gemma.model.common.auditAndSecurity.Contact find(
            final ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        try {
            return this.handleFind( contact );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.ContactServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.ContactService.find(ubic.gemma.model.common.auditAndSecurity.Contact contact)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#findOrCreate(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    public ubic.gemma.model.common.auditAndSecurity.Contact findOrCreate(
            final ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        try {
            return this.handleFindOrCreate( contact );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.ContactServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.ContactService.findOrCreate(ubic.gemma.model.common.auditAndSecurity.Contact contact)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#remove(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    public void remove( final ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        try {
            this.handleRemove( contact );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.ContactServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.ContactService.remove(ubic.gemma.model.common.auditAndSecurity.Contact contact)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>contact</code>'s DAO.
     */
    public void setContactDao( ubic.gemma.model.common.auditAndSecurity.ContactDao contactDao ) {
        this.contactDao = contactDao;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#update(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    public void update( final ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        try {
            this.handleUpdate( contact );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.ContactServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.ContactService.update(ubic.gemma.model.common.auditAndSecurity.Contact contact)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>contact</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.ContactDao getContactDao() {
        return this.contactDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.auditAndSecurity.Contact)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.Contact handleFind(
            ubic.gemma.model.common.auditAndSecurity.Contact contact ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.auditAndSecurity.Contact)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.Contact handleFindOrCreate(
            ubic.gemma.model.common.auditAndSecurity.Contact contact ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.auditAndSecurity.Contact)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.auditAndSecurity.Contact contact )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.auditAndSecurity.Contact)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.auditAndSecurity.Contact contact )
            throws java.lang.Exception;

}