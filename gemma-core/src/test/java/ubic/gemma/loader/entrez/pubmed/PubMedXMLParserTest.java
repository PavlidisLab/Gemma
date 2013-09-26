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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Keyword;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.model.common.description.PublicationType;
import ubic.gemma.model.expression.biomaterial.Compound;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParserTest {

    private static Log log = LogFactory.getLog( PubMedXMLParserTest.class.getName() );

    private InputStream testStream;
    private PubMedXMLParser testParser;

    @Before
    public void setUp() throws Exception {
        testParser = new PubMedXMLParser();
    }

    @After
    public void tearDown() throws Exception {
        testStream.close();
        testParser = null;
        testStream = null;
    }

    @Test
    public void testParse() throws Exception {
        try {
            testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-test.xml" );

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

    @Test
    public void testParseBook() throws Exception {
        try {
            testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-fullbook.xml" );
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertNotNull( br );
            assertEquals( "21796826", br.getPubAccession().getAccession() );
            assertEquals( "Field, Marilyn J; Boat, Thomas F", br.getEditor() );

            assertEquals(
                    "Institute of Medicine (US) Committee on Accelerating Rare Diseases Research and Orphan Product Development",
                    br.getAuthorList() );

            assertEquals( "Rare Diseases and Orphan Products: Accelerating Research and Development",
                    br.getPublication() );

            assertEquals( "Rare Diseases and Orphan Products: Accelerating Research and Development", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "yyyy" );
            Date publicationDate = br.getPublicationDate();
            assertNotNull( publicationDate );
            assertEquals( "2010", f.format( publicationDate ) );

            assertTrue( br.getAbstractText().startsWith(
                    "This Institute of Medicine (IOM) study grew out of discussions" ) );
            assertTrue( br.getAbstractText().endsWith( "interested general public." ) );

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
    @Test
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
            assertEquals( "2003", f.format( publicationDate ) );

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

    @Test
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
    @Test
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

    @Test
    public void testParseMultipartAbstract() throws Exception {
        try {
            testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-mpabs.xml" );

            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertNotNull( br.getAbstractText() );
            assertTrue( br.getAbstractText().startsWith( "PURPOSE: To dete" ) );
            assertTrue( br.getAbstractText().contains(
                    "METHODS: RGCs of Brown Norway rats were retrogradely labeled bilaterally with the "
                            + "fluorescent dye 4-(4-(dihexadecylamino)styryl)-N" ) );
            assertTrue( br.getAbstractText()
                    .contains( "CONCLUSIONS: The SLO is useful for in vivo imaging of rat RGCs" ) );
            log.info( br.getAbstractText() );
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
     * PMID 7914452 is an example of a retracted article.
     * 
     * @throws Exception
     */
    @Test
    public void testParseRetracted() throws Exception {
        try {
            testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-retracted.xml" );

            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertNotNull( br.getAbstractText() );
            assertEquals(
                    "Retracted [In: Garey CE, Schwarzman AL, Rise ML, Seyfried TN. Nat Genet. 1995 Sep;11(1):104 PMID=7550304]",
                    br.getDescription() );

            boolean ok = false;
            for ( PublicationType pt : br.getPublicationTypes() ) {
                if ( "Retracted Publication".equals( pt.getType() ) ) {
                    ok = true;
                }
            }
            assertTrue( ok );

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
}
