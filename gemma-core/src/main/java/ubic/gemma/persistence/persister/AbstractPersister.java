/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.persister;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.SessionImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.persistence.util.EntityUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;

/**
 * Base class for persisters.
 *
 * @author pavlidis
 */
public abstract class AbstractPersister<T> extends HibernateDaoSupport implements Persister<T> {

    protected static final Log log = LogFactory.getLog( AbstractPersister.class.getName() );
    /**
     * Collections smaller than this don't result in logging about progress.
     */
    private static final int MINIMUM_COLLECTION_SIZE_FOR_NOTFICATIONS = 500;
    /**
     * How many times per collection to update us (at most)
     */
    private static final int COLLECTION_INFO_FREQUENCY = 10;

    @Autowired
    public AbstractPersister( SessionFactory sessionFactory ) {
        setSessionFactory( sessionFactory );
    }

    @Override
    @Transactional
    public <S extends T> Collection<S> persist( Collection<S> col ) {
        if ( col == null || col.size() == 0 )
            return col;

        Collection<S> result = new HashSet<>();
        try {
            int count = 0;
            AbstractPersister.log
                    .debug( "Entering + " + this.getClass().getName() + ".persist() with " + col.size() + " objects." );
            int numElementsPerUpdate = this.numElementsPerUpdate( col );
            for ( S entity : col ) {
                if ( AbstractPersister.log.isDebugEnabled() ) {
                    AbstractPersister.log.debug( "Persisting: " + entity );
                }
                result.add( this.persist( entity ) );
                count = this.iteratorStatusUpdate( col, count, numElementsPerUpdate, true );

                if ( Thread.interrupted() ) {
                    AbstractPersister.log.info( "Cancelled" );
                    break;
                }

            }
            this.iteratorStatusUpdate( col, count, numElementsPerUpdate, false );
        } catch ( Exception e ) {
            AbstractPersister.log.fatal( "Error while persisting collection: ", e );
            throw new RuntimeException( e );
        }
        return result;
    }

    @Override
    public <S extends T> S persistOrUpdate( S entity ) {
        if ( entity == null )
            return null;
        throw new UnsupportedOperationException( "Don't know how to persistOrUpdate a " + entity.getClass().getName() );
    }

    @Override
    @Transactional
    public boolean isTransient( T entity ) {
        if ( entity == null )
            return true;
        Long id = EntityUtils.getId( entity );

        if ( id == null )
            return true; // assume.

        /*
         * We normally won't get past this point; the case where it might is when the transaction has been rolled back
         * and is being retried.
         */

        if ( EntityUtils.isProxy( entity ) ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( "Object is a proxy: " + entity.getClass().getSimpleName() + ":" + id );
            return false;
        }

        org.hibernate.Session session = this.getSessionFactory().getCurrentSession();
        if ( session.contains( entity ) ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log
                        .debug( "Found object in session: " + entity.getClass().getSimpleName() + ":" + id );
            return false;
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter // Getting desperate ...
        synchronized ( entity ) {
            Session sess = this.getSessionFactory().openSession();
            sess.setFlushMode( FlushMode.MANUAL );
            Object pe = sess.get( entity.getClass(), id );
            sess.close();
            if ( pe != null ) {
                // Common case.
                if ( AbstractPersister.log.isDebugEnabled() )
                    AbstractPersister.log
                            .debug( "Found object in store: " + entity.getClass().getSimpleName() + ":" + id );
                return false;
            }
        }

        /*
         * Hibernate has a method that, pretty much, does what we've done so far ... but probably does it better.
         */
        String bestGuessEntityName = ( ( SessionImplementor ) session ).bestGuessEntityName( entity );
        if ( ForeignKeys.isNotTransient( bestGuessEntityName, entity, null, ( SessionImplementor ) session ) ) {
            AbstractPersister.log.info( "Hibernate says object is not transient: " + bestGuessEntityName + ":" + id );
            return false;
        }

        /*
         * The ID is filled in, but it probably is a survivor of a rolled-back transaction. It doesn't matter what we
         * return, it's not guaranteed to be right.
         */
        AbstractPersister.log
                .info( "Object has ID but we can't tell if it is persistent: " + entity.getClass().getSimpleName() + ":"
                        + id );
        return true;

    }

    int numElementsPerUpdate( Collection<?> col ) {
        if ( col == null || col.size() < AbstractPersister.COLLECTION_INFO_FREQUENCY )
            return Integer.MAX_VALUE;
        return Math.max( ( int ) Math.ceil( col.size() / ( double ) AbstractPersister.COLLECTION_INFO_FREQUENCY ), 20 );
    }

    @Override
    public void persistCollectionElements( Collection<? extends T> collection ) {
        if ( collection == null )
            return;
        if ( collection.size() == 0 )
            return;

        try {
            StopWatch t = new StopWatch();
            t.start();
            int c = 0;
            for ( T object : collection ) {
                if ( !this.isTransient( object ) )
                    continue;
                T persistedObj = this.persist( object );

                c++;

                if ( t.getTime() > 5000 ) {
                    AbstractPersister.log
                            .info( "Persist " + c + " elements: " + t.getTime() + "ms since last check (last class="
                                    + object.getClass().getSimpleName() + ")" );
                    c = 0;
                    t.reset();
                    t.start();
                }

                if ( persistedObj == null )
                    continue;
                BeanUtils.setProperty( object, "id", BeanUtils.getSimpleProperty( persistedObj, "id" ) );
                assert BeanUtils.getSimpleProperty( object, "id" ) != null;

            }
        } catch ( IllegalAccessException | NoSuchMethodException | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }

        // collection = persistedCollection;
    }

    private int iteratorStatusUpdate( Collection<? extends T> col, int count, int numElementsPerUpdate, boolean increment ) {
        assert col != null && col.size() > 0;
        if ( increment )
            ++count;

        if ( col.size() >= AbstractPersister.MINIMUM_COLLECTION_SIZE_FOR_NOTFICATIONS && AbstractPersister.log
                .isInfoEnabled() && ( !increment || count % numElementsPerUpdate == 0 ) ) {
            String collectionItemsClassName = col.iterator().next().getClass().getName();
            AbstractPersister.log
                    .info( "Processed " + count + "/" + col.size() + " " + collectionItemsClassName + "'s" );
        }
        return count;
    }

}
