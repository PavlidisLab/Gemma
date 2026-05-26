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

package ubic.gemma.core.ontology.basecode.providers;

import java.util.HashSet;
import java.util.Set;

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
 * @author klc
 */
public class ChebiOntologyService extends AbstractBaseCodeOntologyService {

    /** {@code RO:0000087 has role} — links chemicals to their pharmacological roles. */
    private static final String HAS_ROLE_URI = "http://purl.obolibrary.org/obo/RO_0000087";

    public ChebiOntologyService() {
        super( "CHEBI", "chebiOntology" );
        Set<String> props = new HashSet<>( getAdditionalPropertyUris() );
        props.add( HAS_ROLE_URI );
        setAdditionalPropertyUris( props );
    }
}
