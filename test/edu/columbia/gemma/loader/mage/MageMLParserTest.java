package edu.columbia.gemma.loader.mage;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.BioMaterial.BioMaterial;
import org.biomage.BioSequence.BioSequence;
import org.biomage.Experiment.Experiment;
import org.biomage.QuantitationType.QuantitationType;

import edu.columbia.gemma.util.PrettyPrinter;

public class MageMLParserTest extends TestCase {

    protected static final Log log = LogFactory.getLog( MageMLParserTest.class );

    MageMLParser mlp;
    InputStream istBioSequence;
    InputStream istExperiment;
    InputStream istArrayDesign;
    InputStream istBioMaterial;
    InputStream istQuantitationType;
    InputStream istDesignElement;
    InputStream istDrosDesignElement;
    InputStream istPhysicalBioAssay;
    InputStream istTIGRSimpleArrayDesign;
    InputStream istTIGRBiomaterial;

    InputStream istProtocol;
    InputStream istQTAffy;
    InputStream istQTGenePix;

    // zipped
    GZIPInputStream istBigBioSequence;
    GZIPInputStream istBigDesignElement;
    GZIPInputStream istBigArrayDesign;
    GZIPInputStream ist100CP;

    InputStream istDingledine;
    InputStream istExampleBioMaterial;
    InputStream istHematochromatosis;

    ZipInputStream istAffyGiantBioSequencePackage;

    protected void setUp() throws Exception {
        super.setUp();
        mlp = new MageMLParser();

        istBioSequence = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-Biosequence.xml" );
        istExperiment = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-Experiment.xml" );
        istArrayDesign = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-ArrayDesign.xml" );
        istBioMaterial = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-BioMaterial.xml" );
        istQuantitationType = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-QuantitationType.xml" );

        istDesignElement = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-DesignElement.xml" );
        istPhysicalBioAssay = MageMLParserTest.class.getResourceAsStream( "/data/mage/PhysicalBioAssay.xml" );
        istTIGRSimpleArrayDesign = MageMLParserTest.class.getResourceAsStream( "/data/mage/TIGRSimpleArrayDesign.xml" );
        istTIGRBiomaterial = MageMLParserTest.class.getResourceAsStream( "/data/mage/TIGRBiomaterial_package1.xml" );
        istDrosDesignElement = MageMLParserTest.class
                .getResourceAsStream( "/data/mage/DesignElement_minimal.package.xml" );
        istProtocol = MageMLParserTest.class.getResourceAsStream( "/data/mage/Protocol_package.xml" );
        istQTAffy = MageMLParserTest.class.getResourceAsStream( "/data/mage/QT_Affymetrix.xml" );
        istQTGenePix = MageMLParserTest.class.getResourceAsStream( "/data/mage/QT_GenePix.xml" );

        istDingledine = MageMLParserTest.class.getResourceAsStream( "/data/mage/dingledine-example.mageml.jsp.xml" );
        istExampleBioMaterial = MageMLParserTest.class
                .getResourceAsStream( "/data/mage/example-experiment.biomaterial.mageml.jsp.xml" );
        istHematochromatosis = MageMLParserTest.class.getResourceAsStream( "/data/mage/murine_hemochromatosis.xml" );
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mlp = null;
    }

    // todo these tests are lame.
    public void testGetData() throws Exception {

        mlp.parse( istBioSequence );
        Collection result = mlp.getData( BioSequence.class );
        log.debug( result.size() + " elements obtained" );

        mlp.parse( istExperiment );
        result = mlp.getData( Experiment.class );
        log.debug( result.size() + " elements obtained" );

        mlp.parse( istArrayDesign );
        result = mlp.getData( ArrayDesign.class );
        log.debug( result.size() + " elements obtained" );

        mlp.parse( istBioMaterial );
        result = mlp.getData( BioMaterial.class );
        log.debug( result.size() + " elements obtained" );

        mlp.parse( istQuantitationType );
        result = mlp.getData( QuantitationType.class );
        log.debug( result.size() + " elements obtained" );

    }

    public void testGetQuantitationTypes() throws Exception {
        log.debug( ">>> Converting Quantitation Types" );

        log.debug( "converting all: QuantitationType Affymetrix" );
        mlp.parse( istQTAffy );
        List result = ( List ) mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: QuantitationType GenePix" );
        mlp.parse( istQTGenePix );
        result = ( List ) mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: QuantitationType" );
        mlp.parse( istQuantitationType );
        result = ( List ) mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );
    }

    public void testGetAllConvertedData() throws Exception {

        log.debug( "converting all: TIGRBiomaterial" );
        mlp.parse( istTIGRBiomaterial );
        Collection result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: Biosequence" );
        mlp.parse( istBioSequence );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: BioMaterial" );
        mlp.parse( istBioMaterial );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: Experiment" );
        mlp.parse( istExperiment );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: ArrayDesign" );
        mlp.parse( istArrayDesign );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );

        log.debug( "converting all: DesignElement" );
        mlp.parse( istDesignElement );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );

        // this file is faulty, somehow; or there is a bug in the mage stk, or I'm doing something wrong. (actually, it
        // is a MAGE bug)
        // log.info( "converting all: DrosDesignElement" );
        // mlp.parse( istDrosDesignElement );
        // result = mlp.getConvertedData();
        // log.debug( result.size() + " elements obtained" );

        log.debug( "converting all: PhysicalBioAssay" );
        mlp.parse( istPhysicalBioAssay );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: TIGRSimpleArrayDesign" );
        mlp.parse( istTIGRSimpleArrayDesign );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: Protocol" );
        mlp.parse( istProtocol );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
        // log.debug( "\n" + PrettyPrinter.print( result ) );

    }

    public void testMoreTests() throws Exception {

        Collection result = null;

         log.debug( "converting all: Dingledine" );
         mlp.parse( istDingledine );
         result = mlp.getConvertedData();
         log.debug( result.size() + " elements obtained" );
  //       log.debug( "\n" + PrettyPrinter.print( result ) );

        log.debug( "converting all: Example Biomaterial" );
        mlp.parse( istExampleBioMaterial );
        result = mlp.getConvertedData();
        log.debug( result.size() + " elements obtained" );
  //      log.debug( "\n" + PrettyPrinter.print( result ) );

         log.debug( "converting all: Hemochromatosis" );
         mlp.parse( istHematochromatosis );
         result = mlp.getConvertedData();
         log.debug( result.size() + " elements obtained" );
 //        log.debug( "\n" + PrettyPrinter.print( result ) );

    }

    // public void testBigBioSequence() throws Exception {
    //
    // istBigBioSequence = new GZIPInputStream( MageMLParserTest.class
    // .getResourceAsStream( "/data/mage/A-TIGR-1-BioSequence.xml.gz" ) );
    //
    // log.debug( "Parsing big biosequence" );
    // mlp.parse( istBigBioSequence );
    // log.debug( "Converting big biosequence" );
    // Collection result = mlp.getConvertedData();
    // log.debug( result.size() + " elements obtained" );
    //
    // istBigBioSequence.close();
    // }
    //
    // public void testBigDesignElement() throws Exception {
    //
    // istBigDesignElement = new GZIPInputStream( MageMLParserTest.class
    // .getResourceAsStream( "/data/mage/A-TIGR-1-DesignElement.xml.gz" ) );
    //
    // log.debug( "Parsing BigDesignElement" );
    // mlp.parse( istBigDesignElement );
    // log.debug( "Converting BigDesignElement" );
    // Collection result = mlp.getConvertedData();
    // log.debug( result.size() + " elements obtained" );
    //
    // istBigDesignElement.close();
    // }
    //
    // public void testBigArrayDesign() throws Exception {
    // istBigArrayDesign = new GZIPInputStream( MageMLParserTest.class
    // .getResourceAsStream( "/data/mage/A-TIGR-1-ArrayDesign.xml.gz" ) );
    //
    // log.debug( "Parsing BigArrayDesign" );
    // mlp.parse( istBigArrayDesign );
    // log.debug( "Converting BigArrayDesign" );
    // Collection result = mlp.getConvertedData();
    // log.debug( result.size() + " elements obtained" );
    //
    // istBigArrayDesign.close();
    // }

    // /**
    // * A real stress-test.
    // *
    // * @throws Exception
    // */
    // public void testAffyGiantBiosequence() throws Exception {
    // istAffyGiantBioSequencePackage = new ZipInputStream( MageMLParserTest.class
    // .getResourceAsStream( "/data/mage/HG-U133_Plus_2_annot_xml.zip" ) );
    // istAffyGiantBioSequencePackage.getNextEntry();
    //
    // log.debug( "Parsing BigArrayDesign" );
    // mlp.parse( istAffyGiantBioSequencePackage );
    // log.debug( "Converting Giant Biosequence Package (Affy)" );
    // Collection result = mlp.getConvertedData();
    // log.debug( result.size() + " elements obtained" );
    //
    // istAffyGiantBioSequencePackage.close();
    //
    // }

    // public void test100CP() throws Exception { // ist100CP = new GZIPInputStream( MageMLParserTest.class

    // .getResourceAsStream( "/data/mage/11188230_100CP_MAGE-ML.XML.gz" ) );
    //
    // log.debug( "Parsing 100CP" );
    // mlp.parse( ist100CP );
    // log.debug( "Converting 100CP" );
    // Collection result = mlp.getConvertedData();
    // assertTrue( result instanceof Collection );
    // }

}
