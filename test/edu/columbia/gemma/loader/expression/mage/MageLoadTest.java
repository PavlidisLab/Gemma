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
package edu.columbia.gemma.loader.expression.mage;

import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageLoadTest extends MageBaseTest {
    private static Log log = LogFactory.getLog( MageLoadTest.class.getName() );
    MageMLConverter mageMLConverter = null;
    PersisterHelper ml;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.setMageMLConverter( ( MageMLConverter ) ctx.getBean( "mageMLConverter" ) );
        ml = new PersisterHelper();
        ml.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        ml.setExpressionExperimentService( ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" ) );
        ml.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        ml.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        ml.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        ml.setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        ml.setDesignElementService( ( DesignElementService ) ctx.getBean( "designElementService" ) );
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
        ml.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        ml.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        ml.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        ml.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        ml.setCompositeSequenceService( ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" ) );
        ml.setReporterService( ( ReporterService ) ctx.getBean( "reporterService" ) );
        ml.setDesignElementDataVectorService( ( DesignElementDataVectorService ) ctx.getBean( "designElementDataVectorService" ) );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // /*
    // * Class under test for void create(Collection)
    // */
    // public void testCreateCollection() throws Exception {
    // log.info( "Parsing MAGE Jamboree example" );
    //
    // MageMLParser mlp = new MageMLParser();
    //
    // zipXslSetup( mlp, "/data/mage/mageml-example.zip" );
    //
    // ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class
    // .getResourceAsStream( "/data/mage/mageml-example.zip" ) );
    // istMageExamples.getNextEntry();
    // mlp.parse( istMageExamples );
    //
    // Collection<Object> parseResult = mlp.getResults();
    //
    // MageMLConverter mlc = new MageMLConverter( mlp.getSimplifiedXml() );
    //
    // Collection<Object> result = mlc.convert( parseResult );
    //
    // log.info( result.size() + " Objects parsed from the MAGE file." );
    // log.info( "Tally:\n" + mlp );
    // istMageExamples.close();
    // ml.persist( result );
    //
    // }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionRealA() throws Exception {
        log.info( "Parsing MAGE from ArrayExpress (AFMX)" );

        MageMLParser mlp = new MageMLParser();

        xslSetup( mlp, "/data/mage/E-AFMX-13/E-AFMX-13.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );
        mlp.parse( istMageExamples );
        Collection<Object> parseResult = mlp.getResults();
        getMageMLConverter().setSimplifiedXml( mlp.getSimplifiedXml() );
        Collection<Object> result = getMageMLConverter().convert( parseResult );

        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        ml.persist( result );
    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionRealB() throws Exception {
        log.info( "Parsing MAGE from ArrayExpress (WMIT)" );

        MageMLParser mlp = new MageMLParser();

        xslSetup( mlp, "/data/mage/E-WMIT-4.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-WMIT-4.xml" );
        mlp.parse( istMageExamples );
        Collection<Object> parseResult = mlp.getResults();

        getMageMLConverter().setSimplifiedXml( mlp.getSimplifiedXml() );

        Collection<Object> result = getMageMLConverter().convert( parseResult );
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        ml.persist( result );
    }

    /**
     * @return Returns the mageMLConverter.
     */
    public MageMLConverter getMageMLConverter() {
        return mageMLConverter;
    }

    /**
     * @param mageMLConverter The mageMLConverter to set.
     */
    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

}
