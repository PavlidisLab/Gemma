package edu.columbia.gemma.loader.smd.model;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 Columbia University
 * @author pavlidis
 * @version $Id$
 */
public class PublicationMetaTest extends TestCase {
   InputStream testStream;
   SMDPublication pubtest;
   /*
    * @see TestCase#setUp()
    */
   protected void setUp() throws Exception {
      super.setUp();
      testStream = PublicationMetaTest.class.getResourceAsStream( "/data/smd.pub-meta.test.txt" ) ;
      pubtest = new SMDPublication();
   }

   /*
    * @see TestCase#tearDown()
    */
   protected void tearDown() throws Exception {
      super.tearDown();
   }

   /*
    * Class under test for void read(InputStream)
    */
   public void testReadInputStream() throws IOException, SAXException {
      pubtest.read(testStream);
      String expectedReturn = "Diversity of gene expression in adenocarcinoma of the lung.";
      String actualReturn = pubtest.getTitle();
      assertEquals(expectedReturn, actualReturn );
   }

}
