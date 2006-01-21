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
package edu.columbia.gemma.loader.genome.gene.ncbi;

import java.util.Collection;

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
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneIntegrationTest extends BaseServiceTestCase {
    PersisterHelper persisterHelper;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        persisterHelper = new PersisterHelper();
        persisterHelper.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        persisterHelper.setExpressionExperimentService( ( ExpressionExperimentService ) ctx
                .getBean( "expressionExperimentService" ) );
        persisterHelper.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        persisterHelper.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        persisterHelper.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        persisterHelper
                .setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        persisterHelper.setProtocolService( ( ProtocolService ) ctx.getBean( "protocolService" ) );
        persisterHelper.setHardwareService( ( HardwareService ) ctx.getBean( "hardwareService" ) );
        persisterHelper.setSoftwareService( ( SoftwareService ) ctx.getBean( "softwareService" ) );
        persisterHelper.setTaxonService( ( TaxonService ) ctx.getBean( "taxonService" ) );
        persisterHelper.setBioAssayService( ( BioAssayService ) ctx.getBean( "bioAssayService" ) );
        persisterHelper
                .setQuantitationTypeService( ( QuantitationTypeService ) ctx.getBean( "quantitationTypeService" ) );
        persisterHelper.setLocalFileService( ( LocalFileService ) ctx.getBean( "localFileService" ) );
        persisterHelper.setCompoundService( ( CompoundService ) ctx.getBean( "compoundService" ) );
        persisterHelper.setDatabaseEntryService( ( DatabaseEntryService ) ctx.getBean( "databaseEntryService" ) );
        persisterHelper.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        persisterHelper.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        persisterHelper.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        persisterHelper.setCompositeSequenceService( ( CompositeSequenceService ) ctx
                .getBean( "compositeSequenceService" ) );
        persisterHelper.setReporterService( ( ReporterService ) ctx.getBean( "reporterService" ) );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFetchAndLoad() throws Exception {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator();
        Collection<Object> results = sdog.generate( null );
        NcbiGeneConverter ngc = new NcbiGeneConverter();
        Collection<Object> gemmaObj = ngc.convert( results );
        persisterHelper.persist( gemmaObj );
    }
}
