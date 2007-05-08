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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.ontology.CharacteristicStatement;
import ubic.gemma.ontology.MeshService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.ontology.VocabCharacteristicBuilder;

/**
 * Simple class to parse XML in the format defined by
 * {@link http://www.ncbi.nlm.nih.gov/entrez/query/DTD/pubmed_041101.dtd}. The resulting BibliographicReference object
 * is associated with (transient) DatabaseEntry, in turn to a (transient) ExternalDatabase and Characteristics (for
 * MESH)
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParser {

    /**
     * 
     */
    private static final String MEDLINE_RECORD_AUTHOR_XPATH = "child::MedlineCitation/descendant::AuthorList/Author";

    // private static final String MEDLINE_ELEMENT = "MedlineCitation";
    private static final String ABSTRACT_TEXT_ELEMENT = "AbstractText";

    private static final String ERROR_TAG = "Error";

    // private static final String ARTICLE_ELEMENT = "Article";
    // private static final String MEDLINE_JOURNAL_INFO_ELEMENT = "MedlineJournalInfo";
    private static final String MEDLINE_JOURNAL_TITLE_ELEMENT = "MedlineTA";

    // private static final String PAGINATION_ELEMENT = "Pagination";
    private static final String MEDLINE_PAGINATION_ELEMENT = "MedlinePgn";
    private static final String PMID_ELEMENT = "PMID";
    /**
     * Used to define the ExternalDatabase object linked to the result.
     */
    private static final String PUB_MED_EXTERNAL_DB_NAME = "PubMed";
    private static final String PUB_STATUS_ELEMENT = "PubStatus";
    private static final String PUBMED_PUB_DATE_ELEMENT = "PubMedPubDate";
    // private static final String ABSTRACT_ELEMENT = "Abstract";
    private static final String TITLE_ELEMENT = "ArticleTitle";
    protected static final Log log = LogFactory.getLog( PubMedXMLParser.class );

    DocumentBuilder builder;

    /**
     * @param is
     * @return
     */
    public Collection<BibliographicReference> parse( InputStream is ) {

        try {
            if ( is.available() == 0 ) {
                throw new IOException( "XML stream contains no data." );
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments( true );
            // factory.setValidating( true );

            builder = factory.newDocumentBuilder();

            Document document = builder.parse( is );

            return extractBibRefs( document );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        } catch ( ParserConfigurationException e ) {
            throw new RuntimeException( e );
        } catch ( SAXException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param doc
     * @return
     * @throws IOException
     */
    private String extractAuthorList( Node article ) throws IOException, TransformerException {

        NodeList authorList = org.apache.xpath.XPathAPI.selectNodeList( article, MEDLINE_RECORD_AUTHOR_XPATH );

        StringBuilder al = new StringBuilder();
        for ( int i = 0; i < authorList.getLength(); i++ ) {

            Node item = authorList.item( i );
            if ( item.getNodeName().equals( "Author" ) ) {

                NodeList nl = item.getChildNodes();
                for ( int j = 0; j < nl.getLength(); j++ ) {

                    Node m = nl.item( j );

                    if ( m instanceof Element ) {

                        Element f = ( Element ) m;
                        if ( f.getNodeName().equals( "LastName" ) ) {
                            al.append( XMLUtils.getTextValue( f ) );
                            al.append( ", " );
                        } else if ( f.getNodeName().equals( "ForeName" ) ) {
                            al.append( XMLUtils.getTextValue( f ) );

                            al.append( "; " );

                        } else if ( f.getNodeName().equals( "Initials" ) ) {
                            ;
                        }
                    }
                }
            }
        }
        if ( al.length() == 0 ) return "(No authors listed)";
        if ( al.length() < 3 ) return al.toString();
        return al.toString().substring( 0, al.length() - 2 ); // trim trailing semicolon + space.
    }

    /**
     * @param doc
     * @return
     * @throws IOException
     */
    private Collection<BibliographicReference> extractBibRefs( Document document ) throws IOException {

        // Was there an error? (not found)
        if ( document.getElementsByTagName( ERROR_TAG ).getLength() > 0 ) {
            return null;
        }
        Collection<BibliographicReference> result = new HashSet<BibliographicReference>();

        NodeList articles = document.getElementsByTagName( "PubmedArticle" );

        log.debug( articles.getLength() + " articles found in document" );

        try {
            for ( int i = 0; i < articles.getLength(); i++ ) {

                Node article = articles.item( i );

                BibliographicReference bibRef = BibliographicReference.Factory.newInstance();

                processMESH( article, bibRef );

                Node abstractNode = org.apache.xpath.XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + ABSTRACT_TEXT_ELEMENT );
                if ( abstractNode != null ) {
                    bibRef.setAbstractText( XMLUtils.getTextValue( ( Element ) abstractNode ) );
                }

                Node pagesNode = org.apache.xpath.XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + MEDLINE_PAGINATION_ELEMENT );
                bibRef.setPages( XMLUtils.getTextValue( ( Element ) pagesNode ) );

                Node titleNode = org.apache.xpath.XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + TITLE_ELEMENT );
                bibRef.setTitle( XMLUtils.getTextValue( ( Element ) titleNode ) );

                Node volumeNode = org.apache.xpath.XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::Volume" );
                bibRef.setVolume( XMLUtils.getTextValue( ( Element ) volumeNode ) );

                Node issueNode = org.apache.xpath.XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::Issue" );
                bibRef.setIssue( XMLUtils.getTextValue( ( Element ) issueNode ) );

                Node publicationNode = org.apache.xpath.XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + MEDLINE_JOURNAL_TITLE_ELEMENT );
                bibRef.setPublication( XMLUtils.getTextValue( ( Element ) publicationNode ) );

                bibRef.setAuthorList( extractAuthorList( article ) );

                bibRef.setPublicationDate( extractPublicationDate( article ) );

                Node dbEntryNode = org.apache.xpath.XPathAPI.selectSingleNode( article, "child::MedlineCitation/descendant::" + PMID_ELEMENT );
                DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
                dbEntry.setAccession( XMLUtils.getTextValue( ( Element ) dbEntryNode ) );
                
                ExternalDatabase exDb = ExternalDatabase.Factory.newInstance();
                exDb.setName( PUB_MED_EXTERNAL_DB_NAME );
                dbEntry.setExternalDatabase( exDb );

                bibRef.setPubAccession( dbEntry );

                result.add( bibRef );
            }
        } catch ( TransformerException e ) {
            throw new RuntimeException( e );
        }
        return result;
    }

    /**
     * @param article
     * @param bibRef
     * @throws TransformerException
     * @throws IOException
     */
    private void processMESH( Node article, BibliographicReference bibRef ) throws TransformerException, IOException {
        NodeIterator meshHeadingIt = org.apache.xpath.XPathAPI.selectNodeIterator( article,
                "child::MedlineCitation/descendant::" + "MeshHeading" );
        Node meshNode = null;
        while ( ( meshNode = meshHeadingIt.nextNode() ) != null ) {
            Node descriptor = org.apache.xpath.XPathAPI.selectSingleNode( meshNode, "DescriptorName" );
            String d = XMLUtils.getTextValue( ( Element ) descriptor );
            boolean dmajorB = isMajorHeading( descriptor );

            OntologyTerm term = MeshService.find( d );
            if ( term == null ) {
                log.warn( "No MESH term found for: " + d );
                continue;
            }
            VocabCharacteristic vc = MeshService.getCharacteristic( term, dmajorB );

            processQualifiers( meshNode, term, vc );

            bibRef.getAnnotations().add( vc );
            log.info( vc );
        }
    }

    /**
     * @param meshNode
     * @param term
     * @param vc
     * @throws TransformerException
     * @throws IOException
     */
    private void processQualifiers( Node meshNode, OntologyTerm term, VocabCharacteristic vc )
            throws TransformerException, IOException {
        NodeIterator qualifierIt = org.apache.xpath.XPathAPI.selectNodeIterator( meshNode, "QualifierName" );

        Node qualifier = null;
        while ( ( qualifier = qualifierIt.nextNode() ) != null ) {
            String q = XMLUtils.getTextValue( ( Element ) qualifier );

            boolean qmajorB = isMajorHeading( qualifier );
            OntologyTerm qualTerm = MeshService.find( q );

            if ( qualTerm == null ) {
                log.warn( "No MESH term found for: " + q );
                continue;
            }

            CharacteristicStatement cs = MeshService.getQualifierStatement( term, qualTerm, qmajorB );
            VocabCharacteristicBuilder.addStatement( vc, cs );
        }
    }

    private boolean isMajorHeading( Node descriptor ) {
        Attr dmajorTopic = ( Attr ) descriptor.getAttributes().getNamedItem( "MajorTopicYN" );
        return dmajorTopic.getValue().equals( "Y" );
    }

    /**
     * Get the date this was put in pubmed.
     * 
     * @param doc
     * @return
     * @throws IOException
     */
    private Date extractPublicationDate( Node article ) throws IOException, TransformerException {

        NodeList dateList = org.apache.xpath.XPathAPI.selectNodeList( article, "/descendant::"
                + PUBMED_PUB_DATE_ELEMENT );
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int minute = 0;

        for ( int i = 0; i < dateList.getLength(); i++ ) {
            Node item = dateList.item( i );

            if ( item instanceof Element ) {
                Element ele = ( Element ) item;
                if ( ele.hasAttribute( PUB_STATUS_ELEMENT ) && ele.getAttribute( PUB_STATUS_ELEMENT ).equals( "pubmed" ) ) {

                    NodeList dateNodes = ele.getChildNodes();

                    for ( int j = 0; j < dateNodes.getLength(); j++ ) {
                        Node dateitem = dateNodes.item( j );

                        if ( dateitem instanceof Element ) {
                            Element elem = ( Element ) dateitem;

                            int num = Integer.parseInt( XMLUtils.getTextValue( elem ) );

                            if ( elem.getTagName().equals( "Year" ) ) {
                                year = num;
                            } else if ( elem.getTagName().equals( "Month" ) ) {
                                month = num - 1; // pubmed dates appear to be numbered from 1.
                            } else if ( elem.getTagName().equals( "Day" ) ) {
                                day = num;
                            } else if ( elem.getTagName().equals( "Hour" ) ) {
                                hour = num;
                            } else if ( elem.getTagName().equals( "Minute" ) ) {
                                minute = num;
                            } else {
                                assert false : "What are we doing here!";
                            }
                        }
                    }
                }
            }
        }

        Calendar c = Calendar.getInstance();
        c.set( year, month, day, hour, minute );

        return c.getTime();
    }

}
