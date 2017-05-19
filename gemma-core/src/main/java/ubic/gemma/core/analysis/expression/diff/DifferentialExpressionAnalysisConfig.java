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
package ubic.gemma.core.analysis.expression.diff;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Holds the settings used for differential expression analysis, and defines some defaults.
 * 
 * @author keshav
 */
public class DifferentialExpressionAnalysisConfig implements Serializable {

    /**
     * The default value used for retention of the results. If null, everything will be stored.
     */
    public static final Double DEFAULT_QVALUE_THRESHOLD = null;

    /**
     * Default value for whether empirical Bayes moderation of test statistics should be used.
     */
    private static final boolean DEFAULT_EBAYES = false;

    private static final long serialVersionUID = 622877438067070041L;

    private AnalysisType analysisType;

    private Map<ExperimentalFactor, FactorValue> baseLineFactorValues = new HashMap<>();

    private boolean ebayes = DEFAULT_EBAYES;

    private List<ExperimentalFactor> factorsToInclude = new ArrayList<>();

    private Collection<Collection<ExperimentalFactor>> interactionsToInclude = new HashSet<>();

    private Double qvalueThreshold = DEFAULT_QVALUE_THRESHOLD;

    private ExperimentalFactor subsetFactor;

    public void addInteractionToInclude( Collection<ExperimentalFactor> factors ) {
        interactionsToInclude.add( factors );
    }

    public void addInteractionToInclude( ExperimentalFactor... factors ) {
        interactionsToInclude.add( Arrays.asList( factors ) );
    }

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
     * @return true if empirical Bayes moderated test statisics should be used
     */
    public boolean getModerateStatistics() {
        return this.ebayes;
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

    /**
     * @return threshold for retention of results.
     */
    public Double getQvalueThreshold() {
        return qvalueThreshold;
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
     * @param factorsToInclude
     */
    public void setFactorsToInclude( Collection<ExperimentalFactor> factorsToInclude ) {
        if ( factorsToInclude instanceof List<?> ) {
            this.factorsToInclude = ( List<ExperimentalFactor> ) factorsToInclude;
        }
        this.factorsToInclude = ExperimentalDesignUtils.sortFactors( factorsToInclude );
    }

    /**
     * @param factorsToInclude the factorsToInclude to set
     */
    public void setFactorsToInclude( List<ExperimentalFactor> factorsToInclude ) {
        this.factorsToInclude = factorsToInclude;
    }

    /**
     * @param interactionsToInclude the interactionsToInclude to set
     */
    public void setInteractionsToInclude( Collection<Collection<ExperimentalFactor>> interactionsToInclude ) {
        this.interactionsToInclude = interactionsToInclude;
    }

    /**
     * @param ebayes
     */
    public void setModerateStatistics( boolean ebayes ) {
        this.ebayes = ebayes;
    }

    /**
     * @param qValueThreshold threshold for retention of results. Set to null to retain all.
     */
    public void setQvalueThreshold( Double qValueThreshold ) {
        this.qvalueThreshold = qValueThreshold;
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

        if ( this.ebayes ) {
            buf.append( "Empirical Bayes moderated statistics used\n" );
        }

        return buf.toString();

    }

}
