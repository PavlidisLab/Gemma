/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.core.goldenpath;

import ubic.gemma.model.genome.Taxon;

/**
 * Factory for {@link GoldenPathSequenceAnalysis} instances.
 * <p>
 * Provides a Spring-injectable seam so callers do not instantiate
 * {@code GoldenPathSequenceAnalysis} directly. Test contexts can register a
 * primary bean that returns a Mockito mock, allowing fast (non-GoldenPath)
 * variants of tests that exercise the {@code processArrayDesign} pipeline.
 * <p>
 * Each call to {@link #create(Taxon)} returns a new {@link GoldenPathSequenceAnalysis};
 * the caller owns the lifecycle (typically via try-with-resources, since the
 * returned instance implements {@link AutoCloseable}).
 *
 * @author goldenpath-factory-refactor
 */
public interface GoldenPathSequenceAnalysisFactory {

    /**
     * Construct a new {@link GoldenPathSequenceAnalysis} for the given taxon.
     * <p>
     * The returned instance holds JDBC resources and must be closed by the caller.
     *
     * @param taxon the taxon whose GoldenPath database should back the analysis
     * @return a new, open {@link GoldenPathSequenceAnalysis}
     */
    GoldenPathSequenceAnalysis create( Taxon taxon );
}
