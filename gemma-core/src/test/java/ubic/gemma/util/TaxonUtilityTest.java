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

package ubic.gemma.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author kelsey
 * @version $Id$
 */
public class TaxonUtilityTest extends BaseSpringContextTest {

    @Autowired
    GeneService geneService;

    @Test
    public void testIsHuman() throws Exception {
        Taxon humanTax = taxonService.findByCommonName( "human" );
        assertTrue( TaxonUtility.isHuman( humanTax ) );
    }

    @Test
    public void testIsMouse() throws Exception {

        Taxon mouseTax = taxonService.findByCommonName( "mouse" );
        assertTrue( TaxonUtility.isMouse( mouseTax ) );

    }

    @Test
    public void testIsRat() throws Exception {

        Taxon ratTax = taxonService.findByCommonName( "rat" );
        assertTrue( TaxonUtility.isRat( ratTax ) );

    }

}
