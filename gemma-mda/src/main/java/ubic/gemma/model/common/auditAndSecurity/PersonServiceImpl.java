/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

import org.springframework.stereotype.Service;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.PersonService
 */
@Service
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

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleLoadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Person> handleLoadAll() throws Exception {
        return ( Collection<Person> ) this.getPersonDao().loadAll();
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

    public Person load( Long id ) {
        return this.getPersonDao().load( id );
    }

    public void update( Person p ) {
        this.getPersonDao().update( p );

    }

}