package edu.columbia.gemma.loader.expression.mage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * Integration test of MageML: Parser, Converter and Preprocessor
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class MageMLPreprocessorTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( MageMLPreprocessorTest.class );

    BeanFactory ctx = null;
    private MageMLParser mageMLParser = null;
    private MageMLConverter mageMLConverter = null;
    private MageMLPreprocessor mageMLPreprocessor = null;

    public void setup() throws Exception {
        System.out.println( "here" );
        super.setUp();

    }

    public void tearDown() {
        ctx = null;
    }

    /**
     * Tests the conversion of source domain objects (SDO) to gemma domain objects (GDO)
     * 
     * @throws IOException
     * @throws TransformerException
     * TODO a work in progress
     */
    @SuppressWarnings("unchecked")
    public void testPreprocess() throws IOException, TransformerException {
        /* PARSING */
        log.debug( "***** PARSING ***** \n" );

        log.debug( "Parsing MAGE from ArrayExpress (AFMX)" );

        // TODO move this to the setup
        /* initialization of beans */
        ctx = SpringContextUtil.getApplicationContext(); // simplifiedXml will be null in mageMLConverter

        this.setMageMLParser( ( MageMLParser ) ctx.getBean( "mageMLParser" ) );

        this.setMageMLConverter( ( MageMLConverter ) ctx.getBean( "mageMLConverter" ) );

        this.setMageMLPreprocessor( ( MageMLPreprocessor ) ctx.getBean( "mageMLPreprocessor" ) );

        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLPreprocessorTest.class
                .getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );
        getMageMLParser().parse( istMageExamples );

        /* create the simplified xml file using the mageMLParser */
        InputStream ist2MageExamples = MageMLPreprocessorTest.class
                .getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );
        getMageMLParser().createSimplifiedXml( ist2MageExamples );

        // get results from parsing step
        log.info( "Tally:\n" + getMageMLParser() );
        Collection<Object> mageObjects = getMageMLParser().getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        // get xsl transformed xml file
        Document simplifiedXml = getMageMLParser().getSimplifiedXml();
        log.debug( "simplified xml document: " + simplifiedXml );

        // close input streams
        istMageExamples.close();
        ist2MageExamples.close();

        log.debug( "***** CONVERTING ***** \n" );
        /* CONVERTING */
        // create input stream from xsl file.
        if ( simplifiedXml == null ) {
            log.info( "simplfied xml file is null.  Exiting test ..." );
            System.exit( 0 );
        }

        // on Spring initialization, simplifiedXml is still null because it has not been passed a
        // document. Therefore, set it.
        getMageMLConverter().setSimplifiedXml( simplifiedXml );

        Collection<Object> gemmaObjects = getMageMLConverter().convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );
        for ( Object obj : gemmaObjects ) {
            log.debug( obj.getClass() + ": " + obj );
        }

        /* PREPROCESSING */
        log.debug( "***** PREPROCESSING ***** \n" );
        List<BioAssay> bioAssays = getMageMLConverter().getConvertedBioAssays();
        for ( BioAssay ba : bioAssays ) {
            List qtypes = getMageMLConverter().getBioAssayQuantitationTypeDimension( ba );
            
            List designElements = getMageMLConverter().getBioAssayDesignElementDimension( ba );
            
            // get all raw files
            getMageMLPreprocessor().preprocess( ba, qtypes, designElements );
        }

    }

    /**
     * @return Returns the mageMLParser.
     */
    public MageMLParser getMageMLParser() {
        return mageMLParser;
    }

    /**
     * @param mageMLParser The mageMLParser to set.
     */
    public void setMageMLParser( MageMLParser mageMLParser ) {
        this.mageMLParser = mageMLParser;
    }

    /**
     * @param mageMLConverter The mageMLConverter to set.
     */
    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

    /**
     * @return Returns the mageMLConverter.
     */
    public MageMLConverter getMageMLConverter() {
        return mageMLConverter;
    }

    /**
     * @return Returns the mageMLPreprocessor.
     */
    public MageMLPreprocessor getMageMLPreprocessor() {
        return mageMLPreprocessor;
    }

    /**
     * @param mageMLPreprocessor The mageMLPreprocessor to set.
     */
    public void setMageMLPreprocessor( MageMLPreprocessor mageMLPreprocessor ) {
        this.mageMLPreprocessor = mageMLPreprocessor;
    }
}
