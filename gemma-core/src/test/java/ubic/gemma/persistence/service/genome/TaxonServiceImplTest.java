/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.persistence.service.genome;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author paul
 */
public class TaxonServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private TaxonService taxonService;

    /**
     * Situation where the secondary id is treated as the primary, we must not make a new taxon!
     * <p>
     * This test uses a taxon Gemma isn't using right now but this could be relevant in general.
     */
    @Test
    public void testFindOrCreate() {
        Taxon t = Taxon.Factory.newInstance();
        t.setNcbiId( 559292 );

        Taxon yeast = taxonService.findByCommonName( "yeast" );
        assertNotNull( yeast );

        Taxon found = taxonService.findOrCreate( t );

        assertEquals( Integer.valueOf( 4932 ), found.getNcbiId() );
    }

    @Test
    public void testLoadValueObject() {
        Taxon h = taxonService.findByCommonName( "human" );
        assertNotNull( h );
        TaxonValueObject vo = taxonService.loadValueObject( h );
        assertNotNull( vo );
    }

}
