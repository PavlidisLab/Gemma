/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.genome;

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

            java.util.List results = queryObject.list();
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
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( taxon, LockMode.NONE );
                Hibernate.initialize( taxon.getParentTaxon() );
                Hibernate.initialize( taxon.getExternalDatabase() );
                Hibernate.initialize( taxon.getNcbiId() );
                Hibernate.initialize( taxon.getScientificName() );
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
        if ( log.isDebugEnabled() ) log.debug( "Creating new taxon: " + taxon );
        return create( taxon );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#findByAbbreviation(int, java.lang.String)
     */
    @Override
    @SuppressWarnings( { "unchecked" })
    public Taxon handleFindByAbbreviation( final java.lang.String abbreviation ) {
        final String queryString = "from TaxonImpl t where t.abbreviation=:abbreviation";
        List results = getHibernateTemplate()
                .findByNamedParam( queryString, "abbreviation", abbreviation.toLowerCase() );
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
}