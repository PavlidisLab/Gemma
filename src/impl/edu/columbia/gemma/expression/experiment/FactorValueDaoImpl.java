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
package edu.columbia.gemma.expression.experiment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @author pavlidis
 * @version $Id$
 */
public class FactorValueDaoImpl extends edu.columbia.gemma.expression.experiment.FactorValueDaoBase {

    private static Log log = LogFactory.getLog( FactorValueDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.FactorValueDaoBase#find(edu.columbia.gemma.expression.experiment.FactorValue)
     */
    @Override
    public FactorValue find( FactorValue factorValue ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( FactorValue.class );

            if ( factorValue.getValue() != null ) {
                queryObject.add( Restrictions.eq( "value", factorValue.getValue() ) );
            } else if ( factorValue.getOntologyEntry() != null ) {
                queryObject.add( Restrictions.eq( "ontologyEntry", factorValue.getOntologyEntry() ) );
            } else if ( factorValue.getMeasurement() != null ) {
                queryObject.add( Restrictions.eq( "measurement", factorValue.getMeasurement() ) );
                // FIXME, this isn't that simple.
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + FactorValue.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( FactorValue ) results.iterator().next();
                }
            }
            return ( FactorValue ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.FactorValueDaoBase#findOrCreate(edu.columbia.gemma.expression.experiment.FactorValue)
     */
    @Override
    public FactorValue findOrCreate( FactorValue factorValue ) {
        if ( factorValue.getValue() == null && factorValue.getMeasurement() == null
                && factorValue.getOntologyEntry() == null ) {
            log.debug( "FactorValue must have a value (or associated measurement or ontology entry)." );
            return null;
        }
        FactorValue newFactorValue = this.find( factorValue );
        if ( newFactorValue != null ) {
            return newFactorValue;
        }
        log.debug( "Creating new factorValue" );
        return create( factorValue );
    }
}