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

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
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
import java.util.HashMap;
import java.util.Map;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
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
public class PersisterHelperImpl extends RelationshipPersister implements PersisterHelper {

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

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Map<String, ExternalDatabase> xdbCache ) {
        EeWriteServiceImpl impl = eeWriteServiceImpl();
        Map<Object, Taxon> taxonCache = new HashMap<>();
        if ( entity instanceof ExpressionExperiment ) {
            AbstractPersister.log.warn( "Consider doing the 'prepare' step in a separate transaction." );
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
        } else {
            return super.doPersist( entity, xdbCache );
        }
    }

}
