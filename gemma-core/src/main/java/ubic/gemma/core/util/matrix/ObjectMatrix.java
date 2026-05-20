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
package ubic.gemma.core.util.matrix;

import java.util.List;

/**
 * @author Paul
 * 
 */
public interface ObjectMatrix<R, C, V> extends Matrix2D<R, C, V> {

    public V get( int row, int col );

    public V[] getColumn( int column );

    public V[] getRow( int row );

    /**
     * @param startRow
     * @param startCol
     * @param numRow
     * @param numCol
     * @return
     */
    public ObjectMatrix<R, C, V> subset( int startRow, int startCol, int numRow, int numCol );

    /**
     * @param columns
     * @return
     */
    public ObjectMatrix<R, C, V> subsetColumns( List<C> columns );
}
