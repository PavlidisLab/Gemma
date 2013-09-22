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
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Service base class for <code>ContactService</code>, provides access to all services and entities referenced by
 * this service.
 * 
 * @see ContactService
 */
public abstract class ContactServiceBase implements ContactService {

    @Autowired
    private ContactDao contactDao;

    /**
     * @see ContactService#find(Contact)
     */
    @Override
    @Transactional(readOnly = true)
    public Contact find( final Contact contact ) {
        return this.handleFind( contact );
    }

    /**
     * @see ContactService#findOrCreate(Contact)
     */
    @Override
    @Transactional
    public Contact findOrCreate( final Contact contact ) {
        return this.handleFindOrCreate( contact );

    }

    /**
     * @see ContactService#remove(Contact)
     */
    @Override
    @Transactional
    public void remove( final Contact contact ) {
        this.handleRemove( contact );

    }

    /**
     * @see ContactService#update(Contact)
     */
    @Override
    @Transactional
    public void update( final Contact contact ) {
        this.handleUpdate( contact );

    }

    /**
     * Gets the reference to <code>contact</code>'s DAO.
     */
    ContactDao getContactDao() {
        return this.contactDao;
    }

    /**
     * Performs the core logic for {@link #find(Contact)}
     */
    protected abstract Contact handleFind( Contact contact );

    /**
     * Performs the core logic for {@link #findOrCreate(Contact)}
     */
    protected abstract Contact handleFindOrCreate( Contact contact );

    /**
     * Performs the core logic for {@link #remove(Contact)}
     */
    protected abstract void handleRemove( Contact contact );

    /**
     * Performs the core logic for {@link #update(Contact)}
     */
    protected abstract void handleUpdate( Contact contact );

}