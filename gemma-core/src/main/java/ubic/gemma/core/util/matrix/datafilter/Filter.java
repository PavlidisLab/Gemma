/*
 * The baseCode project
 * 
 * Copyright (c) 2008-2019 University of British Columbia
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
package ubic.gemma.core.util.matrix.datafilter;

import ubic.gemma.core.util.matrix.Matrix2D;

/**
 * An interface representing the functionality of a class that can filter 2-d matrix-based data by row-oriented
 * criteria.
 * 
 * @author Pavlidis
 * 
 */
public interface Filter<M extends Matrix2D<R, C, V>, R, C, V> {

    /**
     * Filter the data
     * 
     * @param data a NamedMatrix. Some types of filters require that this be of a particular type of implementation of
     *        the Filter interface.
     * @return The resulting filtered matrix
     */
    public M filter( M data );
}