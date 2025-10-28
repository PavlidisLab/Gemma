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
package ubic.gemma.persistence.persister;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pavlidis
 */
public class GenomePersisterTest extends BaseSpringContextTest {

    @Autowired
    BioSequenceService biosequenceService;

    @Test
    public void testPersistGene() {

        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 8 ) ) );

        Set<GeneProduct> gps = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            GeneProduct gp = GeneProduct.Factory.newInstance();
            gp.setName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
            gp.setGene( gene );
            gp.setNcbiGi( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
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

    /*
     * Going the opposite way as the other test.
     *
     */
    @Test
    public void testPersistGeneProduct() {
        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 8 ) ) );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        gp.setGene( gene );
        gp.setNcbiGi( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        gene.getProducts().add( gp );

        gp = ( GeneProduct ) this.persisterHelper.persist( gp );

        assertNotNull( gp.getId() );
        assertNotNull( gp.getGene().getId() );
    }

    @Test
    public void testUpdateBioSequence() {
        Taxon h = this.getTaxon( "human" );

        BioSequence b = BioSequence.Factory.newInstance();
        b.setName( "foo" );
        b.setSequence( "A" );
        b.setTaxon( h );

        Long id = ( ( BioSequence ) this.persisterHelper.persist( b ) ).getId();

        BioSequence br = BioSequence.Factory.newInstance();
        br.setName( "foo" );
        br.setSequence( "T" );
        br.setTaxon( h );
        this.persisterHelper.persistOrUpdate( br ); /// this is what we are testing.

        BioSequence bc = BioSequence.Factory.newInstance();
        bc.setName( "foo" );
        bc.setTaxon( h );
        BioSequence bpl = biosequenceService.find( bc );
        assertNotNull( bpl );
        //  BioSequence bpl = biosequenceService.load( id ); /// this fails.
        assertEquals( "T", bpl.getSequence() );

    }

}
