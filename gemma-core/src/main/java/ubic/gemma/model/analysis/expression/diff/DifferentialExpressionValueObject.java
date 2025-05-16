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

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Represents the results for one probe. Fairly heavy-weight.
 *
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
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
        this.p = o.getPvalue();
        this.corrP = o.getCorrectedPvalue();
        this.probe = o.getProbe().getName();
        this.probeId = o.getProbe().getId();
        this.resultSetId = o.getResultSet().getId();

        this.contrasts = new ContrastsValueObject( this.id );
        for ( ContrastResult c : o.getContrasts() ) {
            contrasts.addContrast( c.getId(), c.getFactorValue() == null ? null : c.getFactorValue().getId(), c.getLogFoldChange(), c.getPvalue(),
                    c.getSecondFactorValue() == null ? null : c.getSecondFactorValue().getId() );
        }
    }

    public DifferentialExpressionValueObject( Long id ) {
        this.id = id;
        this.contrasts = new ContrastsValueObject( this.id );
    }

    /**
     * @param cid                 id of the contrast
     * @param secondFactorValueId null unless this is an interaction
     * @param pvalue              the p value
     * @param factorValueId       factor value id
     * @param logFoldChange       the fold change
     */
    public void addContrast( Long cid, Long factorValueId, Double pvalue, Double logFoldChange,
            Long secondFactorValueId ) {
        this.contrasts.addContrast( cid, factorValueId, logFoldChange, pvalue, secondFactorValueId );
    }

    public ContrastsValueObject getContrasts() {
        return contrasts;
    }

    public void setContrasts( ContrastsValueObject contrasts ) {
        this.contrasts = contrasts;
    }

    public Double getCorrP() {
        return corrP;
    }

    public void setCorrP( Double corrP ) {
        this.corrP = corrP;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection( Direction direction ) {
        this.direction = direction;
    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactorValueObject> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

    public BioAssaySetValueObject getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( BioAssaySetValueObject expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public Boolean getFisherContribution() {
        return fisherContribution;
    }

    public void setFisherContribution( Boolean fisherContribution ) {
        this.fisherContribution = fisherContribution;
    }

    public GeneValueObject getGene() {
        return gene;
    }

    public void setGene( GeneValueObject gene ) {
        this.gene = gene;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Boolean getMetThreshold() {
        return metThreshold;
    }

    public void setMetThreshold( Boolean metThreshold ) {
        this.metThreshold = metThreshold;
    }

    public Double getP() {
        return p;
    }

    public void setP( Double p ) {
        this.p = p;
    }

    public String getProbe() {
        return probe;
    }

    public void setProbe( String probe ) {
        this.probe = probe;
    }

    public Long getProbeId() {
        return probeId;
    }

    public void setProbeId( Long probeId ) {
        this.probeId = probeId;
    }

    public Long getResultSetId() {
        return resultSetId;
    }

    public void setResultSetId( Long long1 ) {
        this.resultSetId = long1;

    }

    public String getSortKey() {
        return sortKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
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

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", p, gene.getOfficialSymbol() );
    }

}
