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

import lombok.Value;
import lombok.With;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;

import org.springframework.lang.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;

/**
 * Base class for {@link Persister} implementations.
 * <p>
 * Important note: persisting is a somewhat complicated process, and for some reason we cannot afford to let Hibernate
 * flush changes to the database until the whole operation is completed. This is why we use the {@link FlushMode#MANUAL},
 * manually flush, and we subsequently restore it to the default {@link FlushMode#AUTO} when done.
 *
 * @author pavlidis
 */
public abstract class AbstractPersister implements Persister {

    /**
     * Shared logger for all persisters.
     */
    protected static final Log log = LogFactory.getLog( AbstractPersister.class.getName() );

    /**
     * Size if batch to report when persisting multiple entities with {@link #doPersist(Collection, Caches)}.
     * <p>
     * Implementations can use this to have a consistent batch size when reporting.
     */
    protected static final int REPORT_BATCH_SIZE = 100;

    /**
     * Various caches to refer back to not-yet persisted entities (and thus not easily obtainable from the persistence
     * context).
     */
    @With
    @Value(staticConstructor = "empty")
    protected static class Caches {
        @Nullable
        ArrayDesignsForExperimentCache arrayDesignCache;
        Map<String, ExternalDatabase> externalDatabaseCache = new HashMap<>();
        /**
         * Keys are either string or integers.
         */
        Map<Object, Taxon> taxonCache = new HashMap<>();
        /**
         * Keys are custom hash codes.
         */
        Map<Integer, Chromosome> chromosomeCache = new HashMap<>();
        /**
         * Keys are custom hash codes.
         */
        Map<Integer, BioAssayDimension> bioAssayDimensionCache = new HashMap<>();
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public <T extends Identifiable> T persist( T entity ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            AbstractPersister.log.trace( String.format( "Persisting a %s.", formatEntity( entity ) ) );
            T persistedEntity = doPersist( entity, Caches.empty( null ) );
            sessionFactory.getCurrentSession().flush();
            return persistedEntity;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    @Override
    @Transactional
    public <T extends Identifiable> T persistOrUpdate( T entity ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            AbstractPersister.log.trace( String.format( "Persisting or updating a %s.", formatEntity( entity ) ) );
            T persistedEntity = doPersistOrUpdate( entity, Caches.empty( null ) );
            sessionFactory.getCurrentSession().flush();
            return persistedEntity;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    @Override
    @Transactional
    public <T extends Identifiable> List<T> persist( Collection<T> col ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            AbstractPersister.log.trace( String.format( "Persisting a collection of %d entities.", col.size() ) );
            List<T> result = doPersist( col, Caches.empty( null ) );
            sessionFactory.getCurrentSession().flush();
            return result;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @OverridingMethodsMustInvokeSuper
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        throw new UnsupportedOperationException( String.format( "Don't know how to persist a %s.", formatEntity( entity ) ) );
    }

    protected final <T extends Identifiable> Set<T> doPersist( Set<T> entities, Caches caches ) {
        Set<T> result = new HashSet<>( entities.size() );
        int i = 0;
        for ( T entity : entities ) {
            result.add( this.doPersist( entity, caches ) );
            if ( i++ % REPORT_BATCH_SIZE == 0 ) {
                AbstractPersister.log.debug( String.format( "Persisted %d/%d entities.", result.size(), entities.size() ) );
            }
        }
        return result;
    }

    protected final <T extends Identifiable> List<T> doPersist( Collection<T> entities, Caches caches ) {
        List<T> result = new ArrayList<>( entities.size() );
        int i = 0;
        for ( T entity : entities ) {
            result.add( this.doPersist( entity, caches ) );
            if ( i++ % REPORT_BATCH_SIZE == 0 ) {
                AbstractPersister.log.debug( String.format( "Persisted %d/%d entities.", result.size(), entities.size() ) );
            }
        }
        return result;
    }

    @OverridingMethodsMustInvokeSuper
    protected <T extends Identifiable> T doPersistOrUpdate( T entity, Caches caches ) {
        throw new UnsupportedOperationException( String.format( "Don't know how to persist or update a %s.", formatEntity( entity ) ) );
    }

    private String formatEntity( Object entity ) {
        Class<?> elementClass = Hibernate.getClass( entity );
        // Hibernate 6: getIdentifier() throws TransientObjectException for any entity not in the
        // session (HB 5 returned null). Branch on contains() first so a transient entity reports
        // cleanly instead of throwing out of a debug-log helper. For detached entities (have an id
        // but aren't in this session) we lose the "with ID N" detail — Hibernate 6 has no
        // dependency-free way to read the id off a detached entity short of reflection on the
        // @Id-annotated field, which isn't worth the complexity for a log string.
        org.hibernate.Session session = sessionFactory.getCurrentSession();
        if ( !session.contains( entity ) ) {
            return String.format( "transient or detached %s entity", elementClass.getSimpleName() );
        }
        Object id = session.getIdentifier( entity );
        return String.format( "persistent %s entity with ID %s", elementClass.getSimpleName(), id );
    }
}
