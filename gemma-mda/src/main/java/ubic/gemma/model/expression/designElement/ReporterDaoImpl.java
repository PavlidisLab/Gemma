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
package ubic.gemma.model.expression.designElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.designElement.Reporter
 */
public class ReporterDaoImpl extends ubic.gemma.model.expression.designElement.ReporterDaoBase {

    private static Log log = LogFactory.getLog( ReporterDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.designElement.ReporterDaoBase#find(ubic.gemma.model.expression.designElement.Reporter
     * )
     */
    @Override
    public Reporter find( Reporter reporter ) {

        if ( reporter.getName() == null ) return null;
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Reporter.class );

            queryObject.add( Restrictions.eq( "name", reporter.getName() ) );

            // queryObject.add( Restrictions.eq( "arrayDesign", reporter.getArrayDesign() ) ); //only works if
            // persistent arrayDesign.

            // This allows finding without having a persistent arrayDesign. TODO make this use the full arraydesign
            // business key.
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
                    result = results.iterator().next();
                }
            }
            return ( Reporter ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.designElement.ReporterDaoBase#findOrCreate(ubic.gemma.model.expression.designElement
     * .Reporter)
     */
    @Override
    public Reporter findOrCreate( Reporter reporter ) {
        if ( reporter.getName() == null || reporter.getArrayDesign() == null ) {
            throw new IllegalArgumentException( "reporter must have name and arrayDesign." );
        }

        Reporter existingReporter = this.find( reporter );
        if ( existingReporter != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing reporter: " + existingReporter );
            return existingReporter;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new reporter: " + reporter.getName() );
        Reporter result = ( Reporter ) create( reporter );
        return result;
    }

}