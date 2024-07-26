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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.List;

/**
 * @author pavlidis
 * @see ExternalDatabase
 */
@Repository
public class ExternalDatabaseDaoImpl extends AbstractDao<ExternalDatabase> implements ExternalDatabaseDao {

    @Autowired
    public ExternalDatabaseDaoImpl( SessionFactory sessionFactory ) {
        super( ExternalDatabase.class, sessionFactory );
    }

    @Override
    protected ExternalDatabase findByBusinessKey( ExternalDatabase externalDatabase ) {
        return this.findOneByProperty( "name", externalDatabase.getName() );
    }

    @Override
    public ExternalDatabase findByName( final String name ) {
        return this.findOneByProperty( "name", name );
    }

    @Override
    public ExternalDatabase findByNameWithAuditTrail( String name ) {
        return ( ExternalDatabase ) getSessionFactory().getCurrentSession()
                .createQuery( "select ed from ExternalDatabase ed join fetch ed.auditTrail where ed.name = :name" )
                .setParameter( "name", name )
                .uniqueResult();
    }

    @Override
    public List<ExternalDatabase> findAllByNameIn( Collection<String> names ) {
        return findByPropertyIn( "name", names );
    }
}