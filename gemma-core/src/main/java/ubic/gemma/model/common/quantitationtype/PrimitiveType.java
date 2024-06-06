/*
 * The gemma-core project
 *
 * Copyright (c) 2017 University of British Columbia
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
package ubic.gemma.model.common.quantitationtype;

/**
 * Primitive storage types for data vectors.
 * @see ubic.basecode.io.ByteArrayConverter
 * @see java.io.DataOutputStream
 */
public enum PrimitiveType {
    DOUBLE( 8 ),
    INT( 4 ),
    LONG( 8 ),
    CHAR( 2 ),
    BOOLEAN( 1 ),
    STRING( -1 ),
    INTARRAY( -1 ),
    DOUBLEARRAY( -1 ),
    CHARARRAY( -1 ),
    BOOLEANARRAY( -1 ),
    STRINGARRAY( -1 );

    private final int sizeInBytes;

    PrimitiveType( int sizeInBytes ) {
        this.sizeInBytes = sizeInBytes;
    }

    /**
     * @return a size in bytes, or {@code -1} if the type is of variable size
     */
    public int getSizeInBytes() {
        return sizeInBytes;
    }
}