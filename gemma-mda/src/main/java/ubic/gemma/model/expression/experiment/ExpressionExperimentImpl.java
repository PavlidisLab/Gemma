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

/**
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentImpl extends ubic.gemma.model.expression.experiment.ExpressionExperiment {

    /**
     *  
     */
    private static final long serialVersionUID = -1342753625018841735L;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.DescribableImpl#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " (" + this.getShortName() + ")";
    }

    @Override
    public Collection getAnalyses() {
  //     return this.getExpressionAnalyses();
        return null;
    }
}