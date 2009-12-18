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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

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

    public Collection<? extends GeneSet> create( final Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.create - 'genesets' can not be null" );
        }

        for ( GeneSet geneSet : entities ) {
            create( geneSet );
        }

        return entities;
    }

    public GeneSet create( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.create - 'geneset' can not be null" );
        }
        this.getHibernateTemplate().save( entity );

        return entity;

    }

    @SuppressWarnings("unchecked")
    public Collection<? extends GeneSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from GeneSetImpl where id in (:ids)", "ids", ids );
    }

    public GeneSet load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CandidateGeneList.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( GeneSet.class, id );

    }

    public Collection<? extends GeneSet> loadAll() {
        final java.util.Collection<GeneSetImpl> results = this.getHibernateTemplate().loadAll( GeneSetImpl.class );
        return results;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.persistence.BaseDao#remove(java.util.Collection)
     */
    public void remove( Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'Collection of geneSet' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Long)
     */
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
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Object)
     */
    public void remove( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.remove - 'geneset entity' can not be null" );
        }
        this.getHibernateTemplate().delete( entity );

    }

    public void update( final Collection<? extends GeneSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneSet.update - 'Collection of geneSets' can not be null" );
        }

        for ( GeneSet geneSet : entities ) {
            update( geneSet );
        }

    }

    public void update( GeneSet entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( "GeneSet.update - 'geneSet' can not be null" );
        }
        this.getHibernateTemplate().update( entity );
    }

}
