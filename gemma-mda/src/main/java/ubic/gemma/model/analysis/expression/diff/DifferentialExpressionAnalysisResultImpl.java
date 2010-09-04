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
public abstract class DifferentialExpressionAnalysisResultImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8952834115689524169L;

    @Override
    public void setCorrectedPvalue( Double correctedPvalue ) {

        if ( correctedPvalue == null ) return;

        super.setCorrectedPvalue( correctedPvalue );

        /*
         * See bug 2013. Here we ensure that the bin is always set. The maximum value is 5, representing qvalues better
         * than 10e-5
         */
        this.setCorrectedPValueBin( ( int ) Math.min( 5, -Math.log10( correctedPvalue ) ) );
    }

}