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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Test prompted by bug 507: making a blat association creates new genes, but the gene products are reused.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatAssociationPersisterTest extends BaseSpringContextTest {

    /*
     * Create a gene and some gene products for it Create a BlatAssociation that points to a template that matches of
     * one of the gene products Persist the BlatAssociation and make sure we don't get extra genes or gene products.
     */

    public void testBlatAssociationPersistingDoesntCreateNewGenes() throws Exception {
        // PersisterHelper ph = this.persisterHelper;

        Gene g = this.getTestPeristentGene();
        GeneProduct gp = this.getTestPersistentGeneProduct( g );
        g.getProducts().add( gp );

        // GeneProduct gpcopy = GeneProduct.Factory.newInstance();
        // gpcopy.

        // Gene gcopy = Gene.Factory.newInstance();

        BlatAssociation ba = BlatAssociation.Factory.newInstance();
        // ba.setGeneProduct( );

    }

}
