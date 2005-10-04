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
package edu.columbia.gemma.loader.genome.gene.ncbi;

import java.util.Collection;

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
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.CompoundDao;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.expression.experiment.FactorValueDao;
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
public class NCBIGeneIntegrationTest extends BaseServiceTestCase {
    PersisterHelper persisterHelper;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        persisterHelper = new PersisterHelper();
        persisterHelper.setBioMaterialDao( ( BioMaterialDao ) ctx.getBean( "bioMaterialDao" ) );
        persisterHelper
                .setExpressionExperimentDao( ( ExpressionExperimentDao ) ctx.getBean( "expressionExperimentDao" ) );
        persisterHelper.setPersonDao( ( PersonDao ) ctx.getBean( "personDao" ) );
        persisterHelper.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
        persisterHelper.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );
        persisterHelper.setExternalDatabaseDao( ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) );
        persisterHelper.setDesignElementDao( ( DesignElementDao ) ctx.getBean( "designElementDao" ) );
        persisterHelper.setProtocolDao( ( ProtocolDao ) ctx.getBean( "protocolDao" ) );
        persisterHelper.setHardwareDao( ( HardwareDao ) ctx.getBean( "hardwareDao" ) );
        persisterHelper.setSoftwareDao( ( SoftwareDao ) ctx.getBean( "softwareDao" ) );
        persisterHelper.setTaxonDao( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
        persisterHelper.setBioAssayDao( ( BioAssayDao ) ctx.getBean( "bioAssayDao" ) );
        persisterHelper.setQuantitationTypeDao( ( QuantitationTypeDao ) ctx.getBean( "quantitationTypeDao" ) );
        persisterHelper.setLocalFileDao( ( LocalFileDao ) ctx.getBean( "localFileDao" ) );
        persisterHelper.setCompoundDao( ( CompoundDao ) ctx.getBean( "compoundDao" ) );
        persisterHelper.setDatabaseEntryDao( ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" ) );
        persisterHelper.setContactDao( ( ContactDao ) ctx.getBean( "contactDao" ) );
        persisterHelper.setBioSequenceDao( ( BioSequenceDao ) ctx.getBean( "bioSequenceDao" ) );
        persisterHelper.setFactorValueDao( ( FactorValueDao ) ctx.getBean( "factorValueDao" ) );
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
