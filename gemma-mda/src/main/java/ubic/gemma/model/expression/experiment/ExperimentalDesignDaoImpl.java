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
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalDesign
 */
@Repository
public class ExperimentalDesignDaoImpl extends ubic.gemma.model.expression.experiment.ExperimentalDesignDaoBase {

    private static Log log = LogFactory.getLog( ExperimentalDesignDaoImpl.class.getName() );

    @Autowired
    public ExperimentalDesignDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignDaoBase#find(ubic.gemma.model.expression.experiment.
     * ExperimentalDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ExperimentalDesign find( ExperimentalDesign experimentalDesign ) {
        try {
            Criteria queryObject = super.getSession().createCriteria( ExperimentalDesign.class );

            queryObject.add( Restrictions.eq( "name", experimentalDesign.getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + ExperimentalDesign.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ExperimentalDesign ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignDaoBase#find(ubic.gemma.model.expression.experiment.
     * ExperimentalDesign)
     */
    @Override
    public ExperimentalDesign findOrCreate( ExperimentalDesign experimentalDesign ) {
        // FIXME move to checkKey; key is not complete!!!
        if ( experimentalDesign.getName() == null ) {
            throw new IllegalArgumentException( "ExperimentalDesign must have name or external accession." );
        }
        ExperimentalDesign existingExperimentalDesign = this.find( experimentalDesign );
        if ( existingExperimentalDesign != null ) {
            return existingExperimentalDesign;
        }
        log.debug( "Creating new ExperimentalDesign: " + experimentalDesign.getName() );
        return create( experimentalDesign );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignDaoBase#handleGetExpressionExperiment(ubic.gemma.model
     * .expression.experiment.ExperimentalDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected ExpressionExperiment handleGetExpressionExperiment( ExperimentalDesign ed ) {

        if ( ed == null ) return null;

        final String queryString = "select distinct ee FROM ExpressionExperimentImpl as ee where ee.experimentalDesign = :ed ";
        List results = getHibernateTemplate().findByNamedParam( queryString, "ed", ed );

        if ( results.size() == 0 ) {
            log.info( "There is no expression experiment that has experimental design id = " + ed.getId() );
            return null;
        }
        return ( ExpressionExperiment ) results.iterator().next();

    }

}