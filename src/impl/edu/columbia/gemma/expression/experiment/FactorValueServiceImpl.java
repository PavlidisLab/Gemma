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
package edu.columbia.gemma.expression.experiment;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.expression.experiment.FactorValueService
 */
public class FactorValueServiceImpl extends edu.columbia.gemma.expression.experiment.FactorValueServiceBase {

    /**
     * @see edu.columbia.gemma.expression.experiment.FactorValueService#getAllFactorValues()
     */
    protected java.util.Collection handleGetAllFactorValues() throws java.lang.Exception {
        return this.getFactorValueDao().loadAll();
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.FactorValueService#saveFactorValue(edu.columbia.gemma.expression.experiment.FactorValue)
     */
    protected void handleSaveFactorValue( edu.columbia.gemma.expression.experiment.FactorValue factorValue )
            throws java.lang.Exception {
        this.getFactorValueDao().create( factorValue );
    }

    @Override
    protected FactorValue handleFindOrCreate( FactorValue factorValue ) throws Exception {
        return this.getFactorValueDao().findOrCreate(factorValue);
    }

    @Override
    protected void handleRemove( FactorValue factorValue ) throws Exception {
        // TODO Auto-generated method stub
        
    }

}