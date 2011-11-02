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
package ubic.gemma.loader.entrez.pubmed;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Keyword;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.model.expression.biomaterial.Compound;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParserTest extends TestCase {

    private static Log log = LogFactory.getLog( PubMedXMLParserTest.class.getName() );

    InputStream testStream;
    PubMedXMLParser testParser;

    public void testParse() throws Exception {
        try {
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
            assertNotNull( br.getAbstractText() );
            assertEquals( "Genome Res", br.getPublication() );
            assertEquals( "15173114", br.getPubAccession().getAccession() );
            assertEquals( "Coexpression analysis of human genes across many microarray data sets.", br.getTitle() );
            assertNotNull( br.getVolume() );
            assertNotNull( br.getPages() );
            SimpleDateFormat f = new SimpleDateFormat( "mm/HH/MM/dd/yyyy" );
            assertEquals( "00/00/06/01/2004", f.format( br.getPublicationDate() ) );
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof java.net.ConnectException ) {
                log.warn( "Test skipped due to connection exception" );
                return;
            } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped due to unknown host exception" );
                return;
            } else {
                throw ( e );
            }
        }
    }

    /**
     * Test uses 20301315
     * 
     * @throws Exception
     */
    public void testParseBookArticle() throws Exception {
        try {
            testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-bookarticle.xml" );
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertNotNull( br );

            assertEquals( "Pagon, Roberta A; Bird, Thomas D; Dolan, Cynthia R; Stephens, Karen", br.getEditor() );

            assertEquals( "Kuhlenbaumer, Gregor; Timmerman, Vincent", br.getAuthorList() );

            assertEquals( "GeneReviews", br.getPublication() );
            assertEquals( "Giant Axonal Neuropathy", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "yyyy" );
            Date publicationDate = br.getPublicationDate();
            assertNotNull( publicationDate );
            assertEquals( "1993", f.format( publicationDate ) );

            assertTrue( br.getAbstractText().startsWith(
                    "Giant axonal neuropathy (GAN) is characterized by a severe early-onset" ) );
            assertTrue( br.getAbstractText().endsWith(
                    "offering custom prenatal testing if the disease-causing mutations in a family are known." ) );

        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof java.net.ConnectException ) {
                log.warn( "Test skipped due to connection exception" );
                return;
            } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped due to unknown host exception" );
                return;
            } else {
                throw ( e );
            }
        }
    }

    public void testParseMesh() throws Exception {
        try {
            testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-mesh-test.xml" );
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            Collection<MedicalSubjectHeading> meshTerms = br.getMeshTerms();
            assertEquals( 16, meshTerms.size() );
            // for ( MedicalSubjectHeading heading : meshTerms ) {
            // log.info( heading.getTerm() + " " + heading.getIsMajorTopic() );
            // for ( MedicalSubjectHeading q : heading.getQualifiers() ) {
            // log.info( " qualifier: " + q.getTerm() + " " + q.getIsMajorTopic() );
            // }
            // }
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof java.net.ConnectException ) {
                log.warn( "Test skipped due to connection exception" );
                return;
            } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped due to unknown host exception" );
                return;
            } else {
                throw ( e );
            }
        }
    }

    /**
     * This uses a medline-format file, instead of the pubmed xml files we get from the eutils.
     * 
     * @throws Exception
     */
    public void testParseMulti() throws Exception {
        try {
            testStream = new GZIPInputStream(
                    PubMedXMLParserTest.class.getResourceAsStream( "/data/loader/medline.multi.xml.gz" ) );
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            assertEquals( 147, brl.size() );
            int expectedNumberofKeywords = 258;
            int expectedNumberofCompounds = 46;
            int actualNumberofKeywords = 0;
            int actualNumberofCompounds = 0;
            for ( BibliographicReference reference : brl ) {
                assertNotNull( reference.getPublicationDate() );
                Collection<Keyword> keywords = reference.getKeywords();
                for ( Keyword keyword : keywords ) {
                    assertNotNull( keyword.getTerm() );
                    // log.info( keyword.getTerm() );
                    actualNumberofKeywords++;
                }
                for ( Compound c : reference.getChemicals() ) {
                    assertNotNull( c.getName() );
                    // log.info( c.getName() );
                    actualNumberofCompounds++;
                }
                assertTrue( reference.getPublicationTypes().size() > 0 );
            }

            assertEquals( expectedNumberofKeywords, actualNumberofKeywords );
            assertEquals( expectedNumberofCompounds, actualNumberofCompounds );

        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof java.net.ConnectException ) {
                log.warn( "Test skipped due to connection exception" );
                return;
            } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped due to unknown host exception" );
                return;
            } else {
                throw ( e );
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-test.xml" );
        testParser = new PubMedXMLParser();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testStream.close();
        testParser = null;
        testStream = null;
    }
}
