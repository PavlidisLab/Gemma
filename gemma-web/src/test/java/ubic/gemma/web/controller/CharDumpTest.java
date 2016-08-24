package ubic.gemma.web.controller;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.description.DatabaseEntry;

import ubic.basecode.dataStructure.CountingMap;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.job.progress.ProgressStatusService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderServiceImpl;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;


import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialDao;
import ubic.gemma.model.expression.biomaterial.BioMaterialDaoImpl;
import ubic.gemma.model.expression.biomaterial.BioMatFactorCountObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.biomaterial.BioMaterialServiceImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.authorization.acl.AclTestUtils;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentController;
import ubic.gemma.web.remote.EntityDelegator;

public class CharDumpTest extends BaseSpringContextTest {

	@Autowired
	private ProgressStatusService progressStatusService;

	@Autowired
	private TaskRunningService taskRunningService;


	private String adName = RandomStringUtils.randomAlphabetic(10);

	@Autowired
	private ExpressionExperimentService eeService;
	
	@Autowired
	private BioMaterialService bioMaterialService;
	

	@Autowired
	private ExperimentalDesignImporter experimentalDesignImporter;

	@Autowired
	private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

	@Autowired
	private AclTestUtils aclTestUtils;

	//private static ExpressionExperimentController testController;
	private ExpressionExperiment ee;
	private Collection<BioMatFactorCountObject> testMap;
	private EntityDelegator testDelegator;
	private String searchkeyName;
	private String searchkeyAcc;
	private Collection<BioMatFactorCountObject> testCharDump;

	 private static final String EE_NAME = RandomStringUtils.randomAlphanumeric( 20 );

	 ExternalDatabase ed;
	 String accession;
	 String contactName;
	 boolean persisted = false;

	@After
	public void tearDown() {
		if (ee != null) {
			eeService.delete(ee);
		}
	}

	@Before
	// TODO:refactor tests for biomat-service
	public void setUp() throws Exception {
		log.info("Starting setup");
		testDelegator = new EntityDelegator();
		BioMaterial testbm = this.getTestPersistentBioMaterial();
		this.bioMaterialService.findOrCreate(testbm);
		
		searchkeyName = testbm.getName();
		searchkeyAcc = testbm.getExternalAccession().getAccession();

		// create a couple more.
		for (int i = 0; i < 100; i++) {
			this.bioMaterialService.findOrCreate(this.getTestPersistentBioMaterial());
		}

		 if ( !persisted ) {
		 ee = this.getTestPersistentCompleteExpressionExperiment( false );
		 ee.setName( EE_NAME );
		
		 DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
		 accession = accessionEntry.getAccession();
		 ed = accessionEntry.getExternalDatabase();
		 ee.setAccession( accessionEntry );
//		
		 eeService.update( ee );
		 ee = eeService.thaw( ee );
		 persisted = true;
		 testCharDump = bioMaterialService.charDumpService(ee.getId());
		 System.out.print(testCharDump.toString());
		
		 } else {
		 log.debug( "Skipping making new ee for test" );
		 }
		 
		 

//		String eeShortName = RandomStringUtils.randomAlphabetic(10);
//		metaData.setShortName(eeShortName);
//		metaData.setDescription("bar");
//		metaData.setIsRatio(false);
//		metaData.setTaxon(human);
//		metaData.setQuantitationTypeName("rma");
//		metaData.setScale(ScaleType.LOG2);
//		metaData.setType(StandardQuantitationType.AMOUNT);
//
//			// TODO: figure out how to do setbioassays
		ee = eeService.thawLite(ee);
//		// TODO: associate expression experiment with biomats
		log.info("Ending setup");
	}

	@Test
	/**
	 * test for return value for null Delegator
	 */
	public void testNullDelegator() {
		
		long nullExperimentid = 0;
		Collection<BioMatFactorCountObject>nullMap = bioMaterialService.charDumpService(nullExperimentid);	
		for (BioMatFactorCountObject object : nullMap)
		{
			System.out.println(object.toString());
		}
	}
	

	@Test
	/**test for return value for valid Delegator
	 */
	public void testValidDelegator() {
			
		try{
			testMap = bioMaterialService.charDumpService(ee.getId());
			assertEquals(testMap, testCharDump);
			for (BioMatFactorCountObject object : testMap)
			{
				System.out.print(object.toString());
			}
			}
			catch(NullPointerException e){
				fail("should not be null");
			}
	
	}
	// other tests

}
