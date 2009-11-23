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
     * @see ubic.gemma.model.expression.experiment.ExpressionExperiment#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object object ) {
        if ( !this.getClass().equals( object.getClass() ) ) {
            return false;
        }
        ExpressionExperiment that = ( ExpressionExperiment ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else if ( this.getShortName() != null && that.getShortName() != null ) {
            return this.getShortName().equals( that.getShortName() );
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperiment#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 1;
        if ( this.getId() != null ) {
            return this.getId().hashCode();
        } else if ( this.getShortName() != null ) {
            return this.getShortName().hashCode();
        }
        return result;

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.DescribableImpl#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " (" + this.getShortName() + ")";
    }

}