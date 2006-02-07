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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.common.quantitationtype;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * @see edu.columbia.gemma.common.quantitationtype.QuantitationType
 */
public class QuantitationTypeDaoImpl extends edu.columbia.gemma.common.quantitationtype.QuantitationTypeDaoBase {

    private static Log log = LogFactory.getLog( QuantitationTypeDaoImpl.class.getName() );

    @Override
    public QuantitationType find( QuantitationType quantitationType ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( QuantitationType.class );
            
            queryObject.add( Restrictions.eq( "name", quantitationType.getName() ) );

            queryObject.add( Restrictions.eq( "generalType", quantitationType.getGeneralType() ) );

            queryObject.add( Restrictions.eq( "type", quantitationType.getType() ) );

            if ( quantitationType.getRepresentation() != null )
                queryObject.add( Restrictions.eq( "representation", quantitationType.getRepresentation() ) );

            if ( quantitationType.getScale() != null )
                queryObject.add( Restrictions.eq( "scale", quantitationType.getScale() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + edu.columbia.gemma.common.quantitationtype.QuantitationType.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( edu.columbia.gemma.common.quantitationtype.QuantitationType ) results.iterator().next();
                }
            }
            return ( QuantitationType ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public QuantitationType findOrCreate( QuantitationType quantitationType ) {
        if ( quantitationType == null || quantitationType.getName() == null ) {
            log.warn( "QuantitationType was null or had no name : " + quantitationType );
            return null;
        }
        QuantitationType newQuantitationType = find( quantitationType );
        if ( newQuantitationType != null ) {
            log.debug( "Found existing quantitationType: " + newQuantitationType );
            BeanPropertyCompleter.complete( newQuantitationType, quantitationType );
            return newQuantitationType;
        }
        log.debug( "Creating new quantitationType: " + quantitationType );
        return ( QuantitationType ) create( quantitationType );
    }

}