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
package edu.columbia.gemma.common.auditAndSecurity;

/**
 * <hr>
 * <p>
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.auditAndSecurity.ContactService
 */
public class ContactServiceImpl extends edu.columbia.gemma.common.auditAndSecurity.ContactServiceBase {

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.ContactService#create(edu.columbia.gemma.common.auditAndSecurity.Contact)
     */
    protected edu.columbia.gemma.common.auditAndSecurity.Contact handleCreate(
            edu.columbia.gemma.common.auditAndSecurity.Contact contact ) throws java.lang.Exception {
        return ( Contact ) this.getContactDao().create( contact );
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.ContactService#find(edu.columbia.gemma.common.auditAndSecurity.Contact)
     */
    protected edu.columbia.gemma.common.auditAndSecurity.Contact handleFind(
            edu.columbia.gemma.common.auditAndSecurity.Contact contact ) throws java.lang.Exception {
        return this.getContactDao().find( contact );
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.ContactService#remove(edu.columbia.gemma.common.auditAndSecurity.Contact)
     */
    protected void handleRemove( edu.columbia.gemma.common.auditAndSecurity.Contact contact )
            throws java.lang.Exception {
        this.getContactDao().remove( contact );
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.ContactService#update(edu.columbia.gemma.common.auditAndSecurity.Contact)
     */
    protected void handleUpdate( edu.columbia.gemma.common.auditAndSecurity.Contact contact )
            throws java.lang.Exception {
        this.getContactDao().update( contact );
    }

    @Override
    protected Contact handleFindOrCreate( Contact contact ) throws Exception {
        return this.getContactDao().findOrCreate( contact );
    }

}