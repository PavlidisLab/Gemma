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
package ubic.gemma.persistence;

import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.classic.Session;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.SessionImplementor;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.EntityUtils;

/**
 * Base class for persisters.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractPersister extends HibernateDaoSupport implements Persister {

    /**
     * How many times per collection to update us (at most)
     */
    private static final int COLLECTION_INFO_FREQUENCY = 10;

    protected static Log log = LogFactory.getLog( AbstractPersister.class.getName() );

    /**
     * This should match the JDBC batch size for Hibernate.
     */
    protected static final int SESSION_BATCH_SIZE = ConfigUtils.getInt( "gemma.hibernate.jdbc_batch_size" );

    /**
     * Collections smaller than this don't result in logging about progress.
     */
    public static final int MINIMUM_COLLECTION_SIZE_FOR_NOTFICATIONS = 500;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.Persister#isTransient(java.lang.Object)
     */
    @Override
    public boolean isTransient( Object entity ) {
        if ( entity == null ) return true;
        Long id = EntityUtils.getId( entity );

        if ( id == null ) return true; // assume.

        /*
         * We normally won't get past this point; the case where it might is when the transaction has been rolled back
         * and is being retried.
         */

        if ( EntityUtils.isProxy( entity ) ) {
            if ( log.isDebugEnabled() )
                log.debug( "Object is a proxy: " + entity.getClass().getSimpleName() + ":" + id );
            return false;
        }

        org.hibernate.Session session = this.getSessionFactory().getCurrentSession();
        if ( session.contains( entity ) ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found object in session: " + entity.getClass().getSimpleName() + ":" + id );
            return false;
        }

        // Getting desperate ...
        synchronized ( entity ) {
            Session sess = this.getSessionFactory().openSession();
            sess.setFlushMode( FlushMode.MANUAL );
            Object pe = sess.get( entity.getClass(), id );
            sess.close();
            if ( pe != null ) {
                // Common case.
                if ( log.isDebugEnabled() )
                    log.debug( "Found object in store: " + entity.getClass().getSimpleName() + ":" + id );
                return false;
            }
        }

        /*
         * Hibernate has a method that, pretty much, does what we've done so far ... but probably does it better.
         */
        String bestGuessEntityName = ( ( SessionImplementor ) session ).bestGuessEntityName( entity );
        if ( ForeignKeys.isNotTransient( bestGuessEntityName, entity, null, ( SessionImplementor ) session ) ) {
            log.info( "Hibernate says object is not transient: " + bestGuessEntityName + ":" + id );
            return false;
        }

        /*
         * The ID is filled in, but it probably is a survivor of a rolled-back transaction. It doesn't matter what we
         * return, it's not guaranteed to be right.
         */
        log.info( "Object has ID but we can't tell if it is persistent: " + entity.getClass().getSimpleName() + ":"
                + id );
        return true;

    }

    /*
     * @see ubic.gemma.model.loader.loaderutils.Loader#create(java.util.Collection)
     */
    @Override
    public Collection<?> persist( Collection<?> col ) {
        if ( col == null || col.size() == 0 ) return col;

        Collection<Object> result = new HashSet<Object>();
        try {
            int count = 0;
            log.debug( "Entering + " + this.getClass().getName() + ".persist() with " + col.size() + " objects." );
            int numElementsPerUpdate = numElementsPerUpdate( col );
            for ( Object entity : col ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "Persisting: " + entity );
                }
                result.add( persist( entity ) );
                count = iteratorStatusUpdate( col, count, numElementsPerUpdate, true );

                if ( Thread.interrupted() ) {
                    log.info( "Cancelled" );
                    break;
                }

            }
            iteratorStatusUpdate( col, count, numElementsPerUpdate, false );
        } catch ( Exception e ) {
            log.fatal( "Error while persisting collection: ", e );
            throw new RuntimeException( e );
        }
        return result;
    }

    /**
     * @param startTime
     * @return
     */
    protected double elapsedMinutes( long startTime ) {
        return Double.parseDouble( NumberFormat.getNumberInstance().format(
                0.001 * ( System.currentTimeMillis() - startTime ) / 60.0 ) );
    }

    /**
     * @param col
     * @param count
     * @param numElementsPerUpdate
     * @return
     */
    protected int iteratorStatusUpdate( Collection<?> col, int count, int numElementsPerUpdate, boolean increment ) {
        assert col != null && col.size() > 0;
        if ( increment ) ++count;

        if ( col.size() >= MINIMUM_COLLECTION_SIZE_FOR_NOTFICATIONS && log.isInfoEnabled()
                && ( !increment || count % numElementsPerUpdate == 0 ) ) {
            String collectionItemsClassName = col.iterator().next().getClass().getName();
            log.info( "Processed " + count + "/" + col.size() + " " + collectionItemsClassName + "'s" );
        }
        return count;
    }

    protected int numElementsPerUpdate( Collection<?> col ) {
        if ( col == null || col.size() < COLLECTION_INFO_FREQUENCY ) return Integer.MAX_VALUE;
        return Math.max( ( int ) Math.ceil( col.size() / ( double ) COLLECTION_INFO_FREQUENCY ), 20 );
    }

    /**
     * Persist the elements in a collection.
     * <p>
     * This method is necessary because in-place persisting does not work.
     * 
     * @param collection
     * @return
     */
    protected void persistCollectionElements( Collection<?> collection ) {
        if ( collection == null ) return;
        if ( collection.size() == 0 ) return;

        try {
            StopWatch t = new StopWatch();
            t.start();
            int c = 0;
            for ( Object object : collection ) {
                if ( !isTransient( object ) ) continue;
                Object persistedObj = persist( object );

                c++;

                if ( t.getTime() > 5000 ) {
                    log.info( "Persist " + c + " elements: " + t.getTime() + "ms since last check (last class="
                            + object.getClass().getSimpleName() + ")" );
                    c = 0;
                    t.reset();
                    t.start();
                }

                if ( persistedObj == null ) continue;
                BeanUtils.setProperty( object, "id", BeanUtils.getSimpleProperty( persistedObj, "id" ) );
                assert BeanUtils.getSimpleProperty( object, "id" ) != null;

            }
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }

        // collection = persistedCollection;
    }

    /**
     * Persist or update the elements in a collection.
     * <p>
     * This method is necessary because in-place persisting does not work.
     * 
     * @param collection
     * @return
     */
    protected void persistOrUpdateCollectionElements( Collection<?> collection ) {
        if ( collection == null ) return;
        if ( collection.size() == 0 ) return;

        try {
            for ( Object object : collection ) {
                Object persistedObj = persistOrUpdate( object );
                if ( persistedObj == null ) continue;
                BeanUtils.setProperty( object, "id", BeanUtils.getSimpleProperty( persistedObj, "id" ) );
                assert BeanUtils.getSimpleProperty( object, "id" ) != null;
            }
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

}
