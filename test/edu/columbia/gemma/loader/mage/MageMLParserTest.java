package edu.columbia.gemma.loader.mage;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.BioMaterial.BioMaterial;
import org.biomage.BioSequence.BioSequence;
import org.biomage.Experiment.Experiment;
import org.biomage.QuantitationType.QuantitationType;

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
        istDrosDesignElement = MageMLParserTest.class.getResourceAsStream( "/data/mage/DesignElement_minimal.package.xml" );
        istProtocol = MageMLParserTest.class.getResourceAsStream( "/data/mage/Protocol_package.xml" );
        istQTAffy = MageMLParserTest.class.getResourceAsStream( "/data/mage/QT_Affymetrix.xml" );
        istQTGenePix = MageMLParserTest.class.getResourceAsStream( "/data/mage/QT_GenePix.xml" );

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mlp = null;
    }

   
    // todo these tests are lame.
    public void testGetData() throws Exception {

        mlp.parse( istBioSequence );
        Collection result = mlp.getData( BioSequence.class );
        log.warn( result.size() + " elements obtained" );

        mlp.parse( istExperiment );
        result = mlp.getData( Experiment.class );
        log.warn( result.size() + " elements obtained" );

        mlp.parse( istArrayDesign );
        result = mlp.getData( ArrayDesign.class );
        log.warn( result.size() + " elements obtained" );

        mlp.parse( istBioMaterial );
        result = mlp.getData( BioMaterial.class );
        log.warn( result.size() + " elements obtained" );

        mlp.parse( istQuantitationType );
        result = mlp.getData( QuantitationType.class );
        log.warn( result.size() + " elements obtained" );

    }

    public void testGetConvertedData() throws Exception {
        mlp.parse( istBioSequence );
        Collection result = mlp.getConvertedData( BioSequence.class );
        assertTrue( result instanceof Collection );
    }

    public void testGetAllConvertedData() throws Exception {

        log.warn( "converting all: Biosequence" );
        mlp.parse( istBioSequence );
        Collection result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: QuantitationType" );
        mlp.parse( istQuantitationType );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: BioMaterial" );
        mlp.parse( istBioMaterial );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: Experiment" );
        mlp.parse( istExperiment );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: ArrayDesign" );
        mlp.parse( istArrayDesign );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: DesignElement" );
        mlp.parse( istDesignElement );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        // this file is faulty, somehow; or there is a bug in the mage stk, or I'm doing something wrong.
        log.info( "converting all: DrosDesignElement" );
        mlp.parse( istDrosDesignElement );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: PhysicalBioAssay" );
        mlp.parse( istPhysicalBioAssay );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: TIGRSimpleArrayDesign" );
        mlp.parse( istTIGRSimpleArrayDesign );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: TIGRBiomaterial" );
        mlp.parse( istTIGRBiomaterial );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: Protocol" );
        mlp.parse( istProtocol );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: QuantitationType Affymetrix" );
        mlp.parse( istQTAffy );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );

        log.warn( "converting all: QuantitationType GenePix" );
        mlp.parse( istQTGenePix );
        result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );
    }

    public void testBigBioSequence() throws Exception {

        istBigBioSequence = new GZIPInputStream( MageMLParserTest.class
                .getResourceAsStream( "/data/mage/A-TIGR-1-BioSequence.xml.gz" ) );

        log.warn( "Parsing big biosequence" );
        mlp.parse( istBigBioSequence );
        log.warn( "Converting big biosequence" );
        Collection result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );
    }

    public void testBigDesignElement() throws Exception {

        istBigDesignElement = new GZIPInputStream( MageMLParserTest.class
                .getResourceAsStream( "/data/mage/A-TIGR-1-DesignElement.xml.gz" ) );

        log.warn( "Parsing BigDesignElement" );
        mlp.parse( istBigDesignElement );
        log.warn( "Converting BigDesignElement" );
        Collection result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );
    }

    public void testBigArrayDesign() throws Exception {
        istBigArrayDesign = new GZIPInputStream( MageMLParserTest.class
                .getResourceAsStream( "/data/mage/A-TIGR-1-ArrayDesign.xml.gz" ) );

        log.warn( "Parsing BigArrayDesign" );
        mlp.parse( istBigArrayDesign );
        log.warn( "Converting BigArrayDesign" );
        Collection result = mlp.getConvertedData();
        log.warn( result.size() + " elements obtained" );
    }

    // public void test100CP() throws Exception {
    // ist100CP = new GZIPInputStream( MageMLParserTest.class
    // .getResourceAsStream( "/data/mage/11188230_100CP_MAGE-ML.XML.gz" ) );
    //
    // log.warn( "Parsing 100CP" );
    // mlp.parse( ist100CP );
    // log.warn( "Converting 100CP" );
    // Collection result = mlp.getConvertedData();
    // assertTrue( result instanceof Collection );
    // }

    
    public static Test suite() {
        return new TestSuite( MageMLParserTest.class );
    }

    public static void main( String args[] ) {
        junit.textui.TestRunner.run( suite() );
    }

}
