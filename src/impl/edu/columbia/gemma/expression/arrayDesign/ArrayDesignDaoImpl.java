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
package edu.columbia.gemma.expression.arrayDesign;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesign
 */
public class ArrayDesignDaoImpl extends edu.columbia.gemma.expression.arrayDesign.ArrayDesignDaoBase {

    private static Log log = LogFactory.getLog( ArrayDesignDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao#find(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign find( ArrayDesign arrayDesign ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( ArrayDesign.class );
            queryObject.add( Restrictions.eq( "name", arrayDesign.getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + ArrayDesign.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( ArrayDesign ) results.iterator().next();
                }
            }
            return ( ArrayDesign ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao#findOrCreate(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getName() == null ) {
            log.debug( "Array design must have a name to use as comparison key" );
            return null;
        }
        ArrayDesign newArrayDesign = this.find( arrayDesign );
        if ( newArrayDesign != null ) {
            BeanPropertyCompleter.complete( newArrayDesign, arrayDesign );
            return newArrayDesign;
        }
        log.debug( "Creating new arrayDesign: " + arrayDesign.getName() );
        return ( ArrayDesign ) create( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignDaoBase#numCompositeSequences(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Integer numCompositeSequences( Long id ) {
        return super.numCompositeSequences(
                "select count (*) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id",
                id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignDaoBase#numReporters(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Integer numReporters( Long id ) {
        return super.numReporters(
                "select count (*) from  ReporterImpl as rep inner join rep.arrayDesign as ar where ar.id = :id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignDaoBase#loadCompositeSequences(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Collection loadCompositeSequences( Long id ) {
        return super
                .loadCompositeSequences(
                        "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id",
                        id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignDaoBase#loadReporters(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Collection loadReporters( Long id ) {
        return super.loadReporters(
                "select rep from ReporterImpl as rep inner join rep.arrayDesign as ar where ar.id = :id", id );

    }

}