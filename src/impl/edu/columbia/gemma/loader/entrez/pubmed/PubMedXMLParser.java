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
package edu.columbia.gemma.loader.entrez.pubmed;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xpath.internal.XPathAPI;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;

/**
 * Simple class to parse XML in the format defined by
 * {@link http://www.ncbi.nlm.nih.gov/entrez/query/DTD/pubmed_041101.dtd}. The resulting BibliographicReference object
 * is associated with (transient) DatabaseEntry, in turn to a (transient) ExternalDatabase.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParser {

    protected static final Log log = LogFactory.getLog( PubMedXMLParser.class );

    /**
     * Used to define the ExternalDatabase object linked to the result.
     */
    private static final String PUB_MED_EXTERNAL_DB_NAME = "PubMed";

    private static final String ERROR_TAG = "Error";

    private static final String PUB_STATUS_ELEMENT = "PubStatus";
    private static final String PUBMED_PUB_DATE_ELEMENT = "PubMedPubDate";
    // private static final String ARTICLE_ELEMENT = "Article";
    // private static final String MEDLINE_JOURNAL_INFO_ELEMENT = "MedlineJournalInfo";
    private static final String MEDLINE_JOURNAL_TITLE_ELEMENT = "MedlineTA";
    // private static final String MEDLINE_ELEMENT = "MedlineCitation";
    private static final String ABSTRACT_TEXT_ELEMENT = "AbstractText";
    // private static final String ABSTRACT_ELEMENT = "Abstract";
    private static final String TITLE_ELEMENT = "ArticleTitle";
    // private static final String PAGINATION_ELEMENT = "Pagination";
    private static final String MEDLINE_PAGINATION_ELEMENT = "MedlinePgn";
    private static final String PMID_ELEMENT = "PMID";

    DocumentBuilder builder;

    /**
     * @param is
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public Collection<BibliographicReference> parse( InputStream is ) throws IOException, ParserConfigurationException,
            SAXException {

        if ( is.available() == 0 ) {
            throw new IOException( "XML stream contains no data." );
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments( true );
        // factory.setValidating( true );

        builder = factory.newDocumentBuilder();
        // builder.setErrorHandler( new ErrorHandler() {
        // public void warning( SAXParseException exception ) throws SAXException {
        // throw exception;
        // }
        //
        // public void error( SAXParseException exception ) throws SAXException {
        // throw exception;
        // }
        //
        // public void fatalError( SAXParseException exception ) throws SAXException {
        // throw exception;
        // }
        // } );

        Document document = builder.parse( is );

        return extractBibRefs( document );
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

        log.info( articles.getLength() + " articles found in document" );

        try {
            for ( int i = 0; i < articles.getLength(); i++ ) {

                Node article = articles.item( i );

                BibliographicReference bibRef = BibliographicReference.Factory.newInstance();

                bibRef.setAbstractText( XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + ABSTRACT_TEXT_ELEMENT ).getTextContent() );

                bibRef.setPages( XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + MEDLINE_PAGINATION_ELEMENT ).getTextContent() );

                bibRef.setTitle( XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + TITLE_ELEMENT ).getTextContent() );

                bibRef.setVolume( XPathAPI.selectSingleNode( article, "child::MedlineCitation/descendant::Volume" )
                        .getTextContent() );

                bibRef.setIssue( XPathAPI.selectSingleNode( article, "child::MedlineCitation/descendant::Issue" )
                        .getTextContent() );

                bibRef.setPublication( XPathAPI.selectSingleNode( article,
                        "child::MedlineCitation/descendant::" + MEDLINE_JOURNAL_TITLE_ELEMENT ).getTextContent() );

                bibRef.setAuthorList( extractAuthorList( article ) );

                bibRef.setPublicationDate( extractPublicationDate( article ) );

                DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
                dbEntry.setAccession( XPathAPI.selectSingleNode( article, "/descendant::" + PMID_ELEMENT )
                        .getTextContent() );

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
     * @param doc
     * @return
     * @throws IOException
     */
    private String extractAuthorList( Node article ) throws IOException, TransformerException {

        NodeList authorList = XPathAPI.selectNodeList( article, "child::MedlineCitation/descendant::AuthorList/Author" );

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
     * Get the date this was put in pubmed.
     * 
     * @param doc
     * @return
     * @throws IOException
     */
    private Date extractPublicationDate( Node article ) throws IOException, TransformerException {

        NodeList dateList = XPathAPI.selectNodeList( article, "/descendant::" + PUBMED_PUB_DATE_ELEMENT );
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
