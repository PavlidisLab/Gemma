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
package ubic.gemma.persistence.util;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;

import java.util.Comparator;

/**
 * @author keshav
 */
public class DifferentialExpressionAnalysisResultComparator
        implements Comparator<DifferentialExpressionAnalysisResult> {

    @Override
    public int compare( DifferentialExpressionAnalysisResult ear1, DifferentialExpressionAnalysisResult ear2 ) {

        if ( ear1 != null ) {
            if ( ear2 != null ) {
                if ( ear1.getPvalue() == null && ear2.getPvalue() == null ) {
                    return 0;
                } else if ( ear1.getPvalue() == null ) {
                    return -1;
                } else if ( ear2.getPvalue() == null ) {
                    return 1;
                }
                return ear1.getPvalue().compareTo( ear2.getPvalue() );
            }

            return 1;
        }
        if ( ear2 != null )
            return -1;

        return 0;

    }

    public static final class Factory {
        public static DifferentialExpressionAnalysisResultComparator newInstance() {
            return new DifferentialExpressionAnalysisResultComparator();
        }
    }
}
