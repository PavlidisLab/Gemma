/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.persistence.service.genome.gene;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;

import static org.junit.Assert.assertNotNull;

/**
 * @author cmcdonald
 */
public class GeneSearchTest extends BaseSpringContextTest {

    @Autowired
    private GeneService geneService = null;

    @Test
    public void testSearchGenes() throws SearchException {
        Gene gene = Gene.Factory.newInstance();

        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( "test_search" );
        gene.setOfficialName( "test_search" );
        gene.setOfficialSymbol( "test_search" );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );
        PhysicalLocation pl1 = PhysicalLocation.Factory.newInstance();
        Chromosome chromosome = new Chromosome( "X", null, this.getTestPersistentBioSequence(), human );

        chromosome = ( Chromosome ) persisterHelper.persist( chromosome );
        pl1.setChromosome( chromosome );
        pl1.setNucleotide( 10000010L );
        pl1.setNucleotideLength( 1001 );
        pl1.setStrand( "-" );
        gene.setPhysicalLocation( pl1 );

        gene = geneService.create( gene );

        Collection<GeneValueObject> searchResults = geneService.searchGenes( "test_search", 1L );

        assertNotNull( searchResults );

        GeneValueObject gvo = searchResults.iterator().next();

        assertNotNull( gvo );

        geneService.remove( gene );

    }

}
