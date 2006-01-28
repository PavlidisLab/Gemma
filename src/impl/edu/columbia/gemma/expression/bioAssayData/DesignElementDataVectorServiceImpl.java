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
package edu.columbia.gemma.expression.bioAssayData;

import java.util.Collection;

import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorService
 */
public class DesignElementDataVectorServiceImpl extends
        edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorServiceBase {

    @Override
    protected DesignElementDataVector handleFindOrCreate( DesignElementDataVector designElementDataVector )
            throws Exception {

        return this.getDesignElementDataVectorDao().findOrCreate( designElementDataVector );
    }

    @Override
    protected void handleRemove( DesignElementDataVector designElementDataVector ) throws Exception {
        this.getDesignElementDataVectorDao().remove( designElementDataVector );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorServiceBase#handleLoadAll(edu.columbia.gemma.expression.experiment.ExpressionExperiment,
     *      edu.columbia.gemma.common.quantitationtype.QuantitationType,
     *      edu.columbia.gemma.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    protected Collection handleLoadAll( ExpressionExperiment expressionExperiment, QuantitationType quantitationType,
            BioAssayDimension bioAssayDimension ) throws Exception {

        // TODO Auto-generated method stub
        return null;
    }

}