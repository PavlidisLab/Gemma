/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.expression.biomaterial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.BioMaterialService
 */
public class BioMaterialServiceImpl extends ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase {

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#getBioMaterials()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.List handleGetBioMaterials() throws java.lang.Exception {
        List<BioMaterial> results = new ArrayList<BioMaterial>();
        results.addAll( this.getBioMaterialDao().loadAll() );
        return results;
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getBioMaterialDao().countAll();
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#saveBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    protected void handleSaveBioMaterial( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial )
            throws java.lang.Exception {
        this.getBioMaterialDao().create( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected BioMaterial handleFindOrCreate( BioMaterial bioMaterial ) throws Exception {
        return this.getBioMaterialDao().findOrCreate( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#remove(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected void handleRemove( BioMaterial bioMaterial ) throws Exception {
        this.getBioMaterialDao().remove( bioMaterial );

    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrId(java.lang.Long)
     */
    @Override
    protected BioMaterial handleFindById( Long id ) throws Exception {
        return this.getBioMaterialDao().findById( id );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#loadAll()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getBioMaterialDao().loadAll();
    }

}