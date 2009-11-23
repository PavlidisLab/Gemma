/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.common.protocol;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.protocol.Software
 */
@Repository
public class SoftwareDaoImpl extends ubic.gemma.model.common.protocol.SoftwareDaoBase {

    @Autowired
    public SoftwareDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Software find( Software software ) {
        try {
            Criteria queryObject = super.getSession().createCriteria( Software.class );

            queryObject.add( Restrictions.eq( "name", software.getName() ) );

            if ( software.getDescription() != null )
                queryObject.add( Restrictions.eq( "description", software.getDescription() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + ubic.gemma.model.common.protocol.Software.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Software ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Software findOrCreate( Software software ) {
        if ( software == null || software.getName() == null ) throw new IllegalArgumentException();
        Software newSoftware = find( software );
        if ( newSoftware != null ) {
            return newSoftware;
        }
        return create( software );
    }
}