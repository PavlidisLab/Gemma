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
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.preprocess.filter.RepetitiveValuesFilter;
import ubic.gemma.model.expression.bioAssay.BioAssay;
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
     * Default value for whether moderated test statistics should be used.
     */
    public static final boolean DEFAULT_MODERATE_STATISTICS = true;

    /**
     * Type of analysis to perform.
     */
    private AnalysisType analysisType;

    /**
     * For each categorical factor, indicate the baseline factor value to use.
     */
    private final Map<ExperimentalFactor, FactorValue> baselineFactorValues = new HashMap<>();

    /**
     * Whether moderated test statistics should be used.
     * <p>
     * This is usually done with the empirical Bayes method.
     */
    private boolean moderateStatistics = DifferentialExpressionAnalysisConfig.DEFAULT_MODERATE_STATISTICS;

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
     * If this is non-null, this is a subset analysis for this factor value.
     * <p>
     * Only applicable for analysis on a {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet}.
     */
    @Nullable
    private FactorValue subsetFactorValue;

    /**
     * Keep processing other subsets when encountering an {@link AnalysisException} on a subset.
     * <p>
     * If all subset fails, an {@link AllSubSetAnalysesFailedException} will be raised which will contain individual
     * subset failures.
     */
    private boolean ignoreFailingSubsets = false;

    /**
     * Set true for RNA-seq data sets
     */
    private boolean useWeights = false;

    /**
     * Override the minimum number of cells for a particular assay to be included in the analysis.
     * <p>
     * This is only applied if {@link BioAssay#getNumberOfCells()} is populated.
     * <p>
     * Defaults to {@link DifferentialExpressionAnalysisFilter#DEFAULT_MINIMUM_NUMBER_OF_CELLS}.
     */
    @Nullable
    private Integer minimumNumberOfCells = null;

    /**
     * Override the default mode of operation for the {@link RepetitiveValuesFilter}.
     * <p>
     * Defaults to {@link DifferentialExpressionAnalysisFilter#DEFAULT_REPETITIVE_VALUES_FILTER_MODE}
     */
    @Nullable
    private DifferentialExpressionAnalysisFilter.RepetitiveValuesFilterMode repetitiveValuesFilterMode = null;

    /**
     * Override the minimum number of samples to apply the repetitive value filter.
     * <p>
     * Defaults to {@link DifferentialExpressionAnalysisFilter#DEFAULT_MINIMUM_NUMBER_OF_SAMPLES_TO_APPLY_REPETITIVE_VALUES_FILTER}
     */
    @Nullable
    private Integer minimumNumberOfSamplesToApplyRepetitiveValuesFilter = null;

    /**
     * Override the minimum number of unique values (as a fraction of the number of assays) for a particular design
     * element to be included in the analysis.
     * <p>
     * Defaults to {@link DifferentialExpressionAnalysisFilter#DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES}.
     */
    @Nullable
    private Double minimumFractionOfUniqueValues = null;

    /**
     * Override the minimum variance for a particular design element to be included in the analysis.
     * <p>
     * Defaults to {@link DifferentialExpressionAnalysisFilter#DEFAULT_MINIMUM_VARIANCE}.
     */
    @Nullable
    private Double minimumVariance = null;

    /**
     * Whether to create archive files.
     */
    private boolean makeArchiveFile = true;

    /**
     * Maximum time to spend on the analysis, in milliseconds. Ignored if zero or less.
     */
    private long maxAnalysisTimeMillis = 0;

    public DifferentialExpressionAnalysisConfig() {
    }

    /**
     * Copy constructor.
     */
    public DifferentialExpressionAnalysisConfig( DifferentialExpressionAnalysisConfig baseConfig ) {
        this.analysisType = baseConfig.getAnalysisType();
        this.baselineFactorValues.putAll( baseConfig.getBaselineFactorValues() );
        this.moderateStatistics = baseConfig.isModerateStatistics();
        this.factorsToInclude.addAll( baseConfig.getFactorsToInclude() );
        this.interactionsToInclude.addAll( baseConfig.getInteractionsToInclude() );
        this.persist = baseConfig.isPersist();
        this.subsetFactor = baseConfig.getSubsetFactor();
        this.subsetFactorValue = baseConfig.getSubsetFactorValue();
        this.ignoreFailingSubsets = baseConfig.isIgnoreFailingSubsets();
        this.useWeights = baseConfig.isUseWeights();
        this.makeArchiveFile = baseConfig.isMakeArchiveFile();
        this.maxAnalysisTimeMillis = baseConfig.getMaxAnalysisTimeMillis();
        this.repetitiveValuesFilterMode = baseConfig.getRepetitiveValuesFilterMode();
        this.minimumFractionOfUniqueValues = baseConfig.getMinimumFractionOfUniqueValues();
        this.minimumNumberOfSamplesToApplyRepetitiveValuesFilter = baseConfig.getMinimumNumberOfSamplesToApplyRepetitiveValuesFilter();
        this.minimumNumberOfCells = baseConfig.getMinimumNumberOfCells();
        this.minimumVariance = baseConfig.getMinimumVariance();
    }

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

    public void addInteractionsToInclude( Collection<Collection<ExperimentalFactor>> interactions ) {
        for ( Collection<ExperimentalFactor> interaction : interactions ) {
            addInteractionToInclude( interaction );
        }
    }

    public void addBaseLineFactorValues( Map<ExperimentalFactor, FactorValue> baselineConditions ) {
        baselineFactorValues.putAll( baselineConditions );
    }
}
