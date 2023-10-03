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
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.UberonOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * @author paul
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OntologyServiceIntegrationTest extends BaseSpringContextTest {

    @Autowired
    private OntologyService os;

    @Autowired
    private MondoOntologyService diseaseOntologyService;

    @Autowired
    private UberonOntologyService uberonOntologyService;

    @Test
    public void test() throws SearchException, OntologySearchException, InterruptedException {
        assertEquals( ubic.basecode.ontology.providers.OntologyService.LanguageLevel.FULL, diseaseOntologyService.getLanguageLevel() );
        assertEquals( ubic.basecode.ontology.providers.OntologyService.InferenceMode.TRANSITIVE, diseaseOntologyService.getInferenceMode() );
        OntologyTestUtils.initialize( diseaseOntologyService,
                this.getClass().getResourceAsStream( "/data/loader/ontology/dotest.owl.xml" ) );

        Collection<CharacteristicValueObject> name = os.findTermsInexact( "diarrhea", null );

        assertFalse( name.isEmpty() );

        OntologyTerm t1 = os.getTerm( "http://purl.obolibrary.org/obo/DOID_0050001" );
        assertNotNull( t1 );

        // Actinomadura madurae infectious disease
        assertTrue( os.isObsolete( "http://purl.obolibrary.org/obo/DOID_0050001" ) );

        // inflammatory diarrhea, not obsolete as of May 2012.
        assertNotNull( os.getTerm( "http://purl.obolibrary.org/obo/DOID_0050132" ) );
        assertFalse( os.isObsolete( "http://purl.obolibrary.org/obo/DOID_0050132" ) );
    }

    @Test
    @Category(SlowTest.class)
    public void testSubstantiaNigraInUberon() throws InterruptedException {
        assertEquals( ubic.basecode.ontology.providers.OntologyService.LanguageLevel.FULL, uberonOntologyService.getLanguageLevel() );
        assertEquals( ubic.basecode.ontology.providers.OntologyService.InferenceMode.TRANSITIVE, uberonOntologyService.getInferenceMode() );
        OntologyUtils.ensureInitialized( uberonOntologyService );
        OntologyTerm brain = os.getTerm( "http://purl.obolibrary.org/obo/UBERON_0000955" );
        assertNotNull( brain );
        OntologyTerm substantiaNigra = os.getTerm( "http://purl.obolibrary.org/obo/UBERON_0002038" );
        assertNotNull( substantiaNigra );
        OntologyTerm substantiaNigraParsCompacta = os.getTerm( "http://purl.obolibrary.org/obo/UBERON_0001965" );
        assertNotNull( substantiaNigraParsCompacta );
        assertThat( os.getChildren( Collections.singleton( brain ), false, true ) )
                .contains( substantiaNigra, substantiaNigraParsCompacta );
        assertThat( os.getChildren( Collections.singleton( substantiaNigra ), false, true ) )
                .contains( substantiaNigraParsCompacta );
    }
}
