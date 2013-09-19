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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.persistence.AbstractDao;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalDesign
 * @version $Id$
 */
@Repository
public class ExperimentalDesignDaoImpl extends AbstractDao<ExperimentalDesign> implements ExperimentalDesignDao {

    private static Log log = LogFactory.getLog( ExperimentalDesignDaoImpl.class.getName() );

    @Autowired
    public ExperimentalDesignDaoImpl( SessionFactory sessionFactory ) {
        super( ExperimentalDesignImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignDaoBase#find(ubic.gemma.model.expression.experiment.
     * ExperimentalDesign)
     */
    @Override
    public ExperimentalDesign find( ExperimentalDesign experimentalDesign ) {
        try {
            Criteria queryObject = super.getSessionFactory().getCurrentSession()
                    .createCriteria( ExperimentalDesign.class );

            queryObject.add( Restrictions.eq( "name", experimentalDesign.getName() ) );

            List<?> results = queryObject.list();
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

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#find(int, java.lang.String,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public ExperimentalDesign find( final String queryString, final ExperimentalDesign experimentalDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<>();
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add( experimentalDesign );
        argNames.add( "experimentalDesign" );
        Set<ExperimentalDesign> results = new LinkedHashSet<>( this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.experiment.ExperimentalDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( ExperimentalDesign ) result;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findByName(int, java.lang.String)
     */

    @Override
    public ExperimentalDesign findByName( final java.lang.String name ) {
        return this
                .findByName(
                        "from ubic.gemma.model.expression.experiment.ExperimentalDesign as experimentalDesign where experimentalDesign.name = :name",
                        name );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findByName(int, java.lang.String,
     *      java.lang.String)
     */

    public ExperimentalDesign findByName( final java.lang.String queryString, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<>();
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add( name );
        argNames.add( "name" );
        java.util.Set<ExperimentalDesign> results = new LinkedHashSet<>( this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.experiment.ExperimentalDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( ExperimentalDesign ) result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignDaoBase#find(ubic.gemma.model.expression.experiment.
     * ExperimentalDesign)
     */
    @Override
    public ExperimentalDesign findOrCreate( ExperimentalDesign experimentalDesign ) {
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

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */

    public ExperimentalDesign findOrCreate( final java.lang.String queryString,
            final ExperimentalDesign experimentalDesign ) {
        java.util.List<String> argNames = new java.util.ArrayList<>();
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add( experimentalDesign );
        argNames.add( "experimentalDesign" );
        Set<ExperimentalDesign> results = new LinkedHashSet<>( this.getHibernateTemplate().findByNamedParam(
                queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.experiment.ExperimentalDesign"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( ExperimentalDesign ) result;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignDao#getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    public ExpressionExperiment getExpressionExperiment( final ExperimentalDesign experimentalDesign ) {
        try {
            return this.handleGetExpressionExperiment( experimentalDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExperimentalDesignDao.getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign)' --> "
                            + th, th );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignDaoBase#handleGetExpressionExperiment(ubic.gemma.model
     * .expression.experiment.ExperimentalDesign)
     */
    private ExpressionExperiment handleGetExpressionExperiment( ExperimentalDesign ed ) {

        if ( ed == null ) return null;

        final String queryString = "select distinct ee FROM ExpressionExperimentImpl as ee where ee.experimentalDesign = :ed ";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "ed", ed );

        if ( results.size() == 0 ) {
            log.info( "There is no expression experiment that has experimental design id = " + ed.getId() );
            return null;
        }
        return ( ExpressionExperiment ) results.iterator().next();

    }

}