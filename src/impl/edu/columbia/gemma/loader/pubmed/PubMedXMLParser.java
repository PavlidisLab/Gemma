package edu.columbia.gemma.loader.pubmed;

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
import org.xml.sax.SAXException;

import edu.columbia.gemma.common.description.BibliographicReference;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParser {

    public static final String ARTICLE_ELEMENT = "Article";
    public static final String MEDLINE_JOURNAL_INFO_ELEMENT = "MedlineJournalInfo";
    public static final String MEDLINE_JOURNAL_TITLE_ELEMENT = "MedlineTA";
    public static final String MEDLINE_ELEMENT = "MedlineCitation";
    public static final String ABSTRACT_TEXT_ELEMENT = "AbstractText";
    public static final String ABSTRACT_ELEMENT = "Abstract";
    public static final String TITLE_ELEMENT = "ArticleTitle";
    public static final String PAGINATION_ELEMENT = "Pagination";
    public static final String MEDLINE_PAGINATION_ELEMENT = "MedlinePgn";
    public static final String PMID_ELEMENT = "PMID";

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
        DocumentBuilder builder = factory.newDocumentBuilder();
        // builder.setErrorHandler()

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

        bibRef
                .setAbstractText( getTextValue( ( Element ) doc.getElementsByTagName( ABSTRACT_TEXT_ELEMENT ).item( 0 ) ) );
        bibRef.setPages( getTextValue( ( Element ) doc.getElementsByTagName( MEDLINE_PAGINATION_ELEMENT ).item( 0 ) ) );
        bibRef
                .setIdentifier( "pubmed:"
                        + getTextValue( ( Element ) doc.getElementsByTagName( PMID_ELEMENT ).item( 0 ) ) );
        bibRef.setTitle( getTextValue( ( Element ) doc.getElementsByTagName( TITLE_ELEMENT ).item( 0 ) ) );

        bibRef
                .setVolume( Integer
                        .parseInt( getTextValue( ( Element ) doc.getElementsByTagName( "Volume" ).item( 0 ) ) ) );

        bibRef.setIssue( Integer.parseInt( getTextValue( ( Element ) doc.getElementsByTagName( "Issue" ).item( 0 ) ) ) );

        bibRef.setPublication( getTextValue( ( Element ) doc.getElementsByTagName( MEDLINE_JOURNAL_TITLE_ELEMENT )
                .item( 0 ) ) );

        bibRef.setAuthorList( extractAuthorList( doc ) );
        bibRef.setYear( extractPublicationYear( doc ) );
        bibRef.setPublicationDate( extractPublicationDate( doc ) );
        return bibRef;
    }

    /**
     * @param doc
     * @return
     * @throws IOException
     */
    private String extractAuthorList( Document doc ) throws IOException {
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

        NodeList dateList = doc.getElementsByTagName( "PubMedPubDate" );

        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int minute = 0;

        for ( int i = 0; i < dateList.getLength(); i++ ) {
            Node item = dateList.item( i );

            if ( item instanceof Element ) {
                Element ele = ( Element ) item;
                if ( ele.hasAttribute( "PubStatus" ) && ele.getAttribute( "PubStatus" ).equals( "pubmed" ) ) {

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
     * @param doc
     * @return
     * @throws IOException
     */
    private String extractPublicationYear( Document doc ) throws IOException {
        NodeList dateList = doc.getElementsByTagName( "PubDate" ).item( 0 ).getChildNodes();
        for ( int i = 0; i < dateList.getLength(); i++ ) {
            Node item = dateList.item( i );
            if ( item instanceof Element && item.getNodeName().equals( "Year" ) ) {
                return getTextValue( ( Element ) item );
            }
        }
        return null;
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
