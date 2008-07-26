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

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService
 */
public class ExpressionExperimentSubSetServiceImpl extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetServiceBase {

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#getAllExpressionExperimentSubSets()
     */
    @Override
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getExpressionExperimentSubSetDao().loadAll();
    }

    /**
     * Loads one subset, given an id
     * 
     * @return ExpressionExperimentSubSet
     */
    @Override
    protected ExpressionExperimentSubSet handleLoad( Long id ) throws java.lang.Exception {
        return ( ExpressionExperimentSubSet ) this.getExpressionExperimentSubSetDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#saveExpressionExperimentSubSet(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    protected ExpressionExperimentSubSet handleCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet )
            throws java.lang.Exception {
        return ( ExpressionExperimentSubSet ) this.getExpressionExperimentSubSetDao().create(
                expressionExperimentSubSet );
    }

}