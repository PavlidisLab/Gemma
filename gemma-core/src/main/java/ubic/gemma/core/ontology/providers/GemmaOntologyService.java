/*
 * The gemma project
 *
 * Copyright (c) 2015 University of British Columbia
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

package ubic.gemma.core.ontology.providers;

import org.springframework.stereotype.Component;
import ubic.basecode.ontology.providers.AbstractOntologyMemoryBackedService;
import ubic.gemma.persistence.util.Settings;

/**
 * Ontology created for Gemma. See bug 4312
 *
 * @author paul
 */
@Component
public class GemmaOntologyService extends AbstractOntologyMemoryBackedService {
    private static final String GEMMA_ONTOLOGY_URL_CONFIG = "url.gemmaOntology";

    @Override
    public String getOntologyName() {
        return "gemmaOntology";
    }

    @Override
    public String getOntologyUrl() {
        return Settings.getString( GEMMA_ONTOLOGY_URL_CONFIG );
    }

}
