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
package ubic.gemma.persistence.service.genome;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonImpl;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author pavlidis
 * @see Taxon
 */
@Repository
public class TaxonDaoImpl extends HibernateDaoSupport implements TaxonDao {

    private static final Log log = LogFactory.getLog( TaxonDaoImpl.class.getName() );

    @Autowired
    public TaxonDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Taxon find( Taxon taxon ) {

        BusinessKey.checkValidKey( taxon );

        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( Taxon.class )
                .setReadOnly( true );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );
        BusinessKey.addRestrictions( queryObject, taxon );

        List<?> results = queryObject.list();
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

    }

    public void handleThaw( final Taxon taxon ) {
        if ( taxon == null ) {
            log.warn( "Attempt to thaw null" );
            return;
        }
        HibernateTemplate template = this.getHibernateTemplate();
        template.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( taxon );
                Hibernate.initialize( taxon.getParentTaxon() );
                Hibernate.initialize( taxon.getExternalDatabase() );
                session.evict( taxon );
                return null;
            }
        } );
    }

    @Override
    public Taxon findOrCreate( Taxon taxon ) {
        Taxon existingTaxon = find( taxon );
        if ( existingTaxon != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing taxon: " + taxon );
            return existingTaxon;
        }

        if ( StringUtils.isBlank( taxon.getCommonName() ) && StringUtils.isBlank( taxon.getScientificName() ) ) {
            throw new IllegalArgumentException( "Cannot create a taxon without names: " + taxon );
        }

        log.warn( "Creating new taxon: " + taxon );

        return create( taxon );

    }

    /**
     * @see TaxonDao#findByAbbreviation(String)
     */
    public Taxon handleFindByAbbreviation( final java.lang.String abbreviation ) {
        final String queryString = "from TaxonImpl t where t.abbreviation=:abbreviation";
        List<?> results = getHibernateTemplate()
                .findByNamedParam( queryString, "abbreviation", abbreviation.toLowerCase() );
        Taxon result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Taxon" + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = ( Taxon ) results.get( 0 );
        }
        return result;

    }

    @Override
    public Collection<? extends Taxon> load( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam( "from TaxonImpl t where t.id in (:ids)", "ids", ids );
    }

    /**
     * @see TaxonDao#create(Collection)
     */
    @Override
    public Collection<? extends Taxon> create( final Collection<? extends Taxon> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Taxon.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Taxon entity : entities ) {
                    create( entity );
                }
                return null;
            }
        } );
        return entities;
    }

    @Override
    public Taxon create( final Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon.create - 'taxon' can not be null" );
        }

        if ( StringUtils.isBlank( taxon.getCommonName() ) && StringUtils.isBlank( taxon.getScientificName() ) ) {
            throw new IllegalArgumentException( "Cannot create a taxon without names: " + taxon );
        }

        this.getHibernateTemplate().save( taxon );
        return taxon;
    }

    @Override
    public Taxon findByAbbreviation( final java.lang.String abbreviation ) {
        return this.handleFindByAbbreviation( abbreviation );
    }

    @Override
    public Taxon findByCommonName( final java.lang.String commonName ) {
        return this.findByCommonName( "from TaxonImpl t where t.commonName=:commonName", commonName );
    }

    public Taxon findByCommonName( final java.lang.String queryString, final java.lang.String commonName ) {
        List<String> argNames = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        args.add( commonName );
        argNames.add( "commonName" );
        List<?> results = this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() );
        Object result = null;

        //TODO REFACTOR OM MY GOD

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Taxon" + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.get( 0 );
        }

        return ( Taxon ) result;
    }

    /**
     * @see TaxonDao#findByScientificName(String)
     */
    @Override
    public Taxon findByScientificName( final String scientificName ) {
        Criteria crit = this.getSessionFactory().getCurrentSession().createCriteria(TaxonImpl.class);
        crit.add( Restrictions.ilike("scientificName", scientificName));
        crit.setMaxResults( 1 );
        return ( Taxon ) crit.uniqueResult();
    }

    @Override
    public Collection<Taxon> findTaxonUsedInEvidence() {
        String query = "select distinct taxon from GeneImpl as g join g.phenotypeAssociations as evidence join g.taxon as taxon";
        //noinspection unchecked
        return ( Collection<Taxon> ) this.getHibernateTemplate().find( query );
    }

    /**
     * @see TaxonDao#findByScientificName(String)
     */

    public Taxon findByScientificName( final java.lang.String queryString, final java.lang.String scientificName ) {
        List<String> argNames = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        args.add( scientificName );
        argNames.add( "scientificName" );
        List<?> results = this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Taxon" + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( Taxon ) result;
    }

    /**
     * @see TaxonDao#findChildTaxaByParent(Taxon)
     */

    @Override
    public Collection<Taxon> findChildTaxaByParent( Taxon parentTaxon ) {
        String queryString = "from TaxonImpl as taxon where taxon.parentTaxon = :parentTaxon";
        //noinspection unchecked
        return ( Collection<Taxon> ) this.getHibernateTemplate()
                .findByNamedParam( queryString, "parentTaxon", parentTaxon );
    }

    /**
     * @see TaxonDao#load(Long)
     */
    @Override
    public Taxon load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Taxon.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( TaxonImpl.class, id );
        return ( Taxon ) entity;
    }

    /**
     * @see TaxonDao#loadAll()
     */

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Taxon> loadAll() {
        final Collection<?> results = this.getHibernateTemplate().loadAll( TaxonImpl.class );

        return ( Collection<Taxon> ) results;
    }

    /**
     * @see TaxonDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Taxon.remove - 'id' can not be null" );
        }
        Taxon entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see TaxonDao#remove(Collection)
     */
    @Override
    public void remove( Collection<? extends Taxon> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Taxon.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see TaxonDao#remove(Object)
     */
    @Override
    public void remove( Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon.remove - 'taxon' can not be null" );
        }
        this.getHibernateTemplate().delete( taxon );
    }

    /**
     * @see TaxonDao#thaw(Taxon)
     */
    @Override
    public void thaw( final Taxon taxon ) {
        this.handleThaw( taxon );
    }

    /**
     * @see TaxonDao#update(Collection)
     */
    @Override
    public void update( final Collection<? extends Taxon> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Taxon.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Taxon entity : entities ) {
                    update( entity );
                }
                return null;
            }
        } );
    }

    /**
     * @see TaxonDao#update(Object)
     */
    @Override
    public void update( Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon.update - 'taxon' can not be null" );
        }
        this.getHibernateTemplate().update( taxon );
    }

}