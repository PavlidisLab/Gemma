/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.model.association;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 * @version $Id$
 */
public class Gene2GoAssociationServiceTest extends BaseSpringContextTest {

    // Gene2GOAssociationService gene2GoAssociationS;
    // TaxonService taxonS;
    // GeneService geneS;
    // OntologyEntryService ontologyEntryS;
    // ExternalDatabaseDao edd;
    //    
    //    
    // // test data
    //    
    // OntologyEntry testOE;
    // Gene testGene;
    // Taxon mouse;
    // OntologyEntry childOE;
    // Gene test2Gene;
    //    
    // @Override
    // protected void onSetUpInTransaction() throws Exception {
    // super.onSetUpInTransaction();
    // this.endTransaction();
    //
    // gene2GoAssociationS = ( Gene2GOAssociationService ) this.getBean( "gene2GOAssociationService" );
    // taxonS = ( TaxonService ) this.getBean( "taxonService" );
    // geneS = (GeneService) this.getBean("geneService");
    // ontologyEntryS = (OntologyEntryService ) this.getBean( "ontologyEntryService" );
    // edd = ( ExternalDatabaseDao ) this.getBean( "externalDatabaseDao" );
    //    
    // ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
    // ed.setName( "foo" );
    //
    // ed = ( ExternalDatabase ) edd.create( ed );
    // mouse = taxonS.findByCommonName( "mouse" );
    //        
    //        
    // childOE = OntologyClass.Factory.newInstance();
    // childOE.setAccession( "fake:1051" );
    // childOE.setExternalDatabase( ed );
    // childOE = ontologyEntryS.create( childOE );
    //        
    //        
    // testOE = OntologyClass.Factory.newInstance();
    // testOE.setAccession( "fake:1050" );
    // testOE.setExternalDatabase( ed );
    // testOE.getAssociations().add( childOE);
    // testOE = ontologyEntryS.create( testOE );
    //        
    // testGene = Gene.Factory.newInstance();
    // testGene.setOfficialSymbol( "fakeG" );
    // testGene.setOfficialName( "fakeG" );
    // testGene.setDescription( "test" );
    // testGene.setTaxon( mouse );
    // testGene = geneS.create( testGene );
    //        
    // test2Gene = Gene.Factory.newInstance();
    // test2Gene.setOfficialSymbol( "fake2G" );
    // test2Gene.setOfficialName( "fake2G" );
    // test2Gene.setDescription( "test" );
    // test2Gene.setTaxon( mouse );
    // test2Gene = geneS.create( test2Gene );
    //        
    //        
    // Gene2GOAssociation test1 = Gene2GOAssociation.Factory.newInstance();
    // test1.setOntologyEntry( testOE );
    // test1.setGene( testGene );
    // test1 = gene2GoAssociationS.create( test1 );
    // 
    // Gene2GOAssociation test2 = Gene2GOAssociation.Factory.newInstance();
    // test2.setOntologyEntry( childOE );
    // test2.setGene( test2Gene );
    // test2 = gene2GoAssociationS.create( test2 );
    //        
    // }
    //
    // @Override
    // protected void onTearDownInTransaction() throws Exception {
    // super.onTearDownInTransaction();
    //        
    // ontologyEntryS.remove( testOE );
    // ontologyEntryS.remove( childOE );
    //        
    // geneS.remove( "fake2G" );
    // geneS.remove( "fakeG" );
    //        
    // }
    //    
    // public void testFindByGOTerm(){
    //        
    //        
    // Collection<Gene> genes = gene2GoAssociationS.findByGOTerm( "fake:1050", mouse );
    //        
    // assertEquals(2,genes.size());
    // for(Gene g: genes)
    // assertEquals("test", g.getDescription());
    //        
    // }

}
