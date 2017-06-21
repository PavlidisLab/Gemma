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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.ExternalDatabase</code>.
 * </p>
 *
 * @see ubic.gemma.model.common.description.ExternalDatabase
 */
public abstract class ExternalDatabaseDaoBase extends AbstractDao<ExternalDatabase> implements ExternalDatabaseDao {

    public ExternalDatabaseDaoBase( SessionFactory sessionFactory ) {
        super( ExternalDatabase.class, sessionFactory );
    }

    @Override
    public Collection<ExternalDatabase> findByLocalDbInstallName( final String localInstallDBName ) {
        return this.findByProperty( "localInstallDbName", localInstallDBName );
    }

    @Override
    public ExternalDatabase findByName( final String name ) {
        return this.findOneByProperty( "name", name );
    }

}