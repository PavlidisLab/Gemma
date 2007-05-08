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
import java.text.DateFormat;
import java.text.ParseException;
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
import ubic.gemma.model.common.description.Keyword;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.model.common.description.PublicationType;
import ubic.gemma.model.expression.biomaterial.Compound;

/**
 * Simple class to parse XML in the format defined by
 * {@link http://www.ncbi.nlm.nih.gov/entrez/query/DTD/pubmed_041101.dtd}. The resulting BibliographicReference object
 * is associated with (transient) DatabaseEntry, in turn to a (transient) ExternalDatabase and MESH.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParser {

    private static final String MEDLINE_RECORD_AUTHOR_XPATH = "AuthorList/Author";

    private static final String ERROR_TAG = "Error";

    private static final String MEDLINE_PAGINATION_ELEMENT = "Pagination/MedlinePgn";

    private static final String PUB_MED_EXTERNAL_DB_NAME = "PubMed";
    private static final String PUB_STATUS_ELEMENT = "PubStatus";
    private static final String PUBMED_PUB_DATE_ELEMENT = "PubMedPubDate";
    // private static final String ABSTRACT_ELEMENT = "Abstract";
    private static final String TITLE_ELEMENT = "//ArticleTitle";
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
            factory.setValidating( false );
            builder = factory.newDocumentBuilder();
            Document document = builder.parse( is );
            log.debug( "done parsing" );
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

        NodeList articles = document.getElementsByTagName( "MedlineCitation" );

        log.debug( articles.getLength() + " articles found in document" );

        try {
            for ( int i = 0; i < articles.getLength(); i++ ) {

                Node record = articles.item( i );

                Node article = org.apache.xpath.XPathAPI.selectSingleNode( record, "Article" );

                BibliographicReference bibRef = BibliographicReference.Factory.newInstance();

                processMESH( record, bibRef );

                Node abstractNode = org.apache.xpath.XPathAPI.selectSingleNode( article, "Abstract/AbstractText" );
                if ( abstractNode != null ) {
                    bibRef.setAbstractText( XMLUtils.getTextValue( ( Element ) abstractNode ) );
                }

                Node pagesNode = org.apache.xpath.XPathAPI.selectSingleNode( article, MEDLINE_PAGINATION_ELEMENT );
                bibRef.setPages( XMLUtils.getTextValue( ( Element ) pagesNode ) );

                Node titleNode = org.apache.xpath.XPathAPI.selectSingleNode( article, TITLE_ELEMENT );
                bibRef.setTitle( XMLUtils.getTextValue( ( Element ) titleNode ) );

                Node volumeNode = org.apache.xpath.XPathAPI.selectSingleNode( article, "Journal/JournalIssue/Volume" );
                bibRef.setVolume( XMLUtils.getTextValue( ( Element ) volumeNode ) );

                Node issueNode = org.apache.xpath.XPathAPI.selectSingleNode( article, "Journal/JournalIssue/Issue" );
                bibRef.setIssue( XMLUtils.getTextValue( ( Element ) issueNode ) );

                Node publicationNode = org.apache.xpath.XPathAPI.selectSingleNode( record,
                        "MedlineJournalInfo/MedlineTA" );
                bibRef.setPublication( XMLUtils.getTextValue( ( Element ) publicationNode ) );

                bibRef.setAuthorList( extractAuthorList( article ) );

                bibRef.setPublicationDate( extractPublicationDate( article ) );

                bibRef.setChemicals( extractChemicals( record ) );

                bibRef.setKeywords( extractKeywords( record ) );

                bibRef.setPublicationTypes( extractPublicationTypes( record ) );

                Node dbEntryNode = org.apache.xpath.XPathAPI.selectSingleNode( record, "PMID" );
                DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
                dbEntry.setAccession( XMLUtils.getTextValue( ( Element ) dbEntryNode ) );
                ExternalDatabase exDb = ExternalDatabase.Factory.newInstance();
                exDb.setName( PUB_MED_EXTERNAL_DB_NAME );
                dbEntry.setExternalDatabase( exDb );

                bibRef.setPubAccession( dbEntry );
                result.add( bibRef );

                if ( i > 0 && i % 100 == 0 ) {
                    log.info( "Processed " + i + " articles" );
                }
            }
        } catch ( TransformerException e ) {
            throw new RuntimeException( e );
        }
        return result;
    }

    /**
     * @param article
     * @return
     * @throws TransformerException
     * @throws IOException
     */
    private Collection<PublicationType> extractPublicationTypes( Node article ) throws TransformerException,
            IOException {
        NodeIterator pubtypeNodeIt = org.apache.xpath.XPathAPI.selectNodeIterator( article,
                "//PublicationTypeList/PublicationType" );

        Node pubtypeNode = null;
        Collection<PublicationType> publicationTypes = new HashSet<PublicationType>();
        while ( ( pubtypeNode = pubtypeNodeIt.nextNode() ) != null ) {

            String type = XMLUtils.getTextValue( ( Element ) pubtypeNode );
            PublicationType pt = PublicationType.Factory.newInstance();
            pt.setType( type );
            publicationTypes.add( pt );
        }
        return publicationTypes;
    }

    /**
     * @param record
     * @return
     * @throws TransformerException
     * @throws IOException
     */
    private Collection<Keyword> extractKeywords( Node record ) throws TransformerException, IOException {
        Collection<Keyword> keywords = new HashSet<Keyword>();
        Node keywordNode = org.apache.xpath.XPathAPI.selectSingleNode( record, "KeywordList" );
        if ( keywordNode == null ) return keywords;
        NodeList childNodes = keywordNode.getChildNodes();
        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node item = childNodes.item( i );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            Element el = ( Element ) item;
            String keyword = XMLUtils.getTextValue( el );
            Boolean isMajor = isMajorHeading( item );
            Keyword kw = Keyword.Factory.newInstance();
            kw.setTerm( keyword );
            kw.setIsMajorTopic( isMajor );
            keywords.add( kw );
        }
        return keywords;
    }

    /**
     * @param record
     * @return
     * @throws TransformerException
     * @throws IOException
     */
    private Collection<Compound> extractChemicals( Node record ) throws TransformerException, IOException {
        Collection<Compound> compounds = new HashSet<Compound>();
        Node chemNodes = org.apache.xpath.XPathAPI.selectSingleNode( record, "ChemicalList" );
        if ( chemNodes == null ) return compounds;

        NodeList childNodes = chemNodes.getChildNodes();
        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node chemNode = childNodes.item( i );
            NodeList termNodes = chemNode.getChildNodes();
            if ( termNodes.getLength() == 0 ) continue;

            Compound c = Compound.Factory.newInstance();
            for ( int j = 0; j < termNodes.getLength(); j++ ) {
                Node item = termNodes.item( j );
                if ( !( item instanceof Element ) ) {
                    continue;
                }
                Element el = ( Element ) item;
                if ( el.getNodeName().equals( "RegistryNumber" ) ) {
                    String regString = XMLUtils.getTextValue( el );
                    c.setRegistryNumber( regString );
                } else {
                    String txt = XMLUtils.getTextValue( el );
                    c.setName( txt );
                }
            }
            log.debug( c.getName() );
            compounds.add( c );
        }

        return compounds;
    }

    /**
     * @param record
     * @param bibRef
     * @throws TransformerException
     * @throws IOException
     */
    private void processMESH( Node record, BibliographicReference bibRef ) throws TransformerException, IOException {
        // NodeIterator meshHeadingIt = org.apache.xpath.XPathAPI.selectNodeIterator( article,
        // "//MeshHeadingList/MeshHeading" );
        Node meshHeadings = org.apache.xpath.XPathAPI.selectSingleNode( record, "MeshHeadingList" );
        if ( meshHeadings == null ) return;
        NodeList childNodes = meshHeadings.getChildNodes();

        // while ( ( meshNode = childNodes.nextNode() ) != null ) {
        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node meshNode = childNodes.item( i );
            NodeList termNodes = meshNode.getChildNodes();
            MedicalSubjectHeading vc = MedicalSubjectHeading.Factory.newInstance();

            if ( termNodes.getLength() == 0 ) continue;

            // these might just be a single Descriptor or a Descriptor with Qualifiers.
            for ( int j = 0; j < termNodes.getLength(); j++ ) {

                Node item = termNodes.item( j );
                if ( !( item instanceof Element ) ) {
                    continue;
                }
                Element descriptor = ( Element ) item;
                if ( descriptor.getNodeName().equals( "DescriptorName" ) ) {
                    String d = XMLUtils.getTextValue( descriptor );
                    boolean dmajorB = isMajorHeading( descriptor );
                    vc.setTerm( d );
                    vc.setIsMajorTopic( dmajorB );
                } else {
                    MedicalSubjectHeading qual = MedicalSubjectHeading.Factory.newInstance();
                    String q = XMLUtils.getTextValue( ( Element ) descriptor );
                    boolean qmajorB = isMajorHeading( descriptor );
                    qual.setIsMajorTopic( qmajorB );
                    qual.setTerm( q );
                    vc.getQualifiers().add( qual );
                }

            }

            // OntologyTerm term = MeshService.find( d );
            // if ( term == null ) {
            // log.warn( "No MESH term found for: " + d );
            // continue;
            // }
            // VocabCharacteristic vc = MeshService.getCharacteristic( term, dmajorB );

            bibRef.getMeshTerms().add( vc );
        }
    }

    // /**
    // * @param meshNode
    // * @param term
    // * @param vc
    // * @throws TransformerException
    // * @throws IOException
    // */
    // private void processQualifiers( Node meshNode, MedicalSubjectHeading vc ) throws TransformerException,
    // IOException {
    // NodeIterator qualifierIt = org.apache.xpath.XPathAPI.selectNodeIterator( meshNode, "//QualifierName" );
    //
    // Node qualifier = null;
    // while ( ( qualifier = qualifierIt.nextNode() ) != null ) {
    // String q = XMLUtils.getTextValue( ( Element ) qualifier );
    //
    // boolean qmajorB = isMajorHeading( qualifier );
    //
    // MedicalSubjectHeading qual = MedicalSubjectHeading.Factory.newInstance();
    // qual.setIsMajorTopic( qmajorB );
    // qual.setTerm( q );
    //
    // // OntologyTerm qualTerm = MeshService.find( q );
    // //
    // // if ( qualTerm == null ) {
    // // log.warn( "No MESH term found for: " + q );
    // // continue;
    // // }
    // //
    // // CharacteristicStatement cs = MeshService.getQualifierStatement( term, qualTerm, qmajorB );
    // // VocabCharacteristicBuilder.addStatement( vc, cs );
    // vc.getQualifiers().add( qual );
    // }
    // }

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
        Date d = extractJournalIssueDate( article );
        if ( d == null ) d = extractPubmedPubdate( article );
        return d;
    }

    /**
     * @param article
     * @return
     * @throws TransformerException
     * @throws IOException
     */
    private Date extractJournalIssueDate( Node article ) throws TransformerException, IOException {
        // get it from the journal information.

        Node dateNode = org.apache.xpath.XPathAPI.selectSingleNode( article, "Journal/JournalIssue/PubDate" );
        Node dn = org.apache.xpath.XPathAPI.selectSingleNode( dateNode, "Day" );
        Node y = org.apache.xpath.XPathAPI.selectSingleNode( dateNode, "Year" );
        Node m = org.apache.xpath.XPathAPI.selectSingleNode( dateNode, "Month" );
        Node medLineDate = org.apache.xpath.XPathAPI.selectSingleNode( dateNode, "MedlineDate" );
        String yearText = XMLUtils.getTextValue( ( Element ) y );
        String medLineText = XMLUtils.getTextValue( ( Element ) medLineDate );
        String monthText = XMLUtils.getTextValue( ( Element ) m );
        String dayText = XMLUtils.getTextValue( ( Element ) dn );
        DateFormat df = DateFormat.getDateInstance( DateFormat.MEDIUM );
        df.setLenient( true );

        if ( yearText == null && medLineText != null ) {
            String[] yearmo = medLineText.split( "\\s" );
            yearText = ( yearmo[0] );
            monthText = ( yearmo[1] );
            monthText = monthText.replaceAll( "-\\w+", "" );
        }

        String dateString = monthText + " " + ( dayText == null ? "1" : dayText ) + ", " + yearText;
        try {
            return df.parse( dateString );
        } catch ( ParseException e ) {

            log.warn( "Could not parse date " + dateString );
            return null;
        }
    }

    /**
     * This is a fallback that should not get used.
     * 
     * @param article
     * @return
     * @throws TransformerException
     * @throws IOException
     * @deprecated
     */
    private Date extractPubmedPubdate( Node article ) throws TransformerException, IOException {
        NodeList dateList = org.apache.xpath.XPathAPI.selectNodeList( article, "/descendant::"
                + PUBMED_PUB_DATE_ELEMENT );
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int minute = 0;

        boolean found = false;
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

                    found = true;

                }
            }
        }

        if ( !found ) return null;
        Calendar c = Calendar.getInstance();
        c.set( year, month, day, hour, minute );
        Date d = c.getTime();
        return d;
    }

}
