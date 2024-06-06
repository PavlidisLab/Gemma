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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.Serializable;
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
        Map<Integer, QuantitationType> quantitationTypeCache = new HashMap<>();
        Map<String, BioAssayDimension> bioAssayDimensionCache = new HashMap<>();
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public Object persist( Object entity ) {
        try {
            sessionFactory.getCurrentSession().setFlushMode( FlushMode.MANUAL );
            AbstractPersister.log.trace( String.format( "Persisting a %s.", formatEntity( entity ) ) );
            Object persistedEntity = doPersist( entity, Caches.empty( null ) );
            sessionFactory.getCurrentSession().flush();
            return persistedEntity;
        } finally {
            sessionFactory.getCurrentSession().setFlushMode( FlushMode.AUTO );
        }
    }

    @Override
    @Transactional
    public Object persistOrUpdate( Object entity ) {
        try {
            sessionFactory.getCurrentSession().setFlushMode( FlushMode.MANUAL );
            AbstractPersister.log.trace( String.format( "Persisting or updating a %s.", formatEntity( entity ) ) );
            Object persistedEntity = doPersistOrUpdate( entity, Caches.empty( null ) );
            sessionFactory.getCurrentSession().flush();
            return persistedEntity;
        } finally {
            sessionFactory.getCurrentSession().setFlushMode( FlushMode.AUTO );
        }
    }

    @Override
    @Transactional
    public List<?> persist( Collection<?> col ) {
        try {
            sessionFactory.getCurrentSession().setFlushMode( FlushMode.MANUAL );
            AbstractPersister.log.trace( String.format( "Persisting a collection of %d entities.", col.size() ) );
            List<?> result = doPersist( col, Caches.empty( null ) );
            sessionFactory.getCurrentSession().flush();
            return result;
        } finally {
            sessionFactory.getCurrentSession().setFlushMode( FlushMode.AUTO );
        }
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @OverridingMethodsMustInvokeSuper
    protected Object doPersist( Object entity, Caches caches ) {
        throw new UnsupportedOperationException( String.format( "Don't know how to persist a %s.", formatEntity( entity ) ) );
    }

    protected final List<?> doPersist( Collection<?> entities, Caches caches ) {
        List<Object> result = new ArrayList<>( entities.size() );
        int i = 0;
        for ( Object entity : entities ) {
            result.add( this.doPersist( entity, caches ) );
            if ( i++ % REPORT_BATCH_SIZE == 0 ) {
                AbstractPersister.log.debug( String.format( "Persisted %d/%d entities.", result.size(), entities.size() ) );
            }
        }
        return result;
    }

    @OverridingMethodsMustInvokeSuper
    protected Object doPersistOrUpdate( Object entity, Caches caches ) {
        throw new UnsupportedOperationException( String.format( "Don't know how to persist or update a %s.", formatEntity( entity ) ) );
    }

    private String formatEntity( Object entity ) {
        Class<?> elementClass = Hibernate.getClass( entity );
        ClassMetadata classMetadata = sessionFactory.getClassMetadata( elementClass );
        if ( classMetadata == null ) {
            throw new IllegalArgumentException( String.format( "Entity %s is not managed by Hibernate.", elementClass.getName() ) );
        }
        Serializable id = classMetadata.getIdentifier( entity, ( SessionImplementor ) getSessionFactory().getCurrentSession() );
        if ( id == null ) {
            return String.format( String.format( "transient %s entity", entity.getClass().getSimpleName() ) );
        } else if ( sessionFactory.getCurrentSession().contains( entity ) ) {
            return String.format( String.format( "persistent %s entity with ID %s", entity.getClass().getSimpleName(), id ) );
        } else {
            return String.format( String.format( "detached %s entity with ID %s", entity.getClass().getSimpleName(), id ) );
        }
    }
}
