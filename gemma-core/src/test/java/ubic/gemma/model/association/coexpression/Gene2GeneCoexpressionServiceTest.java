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

package ubic.gemma.model.association.coexpression;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.EntityUtils;

/**
 * @author klc
 * @version $Id$
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
    public void setup() {

        Taxon mouseTaxon = taxonS.findByCommonName( "mouse" );

        firstGene = Gene.Factory.newInstance();
        firstGene.setName( "test_gene2geneCoexpression" );
        firstGene.setTaxon( mouseTaxon );
        firstGene = geneS.create( firstGene );

        Gene secondGene = Gene.Factory.newInstance();
        secondGene.setName( "test_gene2geneCoexpression2" );
        secondGene = geneS.create( secondGene );

        Gene2GeneCoexpression g2gCoexpression = MouseGeneCoExpression.Factory.newInstance( 0.9, secondGene, firstGene );
        g2gCoexpression.setNumDatasetsSupporting( 3 );

        List<NonPersistentNonOrderedCoexpLink> links = new ArrayList<>();
        links.add( new NonPersistentNonOrderedCoexpLink( g2gCoexpression ) );

        ee = this.getTestPersistentBasicExpressionExperiment();

        g2gCoexpressionService.createOrUpdate( ee, links, new LinkCreator( mouseTaxon ), null );

    }

    @Test
    public void testFindCoexpressionRelationships() {

        Collection<Long> ees = EntityUtils.getIds( ee );
        Collection<CoexpressionValueObject> results = g2gCoexpressionService.findCoexpressionRelationships( firstGene,
                ees, 3, 100, true );
        assertEquals( 1, results.size() );

    }
}
