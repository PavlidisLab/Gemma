/**
 * 
 */
package ubic.gemma.loader.genome.gene;

import java.io.IOException;
import java.util.Collection;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;



/**
 * Test that Gemma can load genes from an external gene file with format :
 * #GeneSymbol  GeneName    Uniprot
 * ZYX ZYXIN   Q15942
 * ZXDC    ZXD FAMILY ZINC FINGER C    Q8C8V1
 * 
 * @author ldonnison
 * @version $Id$
 */
public class ExternalFileGeneLoaderServiceTest extends  BaseSpringContextTest {
    ExternalFileGeneLoaderService externalFileGeneLoaderService =null;
    String geneFile =null;
    
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();  
        Taxon salmonid = Taxon.Factory.newInstance();
        salmonid.setScientificName( "Salmonidae" );
        salmonid.setCommonName( "salmonid" );
        salmonid.setIsSpecies(false);
        salmonid.setIsGenesUsable( false );
        taxonService.findOrCreate( salmonid );
        externalFileGeneLoaderService = new ExternalFileGeneLoaderService();
        externalFileGeneLoaderService.setTaxonService( taxonService );
        externalFileGeneLoaderService.setPersisterHelper( persisterHelper );
        geneFile = (ConfigUtils.getString( "gemma.home" )).concat("/gemma-core/src/test/resources/data/loader/genome/gene/externalGeneFileLoadTest.txt");
       
    }   

    /**
     * Test method for {@link ubic.gemma.loader.genome.gene.ExternalFileGeneLoaderService#load(java.lang.String, java.lang.String)}.
     * Tests that 2 genes are loaded sucessfully into Gemma.
     */
    public void testLoad() {
        GeneService geneService = ( GeneService ) getBean( "geneService" );  
        try {
            externalFileGeneLoaderService.load(geneFile, "salmonid");
            int numbersGeneLoaded = externalFileGeneLoaderService.getLoadedGeneCount();
            assertEquals(2, numbersGeneLoaded);
            Collection<Gene> geneCollection = geneService.findByOfficialName( "ZYXIN");
            Gene gene = geneCollection.iterator().next();
            Collection <GeneProduct> geneProducts = gene.getProducts();
            
            assertEquals("salmonid", gene.getTaxon().getCommonName());
            assertEquals("ZYX", gene.getName());
            assertEquals("ZYX", gene.getOfficialSymbol());
            assertEquals("Imported from external gene file with uniprot id of Q15942", gene.getDescription());
            
            assertEquals(1, geneProducts.size());
            GeneProduct prod = geneProducts.iterator().next();
            assertEquals("Gene product placeholder", prod.getDescription());
           
        } catch(Exception e){
            fail();
        }
        
    }

    /**
     * Tests that if file can not be found file not found exception thrown.
     */
    public void testLoadGeneFileNotFoundIOException() {
        try {
            externalFileGeneLoaderService.load("blank", "salmonid");
        }catch (IOException e ) {
           assertEquals("Cannot read from blank", e.getMessage());            
        }         
        catch ( Exception e ) {
            fail();
        }
    }
    
    /**
     * Tests that if taxon not stored in system IllegalArgumentExceptionThrown
     */
    public void testTaxonNotFoundIllegalArgumentExceptionException() {
        try {
            externalFileGeneLoaderService.load(geneFile, "fishy");
        }catch (IllegalArgumentException e ) {
           assertEquals("No taxon with common name fishy found", e.getMessage());            
        }         
        catch ( Exception e ) {
            fail();
        }
    }
    
    /**
     * Tests that if the file is not in the correct format of 3 tab delimited fields exception thrown.
     */
    public void testFileIncorrectFormatIllegalArgumentExceptionException() {
        try {
           String ncbiFile =  (ConfigUtils.getString( "gemma.home" )).concat("/gemma-core/src/test/resources/data/loader/genome/gene/geneloadtest.txt");
            externalFileGeneLoaderService.load(ncbiFile, "salmonid");
        }catch (IOException e ) {
           assertEquals("Illegal format, expected three columns, got 13", e.getMessage());            
        }         
        catch ( Exception e ) {
            fail();
        }
    }
    
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();       
    }   
    

}
