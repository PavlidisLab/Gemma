/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.expression.diff;

import java.util.List;
import java.util.Vector;

/**
 * Stores selected details of the contrasts for a single DifferentialExpressionResult
 * 
 * @author Paul
 * @version $Id$
 */
public class ContrastsValueObject {

    private List<ContrastVO> contrasts = new Vector<ContrastVO>( 2 ); // commonly only have one.

    private Long resultId;

    public ContrastsValueObject( Long resultId ) {
        this.resultId = resultId;
    }

    public void addContrast( Long id, Long factorValueId, Double logFoldchange, Double pvalue ) {
        contrasts.add( new ContrastVO( id, factorValueId, logFoldchange, pvalue ) );
    }

    public List<ContrastVO> getContrasts() {
        return contrasts;
    }

    public Long getResultId() {
        return resultId;
    }

}