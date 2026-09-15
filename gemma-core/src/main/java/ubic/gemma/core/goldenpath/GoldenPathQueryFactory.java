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
 * Factory for {@link GoldenPathQuery} instances.
 * <p>
 * Sibling to {@link GoldenPathSequenceAnalysisFactory} (commit
 * {@code 317ea9c785}); provides a Spring-injectable seam so callers do not
 * instantiate {@code GoldenPathQuery} directly. Test contexts can register a
 * primary bean that returns a Mockito mock, enabling fast variants of tests
 * that exercise the alignment lookup pipeline without requiring a reachable
 * UCSC GoldenPath database.
 * <p>
 * Each call to {@link #create(Taxon)} returns a new {@link GoldenPathQuery};
 * the caller owns the lifecycle (typically via try-with-resources, since the
 * returned instance implements {@link AutoCloseable}).
 */
public interface GoldenPathQueryFactory {

    /**
     * Construct a new {@link GoldenPathQuery} for the given taxon.
     * <p>
     * The returned instance holds JDBC resources and must be closed by the caller.
     *
     * @param taxon the taxon whose GoldenPath database should back the query
     * @return a new, open {@link GoldenPathQuery}
     */
    GoldenPathQuery create( Taxon taxon );
}
