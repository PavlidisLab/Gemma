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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;

import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactor
 */
public class ExperimentalFactorDaoImpl extends ubic.gemma.model.expression.experiment.ExperimentalFactorDaoBase {

    private static Log log = LogFactory.getLog( ExperimentalFactorDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorDaoBase#find(ubic.gemma.model.expression.experiment.
     * ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor find( ExperimentalFactor experimentalFactor ) {
        try {

            BusinessKey.checkValidKey( experimentalFactor );
            Criteria queryObject = super.getSession( false ).createCriteria( ExperimentalFactor.class );
            BusinessKey.addRestrictions( queryObject, experimentalFactor );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {

                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                            + ExperimentalFactor.class.getName() + "s were found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ExperimentalFactor ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorDaoBase#findOrCreate(ubic.gemma.model.expression.experiment
     * .ExperimentalFactor)
     */
    @Override
    public ExperimentalFactor findOrCreate( ExperimentalFactor experimentalFactor ) {
        ExperimentalFactor existing = this.find( experimentalFactor );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        log.debug( "Creating new arrayDesign: " + experimentalFactor.getName() );
        return ( ExperimentalFactor ) create( experimentalFactor );
    }
}