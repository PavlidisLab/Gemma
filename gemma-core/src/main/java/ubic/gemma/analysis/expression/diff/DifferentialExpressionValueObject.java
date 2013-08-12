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
package ubic.gemma.analysis.expression.diff;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.analysis.Direction;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Represents the results for one probe.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionValueObject {

    private Long id;
    private GeneValueObject gene;
    private ExpressionExperimentValueObject expressionExperiment;
    private String probe;
    private Long probeId;
    private Collection<ExperimentalFactorValueObject> experimentalFactors;
    private Double p;
    private Direction direction;
    private Double corrP;
    private String sortKey;

    private Boolean metThreshold = false;

    private Boolean fisherContribution = false;

    public Double getCorrP() {
        return corrP;
    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    public ExpressionExperimentValueObject getExpressionExperiment() {
        return expressionExperiment;
    }

    public Boolean getFisherContribution() {
        return fisherContribution;
    }

    public GeneValueObject getGene() {
        return gene;
    }

    public Long getId() {
        return id;
    }

    public Boolean getMetThreshold() {
        return metThreshold;
    }

    public Double getP() {
        return p;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getProbe() {
        return probe;
    }

    public Long getProbeId() {
        return probeId;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setCorrP( Double corrP ) {
        this.corrP = corrP;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactorValueObject> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

    public void setExpressionExperiment( ExpressionExperimentValueObject expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setFisherContribution( Boolean fisherContribution ) {
        this.fisherContribution = fisherContribution;
    }

    public void setGene( GeneValueObject gene ) {
        this.gene = gene;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setMetThreshold( Boolean metThreshold ) {
        this.metThreshold = metThreshold;
    }

    public void setP( Double p ) {
        this.p = p;
    }

    public void setDirection( Direction direction ) {
        this.direction = direction;
    }

    public void setProbe( String probe ) {
        this.probe = probe;
    }

    public void setProbeId( Long probeId ) {
        this.probeId = probeId;
    }

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", p, gene.getOfficialSymbol() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
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

        if ( StringUtils.isNotBlank( expressionExperiment.getShortName() ) ) {
            buf.append( expressionExperiment.getShortName() + "\t" );
        }

        buf.append( probe + "\t" );

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
