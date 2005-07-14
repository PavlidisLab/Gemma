/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.expression.designElement;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @see edu.columbia.gemma.expression.designElement.DesignElement
 */
public class DesignElementDaoImpl extends edu.columbia.gemma.expression.designElement.DesignElementDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.designElement.DesignElementDaoBase#find(edu.columbia.gemma.expression.designElement.DesignElement)
     */
    @Override
    public DesignElement find( DesignElement designElement ) {

        if ( designElement.getName() == null ) return null;
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( DesignElement.class );

            // FIXME this will not work, it needs to look at the ArrayDesign too!
            queryObject.add( Restrictions.ilike( "category", designElement.getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + DesignElement.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( DesignElement ) results.iterator().next();
                }
            }
            return ( DesignElement ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }
}