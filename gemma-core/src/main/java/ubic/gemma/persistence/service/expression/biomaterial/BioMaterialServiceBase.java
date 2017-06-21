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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDao;

/**
 * Spring Service base class for <code>BioMaterialService</code>, provides access to all services and entities
 * referenced by this service.
 *
 * @see BioMaterialService
 */
public abstract class BioMaterialServiceBase extends VoEnabledService<BioMaterial, BioMaterialValueObject>
        implements BioMaterialService {

    final BioMaterialDao bioMaterialDao;
    final FactorValueDao factorValueDao;
    final BioAssayDao bioAssayDao;
    final ExperimentalFactorDao experimentalFactorDao;

    public BioMaterialServiceBase( BioMaterialDao bioMaterialDao, FactorValueDao factorValueDao,
            BioAssayDao bioAssayDao, ExperimentalFactorDao experimentalFactorDao ) {
        super( bioMaterialDao );
        this.bioMaterialDao = bioMaterialDao;
        this.factorValueDao = factorValueDao;
        this.bioAssayDao = bioAssayDao;
        this.experimentalFactorDao = experimentalFactorDao;
    }

    /**
     * @see BioMaterialService#copy(BioMaterial)
     */
    @Override
    @Transactional
    public BioMaterial copy( final BioMaterial bioMaterial ) {
        return this.handleCopy( bioMaterial );
    }

    /**
     * @see BioMaterialService#findOrCreate(BioMaterial)
     */
    @Override
    @Transactional
    public BioMaterial findOrCreate( final BioMaterial bioMaterial ) {

        return this.handleFindOrCreate( bioMaterial );
    }

    /**
     * Performs the core logic for {@link #copy(BioMaterial)}
     */
    protected abstract BioMaterial handleCopy( BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #findOrCreate(BioMaterial)}
     */
    protected abstract BioMaterial handleFindOrCreate( BioMaterial bioMaterial );

}