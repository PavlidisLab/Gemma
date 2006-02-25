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
package edu.columbia.gemma.expression.arrayDesign;

import java.util.Collection;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignService
 */
public class ArrayDesignServiceImpl extends edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceBase {

    /**
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignService#getAllArrayDesigns()
     */
    protected java.util.Collection handleGetAllArrayDesigns() throws java.lang.Exception {
        return this.getArrayDesignDao().loadAll();
    }

    /**
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignService#removeArrayDesign(java.lang.String)
     */
    protected void handleRemove( edu.columbia.gemma.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception {
        this.getArrayDesignDao().remove( arrayDesign );
    }

    /**
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignService#findArrayDesignByName(java.lang.String)
     */
    @Override
    protected edu.columbia.gemma.expression.arrayDesign.ArrayDesign handleFindArrayDesignByName( String name )
            throws Exception {
        return this.getArrayDesignDao().findByName( name );
    }

    /**
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignService#updateArrayDesign(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    protected void handleUpdate( edu.columbia.gemma.expression.arrayDesign.ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().update( arrayDesign );
    }

    /**
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignService#find(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected ArrayDesign handleFind( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().find( arrayDesign );
    }

    @Override
    protected ArrayDesign handleFindOrCreate( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().findOrCreate( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceBase#handleGetCompositeSequenceCount(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Integer handleGetCompositeSequenceCount( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequences( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceBase#handleGetReporterCount(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Integer handleGetReporterCount( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numReporters( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceBase#handleCreate(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected ArrayDesign handleCreate( ArrayDesign arrayDesign ) throws Exception {
        return ( ArrayDesign ) this.getArrayDesignDao().create( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceBase#handleLoadReporters(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleLoadReporters( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().loadReporters( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.arrayDesign.ArrayDesignServiceBase#handleLoadCompositeSequences(edu.columbia.gemma.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleLoadCompositeSequences( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().loadCompositeSequences( arrayDesign.getId() );
    }

}