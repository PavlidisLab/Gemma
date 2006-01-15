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
package edu.columbia.gemma.loader.expression.arrayDesign;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.auditAndSecurity.ContactService;
import edu.columbia.gemma.common.auditAndSecurity.PersonService;
import edu.columbia.gemma.common.description.DatabaseEntryService;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.common.description.LocalFileService;
import edu.columbia.gemma.common.description.OntologyEntryService;
import edu.columbia.gemma.common.protocol.HardwareService;
import edu.columbia.gemma.common.protocol.ProtocolService;
import edu.columbia.gemma.common.protocol.SoftwareService;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeService;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.expression.bioAssay.BioAssayService;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorService;
import edu.columbia.gemma.expression.biomaterial.BioMaterialService;
import edu.columbia.gemma.expression.biomaterial.CompoundService;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.expression.designElement.DesignElementService;
import edu.columbia.gemma.expression.designElement.ReporterService;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.expression.experiment.FactorValueService;
import edu.columbia.gemma.genome.TaxonService;
import edu.columbia.gemma.genome.biosequence.BioSequenceService;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * Loads the database with ArrayDesigns. This test is more representative of integration testing than unit testing as it
 * tests both parsing and loading.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignParserIntegrationTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( ArrayDesignParserIntegrationTest.class );

    private ArrayDesignParser arrayDesignParser = null;
    private ArrayDesignPersister arrayDesignLoader = null;
    private PersisterHelper ph;

    /**
     * set up
     */
    protected void setUp() throws Exception {
        super.setUp();
        ph = new PersisterHelper();
        ph.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        ph.setExpressionExperimentService( ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" ) );
        ph.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        ph.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        ph.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        ph.setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        ph.setDesignElementService( ( DesignElementService ) ctx.getBean( "designElementService" ) );
        ph.setProtocolService( ( ProtocolService ) ctx.getBean( "protocolService" ) );
        ph.setHardwareService( ( HardwareService ) ctx.getBean( "hardwareService" ) );
        ph.setSoftwareService( ( SoftwareService ) ctx.getBean( "softwareService" ) );
        ph.setTaxonService( ( TaxonService ) ctx.getBean( "taxonService" ) );
        ph.setBioAssayService( ( BioAssayService ) ctx.getBean( "bioAssayService" ) );
        ph.setQuantitationTypeService( ( QuantitationTypeService ) ctx.getBean( "quantitationTypeService" ) );
        ph.setLocalFileService( ( LocalFileService ) ctx.getBean( "localFileService" ) );
        ph.setCompoundService( ( CompoundService ) ctx.getBean( "compoundService" ) );
        ph.setDatabaseEntryService( ( DatabaseEntryService ) ctx.getBean( "databaseEntryService" ) );
        ph.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        ph.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        ph.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        ph.setCompositeSequenceService( ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" ) );
        ph.setReporterService( ( ReporterService ) ctx.getBean( "reporterService" ) );
        ph.setDesignElementDataVectorService( ( DesignElementDataVectorService ) ctx.getBean( "designElementDataVectorService" ) );
        arrayDesignParser = new ArrayDesignParser();
        arrayDesignLoader = new ArrayDesignPersister();
        arrayDesignLoader.setPersisterHelper( ph );
    }

    /**
     * tear down
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since it's dependencies are
     * localized to the Gemma project it has been added to the test suite.
     * 
     * @throws Exception
     */
    public void testParseAndLoad() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/arraydesign/array.txt" );
        arrayDesignParser.parse( is );
        ph.persist( arrayDesignParser.getResults() );
    }

}
