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
package ubic.gemma.model.expression.biomaterial;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.BaseDao;

/**
 * @see BioMaterial
 * @version $Id$
 */
public interface BioMaterialDao extends BaseDao<BioMaterial> {

    /**
     * 
     */
    public BioMaterial copy( BioMaterial bioMaterial );

    /**
     * 
     */
    public Integer countAll();

    /**
     * 
     */
    public BioMaterial find( BioMaterial bioMaterial );

    /**
     * @param experiment
     * @return
     */
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment );

    /**
     * @param fv
     * @return
     */
    public Collection<BioMaterial> findByFactorValue( FactorValue fv );

    /**
     * 
     */
    public BioMaterial findOrCreate( BioMaterial bioMaterial );

    /**
     * Return the experiment the biomaterial appears in
     * 
     * @param bioMaterialId
     * @return
     */
    public ExpressionExperiment getExpressionExperiment( Long bioMaterialId );

    /**
     * @param ids
     * @return
     */
    @Override
    public Collection<BioMaterial> load( Collection<Long> ids );

    /**
     * Remove the factor value.
     * 
     * @param BioMaterials
     * @param experimentalFactor
     */
    public void removeFactor( Collection<BioMaterial> bioMaterials, ExperimentalFactor experimentalFactor );

    /**
     * @param bioMaterial
     */
    public void thaw( BioMaterial bioMaterial );

    /**
     * @param bioMaterials
     * @return
     */
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials );

}
