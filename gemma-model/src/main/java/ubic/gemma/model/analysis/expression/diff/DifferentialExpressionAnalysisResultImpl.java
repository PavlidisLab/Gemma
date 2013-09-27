/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.analysis.expression.diff;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 * @version $Id$
 */
public class DifferentialExpressionAnalysisResultImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8952834115689524169L;

    private static int getBin( Double value ) {
        return ( int ) Math.min( 5, Math.floor( -Math.log10( value ) ) );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        DifferentialExpressionAnalysisResult other = ( DifferentialExpressionAnalysisResult ) obj;

        if ( this.getId() == null ) {
            if ( other.getId() != null ) return false;
        } else if ( !getId().equals( other.getId() ) ) {
            return false;
        } else {
            return getId().equals( other.getId() );
        }

        // fallback.
        if ( this.getResultSet() == null ) {
            if ( other.getResultSet() != null ) return false;
        } else if ( !getResultSet().equals( other.getResultSet() ) ) return false;
        if ( this.getProbe() == null ) {
            if ( other.getProbe() != null ) return false;
        } else if ( !getProbe().equals( other.getProbe() ) ) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getId() == null ) ? 0 : getId().hashCode() );

        if ( getId() == null ) {
            result = prime * result + ( ( getResultSet() == null ) ? 0 : getResultSet().hashCode() );
            result = prime * result + ( ( getProbe() == null ) ? 0 : getProbe().hashCode() );
        }
        return result;
    }

    @Override
    public void setCorrectedPvalue( Double correctedPvalue ) {

        if ( correctedPvalue == null ) return;

        super.setCorrectedPvalue( correctedPvalue );

        /*
         * See bug 2013. Here we ensure that the bin is always set. The maximum value is 5, representing qvalues better
         * than 10e-5. 0.1-1 -> 0; 0.01-0.099 -> 1; 0.001-0.00999 -> 2; 0.0001- 0.000999 -> 3 etc. Thus "p<0.01" is
         * equivalent to "bin >=2"
         */
        this.setCorrectedPValueBin( getBin( correctedPvalue ) );
    }

    @Override
    public String toString() {
        return "DiffExRes[" + this.getId() + "]: " + this.getProbe() + " p=" + String.format( "%g", this.getPvalue() )
                + " ressetId=" + ( this.getResultSet() == null ? "" : this.getResultSet().getId() );
    }
}