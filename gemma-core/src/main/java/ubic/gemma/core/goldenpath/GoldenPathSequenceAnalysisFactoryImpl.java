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

import org.springframework.stereotype.Component;
import ubic.gemma.model.genome.Taxon;

/**
 * Default {@link GoldenPathSequenceAnalysisFactory} that constructs a real
 * {@link GoldenPathSequenceAnalysis} bound to the configured GoldenPath
 * database for the supplied taxon.
 *
 * @author goldenpath-factory-refactor
 */
@Component
public class GoldenPathSequenceAnalysisFactoryImpl implements GoldenPathSequenceAnalysisFactory {

    @Override
    public GoldenPathSequenceAnalysis create( Taxon taxon ) {
        return new GoldenPathSequenceAnalysis( taxon );
    }
}
