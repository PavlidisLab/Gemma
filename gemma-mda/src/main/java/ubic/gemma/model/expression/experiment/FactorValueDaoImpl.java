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
package ubic.gemma.model.expression.experiment;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @author pavlidis
 * @version $Id$
 */
public class FactorValueDaoImpl extends ubic.gemma.model.expression.experiment.FactorValueDaoBase {

    private static Log log = LogFactory.getLog( FactorValueDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.FactorValueDaoBase#find(ubic.gemma.model.expression.experiment.FactorValue)
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
                    this.debug( results );
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size()
                            + " instances of '" + FactorValue.class.getName() + "' was found when executing query for "
                            + factorValue );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( FactorValue ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param results
     */
    private void debug( List results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nFactorValues found:\n" );
        for ( Object object : results ) {
            FactorValue fv = ( FactorValue ) object;
            sb.append( "\tID=" + fv.getId() + " Value=" + fv.getValue() );
            if ( fv.getMeasurement() != null ) sb.append( " Measurement=" + fv.getMeasurement().getValue() );
            if ( fv.getOntologyEntry() != null ) sb.append( " OntologyEntry=" + fv.getOntologyEntry().getValue() );
            sb.append( "\n" );
        }
        log.error( sb.toString() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.FactorValueDaoBase#findOrCreate(ubic.gemma.model.expression.experiment.FactorValue)
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