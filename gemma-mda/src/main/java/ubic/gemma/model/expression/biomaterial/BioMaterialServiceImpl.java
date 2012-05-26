/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.biomaterial;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.BioMaterialService
 */
@Service
public class BioMaterialServiceImpl extends ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialService#exists(ubic.gemma.model.expression.biomaterial.BioMaterial
     * )
     */
    @Override
    public boolean exists( BioMaterial bioMaterial ) {
        return this.getBioMaterialDao().find( bioMaterial ) != null;
    }

    @Override
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment ) {
        return this.getBioMaterialDao().findByExperiment( experiment );
    }

    @Override
    public Collection<BioMaterial> findByFactorValue( FactorValue fv ) {
        return this.getBioMaterialDao().findByFactorValue( fv );
    }

    @Override
    public ExpressionExperiment getExpressionExperiment( Long id ) {
        return this.getBioMaterialDao().getExpressionExperiment( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialService#thaw(ubic.gemma.model.expression.biomaterial.BioMaterial
     * )
     */
    @Override
    public void thaw( BioMaterial bioMaterial ) {
        this.getBioMaterialDao().thaw( bioMaterial );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#thaw(java.util.Collection)
     */
    @Override
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials ) {
        return this.getBioMaterialDao().thaw( bioMaterials );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase#handleCopy(ubic.gemma.model.expression.biomaterial
     * .BioMaterial)
     */
    @Override
    protected BioMaterial handleCopy( BioMaterial bioMaterial ) throws Exception {
        return this.getBioMaterialDao().copy( bioMaterial );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getBioMaterialDao().countAll();
    }

    /**
     * @param bioMaterial
     * @return
     * @throws Exception
     */
    @Override
    protected BioMaterial handleCreate( BioMaterial bioMaterial ) throws Exception {
        return this.getBioMaterialDao().create( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected BioMaterial handleFindOrCreate( BioMaterial bioMaterial ) throws Exception {
        return this.getBioMaterialDao().findOrCreate( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrId(java.lang.Long)
     */
    @Override
    protected BioMaterial handleLoad( Long id ) throws Exception {
        return this.getBioMaterialDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#loadAll()
     */
    @Override
    protected Collection<BioMaterial> handleLoadAll() throws Exception {
        return ( Collection<BioMaterial> ) this.getBioMaterialDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<BioMaterial> handleLoadMultiple( Collection<Long> ids ) throws Exception {
        return this.getBioMaterialDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#remove(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected void handleRemove( BioMaterial bioMaterial ) throws Exception {
        this.getBioMaterialDao().remove( bioMaterial );

    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#saveBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    protected void handleSaveBioMaterial( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial )
            throws java.lang.Exception {
        this.getBioMaterialDao().create( bioMaterial );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase#handleUpdate(ubic.gemma.model.expression.biomaterial
     * .BioMaterial)
     */
    @Override
    protected void handleUpdate( BioMaterial bioMaterial ) throws Exception {
        this.getBioMaterialDao().update( bioMaterial );
    }
}