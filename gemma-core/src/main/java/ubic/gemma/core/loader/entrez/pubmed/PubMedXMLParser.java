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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.*;
import ubic.gemma.core.loader.entrez.EntrezXmlUtils;
import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.expression.biomaterial.Compound;

import javax.xml.xpath.XPathExpression;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Simple class to parse XML in the format defined by
 * <a href="http://www.ncbi.nlm.nih.gov/entrez/query/DTD/pubmed_041101.dtd">ncbi</a>. The resulting
 * BibliographicReference object is
 * associated with (transient) DatabaseEntry, in turn to a (transient) ExternalDatabase and MeSH.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class PubMedXMLParser {

    protected static final Log log = LogFactory.getLog( PubMedXMLParser.class );
    private static final String ERROR_TAG = "Error";
    private static final String PUB_MED_EXTERNAL_DB_NAME = "PubMed";
    private static final Locale PUB_MED_LOCALE = Locale.ENGLISH;
    private static final DateFormat df = DateFormat.getDateInstance( DateFormat.MEDIUM );
    private static final String[] PUB_MED_DATE_FORMATS = new String[] { "MMM dd, yyyy", "yyyy", "mm dd, yyyy" };

    private static final XPathExpression xRefSource = XMLUtils.compile( "RefSource/text()" );
    private static final XPathExpression xPmid = XMLUtils.compile( "PMID/text()" );

    public static void extractBookPublicationYear( BibliographicReference bibRef, Node item ) {
        NodeList c = item.getChildNodes();
        for ( int i = 0; i < c.getLength(); i++ ) {
            Node a = c.item( i );
            if ( !( a instanceof Element ) ) {
                continue;
            }
            if ( a.getNodeName().equals( "Year" ) ) {
                try {
                    bibRef.setPublicationDate( DateUtils.parseDate( XMLUtils.getTextValue( a ), PUB_MED_LOCALE, PUB_MED_DATE_FORMATS ) );
                } catch ( ParseException e ) {
                    PubMedXMLParser.log.warn( "Could not extract date of publication from : " + XMLUtils
                            .getTextValue( a ) );
                }
            }
        }
    }

    public static void extractPublisher( BibliographicReference bibRef, Node item ) {
        NodeList c = item.getChildNodes();
        for ( int i = 0; i < c.getLength(); i++ ) {
            Node a = c.item( i );
            if ( !( a instanceof Element ) ) {
                continue;
            }
            if ( a.getNodeName().equals( "PublisherName" ) ) {
                bibRef.setPublisher( XMLUtils.getTextValue( a ) );
            } else if ( a.getNodeName().equals( "PublisherLocation" ) ) {
                bibRef.setPublisher( bibRef.getPublisher() + " [ " + XMLUtils.getTextValue( a ) + "]" );
            }
        }
    }

    public static Collection<BibliographicReference> parse( InputStream is ) throws IOException {
        Document document = EntrezXmlUtils.parse( is );
        PubMedXMLParser.log.debug( "done parsing" );
        return extractBibRefs( document );
    }

    private static String extractAuthorList( NodeList authorList ) {
        StringBuilder al = new StringBuilder();
        for ( int i = 0; i < authorList.getLength(); i++ ) {
            Node item = authorList.item( i );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            NodeList nl = item.getChildNodes();
            for ( int j = 0; j < nl.getLength(); j++ ) {

                Node m = nl.item( j );

                if ( m instanceof Element ) {

                    Element f = ( Element ) m;
                    String nodeName = f.getNodeName();
                    switch ( nodeName ) {
                        case "LastName":
                            al.append( XMLUtils.getTextValue( f ) );
                            al.append( ", " );
                            break;
                        case "ForeName":
                        case "CollectiveName":
                            al.append( XMLUtils.getTextValue( f ) );
                            al.append( "; " );
                            break;
                        case "Initials":
                        case "AffiliationInfo":
                        case "Identifier":
                        case "Suffix":
                            break;
                        default:
                            log.warn( "Unrecognized node name " + nodeName );
                    }
                }
            }
        }

        if ( al.length() == 0 )
            return "(No authors listed)";
        if ( al.length() < 3 )
            return al.toString();
        return al.substring( 0, al.length() - 2 ); // trim trailing semicolon + space.
    }

    private static Collection<BibliographicReference> extractBibRefs( Document document ) {

        // Was there an error? (not found)
        if ( document.getElementsByTagName( PubMedXMLParser.ERROR_TAG ).getLength() > 0 ) {
            return null;
        }

        NodeList articles = document.getElementsByTagName( "MedlineCitation" );

        if ( articles.getLength() == 0 ) {
            // mebbe it is a book?
            articles = document.getElementsByTagName( "BookDocument" );
            if ( articles.getLength() > 0 ) {
                return parseBookArticles( articles );
            }
            return new HashSet<>();
        }

        Collection<BibliographicReference> result = new HashSet<>();
        PubMedXMLParser.log.debug( articles.getLength() + " articles found in document" );

        int i = 0;
        for ( ; i < articles.getLength(); i++ ) {
            BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
            Node record = articles.item( i );

            Node article = processRecord( bibRef, record );

            assert article != null;

            Node journal = processArticle( bibRef, article );

            processJournalInfo( bibRef, journal );

            result.add( bibRef );

            if ( i >= 100 && i % 1000 == 0 ) {
                PubMedXMLParser.log.info( "Processed " + i + " articles" );
            }
        }
        if ( i >= 100 )
            PubMedXMLParser.log.info( "Processed " + i + " articles" );

        return result;
    }

    private static Set<Compound> extractChemicals( Node chemNodes ) {
        Set<Compound> compounds = new HashSet<>();
        NodeList childNodes = chemNodes.getChildNodes();
        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node chemNode = childNodes.item( i );
            NodeList termNodes = chemNode.getChildNodes();
            if ( termNodes.getLength() == 0 )
                continue;

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
            PubMedXMLParser.log.debug( c.getName() );
            compounds.add( c );
        }

        return compounds;
    }

    private static Date extractJournalIssueDate( Node dateNode ) {

        String yearText = null;// = XMLUtils.getTextValue( ( Element ) y );
        String medLineText = null;// = XMLUtils.getTextValue( ( Element ) medLineDate );
        String monthText = null;// = XMLUtils.getTextValue( ( Element ) m );
        String dayText = null;// = XMLUtils.getTextValue( ( Element ) dn );

        NodeList childNodes = dateNode.getChildNodes();
        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node c = childNodes.item( i );
            if ( !( c instanceof Element ) ) {
                continue;
            }
            String t = XMLUtils.getTextValue( c );
            switch ( c.getNodeName() ) {
                case "Year":
                    yearText = t;
                    break;
                case "Month":
                    monthText = t;
                    break;
                case "Day":
                    dayText = t;
                    break;
                case "MedlineDate":
                    medLineText = t;
                    break;
                default:
                    log.warn( "Unrecognized node name " + c.getNodeName() );
            }
        }

        df.setLenient( true );

        if ( yearText == null && medLineText != null ) {
            String[] yearmo = medLineText.split( "\\s" );
            switch ( yearmo.length ) {
                case 2:
                    // 1983 Aug
                    yearText = yearmo[0];
                    monthText = yearmo[1];
                    monthText = monthText.replaceAll( "-\\w+", "" );
                    break;
                case 4:
                case 3:
                    // 1983 Jul 9-16
                    // 1983 Aug 31-Sep 6
                    yearText = yearmo[0];
                    monthText = yearmo[1];
                    dayText = yearmo[2].replaceAll( "-\\w+", "" );
                    break;
                case 1:
                    // 1983-84
                    yearText = yearmo[0];
                    yearText = yearText.replaceAll( "-\\w+", "" );
                    break;
                default:
                    PubMedXMLParser.log.warn( "No data information from medline text: " + medLineText );
                    break;
            }
        }

        if ( monthText == null ) {
            monthText = "Jan"; // arbitrary...
        }

        String dateString = monthText + " " + ( dayText == null ? "01" : dayText ) + ", " + yearText;

        try {
            return DateUtils.parseDate( dateString, PUB_MED_LOCALE, PUB_MED_DATE_FORMATS );
        } catch ( ParseException e ) {
            PubMedXMLParser.log.warn( "Could not parse date " + dateString );
            return null;
        }
    }

    private static Set<Keyword> extractKeywords( Node keywordNode ) {
        Set<Keyword> keywords = new HashSet<>();
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

    private static Date extractPublicationDate( Node dateNode ) {
        return extractJournalIssueDate( dateNode );
    }

    private static boolean isRetracted( Node pubtypeList ) {
        //   private Collection<PublicationType> extractPublicationTypes( Node pubtypeList ) {
        NodeList childNodes = pubtypeList.getChildNodes();
        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node item = childNodes.item( i );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String type = XMLUtils.getTextValue( item );

            if ( "Retracted Publication".equals( type ) ) {
                return true;
            }
        }
        return false;
        //   return publicationTypes;
    }

    private static boolean isMajorHeading( Node descriptor ) {
        Attr dmajorTopic = ( Attr ) descriptor.getAttributes().getNamedItem( "MajorTopicYN" );
        return dmajorTopic.getValue().equals( "Y" );
    }

    private static Collection<BibliographicReference> parseBookArticles( NodeList articles ) {
        Collection<BibliographicReference> result = new HashSet<>();
        int i = 0;
        for ( ; i < articles.getLength(); i++ ) {
            BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
            Node record = articles.item( i );

            processBookRecord( bibRef, record );

            result.add( bibRef );

            if ( i >= 100 && i % 1000 == 0 ) {
                PubMedXMLParser.log.info( "Processed " + i + " books" );
            }
        }
        if ( i >= 100 ) PubMedXMLParser.log.info( "Processed " + i + " books" );
        return result;
    }

    private static void processAccession( BibliographicReference bibRef, Node record ) {
        String accession = XMLUtils.getTextValue( record );
        DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
        dbEntry.setAccession( accession );
        ExternalDatabase exDb = ExternalDatabase.Factory.newInstance();
        exDb.setName( PubMedXMLParser.PUB_MED_EXTERNAL_DB_NAME );
        dbEntry.setExternalDatabase( exDb );
        bibRef.setPubAccession( dbEntry );
    }

    private static Node processArticle( BibliographicReference bibRef, Node article ) {
        NodeList childNodes = article.getChildNodes();
        Node journal = null;
        for ( int j = 0; j < childNodes.getLength(); j++ ) {
            Node item = childNodes.item( j );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String name = item.getNodeName();
            switch ( name ) {
                case "ArticleTitle":
                    bibRef.setTitle( XMLUtils.getTextValue( item ) );
                    break;
                case "Journal":
                    journal = item;
                    break;
                case "AuthorList":
                    bibRef.setAuthorList( extractAuthorList( item.getChildNodes() ) );
                    break;
                case "Pagination":
                    bibRef.setPages( XMLUtils.extractOneChildText( item, "MedlinePgn" ) );
                    break;
                case "Abstract":
                    // abstracts can have parts
                    List<String> abstractParts = XMLUtils.extractMultipleChildren( item, "AbstractText" );

                    if ( abstractParts.size() > 1 ) {
                        StringBuilder buf = new StringBuilder();
                        NodeList jNodes = item.getChildNodes();
                        for ( int q = 0; q < jNodes.getLength(); q++ ) {
                            Node jitem = jNodes.item( q );
                            if ( !( jitem instanceof Element ) ) {
                                continue;
                            }
                            if ( jitem.getNodeName().equals( "AbstractText" ) ) {
                                Node lab = jitem.getAttributes().getNamedItem( "Label" );
                                if ( lab != null ) {
                                    String label = lab.getTextContent();

                                    String part = jitem.getTextContent();
                                    if ( StringUtils.isNotBlank( label ) ) {
                                        buf.append( label ).append( ": " ).append( part ).append( "\n" );
                                    } else {
                                        buf.append( part ).append( "\n" );
                                    }
                                }
                            }
                        }
                        bibRef.setAbstractText( buf.toString() );
                    } else {
                        bibRef.setAbstractText( abstractParts.iterator().next() );
                    }
                    break;
                case "PublicationTypeList":
                    bibRef.setRetracted( isRetracted( item ) );
                    break;
                case "Language":
                case "DateCompleted":
                case "DateRevised":
                case "CitationSubset":
                case "AffiliationInfo":
                case "GrantList":
                case "KeywordList":
                case "ELocationID":
                case "ArticleDate":
                    break;
                default:
                    log.debug( "Unrecognized node name " + name );
            }
        }
        return journal;
    }

    private static void processBookInfo( BibliographicReference bibRef, Node article ) {
        NodeList childNodes = article.getChildNodes();

        for ( int j = 0; j < childNodes.getLength(); j++ ) {
            Node item = childNodes.item( j );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String name = item.getNodeName();
            if ( name.equals( "Publisher" ) ) {
                PubMedXMLParser.extractPublisher( bibRef, item );
            } else if ( name.equals( "PubDate" ) && bibRef.getPublicationDate() == null ) {
                PubMedXMLParser.extractBookPublicationYear( bibRef, item );
            } else if ( name.equals( "AuthorList" ) ) {
                if ( ( ( Element ) item ).hasAttribute( "Type" ) ) {
                    if ( ( ( Element ) item ).getAttribute( "Type" ).equals( "editors" ) ) {
                        bibRef.setEditor( extractAuthorList( item.getChildNodes() ) );
                    } else {
                        bibRef.setAuthorList( extractAuthorList( item.getChildNodes() ) );
                    }
                }
            } else if ( name.equals( "BookTitle" ) ) {
                if ( bibRef.getTitle() == null )
                    bibRef.setTitle( XMLUtils.getTextValue( item ) );
                bibRef.setPublication( XMLUtils.getTextValue( item ) );
            }
        }
    }

    /**
     * Fill in information about the book: Publisher, Editor(s), Publication year
     *
     * @param bibRef bib ref
     * @param record record
     */
    private static void processBookRecord( BibliographicReference bibRef, Node record ) {

        NodeList recordNodes = record.getChildNodes();
        for ( int p = 0; p < recordNodes.getLength(); p++ ) {
            Node item = recordNodes.item( p );
            if ( !( item instanceof Element ) ) {
                continue;
            }

            String name = item.getNodeName();
            switch ( name ) {
                case "ArticleTitle":
                    // this is the title of the chapter.
                    bibRef.setTitle( StringUtils.strip( XMLUtils.getTextValue( item ) ) );
                    break;
                case "Book":
                    processBookInfo( bibRef, item );
                    break;
                case "AuthorList":
                    bibRef.setAuthorList( extractAuthorList( item.getChildNodes() ) );
                    break;
                case "Abstract":
                    bibRef.setAbstractText( "" );
                    NodeList abstractTextSections = item.getChildNodes();
                    for ( int q = 0; q < abstractTextSections.getLength(); q++ ) {
                        Node jitem = abstractTextSections.item( q );
                        if ( !( jitem instanceof Element ) ) {
                            continue;
                        }
                        if ( jitem.getNodeName().equals( "AbstractText" ) ) {
                            bibRef.setAbstractText(
                                    bibRef.getAbstractText() + ( XMLUtils.getTextValue( jitem ) ) + " " );
                        }

                        bibRef.setAbstractText( bibRef.getAbstractText().trim() );
                    }
                    break;
                case "PMID":
                    processAccession( bibRef, item );
                    break;
                case "ContributionDate":
                    /*
                     * Unusual, but happens for books that are updated with new sections. We use this instead of the
                     * publication date.
                     */
                    PubMedXMLParser.extractBookPublicationYear( bibRef, item );
                    break;
                default:
                    log.debug( "Unrecognized node name " + name );
            }
        }

    }

    private static void processJournalInfo( BibliographicReference bibRef, Node journal ) {
        NodeList journalNodes = journal.getChildNodes();
        for ( int j = 0; j < journalNodes.getLength(); j++ ) {
            Node item = journalNodes.item( j );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String name = item.getNodeName();
            if ( name.equals( "JournalIssue" ) ) {
                NodeList journalIssueNodes = item.getChildNodes();
                for ( int k = 0; k < journalIssueNodes.getLength(); k++ ) {
                    Node jitem = journalIssueNodes.item( k );
                    if ( !( jitem instanceof Element ) ) {
                        continue;
                    }
                    String jname = jitem.getNodeName();
                    switch ( jname ) {
                        case "Volume":
                            bibRef.setVolume( XMLUtils.getTextValue( jitem ) );
                            break;
                        case "Issue":
                            bibRef.setIssue( XMLUtils.getTextValue( jitem ) );
                            break;
                        case "PubDate":
                            bibRef.setPublicationDate( extractPublicationDate( jitem ) );
                            break;
                        default:
                            log.debug( "Unrecognized node name " + jname );
                    }
                }
            }
        }
    }

    private static void processMESH( Node meshHeadings, BibliographicReference bibRef ) {
        NodeList childNodes = meshHeadings.getChildNodes();

        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node meshNode = childNodes.item( i );
            NodeList termNodes = meshNode.getChildNodes();
            MedicalSubjectHeading vc = MedicalSubjectHeading.Factory.newInstance();

            if ( termNodes.getLength() == 0 )
                continue;

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
                    String q = XMLUtils.getTextValue( descriptor );
                    boolean qmajorB = isMajorHeading( descriptor );
                    qual.setIsMajorTopic( qmajorB );
                    qual.setTerm( q );
                    vc.getQualifiers().add( qual );
                }

            }

            bibRef.getMeshTerms().add( vc );
        }
    }

    private static Node processRecord( BibliographicReference bibRef, Node record ) {
        Node article = null;

        NodeList recordNodes = record.getChildNodes();
        for ( int p = 0; p < recordNodes.getLength(); p++ ) {
            Node item = recordNodes.item( p );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String name = item.getNodeName();
            switch ( name ) {
                case "Article":
                    article = item;
                    break;
                case "ChemicalList":
                    bibRef.setChemicals( extractChemicals( item ) );
                    break;
                case "MeshHeadingList":
                    processMESH( item, bibRef );
                    break;
                case "KeywordList":
                    bibRef.setKeywords( extractKeywords( item ) );
                    break;
                case "MedlineJournalInfo": {
                    NodeList jNodes = item.getChildNodes();
                    for ( int q = 0; q < jNodes.getLength(); q++ ) {
                        Node jitem = jNodes.item( q );
                        if ( !( jitem instanceof Element ) ) {
                            continue;
                        }
                        if ( jitem.getNodeName().equals( "MedlineTA" ) ) {
                            bibRef.setPublication( XMLUtils.getTextValue( jitem ) );
                        }
                    }
                    break;
                }
                case "PMID":
                    processAccession( bibRef, item );
                    break;
                case "CommentsCorrectionsList":
                    NodeList jNodes = item.getChildNodes();
                    for ( int q = 0; q < jNodes.getLength(); q++ ) {
                        Node jitem = jNodes.item( q );
                        if ( !( jitem instanceof Element ) ) {
                            continue;
                        }
                        Node reftype = jitem.getAttributes().getNamedItem( "RefType" );

                        if ( reftype == null )
                            continue;

                        String reftypeName = ( ( Attr ) reftype ).getValue();
                        PubMedXMLParser.log.debug( reftypeName );
                        if ( reftypeName.equals( "RetractionIn" ) ) {
                            String ref = XMLUtils.evaluateToString( xRefSource, jitem );
                            String pmid = XMLUtils.evaluateToString( xPmid, jitem );
                            String description = "Retracted [In: " + ref + " PMID=" + pmid + "]";
                            bibRef.setDescription( description );
                            /*
                             * Such papers also have <PublicationType>Retracted Publication</PublicationType>
                             */
                        }

                    }

                    break;
                case "DateCompleted":
                case "CitationSubset":
                case "DateRevised":
                case "OtherID":
                case "CoiStatement":
                case "InvestigatorList":
                case "OtherAbstract":
                case "Suffix":
                case "SupplMeshList": //hmm.
                case "GeneralNote":
                case "NumberOfReferences":
                    break;
                default:
                    log.warn( "Unrecognized node name " + name );
            }
        }
        return article;
    }

}
