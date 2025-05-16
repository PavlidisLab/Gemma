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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see BioMaterial
 */
public interface BioMaterialDao extends BaseVoEnabledDao<BioMaterial, BioMaterialValueObject> {

    BioMaterial copy( BioMaterial bioMaterial );

    /**
     * Find all the sub-biomaterials for a given biomaterial related by {@link BioMaterial#getSourceBioMaterial()}.
     * @param direct if true, only direct sub-biomaterials are retained, otherwise the entire hierarchy is visited
     *               recursively.
     */
    List<BioMaterial> findSubBioMaterials( BioMaterial bioMaterial, boolean direct );

    /**
     * Find all the sub-biomaterials for a given biomaterial related by {@link BioMaterial#getSourceBioMaterial()}.
     * @param bioMaterials a collection of biomaterials to visit
     * @param direct       if true, only direct sub-biomaterials are retained, otherwise the entire hierarchy is visited
     *                     recursively.
     */
    List<BioMaterial> findSubBioMaterials( Collection<BioMaterial> bioMaterials, boolean direct );

    Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment );

    Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor );

    /**
     * Obtain all the experiments a biomaterial is used in from its hierarchy.
     * <p>
     * This also includes experiments that are using this via one of their parent?
     */
    Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> getExpressionExperiments( BioMaterial bm );
}
