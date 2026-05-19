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

    @Autowired
    private EeWriteServiceImpl eeWriteService;
    @Autowired
    private ExpressionExperimentPrePersistService expressionExperimentPrePersistService;

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
        if ( entity instanceof ExpressionExperiment ) {
            if ( caches.getArrayDesignCache() == null ) {
                AbstractPersister.log.warn( "Consider doing the 'prepare' step in a separate transaction." );
                caches = caches.withArrayDesignCache( this.prepare( ( ExpressionExperiment ) entity ) );
            }
            return ( T ) eeWriteService.persistExpressionExperiment( ( ExpressionExperiment ) entity, caches );
        } else if ( entity instanceof BioAssayDimension ) {
            return ( T ) eeWriteService.persistBioAssayDimension( ( BioAssayDimension ) entity, caches );
        } else if ( entity instanceof BioMaterial ) {
            return ( T ) eeWriteService.persistBioMaterial( ( BioMaterial ) entity, caches );
        } else if ( entity instanceof BioAssay ) {
            return ( T ) eeWriteService.persistBioAssay( ( BioAssay ) entity, caches );
        } else if ( entity instanceof Compound ) {
            return ( T ) eeWriteService.persistCompound( ( Compound ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return ( T ) eeWriteService.persistExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) entity );
        } else {
            return super.doPersist( entity, caches );
        }
    }

}
