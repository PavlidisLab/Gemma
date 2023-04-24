/*
 * The Gemma project
 *
 * Copyright (c) 2007-2013 University of British Columbia
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
package ubic.gemma.core.ontology;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author paul
 */
public class OntologyServiceTest extends BaseSpringContextTest {

    @Autowired
    private OntologyService os;

    @Autowired
    private DiseaseOntologyService diseaseOntologyService;

    @Test
    public void test() throws SearchException, OntologySearchException, InterruptedException {
        OntologyTestUtils.initialize( diseaseOntologyService,
                this.getClass().getResourceAsStream( "/data/loader/ontology/dotest.owl.xml" ) );

        Collection<CharacteristicValueObject> name = os.findTermsInexact( "diarrhea", null );

        assertTrue( name.size() > 0 );

        OntologyTerm t1 = os.getTerm( "http://purl.obolibrary.org/obo/DOID_0050001" );
        assertNotNull( t1 );

        // Actinomadura madurae infectious disease
        assertTrue( os.isObsolete( "http://purl.obolibrary.org/obo/DOID_0050001" ) );

        // inflammatory diarrhea, not obsolete as of May 2012.
        assertNotNull( os.getTerm( "http://purl.obolibrary.org/obo/DOID_0050132" ) );
        assertTrue( !os.isObsolete( "http://purl.obolibrary.org/obo/DOID_0050132" ) );

    }
}
