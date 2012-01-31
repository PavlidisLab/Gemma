/*
 * The Gemma project.
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneSet</code>.
 * 
 * @author kelsey
 * @see ubic.gemma.model.genome.gene.GeneSet
 * @version $Id$
 */
@Repository
public class GeneSetDaoImpl extends HibernateDaoSupport implements GeneSetDao {

    @Autowired
    public GeneSetDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.util.Collection)
     */
    @Override
    public Collection<? extends GeneSet> create( final Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.create - 'genesets' can not be null" );
        }

        for ( GeneSet geneSet : entities ) {
            create( geneSet );
        }

        return entities;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    @Override
    public GeneSet create( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.create - 'geneset' can not be null" );
        }
        this.getHibernateTemplate().save( entity );

        return entity;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetDao#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gs from GeneSetImpl gs inner join gs.members m inner join m.gene g where g = :g", "g", gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetDao#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<GeneSet> findByName( String name ) {
        if ( StringUtils.isBlank( name ) ) return new HashSet<GeneSet>();
        return this.getHibernateTemplate().findByNamedParam(
                "select gs from GeneSetImpl gs where gs.name like :name order by gs.name", "name", name + "%" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetDao#findByName(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        if ( StringUtils.isBlank( name ) ) return new HashSet<GeneSet>();
        assert taxon != null;

        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select gs from GeneSetImpl gs join gs.members gm join gm.gene g where g.taxon = :taxon and gs.name like :query order by gs.name",
                        new String[] { "query", "taxon" }, new Object[] { name + "%", taxon } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.util.Collection)
     */
    @Override
    public Collection<? extends GeneSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from GeneSetImpl where id in (:ids)", "ids", ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.lang.Long)
     */
    @Override
    public GeneSet load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( GeneSetImpl.class, id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#loadAll()
     */
    @Override
    public Collection<? extends GeneSet> loadAll() {
        final java.util.Collection<GeneSetImpl> results = this.getHibernateTemplate().loadAll( GeneSetImpl.class );
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetDao#loadAll(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<GeneSet> loadAll( Taxon tax ) {
        if ( tax == null ) return ( Collection<GeneSet> ) this.loadAll();
        return this.getHibernateTemplate().findByNamedParam(
                "select distinct gs from GeneSetImpl gs join gs.members m join m.gene g where g.taxon = :t", "t", tax );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.util.Collection)
     */
    @Override
    public void remove( Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'Collection of geneSet' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Object)
     */
    @Override
    public void remove( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'geneset entity' can not be null" );
        }
        this.getHibernateTemplate().delete( entity );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Long)
     */
    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'id' can not be null" );
        }
        GeneSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.util.Collection)
     */
    @Override
    public void update( final Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.update - 'Collection of geneSets' can not be null" );
        }

        for ( GeneSet geneSet : entities ) {
            update( geneSet );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.lang.Object)
     */
    @Override
    public void update( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.update - 'geneSet' can not be null" );
        }
        this.getHibernateTemplate().update( entity );
    }

    @Override
    public Collection<? extends GeneSet> loadMyGeneSets() {
        return loadAll( );
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets( Taxon tax ) {
        return loadAll( tax );
    }

    @Override
    public Collection<? extends GeneSet> loadMySharedGeneSets() {
        return loadAll();
    }

}
