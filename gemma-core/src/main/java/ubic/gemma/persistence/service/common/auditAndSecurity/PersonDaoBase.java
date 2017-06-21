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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.SessionFactory;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.Person</code>.
 * </p>
 *
 * @see ubic.gemma.model.common.auditAndSecurity.Person
 */
public abstract class PersonDaoBase extends AbstractDao<Person> implements PersonDao {

    public PersonDaoBase( SessionFactory sessionFactory ) {
        super( Person.class, sessionFactory );
    }

    /**
     * @see PersonDao#findByFullName(String, String)
     */
    @Override
    public Collection<Person> findByFullName( String firstName, String lastName ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from Person p where p.name = :firstName and p.lastName=:lastName " )
                .setParameter( "firstName", firstName ).setParameter( "lastName", lastName ).list();
    }

    /**
     * @see PersonDao#findByLastName(String)
     */
    @Override
    public Collection<Person> findByLastName( final String lastName ) {
        return this.findByProperty( "lastName", lastName );
    }

}