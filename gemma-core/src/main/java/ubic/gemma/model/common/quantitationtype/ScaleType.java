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

import java.util.*;

public enum ScaleType {
    /**
     * This is effectively the opposite of "log-transformed" (or any other transformation)
     */
    LINEAR,
    LN,
    LOG2,
    LOG10,
    /**
     * Data is log-transformed as per {@code ln X + 1}
     */
    LOG1P,
    LOGBASEUNKNOWN,
    /**
     * Deprecated, do not use.
     */
    FOLDCHANGE,
    OTHER,
    /**
     * An unscaled measurement is one that has no inherent scale; e.g., a categorial value.
     */
    UNSCALED,
    /**
     * Constrained to be a value between 0 and 1.
     */
    PERCENT1,
    /**
     * Constrained to be a value between 0 and 100.
     */
    PERCENT,
    /**
     * Indicates value was (originally) an integer count of something, such as RNAseq reads. This does not mean the
     * value is necessarily an integer.
     */
    COUNT;
}