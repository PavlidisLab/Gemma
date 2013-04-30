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
package ubic.gemma.model.common.description;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.DatabaseEntry
 */
@Repository
public class DatabaseEntryDaoImpl extends ubic.gemma.model.common.description.DatabaseEntryDaoBase {

    private static Log log = LogFactory.getLog( DatabaseEntryDaoImpl.class.getName() );

    @Autowired
    public DatabaseEntryDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public DatabaseEntry find( DatabaseEntry databaseEntry ) {

        DetachedCriteria queryObject = DetachedCriteria.forClass( DatabaseEntry.class );
        BusinessKey.checkKey( databaseEntry );

        BusinessKey.addRestrictions( queryObject, databaseEntry );

        List<?> results = this.getHibernateTemplate().findByCriteria( queryObject );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                log.debug( debug( results ) );
                result = results.iterator().next();
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( DatabaseEntry ) result;

    }

    @Override
    public Collection<? extends DatabaseEntry> load( Collection<Long> ids ) {
        if ( ids == null || ids.isEmpty() ) return new HashSet<DatabaseEntry>();
        final String queryString = "select ad from DatabaseEntryImpl d where ad.id in (:ids) ";
        return getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from DatabaseEntryImpl";
        try {
            org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    private String debug( List<?> results ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "Multiple database entries match:\n" );
        for ( Object object : results ) {
            DatabaseEntry de = ( DatabaseEntry ) object;
            buf.append( de + "\n" );
        }
        return buf.toString();
    }

}