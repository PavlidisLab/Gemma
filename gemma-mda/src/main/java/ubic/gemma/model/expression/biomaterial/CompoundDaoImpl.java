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
package ubic.gemma.model.expression.biomaterial;

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
 * @see ubic.gemma.model.expression.biomaterial.Compound
 */
@Repository
public class CompoundDaoImpl extends ubic.gemma.model.expression.biomaterial.CompoundDaoBase {

    private static Log log = LogFactory.getLog( CompoundDaoImpl.class.getName() );

    @Autowired
    public CompoundDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.CompoundDaoBase#find(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public Compound find( Compound compound ) {

        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( Compound.class );
        queryObject.add( Restrictions.eq( "name", compound.getName() ) );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + Compound.class.getName() + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( Compound ) result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.CompoundDaoBase#findOrCreate(ubic.gemma.model.expression.biomaterial.
     * Compound)
     */
    @Override
    public Compound findOrCreate( Compound compound ) {
        if ( compound == null || compound.getName() == null ) {
            throw new IllegalArgumentException(
                    "Compound must not be null and must have a name to use as comparison key" );
        }
        Compound newCompound = this.find( compound );
        if ( newCompound != null ) {
            return newCompound;
        }
        log.debug( "Creating new compound: " + compound.getName() );
        return create( compound );
    }
}