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

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;

/**
 * @see BioMaterial
 */
public interface BioMaterialDao extends BaseVoEnabledDao<BioMaterial, BioMaterialValueObject> {

    BioMaterial copy( BioMaterial bioMaterial );

    Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment );

    Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor );

    /**
     * @param bioMaterialId biomaterial id
     * @return the experiment the biomaterial appears in
     */
    ExpressionExperiment getExpressionExperiment( Long bioMaterialId );

    /**
     * Thaw the given BioMaterial.
     * <p>
     * The following fields are initialized: sourceTaxon, treatments and factorValues.experimentalFactor.
     */
    void thaw( BioMaterial bioMaterial );
}
