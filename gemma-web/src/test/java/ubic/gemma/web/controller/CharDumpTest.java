package ubic.gemma.web.controller;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.CountingMap;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.job.progress.ProgressStatusService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.authorization.acl.AclTestUtils;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentController;
import ubic.gemma.web.remote.EntityDelegator;
public class CharDumpTest extends BaseSpringContextTest{
	
	@Autowired
    private ProgressStatusService progressStatusService;

    @Autowired
    private TaskRunningService taskRunningService;

    private ExpressionExperiment ee;

    private String adName = RandomStringUtils.randomAlphabetic( 10 );

    @Autowired
    private ExpressionExperimentService eeService;
    
    private BioMaterialService bioMaterialService;
    
    private BioMaterial bioMat;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    private AclTestUtils aclTestUtils;

	
	private static ExpressionExperimentController testController;
	private Map<String, Integer> testMap;
    private EntityDelegator testDelegator;
    
    @After
    public void tearDown() {
        if ( ee != null ) {
            eeService.delete( ee );
        }
    }

	@Before
	public void setUp() throws Exception{
		testController = new ExpressionExperimentController();
		testDelegator = new EntityDelegator();
		//initialize the test maps as an empty map
		testMap = new CountingMap<>();
		
		 	SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

	        Taxon human = taxonService.findByCommonName( "human" );

	        String eeShortName = RandomStringUtils.randomAlphabetic( 10 );
	        metaData.setShortName( eeShortName );
	        metaData.setDescription( "bar" );
	        metaData.setIsRatio( false );
	        metaData.setTaxon( human );
	        metaData.setQuantitationTypeName( "rma" );
	        metaData.setScale( ScaleType.LOG2 );
	        metaData.setType( StandardQuantitationType.AMOUNT );

	        ArrayDesign ad = ArrayDesign.Factory.newInstance();

	        ad.setShortName( adName );
	        ad.setName( "foobly foo" );
	        ad.setPrimaryTaxon( human );
	        ad.setTechnologyType( TechnologyType.ONECOLOR );

	        metaData.getArrayDesigns().add( ad );
	       
	        try (InputStream data = this.getClass().getResourceAsStream(
	                "/data/loader/expression/experimentalDesignTestData.txt" );) {

	            ee = simpleExpressionDataLoaderService.create( metaData, data );
	            //TODO: figure out how to do setbioassays
	            bioMat = bioMaterialService.create(getTestPersistentBioMaterial());
	            Collection<BioAssay>testBioAssay = new ArrayList<>();
	            ee.setBioAssays(testBioAssay);
	        }
	        ee = eeService.thawLite( ee );
	        //TODO: associate expression experiment with biomats
	}
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																
	@Test
	/**test for return value for null Delegator
	 */																																																																																																																																																																																																																																																																					
	public void testNullDelegator() {
		assertTrue(true);
		Map<String, Integer> nullMap = testController.getCharDump(testDelegator);
		assertEquals(nullMap,null);
		
	}
	
	@Test
	/**test for return value for Delegator with null id
	 */
	public void testNullId(){
		
		Map<String, Integer> nullIdMap = testController.getCharDump(testDelegator);
		assertEquals(nullIdMap,null);
	}
	
	@Test
	/**test for return value for Delegator with invalid id
	 */
	public void testInvalidId(){
		
		
		testDelegator.setId( (long) 999999999);//intentionally invalid
		assertTrue(testDelegator.getId()!=ee.getId());
		Map<String, Integer> invalidIdMap = testController.getCharDump(testDelegator);
		assertEquals(invalidIdMap,null);
	}
	
	@Test
	/**test for return value for valid Delegator
	 */
	public void testValidDelegator(){
		
		assertTrue(true);
		//TODO: find something to set as valid delegator
		testDelegator.setId(ee.getId());
		assertTrue(ee.getId()==testDelegator.getId());
		
		Map<String, Integer> validIdMap = testController.getCharDump(testDelegator);
		
//		testMap = somethingToDoWithMap
		//TODO: figure out what the expected value should be
		
		assertEquals(validIdMap, testMap);
		//TODO: comment out after figuring out what expected value should be
		//fail("you haven't implemented the method yet genius.");
	}
	//other tests

}
