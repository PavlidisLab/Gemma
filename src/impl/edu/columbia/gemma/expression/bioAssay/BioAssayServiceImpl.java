/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.expression.bioAssay;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.expression.bioAssay.BioAssayService
 */
public class BioAssayServiceImpl extends edu.columbia.gemma.expression.bioAssay.BioAssayServiceBase {

    /**
     * @see edu.columbia.gemma.expression.bioAssay.BioAssayService#saveBioAssay(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    protected void handleSaveBioAssay( edu.columbia.gemma.expression.bioAssay.BioAssay bioAssay )
            throws java.lang.Exception {
        this.getBioAssayDao().create( bioAssay );
    }

    /**
     * @see edu.columbia.gemma.expression.bioAssay.BioAssayService#getAllBioAssays()
     */
    protected java.util.Collection handleGetAllBioAssays() throws java.lang.Exception {
        return this.getBioAssayDao().loadAll();
    }

    @Override
    protected BioAssay handleFindOrCreate( BioAssay bioAssay ) throws Exception {
        return this.getBioAssayDao().findOrCreate( bioAssay );
    }

    @Override
    protected void handleRemove( BioAssay bioAssay ) throws Exception {
        this.getBioAssayDao().remove( bioAssay );
        
    }

}