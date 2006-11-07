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
package ubic.gemma.model.expression.arrayDesign;

import java.util.Collection;

import ubic.gemma.model.genome.Taxon;

/**
 * @author klc
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService
 */
public class ArrayDesignServiceImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase {

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getAllArrayDesigns()
     */
    @Override
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getArrayDesignDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#removeArrayDesign(java.lang.String)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception {
        this.getArrayDesignDao().remove( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findArrayDesignByName(java.lang.String)
     */
    @Override
    protected ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindArrayDesignByName( String name )
            throws Exception {
        return this.getArrayDesignDao().findByName( name );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#updateArrayDesign(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().update( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Integer handleGetCompositeSequenceCount( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequences( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetReporterCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Integer handleGetReporterCount( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numReporters( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected ArrayDesign handleCreate( ArrayDesign arrayDesign ) throws Exception {
        return ( ArrayDesign ) this.getArrayDesignDao().create( arrayDesign );
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see
    // ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadReporters(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
    // */
    // @Override
    // protected Collection handleLoadReporters( ArrayDesign arrayDesign ) throws Exception {
    // return this.getArrayDesignDao().loadReporters( arrayDesign.getId() );
    // }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleLoadCompositeSequences( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().loadCompositeSequences( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoad(long)
     */
    @Override
    protected ArrayDesign handleLoad( long id ) throws Exception {
        return ( ArrayDesign ) this.getArrayDesignDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.handleGetAllAssociatedBioAssays(long)
     */
    @Override
    protected java.util.Collection handleGetAllAssociatedBioAssays( java.lang.Long id ) {
        return this.getArrayDesignDao().getAllAssociatedBioAssays( id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.handleGetTaxon(long)
     */
    @Override
    protected Taxon handleGetTaxon( java.lang.Long id ) {
        return this.getArrayDesignDao().getTaxon( id );

    }

    @Override
    protected ArrayDesign handleFindByShortName( String shortName ) throws Exception {
        return this.getArrayDesignDao().findByShortName( shortName );
    }

    @Override
    protected void handleThaw( ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().thaw( arrayDesign );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getArrayDesignDao().countAll();
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumBioSequencesById(long)
     */
    @Override
    protected long handleNumBioSequencesById( long id ) throws Exception {
        return this.getArrayDesignDao().numBioSequencesById( id );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumBlatResultsById(long)
     */
    @Override
    protected long handleNumBlatResultsById( long id ) throws Exception {
        return this.getArrayDesignDao().numBlatResultsById( id );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumGeneProductsById(long)
     */
    @Override
    protected long handleNumGenesById( long id ) throws Exception {
        return this.getArrayDesignDao().numGenesById( id );
    }

}