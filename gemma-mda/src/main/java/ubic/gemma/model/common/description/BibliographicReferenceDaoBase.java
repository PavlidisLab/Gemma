/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.description;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Base DAO Class: is able to create, update, remove, load, and find objects of type <code>BibliographicReference</code>
 * .
 * 
 * @see BibliographicReference
 * @version $Id$
 */
public abstract class BibliographicReferenceDaoBase extends HibernateDaoSupport implements BibliographicReferenceDao {

    /**
     * @see BibliographicReferenceDao#create(int, Collection)
     */
    @Override
    public Collection<? extends BibliographicReference> create(
            final Collection<? extends BibliographicReference> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibliographicReference.create - 'entities' can not be null" );
        }

        for ( Iterator<? extends BibliographicReference> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
            create( entityIterator.next() );
        }

        return entities;
    }

    /**
     * @see BibliographicReferenceDao#create(int transform, BibliographicReference)
     */
    @Override
    public BibliographicReference create( final BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference.create - 'bibliographicReference' can not be null" );
        }
        this.getHibernateTemplate().save( bibliographicReference );
        return bibliographicReference;
    }

    /**
     * @see BibliographicReferenceDao#findByExternalId(int, String, String)
     */
    @Override
    public BibliographicReference findByExternalId( final String id, final String databaseName ) {
        return this
                .findByExternalId(
                        "from BibliographicReferenceImpl b where b.pubAccession.accession=:id AND b.pubAccession.externalDatabase.name=:databaseName",
                        id, databaseName );
    }

    /**
     * @see BibliographicReferenceDao#findByExternalId(int, String, String, String)
     */

    public BibliographicReference findByExternalId( final String queryString, final String id, final String databaseName ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( id );
        argNames.add( "id" );
        args.add( databaseName );
        argNames.add( "databaseName" );
        Set<BibliographicReference> results = new LinkedHashSet<BibliographicReference>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        BibliographicReference result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'BibliographicReference" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see BibliographicReferenceDao#findByExternalId(int, String, DatabaseEntry)
     */

    public BibliographicReference findByExternalId( final String queryString, final DatabaseEntry externalId ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( externalId );
        argNames.add( "externalId" );
        Set<BibliographicReference> results = new LinkedHashSet<BibliographicReference>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        BibliographicReference result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'BibliographicReference" + "' was found when executing query --> '"
                            + queryString + "' for: " + externalId.getAccession() );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see BibliographicReferenceDao#findByExternalId(int, DatabaseEntry)
     */
    @Override
    public BibliographicReference findByExternalId( final DatabaseEntry externalId ) {
        return this.findByExternalId( "from BibliographicReferenceImpl b where b.pubAccession=:externalId", externalId );
    }

    /**
     * @see BibliographicReferenceDao#findByTitle(int, String, String)
     */

    public BibliographicReference findByTitle( final String queryString, final String title ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( title );
        argNames.add( "title" );
        Set<BibliographicReference> results = new LinkedHashSet<BibliographicReference>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        BibliographicReference result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'BibliographicReference" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see BibliographicReferenceDao#getAllExperimentLinkedReferences()
     */
    @Override
    public Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences() {
        return this.handleGetAllExperimentLinkedReferences();
    }

    /**
     * @see BibliographicReferenceDao#getRelatedExperiments(BibliographicReference)
     */
    @Override
    public Collection<ExpressionExperiment> getRelatedExperiments( final BibliographicReference bibliographicReference ) {
        return this.handleGetRelatedExperiments( bibliographicReference );
    }

    /**
     * @see BibliographicReferenceDao#load(int, Long)
     */

    @Override
    public BibliographicReference load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BibliographicReference.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( BibliographicReferenceImpl.class, id );
        return ( BibliographicReference ) entity;
    }

    /**
     * @see BibliographicReferenceDao#loadMultiple(Collection)
     */
    @Override
    public Collection<BibliographicReference> load( final Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'BibliographicReferenceDao.loadMultiple(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see BibliographicReferenceDao#loadAll(int)
     */

    @Override
    @SuppressWarnings("unchecked")
    public Collection<BibliographicReference> loadAll() {
        final Collection<?> results = this.getHibernateTemplate().loadAll( BibliographicReferenceImpl.class );
        return ( Collection<BibliographicReference> ) results;
    }

    /**
     * @see BibliographicReferenceDao#remove(Long)
     */

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BibliographicReference.remove - 'id' can not be null" );
        }
        BibliographicReference entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(Collection)
     */

    @Override
    public void remove( Collection<? extends BibliographicReference> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibliographicReference.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see BibliographicReferenceDao#remove(BibliographicReference)
     */
    @Override
    public void remove( BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference.remove - 'bibliographicReference' can not be null" );
        }
        this.getHibernateTemplate().delete( bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */

    @Override
    public void update( final Collection<? extends BibliographicReference> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibliographicReference.update - 'entities' can not be null" );
        }

        for ( Iterator<? extends BibliographicReference> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
            update( entityIterator.next() );
        }

    }

    /**
     * @see BibliographicReferenceDao#update(BibliographicReference)
     */
    @Override
    public void update( BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference.update - 'bibliographicReference' can not be null" );
        }
        this.getHibernateTemplate().update( bibliographicReference );
    }

    /**
     * Performs the core logic for {@link #getAllExperimentLinkedReferences()}
     */
    protected abstract Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences();

    /**
     * Performs the core logic for {@link #getRelatedExperiments(BibliographicReference)}
     */
    protected abstract Collection<ExpressionExperiment> handleGetRelatedExperiments(
            BibliographicReference bibliographicReference );

    /**
     * Performs the core logic for {@link #loadMultiple(Collection)}
     */
    protected abstract Collection<BibliographicReference> handleLoadMultiple( Collection<Long> ids );

}