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
package edu.columbia.gemma.loader.expression.geo;

import edu.columbia.gemma.BaseDAOTestCase;
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
import edu.columbia.gemma.expression.biomaterial.BioMaterialService;
import edu.columbia.gemma.expression.biomaterial.CompoundService;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.expression.designElement.ReporterService;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.expression.experiment.FactorValueService;
import edu.columbia.gemma.genome.TaxonService;
import edu.columbia.gemma.genome.biosequence.BioSequenceService;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * This is an integration test
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetServiceIntegrationTest extends BaseDAOTestCase {
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
        ml.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        ml
                .setExpressionExperimentService( ( ExpressionExperimentService ) ctx
                        .getBean( "expressionExperimentService" ) );
        ml.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        ml.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        ml.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        ml.setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        ml.setReporterService( ( ReporterService ) ctx.getBean( "reporterService" ) );
        ml.setCompositeSequenceService( ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" ) );
        ml.setProtocolService( ( ProtocolService ) ctx.getBean( "protocolService" ) );
        ml.setHardwareService( ( HardwareService ) ctx.getBean( "hardwareService" ) );
        ml.setSoftwareService( ( SoftwareService ) ctx.getBean( "softwareService" ) );
        ml.setTaxonService( ( TaxonService ) ctx.getBean( "taxonService" ) );
        ml.setBioAssayService( ( BioAssayService ) ctx.getBean( "bioAssayService" ) );
        ml.setQuantitationTypeService( ( QuantitationTypeService ) ctx.getBean( "quantitationTypeService" ) );
        ml.setLocalFileService( ( LocalFileService ) ctx.getBean( "localFileService" ) );
        ml.setCompoundService( ( CompoundService ) ctx.getBean( "compoundService" ) );
        ml.setDatabaseEntryService( ( DatabaseEntryService ) ctx.getBean( "databaseEntryService" ) );
        ml.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        ml.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        ml.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
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

    public void testFetchAndLoadMultiChipPerSeries() throws Exception {
        gds.fetchAndLoad( "GDS472" ); // HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
    }

    public void testFetchAndLoadWithRawData() throws Exception {
        gds.fetchAndLoad( "GDS562" );
    }

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

}
