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
package ubic.gemma.model.association;

import java.util.Collection;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.Gene2GOAssociation</code>.
 * </p>
 * 
 * @see ubic.gemma.model.association.Gene2GOAssociation
 */
public abstract class Gene2GOAssociationDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.association.Gene2GOAssociationDao {

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends Gene2GOAssociation> create(
            final java.util.Collection<? extends Gene2GOAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.association.Gene2GOAssociation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    public Collection<? extends Gene2GOAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from Gene2GOAssociationImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#create(int transform,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */
    public Gene2GOAssociation create( final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        if ( gene2GOAssociation == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.create - 'gene2GOAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( gene2GOAssociation );
        return gene2GOAssociation;
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#find(int, java.lang.String,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    public Gene2GOAssociation find( final java.lang.String queryString,
            final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene2GOAssociation );
        argNames.add( "gene2GOAssociation" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.association.Gene2GOAssociation"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( Gene2GOAssociation ) result;
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#find(int,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    public Gene2GOAssociation find( final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        return this
                .find( "from ubic.gemma.model.association.Gene2GOAssociation as gene2GOAssociation where gene2GOAssociation.gene2GOAssociation = :gene2GOAssociation",
                        gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#findAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<Gene2GOAssociation> findAssociationByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindAssociationByGene( gene );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationDao.findAssociationByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#findByGene(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<VocabCharacteristic> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindByGene( gene );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationDao.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#findByGoTerm(java.lang.String,
     *      ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<Gene> findByGoTerm( final java.lang.String goId,
            final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByGoTerm( goId, taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationDao.findByGoTerm(java.lang.String goId, ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#findByGOTerm(java.util.Collection,
     *      ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<Gene> findByGOTerm( final java.util.Collection goTerms,
            final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByGOTerm( goTerms, taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationDao.findByGOTerm(java.util.Collection goTerms, ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    public Object findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene2GOAssociation );
        argNames.add( "gene2GOAssociation" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.association.Gene2GOAssociation"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#findOrCreate(int,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    public Gene2GOAssociation findOrCreate( final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        return ( Gene2GOAssociation ) this
                .findOrCreate(

                        "from ubic.gemma.model.association.Gene2GOAssociation as gene2GOAssociation where gene2GOAssociation.gene2GOAssociation = :gene2GOAssociation",
                        gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#load(int, java.lang.Long)
     */
    public Gene2GOAssociation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.association.Gene2GOAssociationImpl.class, id );
        return ( ubic.gemma.model.association.Gene2GOAssociation ) entity;
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#loadAll(int)
     */
    public java.util.Collection<? extends Gene2GOAssociation> loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.association.Gene2GOAssociationImpl.class );

        return results;
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.association.Gene2GOAssociation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection<? extends Gene2GOAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#remove(ubic.gemma.model.association.Gene2GOAssociation)
     */
    public void remove( ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        if ( gene2GOAssociation == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.remove - 'gene2GOAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#removeAll()
     */
    public void removeAll() {
        try {
            this.handleRemoveAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationDao.removeAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.association.Gene2GOAssociation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationDao#update(ubic.gemma.model.association.Gene2GOAssociation)
     */
    public void update( ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        if ( gene2GOAssociation == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.update - 'gene2GOAssociation' can not be null" );
        }
        this.getHibernateTemplate().update( gene2GOAssociation );
    }

    /**
     * Performs the core logic for {@link #findAssociationByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection handleFindAssociationByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<VocabCharacteristic> handleFindByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGoTerm(java.lang.String, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection handleFindByGoTerm( java.lang.String goId,
            ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGOTerm(java.util.Collection, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection handleFindByGOTerm( java.util.Collection goTerms,
            ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #removeAll()}
     */
    protected abstract void handleRemoveAll() throws java.lang.Exception;

}