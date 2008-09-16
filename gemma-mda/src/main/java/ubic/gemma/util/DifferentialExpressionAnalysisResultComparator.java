/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.util;

import java.util.Comparator;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;

/**
 * A {@link Comparator} to compare {@link ExpressionAnalysisResult} by p value.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisResultComparator implements Comparator<DifferentialExpressionAnalysisResult> {

    /**
     * @author keshav
     * @version $Id$
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.analysis.ExpressionAnalysisResult}.
         */
        public static DifferentialExpressionAnalysisResultComparator newInstance() {
            return new DifferentialExpressionAnalysisResultComparator();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( DifferentialExpressionAnalysisResult ear1, DifferentialExpressionAnalysisResult ear2 ) {

        if ( ear1 != null ) {
            if ( ear2 != null )
                return ear1.getPvalue().compareTo( ear2.getPvalue() );
            else
                return 1;
        } else {
            if ( ear2 != null )
                return -1;
            else
                return 0;
        }
    }
}
