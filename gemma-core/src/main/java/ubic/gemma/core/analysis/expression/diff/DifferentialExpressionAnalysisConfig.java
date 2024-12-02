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

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Holds the settings used for differential expression analysis, and defines some defaults.
 *
 * @author keshav
 */
@Data
public class DifferentialExpressionAnalysisConfig {

    /**
     * Default value for whether empirical Bayes moderation of test statistics should be used.
     */
    public static final boolean DEFAULT_EBAYES = true;

    /**
     * Type of analysis to perform.
     */
    private AnalysisType analysisType;

    /**
     * For each categorical factor, indicate the baseline factor value to use.
     */
    private final Map<ExperimentalFactor, FactorValue> baselineFactorValues = new HashMap<>();

    /**
     * Whether empirical Bayes moderated test statistics should be used.
     */
    private boolean moderateStatistics = DifferentialExpressionAnalysisConfig.DEFAULT_EBAYES;

    private final Set<ExperimentalFactor> factorsToInclude = new HashSet<>();

    private final Set<Set<ExperimentalFactor>> interactionsToInclude = new HashSet<>();

    /**
     * Indicate if this analysis should be persisted.
     */
    private boolean persist = true;

    /**
     * Factor to subset the analysis on, if non-null.
     */
    @Nullable
    private ExperimentalFactor subsetFactor;

    /**
     * If this is non-null, this was a subset analysis, for this factor value.
     * <p>
     * Only applicable for analysis on a {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet}.
     */
    @Nullable
    private FactorValue subsetFactorValue;

    /**
     * Set true for RNA-seq data sets
     */
    private boolean useWeights = false;

    /**
     * Whether to create archive files.
     */
    private boolean makeArchiveFile = true;

    /**
     * Maximum time to spend on the analysis, in milliseconds. Ignored if zero or less.
     */
    private long maxAnalysisTimeMillis = 0;

    /**
     * Add a collection of factors to include in the analysis.
     */
    public void addFactorsToInclude( Collection<ExperimentalFactor> factors ) {
        factorsToInclude.addAll( factors );
    }

    /**
     * Add an interaction of two factors to include in the analysis.
     */
    public void addInteractionToInclude( Collection<ExperimentalFactor> factors ) {
        HashSet<ExperimentalFactor> fs = new HashSet<>( factors );
        Assert.isTrue( fs.size() == 2, "An interaction must have two factors." );
        interactionsToInclude.add( fs );
    }

    public void addBaseLineFactorValues( Map<ExperimentalFactor, FactorValue> baselineConditions ) {
        baselineFactorValues.putAll( baselineConditions );
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

        if ( !baselineFactorValues.isEmpty() ) {
            buf.append( "# Baselines:\n" );
            for ( ExperimentalFactor ef : baselineFactorValues.keySet() ) {
                buf.append( "# " ).append( ef.getName() ).append( ": Baseline = " )
                        .append( baselineFactorValues.get( ef ) ).append( "\n" );
            }
        }

        if ( this.moderateStatistics ) {
            buf.append( "# Empirical Bayes moderated statistics used\n" );
        }

        return buf.toString();
    }
}
