/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;

/**
 * Simple class to parse XML in the format defined by
 * {@link http://www.ncbi.nlm.nih.gov/entrez/query/DTD/pubmed_041101.dtd}. The resulting BibliographicReference object
 * is associated with (transient) DatabaseEntry, in turn to a (transient) ExternalDatabase.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParser {

    /**
     * Used to define the ExternalDatabase object linked to the result.
     */
    private static final String PUB_MED_EXTERNAL_DB_NAME = "PubMed";

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

    /**
     * @param is
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public BibliographicReference parse( InputStream is ) throws IOException, SAXException,
            ParserConfigurationException {

        if ( is.available() == 0 ) {
            throw new IOException( "XML stream contains no data." );
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments( true );
        // factory.setValidating( true );

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler( new ErrorHandler() {
            public void warning( SAXParseException exception ) throws SAXException {
                throw exception;
            }

            public void error( SAXParseException exception ) throws SAXException {
                throw exception;
            }

            public void fatalError( SAXParseException exception ) throws SAXException {
            }
        } );

        Document document = builder.parse( is );
        return setUpBibRef( document );
    }

    /**
     * @param doc
     * @return
     * @throws IOException
     */
    private BibliographicReference setUpBibRef( Document doc ) throws IOException {

        BibliographicReference bibRef = BibliographicReference.Factory.newInstance();

        // inserted null checks so that documents with older formats would still load
        if ( doc.getElementsByTagName( ABSTRACT_TEXT_ELEMENT ).getLength() > 0 )
            bibRef.setAbstractText( getTextValue( ( Element ) doc.getElementsByTagName( ABSTRACT_TEXT_ELEMENT )
                    .item( 0 ) ) );

        if ( doc.getElementsByTagName( MEDLINE_PAGINATION_ELEMENT ).getLength() > 0 )
            bibRef
                    .setPages( getTextValue( ( Element ) doc.getElementsByTagName( MEDLINE_PAGINATION_ELEMENT )
                            .item( 0 ) ) );

        if ( doc.getElementsByTagName( TITLE_ELEMENT ).getLength() > 0 )
            bibRef.setTitle( getTextValue( ( Element ) doc.getElementsByTagName( TITLE_ELEMENT ).item( 0 ) ) );

        if ( doc.getElementsByTagName( "Volume" ).getLength() > 0 )
            bibRef.setVolume( getTextValue( ( Element ) doc.getElementsByTagName( "Volume" ).item( 0 ) ) );

        if ( doc.getElementsByTagName( "Issue" ).getLength() > 0 )
            bibRef.setIssue( getTextValue( ( Element ) doc.getElementsByTagName( "Issue" ).item( 0 ) ) );

        if ( doc.getElementsByTagName( MEDLINE_JOURNAL_TITLE_ELEMENT ).getLength() > 0 )
            bibRef.setPublication( getTextValue( ( Element ) doc.getElementsByTagName( MEDLINE_JOURNAL_TITLE_ELEMENT )
                    .item( 0 ) ) );

        bibRef.setAuthorList( extractAuthorList( doc ) );
        // bibRef.setYear( extractPublicationYear( doc ) );
        bibRef.setPublicationDate( extractPublicationDate( doc ) );

        DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
        dbEntry.setAccession( getTextValue( ( Element ) doc.getElementsByTagName( PMID_ELEMENT ).item( 0 ) ) );

        ExternalDatabase exDb = ExternalDatabase.Factory.newInstance();
        exDb.setName( PUB_MED_EXTERNAL_DB_NAME );
        dbEntry.setExternalDatabase( exDb );

        bibRef.setPubAccession( dbEntry );

        return bibRef;
    }

    /**
     * @param doc
     * @return
     * @throws IOException
     */
    private String extractAuthorList( Document doc ) throws IOException {

        if ( doc.getElementsByTagName( "AuthorList" ).item( 0 ) == null ) {
            throw new IOException( "No results found" );
        }

        NodeList authorList = doc.getElementsByTagName( "AuthorList" ).item( 0 ).getChildNodes();
        StringBuffer al = new StringBuffer();
        for ( int i = 0; i < authorList.getLength(); i++ ) {

            Node item = authorList.item( i );
            if ( item.getNodeName().equals( "Author" ) ) {

                NodeList nl = item.getChildNodes();
                for ( int j = 0; j < nl.getLength(); j++ ) {

                    Node m = nl.item( j );

                    if ( m instanceof Element ) {

                        Element f = ( Element ) m;
                        if ( f.getNodeName().equals( "LastName" ) ) {
                            al.append( getTextValue( f ) );
                            al.append( ", " );
                        } else if ( f.getNodeName().equals( "ForeName" ) ) {
                            al.append( getTextValue( f ) );

                            al.append( "; " );

                        } else if ( f.getNodeName().equals( "Initials" ) ) {
                            ;
                        }
                    }
                }
            }
        }
        return al.toString().substring( 0, al.length() - 2 ); // trim trailing semicolon + space.
    }

    /**
     * Get the date this was put in pubmed.
     * 
     * @param doc
     * @return
     * @throws IOException
     */
    private Date extractPublicationDate( Document doc ) throws IOException {

        NodeList dateList = doc.getElementsByTagName( PUBMED_PUB_DATE_ELEMENT );

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

                            int num = Integer.parseInt( getTextValue( elem ) );

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

    /**
     * Make the horrible DOM API slightly more bearable: get the text value we know this element contains.
     * <p>
     * Borrowed from the Spring API.
     * 
     * @throws IOException
     */
    protected String getTextValue( Element ele ) throws IOException {
        StringBuffer value = new StringBuffer();
        NodeList nl = ele.getChildNodes();
        for ( int i = 0; i < nl.getLength(); i++ ) {
            Node item = nl.item( i );
            if ( item instanceof org.w3c.dom.CharacterData ) {
                if ( !( item instanceof org.w3c.dom.Comment ) ) {
                    value.append( item.getNodeValue() );
                }
            } else {
                throw new IOException( "element is just allowed to have text and comment nodes, not: "
                        + item.getClass().getName() );
            }
        }
        return value.toString();
    }

}
