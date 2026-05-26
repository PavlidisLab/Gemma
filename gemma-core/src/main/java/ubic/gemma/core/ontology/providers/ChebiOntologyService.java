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

package ubic.gemma.core.ontology.providers;

import ubic.gemma.core.config.Configuration;
import ubic.gemma.core.ontology.jena.RO;
import ubic.gemma.core.ontology.jena.UrlOntologyService;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <a href="https://obofoundry.org/ontology/chebi.html">Chemical Entities of Biological Interest</a>
 *
 * <p>CHEBI's chemistry-based {@code subClassOf} hierarchy ("estradiol is-a steroid is-a lipid")
 * isn't what we usually want for treatment classification — we care about pharmacological
 * <em>roles</em> ("estradiol has-role hormone", "sorafenib has-role kinase inhibitor"). CHEBI
 * captures those via the {@code RO:0000087 has_role} object property. Adding it to the
 * loader's additional-property set means {@code getChildren(roleClass, includeAdditionalProperties=true)}
 * walks both {@code subClassOf} (to subroles) AND the inverse of {@code has_role} (to chemicals
 * bearing the role), returning a unified set that callers can intersect with the corpus.
 *
 * <p>Extends {@link UrlOntologyService} directly (not via {@link AbstractDelegatingOntologyService})
 * so CHEBI-specific overrides — notably {@code loadModel} for the slim-CHEBI cache planned in
 * Phase 4 of the ontology-hierarchy refactor — can hook into the load path.
 *
 * @author klc
 */
public class ChebiOntologyService extends UrlOntologyService {

    public ChebiOntologyService() {
        super( "CHEBI",
            requireNonNull( Configuration.getString( "url.chebiOntology" ) ),
            Boolean.TRUE.equals( Configuration.getBoolean( "load.chebiOntology" ) ),
            "chebiOntology" );
        Set<String> props = new HashSet<>( getAdditionalPropertyUris() );
        props.add( RO.hasRole.getURI() );
        setAdditionalPropertyUris( props );
    }
}
