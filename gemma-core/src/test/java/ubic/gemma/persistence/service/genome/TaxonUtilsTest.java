/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonUtils;

import static org.junit.Assert.assertTrue;

/**
 * @author kelsey
 *
 */
public class TaxonUtilsTest extends BaseSpringContextTest {

    @Autowired
    GeneService geneService;

    @Test
    public void testIsHuman() {
        Taxon humanTax = taxonService.findByCommonName( "human" );
        assertTrue( TaxonUtils.isHuman( humanTax ) );
    }

    @Test
    public void testIsMouse() {

        Taxon mouseTax = taxonService.findByCommonName( "mouse" );
        assertTrue( TaxonUtils.isMouse( mouseTax ) );

    }

    @Test
    public void testIsRat() {

        Taxon ratTax = taxonService.findByCommonName( "rat" );
        assertTrue( TaxonUtils.isRat( ratTax ) );

    }

}
