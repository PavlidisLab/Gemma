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
package ubic.gemma.persistence.service.common.measurement;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.List;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.measurement.Unit</code>.
 * </p>
 *
 * @author paul
 * @see ubic.gemma.model.common.measurement.Unit
 */
@Repository
public class UnitDaoImpl extends AbstractDao<Unit> implements UnitDao {

    @Autowired
    public UnitDaoImpl( SessionFactory sessionFactory ) {
        super( Unit.class, sessionFactory );
    }

    @Override
    public Unit find( Unit unit ) {
        BusinessKey.checkValidKey( unit );
        Criteria queryObject = BusinessKey.createQueryObject( this.getSessionFactory().getCurrentSession(), unit );
        List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        results.size() + " " + Unit.class.getName() + "s were found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( Unit ) result;
    }
}