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
        return this.getProbe() + " p=" + String.format( "%g", this.getPvalue() );
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null ) return super.hashCode();
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( super.getProbe() == null ) ? 0 : super.getProbe().hashCode() );
        result = prime * result
                + ( ( super.getQuantitationType() == null ) ? 0 : super.getQuantitationType().hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;

        if ( getClass() != obj.getClass() ) return false;

        DifferentialExpressionAnalysisResult other = ( DifferentialExpressionAnalysisResult ) obj;

        if ( this.getId() != null ) {
            return ( other.getId() == null || !this.getId().equals( other.getId() ) );
        }

        if ( super.getProbe() == null ) {
            if ( other.getProbe() != null ) return false;
        } else if ( !super.getProbe().equals( other.getProbe() ) ) return false;

        if ( super.getQuantitationType() == null ) {
            if ( other.getQuantitationType() != null ) return false;
        } else if ( !super.getQuantitationType().equals( other.getQuantitationType() ) ) return false;

        return true;
    }
}