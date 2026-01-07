/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.analysis.preprocess.filter;

import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;

/**
 * Base interface for expression data filters.
 * <p>
 * A filter is a function that takes an expression data matrix and produces a filtered version of it.
 *
 * @param <T> the type of expression data matrix this filter operates on
 * @author pavlidis
 */
@FunctionalInterface
public interface ExpressionDataFilter<T extends ExpressionDataMatrix<?>> {

    /**
     * Apply the filter to the given data matrix.
     * <p>
     * If the filter is not applicable as per {@link #appliesTo(ExpressionDataMatrix)}, the original data matrix must be
     * returned unchanged.
     * <p>
     * Filters should not modify the input data matrix in place, and always returned a new filtered instance, unless no
     * change is needed in which case the original instance can be returned.
     *
     * @param dataMatrix the data matrix to filter
     * @return a filtered data matrix
     * @throws FilteringException if anything went wrong during filtering, usually when no rows or columns are left
     */
    T filter( T dataMatrix ) throws FilteringException;

    /**
     * Check if the filter is applicable to the given data matrix.
     */
    default boolean appliesTo( T dataMatrix ) {
        return true;
    }
}
