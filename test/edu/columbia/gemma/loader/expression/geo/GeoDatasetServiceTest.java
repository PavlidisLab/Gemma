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
package edu.columbia.gemma.loader.expression.geo;

import edu.columbia.gemma.BaseDAOTestCase;
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
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.CompoundDao;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.genome.biosequence.BioSequenceDao;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetServiceTest extends BaseDAOTestCase {
    GeoDatasetService gds;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gds = new GeoDatasetService();
        PersisterHelper ml = new PersisterHelper();
        GeoConverter geoConv = new GeoConverter();
        ml = new PersisterHelper();
        ml.setBioMaterialDao( ( BioMaterialDao ) ctx.getBean( "bioMaterialDao" ) );
        ml.setExpressionExperimentDao( ( ExpressionExperimentDao ) ctx.getBean( "expressionExperimentDao" ) );
        ml.setPersonDao( ( PersonDao ) ctx.getBean( "personDao" ) );
        ml.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
        ml.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );
        ml.setExternalDatabaseDao( ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) );
        ml.setDesignElementDao( ( DesignElementDao ) ctx.getBean( "designElementDao" ) );
        ml.setProtocolDao( ( ProtocolDao ) ctx.getBean( "protocolDao" ) );
        ml.setHardwareDao( ( HardwareDao ) ctx.getBean( "hardwareDao" ) );
        ml.setSoftwareDao( ( SoftwareDao ) ctx.getBean( "softwareDao" ) );
        ml.setTaxonDao( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
        ml.setBioAssayDao( ( BioAssayDao ) ctx.getBean( "bioAssayDao" ) );
        ml.setQuantitationTypeDao( ( QuantitationTypeDao ) ctx.getBean( "quantitationTypeDao" ) );
        ml.setLocalFileDao( ( LocalFileDao ) ctx.getBean( "localFileDao" ) );
        ml.setCompoundDao( ( CompoundDao ) ctx.getBean( "compoundDao" ) );
        ml.setDatabaseEntryDao( ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" ) );
        ml.setContactDao( ( ContactDao ) ctx.getBean( "contactDao" ) );
        ml.setBioSequenceDao((BioSequenceDao)ctx.getBean("bioSequenceDao"));
        gds.setPersister( ml );
        gds.setConverter( geoConv );
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /** This is an integration test */

    public void testFetchAndLoadB() throws Exception {
        gds.fetchAndLoad( "GDS942" );
    }

    public void testFetchAndLoadC() throws Exception {
        gds.fetchAndLoad( "GDS100" );
    }

    public void testFetchAndLoadD() throws Exception {
        gds.fetchAndLoad( "GDS1033" );
    }

    public void testFetchAndLoadE() throws Exception {
        gds.fetchAndLoad( "GDS835" );
    }

    public void testFetchAndLoadF() throws Exception {
        gds.fetchAndLoad( "GDS58" );
    }

    public void testFetchAndLoadG() throws Exception {
        gds.fetchAndLoad( "GDS940" );
    }
}
