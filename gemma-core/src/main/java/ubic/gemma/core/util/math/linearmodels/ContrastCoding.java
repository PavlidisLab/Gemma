/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.core.util.math.linearmodels;

/**
 * How a categorical factor's levels are turned into columns of a design matrix, which is what decides what each
 * coefficient of that factor means.
 * <p>
 * Both codings give a factor with k levels k-1 columns, so the model has the same rank either way and nothing
 * downstream of the fit changes shape. What changes is the question each coefficient answers.
 *
 * @author paul
 * @see DesignMatrix#setContrastCoding(String, ContrastCoding)
 */
public enum ContrastCoding {

    /**
     * Each level gets an indicator column; one level has none and is the baseline. A coefficient is that level's
     * difference from the baseline. R's {@code contr.treatment}, and Gemma's behaviour everywhere until this enum
     * existed.
     * <p>
     * This is the right coding when one level is a control that the others are meant to be read against — a
     * wild-type, an untreated arm, a vehicle.
     */
    TREATMENT,

    /**
     * Each level gets a column, the one left over takes {@code -1} in all of them, and a coefficient is that
     * level's deviation from the mean of the level means. R's {@code contr.sum}; also called deviation or
     * sum-to-zero coding.
     * <p>
     * This is the coding for a factor with no control level — a panel of tissues, a set of cell lines, a
     * collection of time points treated symmetrically. Under treatment coding one of those has to be nominated as
     * the baseline, every other level is then reported relative to that arbitrary choice, and the nominated one
     * gets no contrast of its own. Here every level is reported against the same reference and none of them is
     * privileged.
     * <p>
     * 🛑 The reference is the mean of the LEVEL means, not the mean of the samples. They differ whenever the
     * levels have different numbers of samples, and the level means are what the model estimates.
     * <p>
     * The deviations sum to zero by construction, so the level without a column of its own is still fully
     * determined: its deviation is minus the sum of the others.
     */
    SUM_TO_ZERO
}
