/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.core.image.aba;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Alan brain Atlas service test.
 *
 * @author kelsey
 */
public class AllenBrainAtlasServiceTest extends BaseSpringContextTest {

    @Autowired
    private AllenBrainAtlasService abaService = null;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private Persister<Gene> persisterHelper;

    private Gene gene;

    @Before
    public void SetUp() {
        gene = Gene.Factory.newInstance();
        gene.setName( "glutamate receptor, ionotropic, NMDA1 (zeta 1)" );
        gene.setOfficialSymbol( "grin1" );
        gene.setNcbiGeneId( 14810 );
        gene.setTaxon( taxonService.findByCommonName( "mouse" ) );

        this.persisterHelper.persistOrUpdate( gene );
    }

    @After
    public void Cleanup() {
        this.geneService.remove( gene );
    }

    @Test
    public void testGetGene() throws Exception {
        AbaGene abaGene;
        Gene gene = geneService.findByOfficialSymbol( "grin1", taxonService.findByCommonName( "mouse" ) );

        if ( gene == null ) {
            log.error( "Mouse Grin1 gene could not be found in Gemma" );
        }

        abaGene = abaService.getGene( gene );

        assertNotNull( abaGene );
        assertNotNull( abaGene.getImageSeries() );
        assertTrue( !abaGene.getImageSeries().isEmpty() );
    }

}
