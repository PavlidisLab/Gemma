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
package ubic.gemma.model.expression.bioAssay;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class BioAssayDaoImpl extends ubic.gemma.model.expression.bioAssay.BioAssayDaoBase {

    private static Log log = LogFactory.getLog( BioAssayDaoImpl.class.getName() );

    @Autowired
    public BioAssayDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#find(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @SuppressWarnings("unchecked")
    @Override
    public BioAssay find( BioAssay bioAssay ) {
        try {
            Criteria queryObject = BusinessKey.createQueryObject( super.getSession(), bioAssay );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BioAssay.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( BioAssay ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findByAccession(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        if ( StringUtils.isBlank( accession ) ) return new HashSet<BioAssay>();

        return this.getHibernateTemplate().findByNamedParam(
                "select distinct b from BioAssayImpl b inner join b.accession a where a.accession = :query", "query",
                accession );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#findOrCreate(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public BioAssay findOrCreate( BioAssay bioAssay ) {
        if ( bioAssay == null || bioAssay.getName() == null ) {
            throw new IllegalArgumentException( "BioAssay was null or had no name : " + bioAssay );
        }
        BioAssay newBioAssay = find( bioAssay );
        if ( newBioAssay != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing bioAssay: " + newBioAssay );
            return newBioAssay;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new bioAssay: " + bioAssay );
        return create( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#handleThaw(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public void handleThaw( final BioAssay bioAssay ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( bioAssay, LockMode.NONE );
                Hibernate.initialize( bioAssay.getArrayDesignUsed() );
                Hibernate.initialize( bioAssay.getDerivedDataFiles() );
                for ( BioMaterial bm : bioAssay.getSamplesUsed() ) {
                    session.lock( bm, LockMode.NONE );
                    Hibernate.initialize( bm );
                    Hibernate.initialize( bm.getBioAssaysUsedIn() );
                    Hibernate.initialize( bm.getFactorValues() );
                    session.evict( bm );
                }
                session.evict( bioAssay );
                return null;
            }
        } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#thaw(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() ) return bioAssays;
        List<?> thawedBioassays = this.getHibernateTemplate().findByNamedParam(
                "select distinct b from BioAssayImpl b left join fetch b.arrayDesignUsed"
                        + " left join fetch b.derivedDataFiles left join fetch b.samplesUsed bm"
                        + " left join bm.factorValues left join bm.bioAssaysUsedIn left join fetch "
                        + " b.auditTrail at left join fetch at.events where b.id in (:ids) ", "ids",
                EntityUtils.getIds( bioAssays ) );
        return ( Collection<BioAssay> ) thawedBioassays;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#handleCountAll()
     */
    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioAssayImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );
            queryObject.setCacheable( true );
            return ( ( Long ) queryObject.iterate().next() ).intValue();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}