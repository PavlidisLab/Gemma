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

import java.util.BitSet;

/**
 * Primitive storage types for data vectors.
 */
public enum PrimitiveType {
    FLOAT( Float.class, 4 ),
    DOUBLE( Double.class, 8 ),
    INT( Integer.class, 4 ),
    LONG( Long.class, 8 ),
    CHAR( Character.class, 2 ),
    BOOLEAN( Boolean.class, 1 ),
    STRING( String.class, -1 ),
    BITSET( BitSet.class, -1 );

    private final Class<?> javaClass;
    private final int sizeInBytes;

    PrimitiveType( Class<?> javaClass, int sizeInBytes ) {
        this.javaClass = javaClass;
        this.sizeInBytes = sizeInBytes;
    }

    /**
     * Obtain the Java class that corresponds to this primitive type.
     */
    public Class<?> getJavaClass() {
        return javaClass;
    }

    /**
     * @return a size in bytes, or {@code -1} if the type is of variable size
     */
    public int getSizeInBytes() {
        return sizeInBytes;
    }
}