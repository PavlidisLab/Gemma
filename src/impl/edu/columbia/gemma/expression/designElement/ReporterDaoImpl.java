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
package edu.columbia.gemma.expression.designElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.designElement.Reporter
 */
public class ReporterDaoImpl extends edu.columbia.gemma.expression.designElement.ReporterDaoBase {

    private static Log log = LogFactory.getLog( ReporterDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.designElement.ReporterDaoBase#find(edu.columbia.gemma.expression.designElement.Reporter)
     */
    @Override
    public Reporter find( DesignElement reporter ) {

        if ( reporter.getName() == null ) return null;
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Reporter.class );

            queryObject.add( Restrictions.eq( "name", reporter.getName() ) );

            // join
            queryObject.createCriteria( "arrayDesign" ).add(
                    Restrictions.eq( "name", reporter.getArrayDesign().getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + Reporter.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( Reporter ) results.iterator().next();
                }
            }
            return ( Reporter ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.designElement.ReporterDaoBase#findOrCreate(edu.columbia.gemma.expression.designElement.Reporter)
     */
    @Override
    public Reporter findOrCreate( Reporter reporter ) {
        if ( reporter.getName() == null || reporter.getArrayDesign() == null ) {
            log.debug( "reporter must name and arrayDesign." );
            return null;
        }
        Reporter newreporter = this.find( reporter );
        if ( newreporter != null ) {
            return newreporter;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new reporter: " + reporter.getName() );
        return ( Reporter ) create( reporter );

    }

}