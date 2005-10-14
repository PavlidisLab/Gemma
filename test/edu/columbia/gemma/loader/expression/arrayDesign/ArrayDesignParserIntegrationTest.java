/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
import edu.columbia.gemma.common.auditAndSecurity.ContactDao;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.DatabaseEntryDao;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.common.protocol.HardwareDao;
import edu.columbia.gemma.common.protocol.ProtocolDao;
import edu.columbia.gemma.common.protocol.SoftwareDao;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.expression.bioAssay.BioAssayDao;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorDao;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.CompoundDao;
import edu.columbia.gemma.expression.designElement.CompositeSequenceDao;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.designElement.ReporterDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.expression.experiment.FactorValueDao;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.genome.biosequence.BioSequenceDao;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * Loads the database with ArrayDesigns. This test is more representative of integration testing than unit testing as it
 * tests both parsing and loading.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
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
        ph.setBioMaterialDao( ( BioMaterialDao ) ctx.getBean( "bioMaterialDao" ) );
        ph.setExpressionExperimentDao( ( ExpressionExperimentDao ) ctx.getBean( "expressionExperimentDao" ) );
        ph.setPersonDao( ( PersonDao ) ctx.getBean( "personDao" ) );
        ph.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
        ph.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );
        ph.setExternalDatabaseDao( ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) );
        ph.setDesignElementDao( ( DesignElementDao ) ctx.getBean( "designElementDao" ) );
        ph.setProtocolDao( ( ProtocolDao ) ctx.getBean( "protocolDao" ) );
        ph.setHardwareDao( ( HardwareDao ) ctx.getBean( "hardwareDao" ) );
        ph.setSoftwareDao( ( SoftwareDao ) ctx.getBean( "softwareDao" ) );
        ph.setTaxonDao( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
        ph.setBioAssayDao( ( BioAssayDao ) ctx.getBean( "bioAssayDao" ) );
        ph.setQuantitationTypeDao( ( QuantitationTypeDao ) ctx.getBean( "quantitationTypeDao" ) );
        ph.setLocalFileDao( ( LocalFileDao ) ctx.getBean( "localFileDao" ) );
        ph.setCompoundDao( ( CompoundDao ) ctx.getBean( "compoundDao" ) );
        ph.setDatabaseEntryDao( ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" ) );
        ph.setContactDao( ( ContactDao ) ctx.getBean( "contactDao" ) );
        ph.setBioSequenceDao( ( BioSequenceDao ) ctx.getBean( "bioSequenceDao" ) );
        ph.setFactorValueDao( ( FactorValueDao ) ctx.getBean( "factorValueDao" ) );
        ph.setCompositeSequenceDao( ( CompositeSequenceDao ) ctx.getBean( "compositeSequenceDao" ) );
        ph.setReporterDao( ( ReporterDao ) ctx.getBean( "reporterDao" ) );
        ph.setDesignElementDataVectorDao( ( DesignElementDataVectorDao ) ctx.getBean( "designElementDataVectorDao" ) );
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
