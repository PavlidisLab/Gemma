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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Keyword;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.model.expression.biomaterial.Compound;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * @author pavlidis
 */
public class PubMedXMLParserTest {

    private static final Log log = LogFactory.getLog( PubMedXMLParserTest.class.getName() );

    private PubMedXMLParser testParser;

    @Before
    public void setUp() {
        testParser = new PubMedXMLParser();
    }

    @Test
    public void testParse() throws Exception {
        try ( InputStream testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-test.xml" ) ) {
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
            assertNotNull( br.getAbstractText() );
            assertEquals( "Genome Res", br.getPublication() );
            assertEquals( "15173114", br.getPubAccession().getAccession() );
            assertEquals( "Coexpression analysis of human genes across many microarray data sets.", br.getTitle() );
            assertNotNull( br.getVolume() );
            assertNotNull( br.getPages() );
            SimpleDateFormat f = new SimpleDateFormat( "mm/HH/MM/dd/yyyy", Locale.ENGLISH );
            assertEquals( "00/00/06/01/2004", f.format( br.getPublicationDate() ) );
        } catch ( IOException e ) {
            this.logOrThrowException( e );
        }
    }

    @Test
    public void testParseBook() throws Exception {
        try ( InputStream testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-fullbook.xml" ) ) {
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

            SimpleDateFormat f = new SimpleDateFormat( "yyyy", Locale.ENGLISH );
            Date publicationDate = br.getPublicationDate();
            assertNotNull( publicationDate );
            assertEquals( "2010", f.format( publicationDate ) );

            assertTrue( br.getAbstractText()
                    .startsWith( "This Institute of Medicine (IOM) study grew out of discussions" ) );
            assertTrue( br.getAbstractText().endsWith( "interested general public." ) );

        } catch ( IOException e ) {
            this.logOrThrowException( e );
        }
    }

    /*
     * Test uses 2030131
     */
    @Test
    public void testParseBookArticle() throws Exception {
        try ( InputStream testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-bookarticle.xml" ) ) {
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertNotNull( br );

            assertEquals( "Pagon, Roberta A; Bird, Thomas D; Dolan, Cynthia R; Stephens, Karen", br.getEditor() );

            assertEquals( "Kuhlenbaumer, Gregor; Timmerman, Vincent", br.getAuthorList() );

            assertEquals( "GeneReviews", br.getPublication() );
            assertEquals( "Giant Axonal Neuropathy", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "yyyy", Locale.ENGLISH );
            Date publicationDate = br.getPublicationDate();
            assertNotNull( publicationDate );
            assertEquals( "2003", f.format( publicationDate ) );

            assertTrue( br.getAbstractText()
                    .startsWith( "Giant axonal neuropathy (GAN) is characterized by a severe early-onset" ) );
            assertTrue( br.getAbstractText().endsWith(
                    "offering custom prenatal testing if the disease-causing mutations in a family are known." ) );

        } catch ( IOException e ) {
            this.logOrThrowException( e );
        }
    }

    @Test
    public void testParseMesh() throws Exception {
        try ( InputStream testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-mesh-test.xml" ) ) {
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
        } catch ( IOException e ) {
            this.logOrThrowException( e );
        }
    }

    /*
     * This uses a medline-format file, instead of the pubmed xml files we get from the eutils.
     */
    @Test
    @Category(SlowTest.class)
    public void testParseMulti() throws Exception {
        assumeThatResourceIsAvailable( "https://dtd.nlm.nih.gov/ncbi/pubmed/out/pubmed_190101.dtd" );
        try ( InputStream testStream = new GZIPInputStream( new ClassPathResource( "/data/loader/medline.multi.xml.gz" ).getInputStream() ) ) {
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
                //   assertTrue( reference.getPublicationTypes().size() > 0 );
            }

            assertEquals( expectedNumberofKeywords, actualNumberofKeywords );
            assertEquals( expectedNumberofCompounds, actualNumberofCompounds );

        } catch ( IOException e ) {
            this.logOrThrowException( e );
        }
    }

    @Test
    public void testParseMultipartAbstract() throws Exception {
        try ( InputStream testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-mpabs.xml" ) ) {
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertNotNull( br.getAbstractText() );
            assertTrue( br.getAbstractText().startsWith( "PURPOSE: To dete" ) );
            assertTrue( br.getAbstractText().contains(
                    "METHODS: RGCs of Brown Norway rats were retrogradely labeled bilaterally with the "
                            + "fluorescent dye 4-(4-(dihexadecylamino)styryl)-N" ) );
            assertTrue(
                    br.getAbstractText().contains( "CONCLUSIONS: The SLO is useful for in vivo imaging of rat RGCs" ) );
            PubMedXMLParserTest.log.info( br.getAbstractText() );
        } catch ( IOException e ) {
            this.logOrThrowException( e );
        }
    }

    /*
     * PMID 7914452 is an example of a retracted article.
     */
    @Test
    public void testParseRetracted() throws Exception {
        assumeThatResourceIsAvailable( "https://dtd.nlm.nih.gov/ncbi/pubmed/out/pubmed_180101.dtd" );
        try ( InputStream testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-retracted.xml" ) ) {
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertNotNull( br.getAbstractText() );
            assertEquals(
                    "Retracted [In: Garey CE, Schwarzman AL, Rise ML, Seyfried TN. Nat Genet. 1995 Sep;11(1):104 PMID=7550304]",
                    br.getDescription() );

            assertTrue( br.getRetracted() );

        } catch ( IOException e ) {
            this.logOrThrowException( e );
        }
    }

    private void logOrThrowException( IOException e ) throws IOException {
        if ( e.getCause() instanceof java.net.ConnectException ) {
            assumeNoException( "Test skipped due to connection exception", e );
        } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
            assumeNoException( "Test skipped due to unknown host exception", e );
        } else {
            throw ( e );
        }
    }
}
