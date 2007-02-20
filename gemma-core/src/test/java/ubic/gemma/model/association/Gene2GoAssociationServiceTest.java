/**
 * 
 */
package ubic.gemma.model.association;

import java.util.Collection;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 *
 */
public class Gene2GoAssociationServiceTest extends BaseSpringContextTest {

    Gene2GOAssociationService gene2GoAssociationS;
    TaxonService    taxonS;
    GeneService     geneS;
    OntologyEntryService ontologyEntryS;
    ExternalDatabaseDao edd;
    
    
//    test data
    
    OntologyEntry testOE;
    Gene testGene;
    Taxon mouse;
    OntologyEntry childOE;
    Gene test2Gene;
    
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        this.endTransaction();

        gene2GoAssociationS = ( Gene2GOAssociationService ) this.getBean( "gene2GOAssociationService" ); 
        taxonS = ( TaxonService ) this.getBean( "taxonService" );
        geneS = (GeneService) this.getBean("geneService");
        ontologyEntryS = (OntologyEntryService ) this.getBean( "ontologyEntryService" );
        edd = ( ExternalDatabaseDao ) this.getBean( "externalDatabaseDao" );
    
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "foo" );

        ed = ( ExternalDatabase ) edd.create( ed );
        mouse = taxonS.findByCommonName( "mouse" );
        
        
        childOE = OntologyEntry.Factory.newInstance();
        childOE.setAccession( "fake:1051" );
        childOE.setExternalDatabase( ed );        
        childOE = ontologyEntryS.create( childOE );
        
        
        testOE = OntologyEntry.Factory.newInstance();
        testOE.setAccession( "fake:1050" );
        testOE.setExternalDatabase( ed );
        testOE.getAssociations().add( childOE);
        testOE = ontologyEntryS.create( testOE );
        
        testGene = Gene.Factory.newInstance();
        testGene.setOfficialSymbol( "fakeG" );
        testGene.setOfficialName( "fakeG" );
        testGene.setDescription( "test" );
        testGene.setTaxon( mouse );
        testGene = geneS.create( testGene );
        
        test2Gene = Gene.Factory.newInstance();
        test2Gene.setOfficialSymbol( "fake2G" );
        test2Gene.setOfficialName( "fake2G" );
        test2Gene.setDescription( "test" );
        test2Gene.setTaxon( mouse );
        test2Gene = geneS.create( test2Gene );
        
        
        Gene2GOAssociation test1 = Gene2GOAssociation.Factory.newInstance();
        test1.setOntologyEntry( testOE );
        test1.setGene( testGene );        
        test1 = gene2GoAssociationS.create( test1 );
 
        Gene2GOAssociation test2 = Gene2GOAssociation.Factory.newInstance();
        test2.setOntologyEntry( childOE );
        test2.setGene( test2Gene );        
        test2 = gene2GoAssociationS.create( test2 );
        
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        
        ontologyEntryS.remove( testOE );
        ontologyEntryS.remove( childOE );
        
        geneS.remove( "fake2G" );
        geneS.remove( "fakeG" );
        
    }
    
    public void testFindByGOTerm(){
        
        
        Collection<Gene> genes = gene2GoAssociationS.findByGOTerm( "fake:1050", mouse );  
        
        assertEquals(2,genes.size());
        for(Gene g: genes)
            assertEquals("test", g.getDescription());
        
    }

}
