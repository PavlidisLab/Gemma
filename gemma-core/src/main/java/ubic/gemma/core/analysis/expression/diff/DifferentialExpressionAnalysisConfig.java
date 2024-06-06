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

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.io.Serializable;
import java.util.*;

/**
 * Holds the settings used for differential expression analysis, and defines some defaults.
 *
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class DifferentialExpressionAnalysisConfig implements Serializable {

    /**
     * Default value for whether empirical Bayes moderation of test statistics should be used.
     */
    public static final boolean DEFAULT_EBAYES = true;

    private static final long serialVersionUID = 622877438067070041L;

    private AnalysisType analysisType = null;

    private Map<ExperimentalFactor, FactorValue> baseLineFactorValues = new HashMap<>();

    private boolean ebayes = DifferentialExpressionAnalysisConfig.DEFAULT_EBAYES;

    private List<ExperimentalFactor> factorsToInclude = new ArrayList<>();

    private Collection<Collection<ExperimentalFactor>> interactionsToInclude = new HashSet<>();

    // save to db or output to console?
    private boolean persist = true;

    private ExperimentalFactor subsetFactor;

    /**
     * If this is non-null, this was a subset analysis, for this factor value.
     */
    private FactorValue subsetFactorValue = null;

    private boolean useWeights = false;
    private boolean makeArchiveFile = true;

    public void addInteractionToInclude( Collection<ExperimentalFactor> factors ) {
        interactionsToInclude.add( factors );
    }

    public void addInteractionToInclude( ExperimentalFactor... factors ) {
        interactionsToInclude.add( Arrays.asList( factors ) );
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType( AnalysisType analysisType ) {
        this.analysisType = analysisType;
    }

    /**
     * @return the baseLineFactorValues
     */
    public Map<ExperimentalFactor, FactorValue> getBaseLineFactorValues() {
        return baseLineFactorValues;
    }

    /**
     * @param baseLineFactorValues the baseLineFactorValues to set
     */
    public void setBaseLineFactorValues( Map<ExperimentalFactor, FactorValue> baseLineFactorValues ) {
        this.baseLineFactorValues = baseLineFactorValues;
    }

    /**
     * @return the factorsToInclude
     */
    public List<ExperimentalFactor> getFactorsToInclude() {
        return factorsToInclude;
    }

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
     * @return the interactionsToInclude
     */
    public Collection<Collection<ExperimentalFactor>> getInteractionsToInclude() {
        return interactionsToInclude;
    }

    /**
     * @param interactionsToInclude the interactionsToInclude to set
     */
    public void setInteractionsToInclude( Collection<Collection<ExperimentalFactor>> interactionsToInclude ) {
        this.interactionsToInclude = interactionsToInclude;
    }

    /**
     * @return true if empirical Bayes moderated test statisics should be used
     */
    public boolean getModerateStatistics() {
        return this.ebayes;
    }

    public void setModerateStatistics( boolean ebayes ) {
        this.ebayes = ebayes;
    }

    public boolean getPersist() {
        return this.persist;
    }

    public void setPersist( boolean persist ) {
        this.persist = persist;

    }

    /**
     * @return the subsetFactor
     */
    public ExperimentalFactor getSubsetFactor() {
        return subsetFactor;
    }

    /**
     * @param subsetFactor the subsetFactor to set
     */
    public void setSubsetFactor( ExperimentalFactor subsetFactor ) {
        this.subsetFactor = subsetFactor;
    }

    public FactorValue getSubsetFactorValue() {
        return subsetFactorValue;
    }

    public void setSubsetFactorValue( FactorValue subsetFactorValue ) {
        this.subsetFactorValue = subsetFactorValue;
    }

    public boolean getUseWeights() {
        return useWeights;
    }

    /*
     * Set true for RNA-seq data sets
     */
    public void setUseWeights( boolean weighted ) {
        this.useWeights = weighted;
    }

    /**
     * @return representation of this analysis with populated protocol holding information from this.
     */
    public DifferentialExpressionAnalysis toAnalysis() {
        DifferentialExpressionAnalysis analysis = DifferentialExpressionAnalysis.Factory.newInstance();
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Differential expression analysis settings" );
        protocol.setDescription( this.toString() );
        analysis.setProtocol( protocol );
        return analysis;
    }

    @Override
    public String toString() {

        StringBuilder buf = new StringBuilder();

        buf.append( "# AnalysisType: " ).append( this.analysisType == null ? "Unknown" : this.analysisType ).append( "\n" );

        buf.append( "# Factors: " ).append( StringUtils.join( this.factorsToInclude, " " ) );

        buf.append( "\n" );

        if ( this.subsetFactor != null ) {
            buf.append( "# SubsetFactor: " ).append( this.subsetFactor ).append( "\n" );
        } else if ( this.subsetFactorValue != null ) {
            buf.append( "# Subset analysis for " ).append( this.subsetFactorValue ).append( "\n" );
        }
        if ( !interactionsToInclude.isEmpty() ) {
            buf.append( "# Interactions:  " ).append( StringUtils.join( interactionsToInclude, ":" ) ).append( "\n" );
        }

        if ( !baseLineFactorValues.isEmpty() ) {
            buf.append( "# Baselines:\n" );
            for ( ExperimentalFactor ef : baseLineFactorValues.keySet() ) {
                buf.append( "# " ).append( ef.getName() ).append( ": Baseline = " )
                        .append( baseLineFactorValues.get( ef ) ).append( "\n" );
            }
        }

        if ( this.ebayes ) {
            buf.append( "# Empirical Bayes moderated statistics used\n" );
        }

        return buf.toString();

    }

    public boolean getMakeArchiveFile() {
        return makeArchiveFile;
    }

    public void setMakeArchiveFile( boolean makeArchiveFile ) {
        this.makeArchiveFile = makeArchiveFile;
    }
}
