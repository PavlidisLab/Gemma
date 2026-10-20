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
package ubic.gemma.model.analysis.expression.diff;

import lombok.Data;
import ubic.gemma.model.common.ValueObject;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * Stores selected details of the contrasts for a single DifferentialExpressionResult
 *
 * @author Paul
 */
@Data
@ValueObject
public class ContrastsValueObject implements Serializable {

    private Long resultId;

    private final List<ContrastVO> contrasts = new Vector<>( 2 ); // commonly only have one.

    public ContrastsValueObject() {
        super();
    }

    public ContrastsValueObject( Long resultId ) {
        this.resultId = resultId;
    }

    public ContrastsValueObject( DifferentialExpressionAnalysisResult o ) {
        this( o.getId() );
        for ( ContrastResult c : o.getContrasts() ) {
            contrasts.add( new ContrastVO( c ) );
        }
    }
}