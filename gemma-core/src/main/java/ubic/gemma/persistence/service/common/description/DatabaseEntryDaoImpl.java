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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.AbstractDao;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>DatabaseEntry</code>.
 *
 * @see DatabaseEntry
 */
@Repository
public class DatabaseEntryDaoImpl extends AbstractDao<DatabaseEntry> implements DatabaseEntryDao {

    /* ********************************
     * Constructors
     * ********************************/

    public DatabaseEntryDaoImpl() {
        super( DatabaseEntry.class );
    }

    @Autowired
    public DatabaseEntryDaoImpl( SessionFactory sessionFactory ) {
        this();
        super.setSessionFactory( sessionFactory );
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public Integer countAll() {
        try {
            final String query = "select count(*) from DatabaseEntry";
            try {
                org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( query );

                return ( Integer ) queryObject.iterate().next();
            } catch ( org.hibernate.HibernateException ex ) {
                throw super.convertHibernateAccessException( ex );
            }
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'DatabaseEntryDao.countAll()' --> " + th, th );
        }
    }

    @Override
    public DatabaseEntry findByAccession( String accession ) {
        return ( DatabaseEntry ) this.getSession()
                .createQuery( "from DatabaseEntry d where d.accession=:accession" )
                .setParameter( "accession", accession ).uniqueResult();
    }

    @Override
    public DatabaseEntry find( DatabaseEntry databaseEntry ) {
        return ( DatabaseEntry ) this.getSession().get( DatabaseEntry.class, databaseEntry.getId() );
    }

}