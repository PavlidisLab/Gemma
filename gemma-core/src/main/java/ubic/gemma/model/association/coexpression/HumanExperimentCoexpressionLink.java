/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.model.association.coexpression;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 */
public class HumanExperimentCoexpressionLink extends ExperimentCoexpressionLink {

    public HumanExperimentCoexpressionLink() {
        this( null, null, null, null );
    }

    public HumanExperimentCoexpressionLink( ExpressionExperiment experiment, Long linkId, Long firstGene,
            Long secondGene ) {
        super( experiment, linkId, firstGene, secondGene );
    }

}
