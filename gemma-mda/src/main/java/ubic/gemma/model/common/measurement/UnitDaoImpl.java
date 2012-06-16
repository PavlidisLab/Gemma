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
package ubic.gemma.model.common.measurement;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @author paul
 * @version $Id$
 * @see ubic.gemma.model.common.measurement.Unit
 */
@Repository
public class UnitDaoImpl extends ubic.gemma.model.common.measurement.UnitDaoBase {

    @Autowired
    public UnitDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Unit find( Unit unit ) {
        try {

            BusinessKey.checkValidKey( unit );

            Criteria queryObject = BusinessKey.createQueryObject( super.getSession(), unit );

            java.util.List<?> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                            + Unit.class.getName() + "s were found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Unit ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.measurement.UnitDao#findOrCreate(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    public Unit findOrCreate( Unit unit ) {
        Unit existing = this.find( unit );
        if ( existing != null ) {
            return existing;
        }

        return create( unit );
    }

}