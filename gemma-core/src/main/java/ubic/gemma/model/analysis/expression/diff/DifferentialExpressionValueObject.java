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
package ubic.gemma.model.analysis.expression.diff;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.ValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubsetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents the results for one probe. Fairly heavy-weight.
 *
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Data
@ValueObject
public class DifferentialExpressionValueObject implements Serializable {

    private ContrastsValueObject contrasts;
    private Double corrP;
    private Direction direction;
    private Collection<ExperimentalFactorValueObject> experimentalFactors = new HashSet<>();
    private BioAssaySetValueObject expressionExperiment;
    private Boolean fisherContribution = false;
    private GeneValueObject gene;
    private Long id;
    private Boolean metThreshold = false;
    private Double p;
    private String probe;
    private Long probeId;
    private Long resultSetId = null;
    private String sortKey;

    public DifferentialExpressionValueObject() {
        super();
    }

    public DifferentialExpressionValueObject( DifferentialExpressionAnalysisResult o ) {
        this.id = o.getId();
        this.p = o.getPvalue();
        this.corrP = o.getCorrectedPvalue();
        this.probe = o.getProbe().getName();
        this.probeId = o.getProbe().getId();
        this.resultSetId = o.getResultSet().getId();
        this.contrasts = new ContrastsValueObject( o );
    }

    public DifferentialExpressionValueObject( Long id ) {
        this.id = id;
        this.contrasts = new ContrastsValueObject( this.id );
    }

    @Override
    public int hashCode() {
        return Objects.hash( id );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( this.getClass() != obj.getClass() ) {
            return false;
        }
        DifferentialExpressionValueObject other = ( DifferentialExpressionValueObject ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        if ( gene == null ) {
            buf.append( "-\t" );
        } else if ( StringUtils.isNotBlank( gene.getOfficialSymbol() ) ) {
            buf.append( gene.getOfficialSymbol() );
        } else if ( StringUtils.isNotBlank( gene.getOfficialName() ) ) {
            buf.append( gene.getOfficialName() );
        } else {
            buf.append( gene.getName() );
        }
        buf.append( "\t" );

        if ( expressionExperiment instanceof ExpressionExperimentValueObject ) {
            buf.append( ( ( ExpressionExperimentValueObject ) expressionExperiment ).getShortName() ).append( "\t" );
        } else if ( expressionExperiment instanceof ExpressionExperimentSubsetValueObject ) {
            buf.append( ( ( ExpressionExperimentSubsetValueObject ) expressionExperiment ).getSourceExperimentShortName() ).append( "\t" );
        }

        buf.append( probe ).append( "\t" );

        int i = 0;
        for ( ExperimentalFactorValueObject f : experimentalFactors ) {
            buf.append( f.getName() );
            if ( i < ( experimentalFactors.size() - 1 ) ) {
                buf.append( ", " );
            } else {
                buf.append( "\t" );
            }
            i++;
        }

        buf.append( p );

        return buf.toString();
    }
}
