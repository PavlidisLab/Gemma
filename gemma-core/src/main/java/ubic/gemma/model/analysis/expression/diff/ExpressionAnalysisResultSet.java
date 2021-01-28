/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.HashSet;

/**
 * A group of results for an ExpressionExperiment.
 */
public class ExpressionAnalysisResultSet extends FactorAssociatedAnalysisResultSet<DifferentialExpressionAnalysisResult> {

    private static final long serialVersionUID = 7226901182513177574L;
    private Integer numberOfProbesTested;
    private Integer numberOfGenesTested;
    private FactorValue baselineGroup;
    private Collection<DifferentialExpressionAnalysisResult> results = new java.util.HashSet<>();
    private DifferentialExpressionAnalysis analysis;
    private PvalueDistribution pvalueDistribution;
    private Collection<HitListSize> hitListSizes = new HashSet<>();

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for ( DifferentialExpressionAnalysisResult dear : this.getResults() ) {
            int count = 0;

            CompositeSequence cs = dear.getProbe();
            buf.append( cs.getName() ).append( "\t" );
            for ( BioSequence2GeneProduct bs2gp : cs.getBiologicalCharacteristic().getBioSequence2GeneProduct() ) {
                Gene g = bs2gp.getGeneProduct().getGene();
                if ( g != null ) {
                    buf.append( bs2gp.getGeneProduct().getGene().getOfficialSymbol() ).append( "," );
                    count++;
                }
            }
            if ( count != 0 )
                buf.deleteCharAt( buf.lastIndexOf( "," ) ); // removing trailing ,
            buf.append( "\t" );

            count = 0;
            for ( ExperimentalFactor ef : this.getExperimentalFactors() ) {
                buf.append( ef.getName() ).append( "," );
                count++;
            }
            if ( count != 0 )
                buf.deleteCharAt( buf.lastIndexOf( "," ) ); // removing trailing ,

            buf.append( "\t" );

            buf.append( dear.getCorrectedPvalue() ).append( "\n" );
        }
        return buf.toString();

    }

    public DifferentialExpressionAnalysis getAnalysis() {
        return this.analysis;
    }

    public void setAnalysis( DifferentialExpressionAnalysis analysis ) {
        this.analysis = analysis;
    }

    /**
     * @return The group considered baseline when computing scores and 'upRegulated'. This might be a control group if it could
     * be recognized. For continuous factors, this would be null. For interaction terms we do not compute this so it
     * will also be null.
     */
    public FactorValue getBaselineGroup() {
        return this.baselineGroup;
    }

    public void setBaselineGroup( FactorValue baselineGroup ) {
        this.baselineGroup = baselineGroup;
    }

    public Collection<HitListSize> getHitListSizes() {
        return this.hitListSizes;
    }

    public void setHitListSizes( Collection<HitListSize> hitListSizes ) {
        this.hitListSizes = hitListSizes;
    }

    /**
     * @return How many genes were tested in the result set. This is determined based on the annotations at one point in time,
     * so might slightly differ if the platform annotations have been updated since.
     */
    public Integer getNumberOfGenesTested() {
        return this.numberOfGenesTested;
    }

    public void setNumberOfGenesTested( Integer numberOfGenesTested ) {
        this.numberOfGenesTested = numberOfGenesTested;
    }

    /**
     * @return How many probes were tested in this result set.
     */
    public Integer getNumberOfProbesTested() {
        return this.numberOfProbesTested;
    }

    public void setNumberOfProbesTested( Integer numberOfProbesTested ) {
        this.numberOfProbesTested = numberOfProbesTested;
    }

    public PvalueDistribution getPvalueDistribution() {
        return this.pvalueDistribution;
    }

    public void setPvalueDistribution( PvalueDistribution pvalueDistribution ) {
        this.pvalueDistribution = pvalueDistribution;
    }

    @Override
    public Collection<DifferentialExpressionAnalysisResult> getResults() {
        return this.results;
    }

    public void setResults( Collection<DifferentialExpressionAnalysisResult> results ) {
        this.results = results;
    }

    public static final class Factory {

        public static ExpressionAnalysisResultSet newInstance() {
            return new ExpressionAnalysisResultSet();
        }

        public static ExpressionAnalysisResultSet newInstance( Collection<ExperimentalFactor> experimentalFactors,
                Integer numberOfProbesTested, java.lang.Integer numberOfGenesTested, FactorValue baselineGroup,
                Collection<DifferentialExpressionAnalysisResult> results, DifferentialExpressionAnalysis analysis,
                PvalueDistribution pvalueDistribution, Collection<HitListSize> hitListSizes ) {
            final ExpressionAnalysisResultSet entity = new ExpressionAnalysisResultSet();
            entity.setExperimentalFactors( experimentalFactors );
            entity.setNumberOfProbesTested( numberOfProbesTested );
            entity.setNumberOfGenesTested( numberOfGenesTested );
            entity.setBaselineGroup( baselineGroup );
            entity.setResults( results );
            entity.setAnalysis( analysis );
            entity.setPvalueDistribution( pvalueDistribution );
            entity.setHitListSizes( hitListSizes );
            return entity;
        }
    }

}