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
package edu.columbia.gemma.expression.bioAssayData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * @see edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector
 * @author pavlidis
 * @version $Id$
 */
public class DesignElementDataVectorDaoImpl extends
        edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorDaoBase {

    private static Log log = LogFactory.getLog( DesignElementDataVectorDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector findOrCreate( DesignElementDataVector designElementDataVector ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( DesignElementDataVector.class );

            queryObject.createCriteria( "designElement" ).add(
                    Restrictions.eq( "name", designElementDataVector.getDesignElement().getName() ) ).createCriteria(
                    "arrayDesign" ).add(
                    Restrictions.eq( "name", designElementDataVector.getDesignElement().getArrayDesign().getName() ) );

            queryObject.createCriteria( "quantitationType" ).add(
                    Restrictions.eq( "name", designElementDataVector.getQuantitationType().getName() ) );

            queryObject.add( Restrictions
                    .eq( "expressionExperiment", designElementDataVector.getExpressionExperiment() ) );

            // FIXME - finish filling in criteria so we never use 'equals' on a domain object.

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + DesignElementDataVector.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( DesignElementDataVector ) results.iterator().next();
                }
            }
            return ( DesignElementDataVector ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector find( DesignElementDataVector designElementDataVector ) {
        if ( designElementDataVector == null || designElementDataVector.getDesignElement() == null
                || designElementDataVector.getExpressionExperiment() == null ) {
            log.warn( "DesignElementDataVector did not have comparable fields " + designElementDataVector );
            return null;
        }
        DesignElementDataVector newDesignElementDataVector = find( designElementDataVector );
        if ( newDesignElementDataVector != null ) {
            log.debug( "Found existing designElementDataVector: " + newDesignElementDataVector );
            BeanPropertyCompleter.complete( newDesignElementDataVector, designElementDataVector );
            return newDesignElementDataVector;
        }
        log.debug( "Creating new designElementDataVector: " + designElementDataVector );
        return ( DesignElementDataVector ) create( designElementDataVector );
    }

}