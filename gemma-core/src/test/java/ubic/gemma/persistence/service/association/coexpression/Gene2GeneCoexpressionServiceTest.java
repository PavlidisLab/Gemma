/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

package ubic.gemma.persistence.service.association.coexpression;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.association.coexpression.MouseGeneCoExpression;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author klc
 *
 */
public class Gene2GeneCoexpressionServiceTest extends BaseSpringContextTest {

    @Autowired
    private CoexpressionService g2gCoexpressionService;

    @Autowired
    private GeneService geneS;

    @Autowired
    private TaxonService taxonS;

    private Gene firstGene;

    private ExpressionExperiment ee;

    @Before
    public void setUp() throws Exception {

        Taxon mouseTaxon = taxonS.findByCommonName( "mouse" );
        assertNotNull( mouseTaxon );

        firstGene = Gene.Factory.newInstance();
        firstGene.setName( "test_gene2geneCoexpression" );
        firstGene.setTaxon( mouseTaxon );
        firstGene = geneS.create( firstGene );

        Gene secondGene = Gene.Factory.newInstance();
        secondGene.setName( "test_gene2geneCoexpression2" );
        secondGene.setTaxon( mouseTaxon );
        secondGene = geneS.create( secondGene );

        List<NonPersistentNonOrderedCoexpLink> links = new ArrayList<>();
        links.add( new NonPersistentNonOrderedCoexpLink( MouseGeneCoExpression.Factory.newInstance( 0.9,
                secondGene.getId(), firstGene.getId() ) ) );

        ee = this.getTestPersistentBasicExpressionExperiment();

        Set<Gene> genesTested = new HashSet<>();
        genesTested.add( firstGene );
        genesTested.add( secondGene );
        g2gCoexpressionService.createOrUpdate( ee, links, new LinkCreator( mouseTaxon ), genesTested );

    }

    @Test
    public void testFindCoexpressionRelationships() {

        Collection<Long> ees = Collections.singleton( ee.getId() );
        Collection<CoexpressionValueObject> results = g2gCoexpressionService.findCoexpressionRelationships( firstGene,
                ees, 1, 100, true );
        assertEquals( 1, results.size() );

    }
}
