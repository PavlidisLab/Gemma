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
 */
public class DifferentialExpressionAnalysisResultImpl extends DifferentialExpressionAnalysisResult {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8952834115689524169L;

    private static int getBin( Double value ) {
        return ( int ) Math.min( 5, Math.floor( -Math.log10( value ) ) );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this.getId() == null ) ? 0 : this.getId().hashCode() );

        if ( this.getId() == null ) {
            result = prime * result + ( ( this.getResultSet() == null ) ? 0 : this.getResultSet().hashCode() );
            result = prime * result + ( ( this.getProbe() == null ) ? 0 : this.getProbe().hashCode() );
        }
        return result;
    }

    @SuppressWarnings("SimplifiableIfStatement") // Better readability
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        DifferentialExpressionAnalysisResult other = ( DifferentialExpressionAnalysisResult ) obj;

        if ( this.getId() == null ) {
            if ( other.getId() != null )
                return false;
        } else if ( !this.getId().equals( other.getId() ) ) {
            return false;
        } else {
            return this.getId().equals( other.getId() );
        }

        // fallback.
        if ( this.getResultSet() == null ) {
            if ( other.getResultSet() != null )
                return false;
        } else if ( !this.getResultSet().equals( other.getResultSet() ) )
            return false;
        if ( this.getProbe() == null ) {
            return other.getProbe() == null;
        } else
            return this.getProbe().equals( other.getProbe() );
    }

    @Override
    public void setCorrectedPvalue( Double correctedPvalue ) {

        if ( correctedPvalue == null )
            return;

        super.setCorrectedPvalue( correctedPvalue );

        /*
         * See bug 2013. Here we ensure that the bin is always set. The maximum value is 5, representing qvalues better
         * than 10e-5. 0.1-1 -> 0; 0.01-0.099 -> 1; 0.001-0.00999 -> 2; 0.0001- 0.000999 -> 3 etc. Thus "p<0.01" is
         * equivalent to "bin >=2"
         */
        this.setCorrectedPValueBin( DifferentialExpressionAnalysisResultImpl.getBin( correctedPvalue ) );
    }

    @Override
    public String toString() {
        return "DiffExRes[" + this.getId() + "]: " + this.getProbe() + " p=" + String.format( "%g", this.getPvalue() )
                + " ressetId=" + ( this.getResultSet() == null ? "" : this.getResultSet().getId() );
    }
}