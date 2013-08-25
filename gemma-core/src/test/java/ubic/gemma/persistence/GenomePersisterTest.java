/*
 * The Gemma project
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
package ubic.gemma.persistence;

import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GenomePersisterTest extends BaseSpringContextTest {

    @Test
    public void testPersistGene() throws Exception {

        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.randomAlphabetic( 10 ) );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.randomNumeric( 8 ) ) );

        Collection<GeneProduct> gps = new HashSet<GeneProduct>();
        for ( int i = 0; i < 10; i++ ) {
            GeneProduct gp = GeneProduct.Factory.newInstance();
            gp.setName( RandomStringUtils.randomAlphabetic( 10 ) );
            gp.setGene( gene );
            gp.setNcbiGi( RandomStringUtils.randomAlphabetic( 10 ) );
            gps.add( gp );
        }

        gene.setProducts( gps );

        gene = ( Gene ) this.persisterHelper.persistOrUpdate( gene );

        assertNotNull( gene.getId() );
        assertNotNull( gene.getName() );
        assertNotNull( gene.getProducts() );
        for ( GeneProduct product : gene.getProducts() ) {
            assertNotNull( product.getId() );
            assertNotNull( product.getName() );
        }
    }

    /**
     * Going the opposite way as the other test.
     * 
     * @throws Exception
     */
    @Test
    public void testPersistGeneProduct() throws Exception {
        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.randomAlphabetic( 10 ) );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.randomNumeric( 8 ) ) );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( RandomStringUtils.randomAlphabetic( 10 ) );
        gp.setGene( gene );
        gp.setNcbiGi( RandomStringUtils.randomAlphabetic( 10 ) );
        gene.getProducts().add( gp );

        gp = ( GeneProduct ) this.persisterHelper.persist( gp );

        assertNotNull( gp.getId() );
        assertNotNull( gp.getGene().getId() );
    }

}
