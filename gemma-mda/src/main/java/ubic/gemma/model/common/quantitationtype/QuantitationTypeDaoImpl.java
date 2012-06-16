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
package ubic.gemma.model.common.quantitationtype;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
@Repository
public class QuantitationTypeDaoImpl extends ubic.gemma.model.common.quantitationtype.QuantitationTypeDaoBase {

    private static Log log = LogFactory.getLog( QuantitationTypeDaoImpl.class.getName() );

    @Autowired
    public QuantitationTypeDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public QuantitationType find( QuantitationType quantitationType ) {
        Criteria queryObject = super.getSession().createCriteria( QuantitationType.class );

        BusinessKey.addRestrictions( queryObject, quantitationType );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                debug( results );
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '"
                                + ubic.gemma.model.common.quantitationtype.QuantitationType.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( QuantitationType ) result;

    }

    @Override
    public QuantitationType findOrCreate( QuantitationType quantitationType ) {
        if ( quantitationType == null || quantitationType.getName() == null ) {
            throw new IllegalArgumentException( "QuantitationType was null or had no name : " + quantitationType );
        }
        QuantitationType newQuantitationType = find( quantitationType );
        if ( newQuantitationType != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing quantitationType: " + newQuantitationType );
            return newQuantitationType;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new quantitationType: " + quantitationType );
        return create( quantitationType );
    }

    /**
     * @param results
     */
    private void debug( Collection results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nMultiple QuantitationTypes found matching query:\n" );
        for ( Object object : results ) {
            QuantitationType entity = ( QuantitationType ) object;
            sb.append( entity + "\n" );
        }
        log.error( sb.toString() );
    }

}