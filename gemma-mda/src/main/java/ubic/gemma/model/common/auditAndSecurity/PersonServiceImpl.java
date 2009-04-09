/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.PersonService
 */
public class PersonServiceImpl extends ubic.gemma.model.common.auditAndSecurity.PersonServiceBase {

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleCreate(ubic.gemma.model.common.auditAndSecurity
     * .Person)
     */
    @Override
    protected Person handleCreate( Person person ) throws Exception {
        return this.getPersonDao().create( person );
    }

    @Override
    protected Collection handleFindByFullName( String name, String lastName ) throws Exception {
        return this.getPersonDao().findByFullName( name, lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#findByName(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    protected ubic.gemma.model.common.auditAndSecurity.Person handleFindByName( java.lang.String firstName,
            java.lang.String lastName, java.lang.String middleName ) throws java.lang.Exception {
        Collection results = this.getPersonDao().findByFullName( firstName, lastName, middleName );
        Person result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException( "More than one instance of '"
                    + ubic.gemma.model.common.auditAndSecurity.Contact.class.getName()
                    + "' was found when executing query" );

        } else if ( results.size() == 1 ) {
            result = ( ubic.gemma.model.common.auditAndSecurity.Person ) results.iterator().next();
        }
        return result;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#findOrCreate(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected Person handleFindOrCreate( Person person ) throws Exception {
        return this.getPersonDao().findOrCreate( person );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#getAllPersons()
     */
    protected java.util.Collection handleGetAllPersons() throws java.lang.Exception {
        return this.getPersonDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleLoadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Person> handleLoadAll() throws Exception {
        return this.getPersonDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleExpfindByName(java.lang.String,
     * java.lang.String, java.lang.String)
     */

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleRemove(ubic.gemma.model.common.auditAndSecurity
     * .Person)
     */
    @Override
    protected void handleRemove( Person person ) throws Exception {
        this.getPersonDao().remove( person );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#removePerson(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    protected void handleRemovePerson( ubic.gemma.model.common.auditAndSecurity.Person person )
            throws java.lang.Exception {
        this.getPersonDao().remove( person );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#savePerson(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    protected Person handleSavePerson( ubic.gemma.model.common.auditAndSecurity.Person person )
            throws java.lang.Exception {
        return this.getPersonDao().create( person );
    }

}