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
package edu.columbia.gemma.common.description;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.description.ExternalDatabase
 */
public class ExternalDatabaseDaoImpl extends edu.columbia.gemma.common.description.ExternalDatabaseDaoBase {

    private static Log log = LogFactory.getLog( ExternalDatabaseDaoImpl.class.getName() );

    @Override
    public ExternalDatabase find( ExternalDatabase externalDatabase ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( ExternalDatabase.class );
            queryObject.add( Restrictions.eq( "name", externalDatabase.getName() ) );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + edu.columbia.gemma.common.description.ExternalDatabase.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( edu.columbia.gemma.common.description.ExternalDatabase ) results.iterator().next();
                }
            }
            return ( ExternalDatabase ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public ExternalDatabase findOrCreate( ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null || externalDatabase.getName() == null ) return null;
        ExternalDatabase newExternalDatabase = find( externalDatabase );
        if ( newExternalDatabase != null ) {
            BeanPropertyCompleter.complete( newExternalDatabase, externalDatabase );
            return newExternalDatabase;
        }
        log.debug( "Creating new externalDatabase: " + externalDatabase.getName() );
        return ( ExternalDatabase ) create( externalDatabase );
    }
}