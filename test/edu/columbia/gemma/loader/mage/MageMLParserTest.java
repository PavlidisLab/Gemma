package edu.columbia.gemma.loader.mage;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.BioAssay.BioAssay;
import org.biomage.BioAssay.Channel;
import org.biomage.BioAssayData.BioAssayData;
import org.biomage.BioMaterial.BioMaterial;
import org.biomage.BioSequence.BioSequence;
import org.biomage.Experiment.Experiment;
import org.biomage.QuantitationType.QuantitationType;

import junit.framework.TestCase;

public class MageMLParserTest extends TestCase {

    MageMLParser mlp;
    InputStream istBioSequence;
    InputStream istExperiment;
    InputStream istArrayDesign;
    InputStream istBioMaterial;
    InputStream istQuantitationType;

    protected void setUp() throws Exception {
        super.setUp();
        mlp = new MageMLParser();

        istBioSequence = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-Biosequence.xml" );
        istExperiment = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-Experiment.xml" );
        istArrayDesign = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-ArrayDesign.xml" );
        istBioMaterial = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-BioMaterial.xml" );
        istQuantitationType = MageMLParserTest.class.getResourceAsStream( "/data/mage/MGP-QuantitationType.xml" );
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mlp = null;
    }

    // todo these tests are lame.
    public void testGetData() throws Exception {

        mlp.parse( istBioSequence );
        Object result = mlp.getData( BioSequence.class );
        assertTrue( result instanceof Collection );

        mlp.parse( istExperiment );
        result = mlp.getData( Experiment.class );
        assertTrue( result instanceof Collection );

        mlp.parse( istArrayDesign );
        result = mlp.getData( ArrayDesign.class );
        assertTrue( result instanceof Collection );

        mlp.parse( istBioMaterial );
        result = mlp.getData( BioMaterial.class );
        assertTrue( result instanceof Collection );

        mlp.parse( istQuantitationType );
        result = mlp.getData( QuantitationType.class );
        assertTrue( result instanceof Collection );

    }

    public void testGetConvertedData() throws Exception {
        mlp.parse( istBioSequence );
        Object result = mlp.getConvertedData( BioSequence.class );
        assertTrue( result instanceof Collection );
    }

    public void testGetAllConvertedData() throws Exception {
        System.err.println( "converting all" );
        
        mlp.parse( istBioSequence );
        Object result = mlp.getConvertedData();
        assertTrue( result instanceof Collection );
        
        mlp.parse( istQuantitationType );
        result = mlp.getConvertedData();
        assertTrue( result instanceof Collection );

        mlp.parse( istBioMaterial );
        result = mlp.getConvertedData();
        assertTrue( result instanceof Collection );

        mlp.parse( istExperiment );
        result = mlp.getConvertedData();
        assertTrue( result instanceof Collection );

        mlp.parse( istArrayDesign );
        result = mlp.getConvertedData();
        assertTrue( result instanceof Collection );

    }

}
