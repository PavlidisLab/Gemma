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
package ubic.gemma.persistence.service.genome.biosequence;

import org.hibernate.jdbc.Work;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.biosequence.BioSequence</code>.
 *
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
public abstract class BioSequenceDaoBase extends HibernateDaoSupport implements BioSequenceDao {

    /**
     * @see BioSequenceDao#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException( "Error performing 'BioSequenceDao.countAll()' --> " + th, th );
        }
    }

    /**
     * @see BioSequenceDao#create(Collection)
     */
    @Override
    public Collection<? extends BioSequence> create( final Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.create - 'entities' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                for ( BioSequence entity : entities ) {
                    create( entity );
                }
            }
        } );
        return entities;
    }

    /**
     * @see BioSequenceDao#create(Object)
     */
    @Override
    public BioSequence create( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.create - 'bioSequence' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().save( bioSequence );
        return bioSequence;
    }

    /**
     * @see BioSequenceDao#findByGenes(Collection)
     */
    @Override
    public Map<Gene, Collection<BioSequence>> findByGenes( final Collection<Gene> genes ) {
        return this.handleFindByGenes( genes );
    }

    /**
     * @see BioSequenceDao#findByName(java.lang.String)
     */
    @Override
    public Collection<BioSequence> findByName( final java.lang.String name ) {
        return this.handleFindByName( name );
    }

    /**
     * @see BioSequenceDao#getGenesByAccession(java.lang.String)
     */
    @Override
    public Collection<Gene> getGenesByAccession( final java.lang.String search ) {
        return this.handleGetGenesByAccession( search );
    }

    /**
     * @see BioSequenceDao#getGenesByName(java.lang.String)
     */
    @Override
    public Collection<Gene> getGenesByName( final java.lang.String search ) {
        return this.handleGetGenesByName( search );
    }

    /**
     * @see BioSequenceDao#load(java.lang.Long)
     */
    @Override
    public BioSequence load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioSequence.load - 'id' can not be null" );
        }
        return ( BioSequence ) this.getSessionFactory().getCurrentSession().get( BioSequenceImpl.class, id );
    }

    /**
     * @see BioSequenceDao#load(Collection)
     */
    @Override
    public Collection<BioSequence> load( final Collection<Long> ids ) {
        return this.handleLoad( ids );
    }

    /**
     * @see BioSequenceDao#loadAll()
     */
    @Override
    public Collection<? extends BioSequence> loadAll() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( BioSequenceImpl.class ).list();
    }

    /**
     * @see BioSequenceDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.biosequence.BioSequence entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void remove( Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'entities' can not be null" );
        }
        for ( BioSequence b : entities ) {
            this.remove( b );
        }
    }

    @Override
    public void remove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.remove - 'bioSequence' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().delete( bioSequence );
    }

    /**
     * @see BioSequenceDao#thaw(Collection)
     */
    @Override
    public Collection<BioSequence> thaw( final Collection<BioSequence> bioSequences ) {

        return this.handleThaw( bioSequences );

    }

    /**
     * @see BioSequenceDao#thaw(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public BioSequence thaw( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {

        return this.handleThaw( bioSequence );

    }

    @Override
    public void update( final Collection<? extends BioSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioSequence.update - 'entities' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                for ( BioSequence entity : entities ) {
                    update( entity );
                }
            }
        } );

    }

    /**
     * @see BioSequenceDao#update(Object)
     */
    @Override
    public void update( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        if ( bioSequence == null ) {
            throw new IllegalArgumentException( "BioSequence.update - 'bioSequence' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().update( bioSequence );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #findByGenes(Collection)}
     */
    protected abstract Map<Gene, Collection<BioSequence>> handleFindByGenes( Collection<Gene> genes );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract Collection<BioSequence> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #getGenesByAccession(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByAccession( java.lang.String search );

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByName( java.lang.String search );

    /**
     * Performs the core logic for {@link #load(Collection)}
     */
    protected abstract Collection<BioSequence> handleLoad( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract Collection<BioSequence> handleThaw( Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract BioSequence handleThaw( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

}