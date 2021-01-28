/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.analysis;

import ubic.gemma.model.IdentifiableValueObject;

import java.util.Collection;

/**
 * Exposes an {@link AnalysisResultSet} to the public API.
 *
 * @param <K> underlying type of analysis result in the set
 * @param <R> type of analysis result set this is wrapping.
 */
public abstract class AnalysisResultSetValueObject<K extends AnalysisResult, R extends AnalysisResultSet<K>> extends IdentifiableValueObject<R> {

    protected AnalysisResultSetValueObject( R analysisResultSet ) {
        super( analysisResultSet.getId() );
    }

    public abstract Collection<AnalysisResultValueObject<K>> getAnalysisResults();
}
