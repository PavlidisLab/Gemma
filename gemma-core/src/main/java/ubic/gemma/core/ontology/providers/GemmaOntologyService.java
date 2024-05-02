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

import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.Nullable;

/**
 * Ontology created for Gemma. See bug 4312
 *
 * @author paul
 */
public class GemmaOntologyService extends AbstractOntologyService {

    private static final String GEMMA_ONTOLOGY_URL_CONFIG = "url.gemmaOntology";

    @Override
    public String getOntologyName() {
        return "Gemma Ontology";
    }

    @Override
    public String getOntologyUrl() {
        return Settings.getString( GEMMA_ONTOLOGY_URL_CONFIG );
    }

    @Override
    protected boolean isOntologyEnabled() {
        return Settings.getBoolean( "load.gemmaOntology" );
    }

    @Nullable
    @Override
    protected String getCacheName() {
        return "gemmaOntology";
    }
}
