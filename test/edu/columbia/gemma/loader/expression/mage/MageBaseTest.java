package edu.columbia.gemma.loader.expression.mage;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import javax.xml.transform.TransformerException;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageBaseTest extends BaseDAOTestCase {

    /**
     * XSL-transform the mage document. This is only needed for testing. In production, this is done as part of the
     * parsing.
     * 
     * @param mlp
     * @param resourceName
     * @throws IOException
     */
    protected void zipXslSetup( MageMLParser mlp, String resourceName ) throws IOException, TransformerException {
        ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class.getResourceAsStream( resourceName ) );
        istMageExamples.getNextEntry();
        assert istMageExamples != null;
        mlp.createSimplifiedXml( istMageExamples );
        istMageExamples.close();
    }

    /**
     * XSL-transform the mage document. This is only needed for testing. In production, this is done as part of the
     * parsing.
     * 
     * @param mlp
     * @param resourceName
     * @throws IOException
     */
    protected void xslSetup( MageMLParser mlp, String resourceName ) throws IOException, TransformerException {
        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( resourceName );
        assert istMageExamples != null;
        mlp.createSimplifiedXml( istMageExamples );
        istMageExamples.close();
    }

}
