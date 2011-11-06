/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.protocol.ProtocolDaoImpl;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.Taxon
 */
@Repository
public class TaxonDaoImpl extends ubic.gemma.model.genome.TaxonDaoBase {

    private static Log log = LogFactory.getLog( ProtocolDaoImpl.class.getName() );

    @Autowired
    public TaxonDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.TaxonDaoBase#find(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("boxing")
    @Override
    public Taxon find( Taxon taxon ) {
        try {

            BusinessKey.checkValidKey( taxon );

            Criteria queryObject = super.getSession().createCriteria( Taxon.class );

            BusinessKey.addRestrictions( queryObject, taxon );

            java.util.List<?> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + taxon.getClass().getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Taxon ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public void handleThaw( final Taxon taxon ) throws Exception {
        if ( taxon == null ) {
            log.warn( "Attempt to thaw null" );
            return;
        }
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( taxon );
                Hibernate.initialize( taxon.getParentTaxon() );
                Hibernate.initialize( taxon.getExternalDatabase() );
                session.evict( taxon );
                return null;
            }
        } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.TaxonDaoBase#findOrCreate(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Taxon findOrCreate( Taxon taxon ) {
        Taxon existingTaxon = find( taxon );
        if ( existingTaxon != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing taxon: " + taxon );
            return existingTaxon;
        }

        if ( StringUtils.isBlank( taxon.getCommonName() ) && StringUtils.isBlank( taxon.getScientificName() ) ) {
            throw new IllegalArgumentException( "Cannot create a taxon without names: " + taxon );
        }

        log.warn( "Creating new taxon: " + taxon );

        return create( taxon );

    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#findByAbbreviation(int, java.lang.String)
     */
    @Override
    public Taxon handleFindByAbbreviation( final java.lang.String abbreviation ) {
        final String queryString = "from TaxonImpl t where t.abbreviation=:abbreviation";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "abbreviation",
                abbreviation.toLowerCase() );
        Taxon result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.Taxon"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = ( Taxon ) results.iterator().next();
        }
        return result;

    }

    @Override
    public Collection<? extends Taxon> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from TaxonImpl t where t.id in (:ids)", "ids", ids );
    }
}