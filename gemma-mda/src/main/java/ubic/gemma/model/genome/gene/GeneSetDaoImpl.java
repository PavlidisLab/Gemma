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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * @see ubic.gemma.persistence.BaseDao#create(Collection)
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
        if ( StringUtils.isBlank( name ) ) return new HashSet<>();
        return this.getHibernateTemplate().findByNamedParam(
                "select gs from GeneSetImpl gs where gs.name like :name order by gs.name", "name", name + "%" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetDao#findByName(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        StopWatch timer = new StopWatch();
        timer.start();
        if ( StringUtils.isBlank( name ) ) return new HashSet<>();
        assert taxon != null;
        // slow? would it be faster to just findbyname and then restrict taxon?
        List<?> result = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select gs from GeneSetImpl gs join gs.members gm join gm.gene g where g.taxon = :taxon and gs.name like :query order by gs.name",
                        new String[] { "query", "taxon" }, new Object[] { name + "%", taxon } );
        if ( timer.getTime() > 500 )
            log.info( "Find genesets by name took " + timer.getTime() + "ms query=" + name + " taxon=" + taxon );
        return ( Collection<GeneSet> ) result;
    }

    private static Logger log = LoggerFactory.getLogger( GeneSetDaoImpl.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(Collection)
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
        final Collection<GeneSetImpl> results = this.getHibernateTemplate().loadAll( GeneSetImpl.class );
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
     * @see ubic.gemma.persistence.BaseDao#remove(Collection)
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
     * @see ubic.gemma.persistence.BaseDao#update(Collection)
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
        return loadAll();
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets( Taxon tax ) {
        return loadAll( tax );
    }

    @Override
    public Collection<? extends GeneSet> loadMySharedGeneSets() {
        return loadAll();
    }

    @Override
    public Collection<? extends GeneSet> loadMySharedGeneSets( Taxon tax ) {
        return loadAll( tax );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetDao#loadValueObjects(java.util.Collection)
     */
    @Override
    public Collection<? extends DatabaseBackedGeneSetValueObject> loadValueObjects( Collection<Long> ids ) {
        Collection<? extends DatabaseBackedGeneSetValueObject> result = this.loadValueObjectsLite( ids );

        /*
         * Populate gene members - a bit inefficient
         */
        Session sess = this.getSessionFactory().getCurrentSession();

        // inner join is okay here, we only care about ones that have genes.
        for ( GeneSetValueObject res : result ) {
            res.setGeneIds( new HashSet<Long>() );
            res.getGeneIds().addAll(
                    sess.createQuery(
                            "select genes.id from GeneSetImpl g join g.members m join m.gene genes where g.id = :id)" )
                            .setParameter( "id", res.getId() ).list() );

        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetDao#loadValueObjectsLite(java.util.Collection)
     */
    @Override
    public Collection<? extends DatabaseBackedGeneSetValueObject> loadValueObjectsLite( Collection<Long> ids ) {
        Collection<DatabaseBackedGeneSetValueObject> result = new HashSet<>();

        if ( ids.isEmpty() ) return result;

        // Left join: includes one that have no members. Caller has to filter them out if they need to.
        Session sess = this.getSessionFactory().getCurrentSession();
        List<Object[]> list = sess
                .createQuery(
                        "select g.id, g.description, count(m), g.name from GeneSetImpl g"
                                + " left join g.members m where g.id in (:ids) group by g.id" )
                .setParameterList( "ids", ids ).list();

        Map<Long, Taxon> taxa = getTaxa( ids );

        for ( Object[] oa : list ) {

            DatabaseBackedGeneSetValueObject dvo = new DatabaseBackedGeneSetValueObject();
            dvo.setDescription( ( String ) oa[1] );
            dvo.setId( ( Long ) oa[0] );
            dvo.setSize( ( ( Long ) oa[2] ).intValue() );
            dvo.setName( ( String ) oa[3] );

            Taxon t = taxa.get( dvo.getId() );
            dvo.setTaxonId( t.getId() );
            dvo.setTaxonName( t.getCommonName() );
            result.add( dvo );
        }

        return result;
    }

    /**
     * @param ids
     * @return
     */
    private Map<Long, Taxon> getTaxa( Collection<Long> ids ) {
        // fast
        Query q = this
                .getSessionFactory()
                .getCurrentSession()
                .createQuery(
                        "select distinct gs.id, t from GeneSetImpl gs join gs.members m"
                                + " join m.gene g join g.taxon t where gs.id in (:ids) group by gs.id" );
        q.setParameterList( "ids", ids );

        Map<Long, Taxon> result = new HashMap<>();
        for ( Object o : q.list() ) {
            Object[] oa = ( Object[] ) o;

            if ( result.containsKey( oa[0] ) ) {
                throw new IllegalStateException( "More than one taxon in gene  set id= " + oa[0] );
            }

            result.put( ( Long ) oa[0], ( Taxon ) oa[1] );

        }
        // for ( Long id : ids ) {
        // result.put( id, this.getTaxon( id ) );
        // }
        return result;
    }

    @Override
    public int getGeneCount( Long id ) {
        List<?> o = this.getHibernateTemplate().findByNamedParam(
                "select count(i) from GeneSetImpl g join g.members i where g.id = id", "id", id );

        for ( Object object : o ) {
            Object[] oa = ( Object[] ) object;
            return ( ( Long ) oa[1] ).intValue();
        }

        return 0;

    }

    @Override
    public Taxon getTaxon( Long id ) {
        // get one gene, check the taxon.
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select g from GeneSetImpl gs join gs.members m join m.gene g where gs.id = :id" );
        q.setParameter( "id", id );
        q.setMaxResults( 1 );

        Gene g = ( Gene ) q.uniqueResult();
        if ( g == null ) {
            return null;
        }
        return g.getTaxon();
    }

}
