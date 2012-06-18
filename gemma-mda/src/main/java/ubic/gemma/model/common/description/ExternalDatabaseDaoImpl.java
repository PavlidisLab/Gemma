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

        if ( externalDatabase == null || externalDatabase.getName() == null ) {
            throw new IllegalArgumentException( "No valid business key for " + externalDatabase );
        }

        Criteria queryObject = super.getSession().createCriteria( ExternalDatabase.class );
        queryObject.add( Restrictions.eq( "name", externalDatabase.getName() ) );
        java.util.List<?> results = queryObject.list();
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