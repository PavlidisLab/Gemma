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

    @Override
    public Person load( Long id ) {
        return this.getPersonDao().load( id );
    }

    @Override
    public void update( Person p ) {
        this.getPersonDao().update( p );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleCreate(ubic.gemma.model.common.auditAndSecurity
     * .Person)
     */
    @Override
    protected Person handleCreate( Person person ) {
        return this.getPersonDao().create( person );
    }

    @Override
    protected Collection<Person> handleFindByFullName( String name, String lastName ) {
        return this.getPersonDao().findByFullName( name, lastName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleExpfindByName(java.lang.String,
     * java.lang.String, java.lang.String)
     */

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#findOrCreate(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected Person handleFindOrCreate( Person person ) {
        return this.getPersonDao().findOrCreate( person );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleLoadAll()
     */
    @Override
    protected Collection<Person> handleLoadAll() {
        return ( Collection<Person> ) this.getPersonDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.PersonServiceBase#handleRemove(ubic.gemma.model.common.auditAndSecurity
     * .Person)
     */
    @Override
    protected void handleRemove( Person person ) {
        this.getPersonDao().remove( person );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#removePerson(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    protected void handleRemovePerson( ubic.gemma.model.common.auditAndSecurity.Person person ) {
        this.getPersonDao().remove( person );
    }

}