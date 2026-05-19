/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.analysis.expression.diff;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.io.Serializable;
import java.util.Collection;

/**
 * A value object with meta analysis results.
 *
 * @author keshav
 */
@Getter
@Setter
@SuppressWarnings({ "WeakerAccess", "unused" }) // Frontend use
public class DifferentialExpressionMetaAnalysisValueObject implements Serializable {

    private GeneValueObject gene = null;

    @Setter(AccessLevel.NONE)
    private String sortKey;
    private Double fisherPValue = null;
    private int numSearchedExperiments;
    private int numExperimentsInScope;
    private int numMetThreshold;

    private Collection<BioAssaySet> activeExperiments = null;

    private Collection<DifferentialExpressionValueObject> probeResults = null;

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", this.getFisherPValue(), this.getGene().getOfficialSymbol() );
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( "# MetaP = " ).append( this.getFisherPValue() ).append( "\n" );

        for ( DifferentialExpressionValueObject result : this.getProbeResults() ) {
            buf.append( result ).append( "\n" );
        }
        return buf.toString();
    }

}
