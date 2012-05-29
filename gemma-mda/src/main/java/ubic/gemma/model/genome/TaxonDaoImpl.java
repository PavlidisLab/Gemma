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
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.Taxon
 */
@Repository
public class TaxonDaoImpl extends HibernateDaoSupport implements TaxonDao {

    private static Log log = LogFactory.getLog( TaxonDaoImpl.class.getName() );

    @Autowired
    public TaxonDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.TaxonDao#find(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Taxon find( Taxon taxon ) {

        BusinessKey.checkValidKey( taxon );

        Criteria queryObject = super.getSession().createCriteria( Taxon.class ).setReadOnly( true );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );
        BusinessKey.addRestrictions( queryObject, taxon );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + taxon.getClass().getName() + "' was found when executing query" );
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
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
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
            result = ( Taxon ) results.get( 0 );
        }
        return result;

    }

    @Override
    public Collection<? extends Taxon> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from TaxonImpl t where t.id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends Taxon> create( final java.util.Collection<? extends Taxon> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Taxon.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( java.util.Iterator<? extends Taxon> entityIterator = entities.iterator(); entityIterator
                        .hasNext(); ) {
                    create( entityIterator.next() );
                }
                return null;
            }
        } );
        return entities;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    @Override
    public Taxon create( final ubic.gemma.model.genome.Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon.create - 'taxon' can not be null" );
        }

        if ( StringUtils.isBlank( taxon.getCommonName() ) && StringUtils.isBlank( taxon.getScientificName() ) ) {
            throw new IllegalArgumentException( "Cannot create a taxon without names: " + taxon );
        }

        this.getHibernateTemplate().save( taxon );
        return taxon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.TaxonDao#findByAbbreviation(java.lang.String)
     */
    @Override
    public Taxon findByAbbreviation( final java.lang.String abbreviation ) {
        return this.handleFindByAbbreviation( abbreviation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.TaxonDao#findByCommonName(java.lang.String)
     */
    @Override
    public Taxon findByCommonName( final java.lang.String commonName ) {
        return this.findByCommonName( "from TaxonImpl t where t.commonName=:commonName", commonName );
    }

    /**
     * @param queryString
     * @param commonName
     * @return
     */
    public Taxon findByCommonName( final java.lang.String queryString, final java.lang.String commonName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( commonName );
        argNames.add( "commonName" );
        List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.Taxon"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.get( 0 );
        }

        return ( Taxon ) result;
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#findByScientificName(int, java.lang.String)
     */
    @Override
    public Taxon findByScientificName( final java.lang.String scientificName ) {
        return this.findByScientificName( "from TaxonImpl t where t.scientificName=:scientificName ", scientificName );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#findByScientificName(int, java.lang.String, java.lang.String)
     */

    public Taxon findByScientificName( final java.lang.String queryString, final java.lang.String scientificName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( scientificName );
        argNames.add( "scientificName" );
        List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.Taxon"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( Taxon ) result;
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#findChildTaxaByParent(ubic.gemma.model.genome.Taxon)
     */

    @Override
    public Collection<Taxon> findChildTaxaByParent( Taxon parentTaxon ) {
        String queryString = "from ubic.gemma.model.genome.TaxonImpl as taxon where taxon.parentTaxon = :parentTaxon";
        Collection<Taxon> childTaxa = this.getHibernateTemplate().findByNamedParam( queryString, "parentTaxon",
                parentTaxon );
        return childTaxa;
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#load(int, java.lang.Long)
     */
    @Override
    public Taxon load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Taxon.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.TaxonImpl.class, id );
        return ( ubic.gemma.model.genome.Taxon ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#loadAll(int)
     */

    @Override
    @SuppressWarnings("unchecked")
    public java.util.Collection<Taxon> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.TaxonImpl.class );

        return ( Collection<Taxon> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Taxon.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.Taxon entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends Taxon> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Taxon.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#remove(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public void remove( ubic.gemma.model.genome.Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon.remove - 'taxon' can not be null" );
        }
        this.getHibernateTemplate().delete( taxon );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao.thaw#thaw(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public void thaw( final ubic.gemma.model.genome.Taxon taxon ) {
        this.handleThaw( taxon );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends Taxon> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Taxon.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( java.util.Iterator<? extends Taxon> entityIterator = entities.iterator(); entityIterator
                        .hasNext(); ) {
                    update( entityIterator.next() );
                }
                return null;
            }
        } );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonDao#update(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public void update( ubic.gemma.model.genome.Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon.update - 'taxon' can not be null" );
        }
        this.getHibernateTemplate().update( taxon );
    }

}