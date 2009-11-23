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
package ubic.gemma.model.common.description;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.ExternalDatabase
 */
@Repository
public class ExternalDatabaseDaoImpl extends ubic.gemma.model.common.description.ExternalDatabaseDaoBase {

    private static Log log = LogFactory.getLog( ExternalDatabaseDaoImpl.class.getName() );

    @Autowired
    public ExternalDatabaseDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public ExternalDatabase find( ExternalDatabase externalDatabase ) {
        try {

            if ( externalDatabase == null || externalDatabase.getName() == null ) {
                throw new IllegalArgumentException( "No valid business key for " + externalDatabase );
            }

            Criteria queryObject = super.getSession().createCriteria( ExternalDatabase.class );
            queryObject.add( Restrictions.eq( "name", externalDatabase.getName() ) );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.description.ExternalDatabase.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ExternalDatabase ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public ExternalDatabase findOrCreate( ExternalDatabase externalDatabase ) {

        ExternalDatabase existingExternalDatabase = find( externalDatabase );
        if ( existingExternalDatabase != null ) {
            return existingExternalDatabase;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new externalDatabase: " + externalDatabase.getName() );
        return create( externalDatabase );
    }
}