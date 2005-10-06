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
package edu.columbia.gemma.loader.expression.mage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;

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
import edu.columbia.gemma.expression.bioAssay.BioAssay;
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
 * Integration test of MageML: Parser, Converter and Preprocessor
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class MageMLPreprocessorIntegrationTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( MageMLPreprocessorIntegrationTest.class );

    private MageMLParser mageMLParser = null;
    private MageMLConverter mageMLConverter = null;
    private MageMLPreprocessor mageMLPreprocessor = null;
    private PersisterHelper ph;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.mageMLPreprocessor = new MageMLPreprocessor( "testPreprocess" );
        this.mageMLParser = ( MageMLParser ) ctx.getBean( "mageMLParser" );
        this.mageMLConverter = ( MageMLConverter ) ctx.getBean( "mageMLConverter" );
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
        ph.setDesignElementDataVectorDao((DesignElementDataVectorDao)ctx.getBean("designElementDataVectorDao"));
        mageMLPreprocessor.setPersisterHelper( ph );
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests the conversion of source domain objects (SDO) to gemma domain objects (GDO), then creates matricies for
     * each of the quantitation types for a give bioAssay, save to disk and persist the resulting vectors.
     * 
     * @throws IOException
     * @throws TransformerException
     */
    @SuppressWarnings("unchecked")
    public void testPreprocess() throws IOException, TransformerException {

        /* PARSING */
        log.info( "***** PARSING *****  " );

        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLPreprocessorIntegrationTest.class
                .getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* create the simplified xml file using the mageMLParser */
        InputStream ist2MageExamples = MageMLPreprocessorIntegrationTest.class
                .getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );
        mageMLParser.createSimplifiedXml( ist2MageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        /* get xsl transformed xml file */
        Document simplifiedXml = mageMLParser.getSimplifiedXml();
        log.debug( "simplified xml document: " + simplifiedXml );

        /* close input streams */
        istMageExamples.close();
        ist2MageExamples.close();

        /* CONVERTING */
        log.info( "***** CONVERTING ***** " );

        /* create input stream from xsl file. */
        if ( simplifiedXml == null ) {
            throw new IllegalStateException( "simplfied xml file is null.  Exiting test ..." );
        }

        /*
         * on Spring initialization, simplifiedXml is still null because it has not been passed a document. Therefore,
         * set it.
         */
        mageMLConverter.setSimplifiedXml( simplifiedXml );

        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );
        if ( log.isDebugEnabled() ) {
            for ( Object obj : gemmaObjects ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        /* CONVERTING */
        log.info( "***** PREPROCESSING ***** " );

        /* get all the gemma bioassays from the converter */
        List<BioAssay> bioAssays = mageMLConverter.getConvertedBioAssays();

        int i = 0;
        while ( true ) {

            log.info( " Quantitation type #" + ( i + 1 ) );

            InputStream[] is = new InputStream[bioAssays.size()];
            mageMLPreprocessor.setSelector( i );
            is[0] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031128_jm 29 c1 72hrao_031128_JM 29 C1 72hrAO_CEL_externaldata.txt.short" );

            is[1] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031128_jm 30 g1 72hrgen_031128_JM 30 G1 72hrGEN_CEL_externaldata.txt.short" );

            is[2] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031128_jm 31 e1 72hre2_031128_JM 31 E1 72hrE2_CEL_externaldata.txt.short" );

            is[3] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031128_jm 32 d1 72hrdes_031128_JM 32 D1 72hrDES_CEL_externaldata.txt.short" );

            is[4] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031201_jm 37 c2 72hrao_031201_JM 37 C2 72hrAO_CEL_externaldata.txt.short" );

            is[5] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031201_jm 38 g2 72hrgen_031201_JM 38 G2 72hrGEN_CEL_externaldata.txt.short" );

            is[6] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031201_jm 39 e2 72hre2_031201_JM 39 E2 72hrE2_CEL_externaldata.txt.short" );

            is[7] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031201_jm 40 d2 72hrdes_031201_JM 40 D2 72hrDES_CEL_externaldata.txt.short" );

            is[8] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031205_jm 45 c3 72hrao_031205_JM 45 C3 72hrAO_CEL_externaldata.txt.short" );

            is[9] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031205_jm 46 g3 72hrgen_031205_JM 46 G3 72hrGEN_CEL_externaldata.txt.short" );

            is[10] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031205_jm 47 e3 72hre2_031205_JM 47 E3 72hrE2_CEL_externaldata.txt.short" );

            is[11] = this.getClass().getResourceAsStream(
                    "/data/mage/E-AFMX-13/031205_jm 48 d3 72hrdes_031205_JM 48 D3 72hrDES_CEL_externaldata.txt.short" );

            try {
                mageMLPreprocessor.preprocessStreams( Arrays.asList( is ), bioAssays, mageMLConverter
                        .getBioAssayDimensions() );
            } catch ( NoMoreQuantitationTypesException e ) {
                log.info( "All done!" );
                break;
            }
            i++;
        }
    }
}
