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
package ubic.gemma.model.expression.arrayDesign;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
public class ArrayDesignDaoImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase {

    private static Log log = LogFactory.getLog( ArrayDesignDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign find( ArrayDesign arrayDesign ) {
        try {

            BusinessKey.checkValidKey( arrayDesign );
            Criteria queryObject = super.getSession( false ).createCriteria( ArrayDesign.class );
            BusinessKey.addRestrictions( queryObject, arrayDesign );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + ArrayDesign.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign ) {
        ArrayDesign existingArrayDesign = this.find( arrayDesign );
        if ( existingArrayDesign != null ) {
            assert existingArrayDesign.getId() != null;
            return existingArrayDesign;
        }
        log.debug( "Creating new arrayDesign: " + arrayDesign.getName() );
        return ( ArrayDesign ) create( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequences(java.lang.Long)
     */
    @Override
    protected Integer handleNumCompositeSequences( Long id ) throws Exception {
        final String queryString = "select count (*) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return queryByIdReturnInteger( id, queryString );

    }

    /**
     * @param id
     * @param queryString
     * @return
     */
    private Integer queryByIdReturnInteger( Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", id );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of 'Integer" + "' was found when executing query --> '"
                                    + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return ( Integer ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param id
     * @param queryString
     * @return
     */
    private Collection queryByIdReturnCollection( Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumReporters(java.lang.Long)
     */
    @Override
    protected Integer handleNumReporters( Long id ) throws Exception {
        final String queryString = "select count (*) from  ReporterImpl as rep inner join rep.reporterArrayDesign as ar where ar.id = :id";
        return queryByIdReturnInteger( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadReporters(java.lang.Long)
     */
    @Override
    protected Collection handleLoadReporters( Long id ) throws Exception {
        final String queryString = "select rep from ReporterImpl as rep inner join rep.reporterArrayDesign as ar where ar.id = :id";
        return queryByIdReturnCollection( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @Override
    protected Collection handleLoadCompositeSequences( Long id ) throws Exception {
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return queryByIdReturnCollection( id, queryString );
    }
}