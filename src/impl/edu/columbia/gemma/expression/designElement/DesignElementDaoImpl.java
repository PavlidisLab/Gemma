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
package edu.columbia.gemma.expression.designElement;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.genome.Gene;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
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

            queryObject.add( Restrictions.eq( "name", designElement.getName() ) );

            // join
            queryObject.createCriteria( "arrayDesign" ).add(
                    Restrictions.eq( "name", designElement.getArrayDesign().getName() ) );

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