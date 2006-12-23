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
package ubic.gemma.model.genome.gene;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author jsantos
 * @version $Id $
 */
public class GeneDaoTest extends BaseSpringContextTest {

    private GeneDao geneDao = null;


    public void testGetCompositeSequenceCountById() {
        geneDao = (GeneDao) this.getBean( "geneDao" );
        Gene gene = Gene.Factory.newInstance();
        gene.setId( (long) 1 );
        gene.setName( "test_genedao" );
        long num  = geneDao.getCompositeSequenceCountById( 1 );
        assertNotNull(num);
    }

    @SuppressWarnings("unchecked")
    public void testGetCompositeSequencesById() {
        geneDao = (GeneDao) this.getBean( "geneDao" );
        Gene gene = Gene.Factory.newInstance();
        gene.setId( (long) 1 );
        gene.setName( "test_genedao" );
        Collection<CompositeSequence> cs = geneDao.getCompositeSequencesById( (long) 1 );
        assertNotNull(cs);
    }

    @SuppressWarnings("unchecked")
    public void testGetByGeneAlias() {
        geneDao = (GeneDao) this.getBean( "geneDao" );
        Gene gene = Gene.Factory.newInstance();
        gene.setId( (long) 1 );
        gene.setName( "test_genedao" );
        
        Collection<GeneAlias> aliases = new ArrayList<GeneAlias>();
        GeneAlias alias = GeneAlias.Factory.newInstance();
        alias.setId( (long) 1 );
        alias.setAlias( "GRIN1" );
        alias.setGene( gene );
        alias.setSymbol( "test_genedao" );
        aliases.add( alias );
        
        gene.setAliases( aliases );
        Collection<Gene> genes = geneDao.getByGeneAlias( "GRIN1" );
        assertNotNull(genes);
    }
    
    @SuppressWarnings("unchecked")
    public void testGetCoexpressedElements() {
        geneDao = (GeneDao) this.getBean( "geneDao" );
        Gene gene = Gene.Factory.newInstance();
        gene.setId( (long) 1 );
        gene.setName( "test_genedao" );
        Collection elements = geneDao.getCoexpressedElements( gene );
        assertNotNull(elements);
    }
    
    @SuppressWarnings("unchecked")
    public void testGetCoexpressedElementsById() {
        geneDao = (GeneDao) this.getBean( "geneDao" );
        Gene gene = Gene.Factory.newInstance();
        gene.setId( (long) 1 );
        gene.setName( "test_genedao" );
        Collection elements = geneDao.getCoexpressedElementsById( (long) 1 );
        assertNotNull(elements);
    }
    
    @SuppressWarnings("unchecked")
    public void testGetCoexpressedGenes() {
        geneDao = (GeneDao) this.getBean( "geneDao" );
        Gene gene = Gene.Factory.newInstance();
        gene.setId( (long) 1 );
        gene.setName( "test_genedao" );
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName("human");
        gene.setTaxon( taxon );
        Collection<ExpressionExperiment> ees = new ArrayList<ExpressionExperiment>();
        CoexpressionCollectionValueObject genes = (CoexpressionCollectionValueObject) geneDao.getCoexpressedGenes( gene, ees, 1 );
        assertNotNull(genes);
    }

}
