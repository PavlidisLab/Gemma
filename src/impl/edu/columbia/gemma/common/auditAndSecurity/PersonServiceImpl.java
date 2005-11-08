/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package edu.columbia.gemma.common.auditAndSecurity;

import java.util.Collection;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.common.auditAndSecurity.PersonService
 */
public class PersonServiceImpl extends edu.columbia.gemma.common.auditAndSecurity.PersonServiceBase {

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#savePerson(edu.columbia.gemma.common.auditAndSecurity.Person)
     */
    protected void handleSavePerson( edu.columbia.gemma.common.auditAndSecurity.Person person )
            throws java.lang.Exception {
        this.getPersonDao().create( person );
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#getAllPersons()
     */
    protected java.util.Collection handleGetAllPersons() throws java.lang.Exception {
        return this.getPersonDao().loadAll();
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#removePerson(edu.columbia.gemma.common.auditAndSecurity.Person)
     */
    protected void handleRemovePerson( edu.columbia.gemma.common.auditAndSecurity.Person person )
            throws java.lang.Exception {
        this.getPersonDao().remove( person );
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.PersonService#findByName(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    protected edu.columbia.gemma.common.auditAndSecurity.Person handleFindByName( java.lang.String firstName,
            java.lang.String lastName, java.lang.String middleName ) throws java.lang.Exception {
        Collection results = this.getPersonDao().findByFullName( firstName, lastName, middleName );
        Person result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException( "More than one instance of '"
                    + edu.columbia.gemma.common.auditAndSecurity.Contact.class.getName()
                    + "' was found when executing query" );

        } else if ( results.size() == 1 ) {
            result = ( edu.columbia.gemma.common.auditAndSecurity.Person ) results.iterator().next();
        }
        return result;
    }

    @Override
    protected Collection handleFindByFullName( String firstName, String middleName, String lastName ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Person handleFindOrCreate( Person person ) throws Exception {
        return this.getPersonDao().findOrCreate(person);
    }
}