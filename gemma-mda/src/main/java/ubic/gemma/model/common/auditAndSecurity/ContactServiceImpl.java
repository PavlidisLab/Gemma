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
 * @see ContactService
 */
@Service
public class ContactServiceImpl extends ContactServiceBase {

    /**
     * @see ContactService#createFromValueObject(Contact)
     */
    protected Contact handleCreate( Contact contact ) {
        return this.getContactDao().create( contact );
    }

    /**
     * @see ContactService#find(Contact)
     */
    @Override
    protected Contact handleFind( Contact contact ) {
        return this.getContactDao().find( contact );
    }

    @Override
    protected Contact handleFindOrCreate( Contact contact ) {
        return this.getContactDao().findOrCreate( contact );
    }

    /**
     * @see ContactService#remove(Contact)
     */
    @Override
    protected void handleRemove( Contact contact ) {
        this.getContactDao().remove( contact );
    }

    /**
     * @see ContactService#update(Contact)
     */
    @Override
    protected void handleUpdate( Contact contact ) {
        this.getContactDao().update( contact );
    }

}