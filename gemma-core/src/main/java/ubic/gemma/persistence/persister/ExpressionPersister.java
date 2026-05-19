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
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.experiment.EeWriteService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentPrePersistService;

import javax.annotation.Nullable;

/**
 * Historical persister entry point for {@link ExpressionExperiment} graphs.
 * <p>
 * Phase 3 (strangler-fig): all body methods have been relocated to
 * {@link EeWriteServiceImpl}. This class is now a thin delegate that keeps the
 * {@link PersisterHelper} dispatch surface ({@link #doPersist}, {@link #persist})
 * working while callers migrate to {@link ubic.gemma.persistence.service.expression.experiment.EeWriteService}
 * directly (chunk E4). The class is scheduled for deletion in chunk E5.
 *
 * @author pavlidis
 * @see EeWriteServiceImpl
 */
public abstract class ExpressionPersister extends ArrayDesignPersister implements PersisterHelper {

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
     * by the strangler-fig {@link #doPersist} dispatch table. Goes away with
     * the persister chain in E5.
     */
    private EeWriteServiceImpl eeWriteServiceImpl() {
        Object target = AopProxyUtils.getSingletonTarget( eeWriteService );
        return ( EeWriteServiceImpl ) ( target != null ? target : eeWriteService );
    }

    @Override
    @Transactional
    public ExpressionExperiment persist( ExpressionExperiment ee, @Nullable ArrayDesignsForExperimentCache cachedArrays ) {
        return eeWriteService.create( ee, cachedArrays );
    }

    @Secured("GROUP_USER")
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee ) {
        return expressionExperimentPrePersistService.prepare( ee );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        EeWriteServiceImpl impl = eeWriteServiceImpl();
        if ( entity instanceof ExpressionExperiment ) {
            if ( caches.getArrayDesignCache() == null ) {
                AbstractPersister.log.warn( "Consider doing the 'prepare' step in a separate transaction." );
                caches = caches.withArrayDesignCache( this.prepare( ( ExpressionExperiment ) entity ) );
            }
            return ( T ) impl.persistExpressionExperiment( ( ExpressionExperiment ) entity, caches );
        } else if ( entity instanceof BioAssayDimension ) {
            return ( T ) impl.persistBioAssayDimension( ( BioAssayDimension ) entity, caches );
        } else if ( entity instanceof BioMaterial ) {
            return ( T ) impl.persistBioMaterial( ( BioMaterial ) entity, caches );
        } else if ( entity instanceof BioAssay ) {
            return ( T ) impl.persistBioAssay( ( BioAssay ) entity, caches );
        } else if ( entity instanceof Compound ) {
            return ( T ) impl.persistCompound( ( Compound ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return ( T ) impl.persistExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) entity );
        } else {
            return super.doPersist( entity, caches );
        }
    }

}
