/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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

import lombok.Getter;
import ubic.gemma.model.analysis.AnalysisResultValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Unlike {@link DiffExResultSetSummaryValueObject}, this value object is meant for the public API.
 */
@Getter
public class DifferentialExpressionAnalysisResultValueObject extends AnalysisResultValueObject<DifferentialExpressionAnalysisResult> {

    private final Long probeId;
    private final String probeName;
    private final List<GeneValueObject> genes;
    private final Double pValue;
    private final Double correctedPvalue;
    private final Double rank;

    public DifferentialExpressionAnalysisResultValueObject( DifferentialExpressionAnalysisResult result, List<Gene> genes ) {
        super( result );
        this.probeId = result.getProbe().getId();
        this.probeName = result.getProbe().getName();
        this.genes = genes.stream().map( GeneValueObject::new ).collect( Collectors.toList() );
        this.pValue = result.getPvalue();
        this.correctedPvalue = result.getCorrectedPvalue();
        this.rank = result.getRank();
    }
}