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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.springframework.stereotype.Service;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.FactorValueService
 */
@Service
public class FactorValueServiceImpl extends ubic.gemma.model.expression.experiment.FactorValueServiceBase {

    public Collection<FactorValue> create( Collection<FactorValue> fvs ) {
        return ( Collection<FactorValue> ) this.getFactorValueDao().create( fvs );
    }

    public Collection<FactorValue> findByValue( String valuePrefix ) {
        return this.getFactorValueDao().findByValue( valuePrefix );
    }

    @Override
    protected FactorValue handleCreate( FactorValue factorValue ) throws Exception {
        return this.getFactorValueDao().create( factorValue );
    }

    @Override
    protected void handleDelete( FactorValue factorValue ) throws Exception {
        this.getFactorValueDao().remove( factorValue );
    }

    @Override
    protected FactorValue handleFindOrCreate( FactorValue factorValue ) throws Exception {
        return this.getFactorValueDao().findOrCreate( factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#getAllFactorValues()
     */
    protected java.util.Collection handleGetAllFactorValues() throws java.lang.Exception {
        return this.getFactorValueDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected FactorValue handleLoad( Long id ) throws Exception {
        return this.getFactorValueDao().load( id );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleLoadAll()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getFactorValueDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#saveFactorValue(ubic.gemma.model.expression.experiment.FactorValue)
     */
    protected void handleSaveFactorValue( ubic.gemma.model.expression.experiment.FactorValue factorValue )
            throws java.lang.Exception {
        this.getFactorValueDao().create( factorValue );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleUpdate(java.util.Collection)
     */
    @Override
    protected void handleUpdate( Collection factorValues ) throws Exception {
        this.getFactorValueDao().update( factorValues );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleUpdate(ubic.gemma.model.expression.experiment
     * .FactorValue)
     */
    @Override
    protected void handleUpdate( FactorValue factorValue ) throws Exception {
        this.getFactorValueDao().update( factorValue );
    }

}