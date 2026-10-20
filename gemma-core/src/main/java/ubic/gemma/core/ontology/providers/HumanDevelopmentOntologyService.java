/*
 * The baseCode project
 *
 * Copyright (c) 2010 University of British Columbia
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

/**
 * <a href="https://www.ebi.ac.uk/ols4/ontologies/ehdaa2">Human developmental anatomy, abstract</a>
 *
 * @author paul
 * @deprecated this ontology was last updated in unmaintained since 2013
 */
@Deprecated
public class HumanDevelopmentOntologyService extends AbstractBaseCodeOntologyService {

    public HumanDevelopmentOntologyService() {
        super( "Human Development Ontology", "humanDevelOntology" );
    }
}
