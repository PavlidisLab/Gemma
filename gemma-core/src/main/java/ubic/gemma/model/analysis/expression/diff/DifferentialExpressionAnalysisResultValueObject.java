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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.Hibernate;
import ubic.gemma.model.analysis.AnalysisResultValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Unlike {@link DiffExResultSetSummaryValueObject}, this value object is meant for the public API.
 */
@Data
@EqualsAndHashCode(of = { "probeId" }, callSuper = true)
public class DifferentialExpressionAnalysisResultValueObject extends AnalysisResultValueObject<DifferentialExpressionAnalysisResult> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long probeId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String probeName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<GeneValueObject> genes;
    private Double pValue;
    private Double correctedPvalue;
    private Double rank;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ContrastResultValueObject> contrasts;

    public DifferentialExpressionAnalysisResultValueObject() {
        super();
    }

    public DifferentialExpressionAnalysisResultValueObject( DifferentialExpressionAnalysisResult result, boolean includeFactorValues ) {
        super( result );
        // getId() does not initialize proxies
        this.probeId = result.getProbe().getId();
        if ( Hibernate.isInitialized( result.getProbe() ) ) {
            this.probeName = result.getProbe().getName();
        }
        this.pValue = result.getPvalue();
        this.correctedPvalue = result.getCorrectedPvalue();
        this.rank = result.getRank();
        if ( Hibernate.isInitialized( result.getContrasts() ) ) {
            this.contrasts = result.getContrasts().stream()
                    .map( c -> new ContrastResultValueObject( c, includeFactorValues ) )
                    .collect( Collectors.toList() );
        } else {
            this.contrasts = null;
        }
    }

    public DifferentialExpressionAnalysisResultValueObject( DifferentialExpressionAnalysisResult result, boolean includeFactorValuesInContrasts, List<Gene> genes, boolean includeTaxonInGenes ) {
        this( result, includeFactorValuesInContrasts );
        this.genes = genes.stream().map( g -> new GeneValueObject( g, includeTaxonInGenes ) ).collect( Collectors.toList() );
    }
}