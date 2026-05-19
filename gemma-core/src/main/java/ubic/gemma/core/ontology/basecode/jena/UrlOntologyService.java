/*
 * The basecode project
 *
 * Copyright (c) 2007-2019 University of British Columbia
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
/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */

package ubic.gemma.core.ontology.basecode.jena;

import ubic.gemma.core.ontology.basecode.model.OntologyModel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * An ontology service that loads an ontology from a URL.
 */
public class UrlOntologyService extends AbstractOntologyService {

    public UrlOntologyService( String ontologyName, String ontologyUrl, boolean ontologyEnabled, @Nullable String cacheName ) {
        super( ontologyName, ontologyUrl, ontologyEnabled, cacheName );
    }

    @Override
    protected OntologyModel loadModel( boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) throws IOException {
        return new OntologyModelImpl( OntologyLoader.createMemoryModel( getOntologyUrl(), getOntologyUrl(), this.getCacheName(), processImports, this.getSpec( languageLevel, inferenceMode ) ) );
    }

    @Override
    protected OntologyModel loadModelFromStream( InputStream is, boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) throws IOException {
        return new OntologyModelImpl( OntologyLoader.createMemoryModel( is, this.getOntologyUrl(), processImports, this.getSpec( languageLevel, inferenceMode ) ) );
    }
}