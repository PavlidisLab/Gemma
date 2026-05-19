/*
 * The Gemma21 project
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

import ubic.gemma.core.ontology.basecode.jena.ClasspathOntologyService;
import ubic.basecode.util.Configuration;

/**
 * MEDIC ONTOLOGY USED BY PHENOCARTA, its represents MESH terms as a tree so with can use the parent structure that a
 * normal mesh term doesnt have
 * <p>
 * MEDIC comes from the CTD folks. See <a href="http://ctd.mdibl.org/voc.go?type=disease">...</a>. Unfortunately I do not know where our
 * medic.owl file came from (PP)
 *
 * @author Nicolas
 */
@Deprecated
public class MedicOntologyService extends AbstractDelegatingOntologyService {

    /**
     * FIXME this shouldn't be hard-coded like this, we should load it like any other ontology service.
     */
    private static final String MEDIC_ONTOLOGY_FILE = "/data/loader/ontology/medic.owl.gz";

    public MedicOntologyService() {
        super( new ClasspathOntologyService( "Medic Ontology", MEDIC_ONTOLOGY_FILE,
            Boolean.TRUE.equals( Configuration.getBoolean( "load.medicOntology" ) ), "medicOntology" ) );
    }
}
