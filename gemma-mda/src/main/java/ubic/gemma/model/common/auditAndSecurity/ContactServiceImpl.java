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
package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.stereotype.Service;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.ContactService
 */
@Service
public class ContactServiceImpl extends ubic.gemma.model.common.auditAndSecurity.ContactServiceBase {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#createFromValueObject(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    protected ubic.gemma.model.common.auditAndSecurity.Contact handleCreate(
            ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        return this.getContactDao().create( contact );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#find(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    protected ubic.gemma.model.common.auditAndSecurity.Contact handleFind(
            ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        return this.getContactDao().find( contact );
    }

    @Override
    protected Contact handleFindOrCreate( Contact contact ) {
        return this.getContactDao().findOrCreate( contact );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#remove(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        this.getContactDao().remove( contact );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactService#update(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        this.getContactDao().update( contact );
    }

}