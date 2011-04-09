/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisConfig implements Serializable {

    private static final long serialVersionUID = 622877438067070041L;

    private AnalysisType analysisType;

    private Map<ExperimentalFactor, FactorValue> baseLineFactorValues = new HashMap<ExperimentalFactor, FactorValue>();

    private List<ExperimentalFactor> factorsToInclude = new ArrayList<ExperimentalFactor>();

    private Collection<Collection<ExperimentalFactor>> interactionsToInclude = new HashSet<Collection<ExperimentalFactor>>();

    private ExperimentalFactor subsetFactor;

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    /**
     * @return the baseLineFactorValues
     */
    public Map<ExperimentalFactor, FactorValue> getBaseLineFactorValues() {
        return baseLineFactorValues;
    }

    /**
     * @return the factorsToInclude
     */
    public List<ExperimentalFactor> getFactorsToInclude() {
        return factorsToInclude;
    }

    /**
     * @return the interactionsToInclude
     */
    public Collection<Collection<ExperimentalFactor>> getInteractionsToInclude() {
        return interactionsToInclude;
    }

    public void addInteractionToInclude( Collection<ExperimentalFactor> factors ) {
        interactionsToInclude.add( factors );
    }

    public void addInteractionToInclude( ExperimentalFactor... factors ) {
        interactionsToInclude.add( Arrays.asList( factors ) );
    }

    /**
     * @return the subsetFactor
     */
    public ExperimentalFactor getSubsetFactor() {
        return subsetFactor;
    }

    public void setAnalysisType( AnalysisType analysisType ) {
        this.analysisType = analysisType;
    }

    /**
     * @param baseLineFactorValues the baseLineFactorValues to set
     */
    public void setBaseLineFactorValues( Map<ExperimentalFactor, FactorValue> baseLineFactorValues ) {
        this.baseLineFactorValues = baseLineFactorValues;
    }

    /**
     * @param factorsToInclude the factorsToInclude to set
     */
    public void setFactorsToInclude( Collection<ExperimentalFactor> factorsToInclude ) {
        this.factorsToInclude = new ArrayList<ExperimentalFactor>( factorsToInclude );
    }

    /**
     * @param interactionsToInclude the interactionsToInclude to set
     */
    public void setInteractionsToInclude( Collection<Collection<ExperimentalFactor>> interactionsToInclude ) {
        this.interactionsToInclude = interactionsToInclude;
    }

    /**
     * @param subsetFactor the subsetFactor to set
     */
    public void setSubsetFactor( ExperimentalFactor subsetFactor ) {
        this.subsetFactor = subsetFactor;
    }

    /**
     * @return representation of this analysis (not completely filled in - only the basic parameters)
     */
    public DifferentialExpressionAnalysis toAnalysis() {
        DifferentialExpressionAnalysis analysis = DifferentialExpressionAnalysis.Factory.newInstance();
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Differential expression analysis settings" );
        protocol.setDescription( this.toString() );
        analysis.setProtocol( protocol );
        return analysis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuilder buf = new StringBuilder();
        buf.append( "Factors:\n" );
        for ( ExperimentalFactor factor : this.factorsToInclude ) {
            buf.append( factor + "\n" );
        }

        if ( !interactionsToInclude.isEmpty() ) {
            buf.append( "Interactions:\n" );

            for ( Collection<ExperimentalFactor> factors : this.interactionsToInclude ) {
                buf.append( StringUtils.join( factors, ":" ) + "\n" );
            }
        }

        if ( !baseLineFactorValues.isEmpty() ) {
            buf.append( "Baselines:\n" );

            for ( ExperimentalFactor ef : baseLineFactorValues.keySet() ) {
                buf.append( ef + "-->" + baseLineFactorValues.get( ef ) + "\n" );
            }
        }

        return buf.toString();

    }

}
