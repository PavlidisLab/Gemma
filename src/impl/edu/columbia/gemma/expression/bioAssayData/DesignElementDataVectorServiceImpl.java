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
package edu.columbia.gemma.expression.bioAssayData;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorService
 */
public class DesignElementDataVectorServiceImpl extends
        edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorServiceBase {

    /**
     * @see edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorService#saveDesignElementDataVector(edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector)
     */
    protected void handleSaveDesignElementDataVector(
            edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector designElementDataVector )
            throws java.lang.Exception {
        this.getDesignElementDataVectorDao().create( designElementDataVector );
    }

    @Override
    protected DesignElementDataVector handleFindOrCreate( DesignElementDataVector designElementDataVector )
            throws Exception {

        return this.getDesignElementDataVectorDao().findOrCreate( designElementDataVector );
    }

    @Override
    protected void handleRemove( DesignElementDataVector designElementDataVector ) throws Exception {
        this.getDesignElementDataVectorDao().remove( designElementDataVector );

    }

}