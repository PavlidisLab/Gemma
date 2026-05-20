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

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.EeWriteService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentPrePersistService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 * <p>
 * Persister-shrink S2e: the multi-level inheritance chain
 * (PHI → RelationshipPersister → ArrayDesignPersister → GenomePersister → CommonPersister →
 * AbstractPersister) is gone. PHI is now a standalone {@code @Service} that owns the
 * {@link FlushMode#MANUAL} window on the public entry points and dispatches in turn through
 * each typed {@code @Autowired} persister bean:
 * EE arms (via {@link EeWriteServiceImpl}) → {@link RelationshipPersister#doRelationship}
 * → {@link ArrayDesignPersister#doArrayDesign} → {@link GenomePersister#doGenome} →
 * {@link CommonPersister#doCommon} → throws.
 * <p>
 * Phase 3 persister-retirement note: the audit-trail priming that used to live in {@code doPersist}
 * has moved to {@link ubic.gemma.persistence.audit.AuditTrailEventListener}, a Hibernate {@code PERSIST}
 * event listener that runs ahead of cascade. Every {@code session.persist} of an {@code Auditable} now
 * flows through that single chokepoint.
 * <p>
 * Persister-shrink S1: the former {@code ExpressionPersister} class has been folded into this class.
 * Production callers ({@code CellXGeneDataLoaderServiceImpl}, {@code SplitExperimentServiceImpl},
 * {@code DifferentialExpressionAnalysisHelperServiceImpl}) have been cut over to
 * {@link EeWriteService#create} / {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService#findOrCreate}.
 * The EE/BioAssay/BioMaterial/BAD/Compound/EESubSet arms in {@link #doPersist} are now used only by
 * test fixtures ({@code PersistentDummyObjectHelper}, {@code TwoChannelMissingValuesTest}); they
 * delegate one line into {@link EeWriteServiceImpl} via an AOP-unwrap helper and will go away when
 * those fixtures migrate (tracked by junit5-batch agents).
 *
 * @author pavlidis
 * @author keshav
 */
@Service
public class PersisterHelperImpl implements PersisterHelper {

    /**
     * Autowired as the {@link EeWriteService} interface (not the {@code Impl})
     * so Spring can inject the {@code @Transactional} JDK proxy without a
     * {@code BeanNotOfRequiredTypeException}. The package-private dispatch
     * methods on {@link EeWriteServiceImpl} ({@code persistExpressionExperiment},
     * {@code persistBioAssay}, etc.) are not on the interface; reach them via
     * {@link #eeWriteServiceImpl()} which unwraps the proxy.
     */
    @Autowired
    private EeWriteService eeWriteService;

    @Autowired
    private ExpressionExperimentPrePersistService expressionExperimentPrePersistService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private CommonPersister commonPersister;

    @Autowired
    private GenomePersister genomePersister;

    @Autowired
    private ArrayDesignPersister arrayDesignPersister;

    @Autowired
    private RelationshipPersister relationshipPersister;

    SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Returns the underlying {@link EeWriteServiceImpl}, unwrapping the Spring
     * AOP proxy if necessary. Needed to reach the package-private dispatch
     * helpers ({@code persistExpressionExperiment}, {@code persistBioAssay},
     * {@code persistBioAssayDimension}, {@code persistBioMaterial},
     * {@code persistCompound}, {@code persistExpressionExperimentSubSet}) used
     * by the {@link #doPersist} dispatch table for the remaining test-fixture
     * callers.
     */
    private EeWriteServiceImpl eeWriteServiceImpl() {
        Object target = AopProxyUtils.getSingletonTarget( eeWriteService );
        return ( EeWriteServiceImpl ) ( target != null ? target : eeWriteService );
    }

    /**
     * @deprecated use {@link EeWriteService#create(ExpressionExperiment, ArrayDesignsForExperimentCache)}
     * directly. Retained only so test fixtures that hit
     * {@link PersisterHelper#persist(ExpressionExperiment, ArrayDesignsForExperimentCache)}
     * keep working.
     */
    @Override
    @Deprecated
    @Transactional
    public ExpressionExperiment persist( ExpressionExperiment ee, @Nullable ArrayDesignsForExperimentCache cachedArrays ) {
        return eeWriteService.create( ee, cachedArrays );
    }

    /**
     * @deprecated use {@link ExpressionExperimentPrePersistService#prepare(ExpressionExperiment)}
     * directly. Retained only so test fixtures that hit
     * {@link PersisterHelper#prepare(ExpressionExperiment)} keep working.
     */
    @Override
    @Deprecated
    @Secured("GROUP_USER")
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee ) {
        return expressionExperimentPrePersistService.prepare( ee );
    }

    /**
     * Persister-shrink S2e: PHI owns the {@link FlushMode#MANUAL} window on the
     * polymorphic public entry points. Inside the window, {@link #doPersist} dispatches
     * to typed beans in order: EE arms → Relationship → ArrayDesign → Genome → Common.
     */
    @Override
    @Transactional
    public <T extends Identifiable> T persist( T entity ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            T persistedEntity = doPersist( entity, new HashMap<>() );
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
            T persistedEntity = doPersistOrUpdate( entity, new HashMap<>() );
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
            List<T> result = doPersist( col, new HashMap<>() );
            sessionFactory.getCurrentSession().flush();
            return result;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    /**
     * Cascade helper formerly on {@code AbstractPersister} as {@code protected final};
     * reached by {@link EeWriteServiceImpl} for {@link DatabaseEntry} characteristics
     * collections. Does NOT manage FlushMode — the caller's outer FlushMode window
     * (from {@link EeWriteServiceImpl#create}) is in effect.
     */
    final <T extends Identifiable> List<T> doPersist( Collection<T> entities, Map<String, ExternalDatabase> xdbCache ) {
        List<T> result = new ArrayList<>( entities.size() );
        for ( T entity : entities ) {
            result.add( this.doPersist( entity, xdbCache ) );
        }
        return result;
    }

    /**
     * Persister-shrink S2e dispatch table. EE arms reach {@link EeWriteServiceImpl}
     * through the AOP-unwrap helper {@link #eeWriteServiceImpl()}; the typed-bean
     * arms ({@link RelationshipPersister#doRelationship},
     * {@link ArrayDesignPersister#doArrayDesign}, {@link GenomePersister#doGenome},
     * {@link CommonPersister#doCommon}) each return {@code null} when the entity
     * isn't theirs, so we fall through in turn and throw at the bottom.
     */
    @SuppressWarnings("unchecked")
    <T extends Identifiable> T doPersist( T entity, Map<String, ExternalDatabase> xdbCache ) {
        EeWriteServiceImpl impl = eeWriteServiceImpl();
        Map<Object, Taxon> taxonCache = new HashMap<>();
        if ( entity instanceof ExpressionExperiment ) {
            CommonPersister.log.warn( "Consider doing the 'prepare' step in a separate transaction." );
            ArrayDesignsForExperimentCache adCache = this.prepare( ( ExpressionExperiment ) entity );
            return ( T ) impl.persistExpressionExperiment( ( ExpressionExperiment ) entity, xdbCache, adCache, taxonCache );
        } else if ( entity instanceof BioAssayDimension ) {
            return ( T ) impl.persistBioAssayDimension( ( BioAssayDimension ) entity, xdbCache, null, taxonCache );
        } else if ( entity instanceof BioMaterial ) {
            return ( T ) impl.persistBioMaterial( ( BioMaterial ) entity, xdbCache, taxonCache );
        } else if ( entity instanceof BioAssay ) {
            return ( T ) impl.persistBioAssay( ( BioAssay ) entity, xdbCache, null, taxonCache );
        } else if ( entity instanceof Compound ) {
            return ( T ) impl.persistCompound( ( Compound ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return ( T ) impl.persistExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) entity );
        }
        T rel = relationshipPersister.doRelationship( entity, xdbCache );
        if ( rel != null ) {
            return rel;
        }
        T ad = ( T ) arrayDesignPersister.doArrayDesign( entity, xdbCache );
        if ( ad != null ) {
            return ad;
        }
        T genome = genomePersister.doGenome( entity, xdbCache );
        if ( genome != null || entity instanceof Taxon ) {
            return genome;
        }
        T common = ( T ) commonPersister.doCommon( entity, xdbCache );
        if ( common != null || entity instanceof ubic.gemma.model.common.description.Characteristic
                || entity instanceof ubic.gemma.model.common.auditAndSecurity.User ) {
            return common;
        }
        throw new UnsupportedOperationException( String.format( "Don't know how to persist a %s.", entity.getClass().getSimpleName() ) );
    }

    /**
     * Persist-or-update dispatch table. Only Genome owns update arms today
     * (BioSequence, Gene, GeneProduct); the other layers throw via the chain
     * fall-through here.
     */
    @SuppressWarnings("unchecked")
    <T extends Identifiable> T doPersistOrUpdate( T entity, Map<String, ExternalDatabase> xdbCache ) {
        T genome = genomePersister.doGenomeUpdate( entity, xdbCache );
        if ( genome != null ) {
            return genome;
        }
        throw new UnsupportedOperationException( String.format( "Don't know how to persist or update a %s.", entity.getClass().getSimpleName() ) );
    }

}
