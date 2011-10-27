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
package ubic.gemma.model.genome;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.PredictedGene</code>.
 * 
 * @see ubic.gemma.model.genome.PredictedGene
 */
public abstract class PredictedGeneDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.PredictedGeneDao {

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#create(int, java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PredictedGene.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.genome.PredictedGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#create(int transform, ubic.gemma.model.genome.PredictedGene)
     */
    public PredictedGene create( final ubic.gemma.model.genome.PredictedGene predictedGene ) {
        if ( predictedGene == null ) {
            throw new IllegalArgumentException( "PredictedGene.create - 'predictedGene' can not be null" );
        }
        this.getHibernateTemplate().save( predictedGene );
        return predictedGene;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#find(int, java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public Object find( final java.lang.String queryString, final ubic.gemma.model.genome.Gene gene ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene );
        argNames.add( "gene" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.Gene"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#find(int, ubic.gemma.model.genome.Gene)
     */

    public Object find( final ubic.gemma.model.genome.Gene gene ) {
        return this.find(
                "from ubic.gemma.model.genome.PredictedGene as predictedGene where predictedGene.gene = :gene", gene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByNcbiId(int, java.lang.String)
     */

    public Gene findByNcbiId( final Integer ncbiId ) {
        return this.findByNcbiId( "from GeneImpl g where g.ncbiGeneId = :ncbiId", ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    public Gene findByNcbiId( final java.lang.String queryString, final Integer ncbiId ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( ncbiId );
        argNames.add( "ncbiId" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        if ( results.isEmpty() ) return null;
        return ( Gene ) results.iterator().next();
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(int, java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( final int transform, final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( transform,
                "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName", officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( final int transform, final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, queryString, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialName(int, java.lang.String)
     */

    public java.util.Collection findByOfficialName( final java.lang.String officialName ) {
        return this.findByOfficialName( "from GeneImpl g where g.officialName=:officialName order by g.officialName",
                officialName );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialName(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficialName( final java.lang.String queryString,
            final java.lang.String officialName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialName );
        argNames.add( "officialName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialSymbolInexact(int, java.lang.String)
     */

    public java.util.Collection findByOfficialSymbolInexact( final java.lang.String officialSymbol ) {
        return this
                .findByOfficialSymbolInexact(
                        "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                        officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialSymbolInexact(int, java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection findByOfficialSymbolInexact( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */
    public java.util.Collection findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( location );
        argNames.add( "location" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByPhysicalLocation(int,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection findByPhysicalLocation( final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation(
                "from ubic.gemma.model.genome.PredictedGene as predictedGene where predictedGene.location = :location",
                location );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findOrCreate(int, java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public Object findOrCreate( final java.lang.String queryString, final ubic.gemma.model.genome.Gene gene ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene );
        argNames.add( "gene" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.Gene"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findOrCreate(int, ubic.gemma.model.genome.Gene)
     */

    public Object findOrCreate( final ubic.gemma.model.genome.Gene gene ) {
        return this.findOrCreate(
                "from ubic.gemma.model.genome.PredictedGene as predictedGene where predictedGene.gene = :gene", gene );
    }

    public abstract PredictedGene geneValueObjectToEntity( GeneValueObject geneValueObject );

    public Collection<? extends PredictedGene> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from PredictedGeneImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#load(int, java.lang.Long)
     */

    public PredictedGene load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PredictedGene.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.PredictedGeneImpl.class, id );
        return ( PredictedGene ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#loadAll(int)
     */

    public java.util.Collection loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.PredictedGeneImpl.class );

        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PredictedGene.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.PredictedGene entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PredictedGene.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#remove(ubic.gemma.model.genome.PredictedGene)
     */
    public void remove( ubic.gemma.model.genome.PredictedGene predictedGene ) {
        if ( predictedGene == null ) {
            throw new IllegalArgumentException( "PredictedGene.remove - 'predictedGene' can not be null" );
        }
        this.getHibernateTemplate().delete( predictedGene );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PredictedGene.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.PredictedGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#update(ubic.gemma.model.genome.PredictedGene)
     */
    public void update( ubic.gemma.model.genome.PredictedGene predictedGene ) {
        if ( predictedGene == null ) {
            throw new IllegalArgumentException( "PredictedGene.update - 'predictedGene' can not be null" );
        }
        this.getHibernateTemplate().update( predictedGene );
    }

}