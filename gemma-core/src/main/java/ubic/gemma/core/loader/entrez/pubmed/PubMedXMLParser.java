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

import javax.annotation.Nullable;
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
public class PubMedXMLParser {

    protected static final Log log = LogFactory.getLog( PubMedXMLParser.class );
    private static final String PUB_MED_EXTERNAL_DB_NAME = "PubMed";
    private static final Locale PUB_MED_LOCALE = Locale.ENGLISH;
    private static final DateFormat df = DateFormat.getDateInstance( DateFormat.MEDIUM );
    private static final String[] PUB_MED_DATE_FORMATS = new String[] { "MMM dd, yyyy", "yyyy", "mm dd, yyyy" };

    private static final XPathExpression xRefSource = XMLUtils.compile( "RefSource/text()" );
    private static final XPathExpression xPmid = XMLUtils.compile( "PMID/text()" );

    static {
        df.setLenient( true );
    }

    public static Collection<BibliographicReference> parse( InputStream is ) throws IOException {
        Document document = EntrezXmlUtils.parse( is );
        PubMedXMLParser.log.debug( "done parsing" );
        return extractBibRefs( document );
    }

    private static Collection<BibliographicReference> extractBibRefs( Document document ) {
        ExternalDatabase exDb = ExternalDatabase.Factory.newInstance();
        exDb.setName( PubMedXMLParser.PUB_MED_EXTERNAL_DB_NAME );

        NodeList articles = document.getElementsByTagName( "MedlineCitation" );

        if ( articles.getLength() == 0 ) {
            // mebbe it is a book?
            articles = document.getElementsByTagName( "BookDocument" );
            if ( articles.getLength() > 0 ) {
                return parseBookArticles( articles, exDb );
            }
            return new HashSet<>();
        }

        Collection<BibliographicReference> result = new HashSet<>();
        PubMedXMLParser.log.debug( articles.getLength() + " articles found in document" );

        int i = 0;
        for ( ; i < articles.getLength(); i++ ) {
            BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
            Node record = articles.item( i );

            Node article = processMedlineCitation( record, bibRef, exDb );

            if ( article != null ) {
                processArticle( article, bibRef );
            } else {
                PubMedXMLParser.log.warn( "No Article node found in MedlineCitation" );
            }

            result.add( bibRef );

            if ( i >= 100 && i % 1000 == 0 ) {
                PubMedXMLParser.log.info( "Processed " + i + " articles" );
            }
        }
        if ( i >= 100 )
            PubMedXMLParser.log.info( "Processed " + i + " articles" );

        return result;
    }

    @Nullable
    private static Node processMedlineCitation( Node medlineCitation, BibliographicReference bibRef, ExternalDatabase exDb ) {
        Node article = null;

        NodeList recordNodes = medlineCitation.getChildNodes();
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
                    processChemicalList( item, bibRef );
                    break;
                case "MeshHeadingList":
                    processMeshHeadingList( item, bibRef );
                    break;
                case "KeywordList":
                    processKeywordList( item, bibRef );
                    break;
                case "MedlineJournalInfo":
                    processMedlineJournalInfo( item, bibRef );
                    break;
                case "PMID":
                    processPMID( item, bibRef, exDb );
                    break;
                case "CommentsCorrectionsList":
                    processCommentsCorrectionsList( item, bibRef );
                    break;
                case "DateCompleted":
                case "DateCreated":
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
                case "PersonalNameSubjectList":
                case "GeneSymbolList":
                    break;
                default:
                    log.warn( "Unrecognized node name " + name + " in MedlineCitation" );
            }
        }
        return article;
    }

    private static void processCommentsCorrectionsList( Node item, BibliographicReference bibRef ) {
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
    }

    private static void processMedlineJournalInfo( Node item, BibliographicReference bibRef ) {
        NodeList jNodes = item.getChildNodes();
        for ( int q = 0; q < jNodes.getLength(); q++ ) {
            Node jitem = jNodes.item( q );
            if ( !( jitem instanceof Element ) ) {
                continue;
            }
            switch ( jitem.getNodeName() ) {
                case "MedlineTA":
                    bibRef.setPublication( XMLUtils.getTextValue( jitem ) );
                    break;
                case "NlmUniqueID":
                case "Country":
                case "ISSNLinking":
                    break;
                default:
                    log.warn( "Unrecognized node name " + jitem.getNodeName() + " in MedlineJournalInfo" );
                    break;
            }
        }
    }

    private static void processArticle( Node article, BibliographicReference bibRef ) {
        NodeList childNodes = article.getChildNodes();
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
                    processJournal( item, bibRef );
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
                    if ( abstractParts.isEmpty() ) {
                        log.warn( "No AbstractText node found in Abstract" );
                    } else if ( abstractParts.size() > 1 ) {
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
                case "Affiliation":
                case "VernacularTitle":
                case "DataBankList":
                    break;
                default:
                    log.warn( "Unrecognized node name " + name + " in Article" );
            }
        }
    }

    private static void processKeywordList( Node keywordNode, BibliographicReference bibRef ) {
        NodeList childNodes = keywordNode.getChildNodes();
        for ( int i = 0; i < childNodes.getLength(); i++ ) {
            Node item = childNodes.item( i );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String keyword = XMLUtils.getTextValue( item );
            boolean isMajor = isMajorHeading( item );
            Keyword kw = Keyword.Factory.newInstance();
            kw.setTerm( keyword );
            kw.setIsMajorTopic( isMajor );
            bibRef.getKeywords().add( kw );
        }
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
        return "Y".equals( dmajorTopic.getValue() );
    }

    private static Collection<BibliographicReference> parseBookArticles( NodeList articles, ExternalDatabase exDb ) {
        Collection<BibliographicReference> result = new HashSet<>();
        int i = 0;
        for ( ; i < articles.getLength(); i++ ) {
            BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
            Node record = articles.item( i );

            processBookRecord( bibRef, record, exDb );

            result.add( bibRef );

            if ( i >= 100 && i % 1000 == 0 ) {
                PubMedXMLParser.log.info( "Processed " + i + " books" );
            }
        }
        if ( i >= 100 ) PubMedXMLParser.log.info( "Processed " + i + " books" );
        return result;
    }

    private static void processPMID( Node pmid, BibliographicReference bibRef, ExternalDatabase exDb ) {
        String accession = XMLUtils.getTextValue( pmid );
        DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance( accession, exDb );
        dbEntry.setAccession( accession );
        dbEntry.setExternalDatabase( exDb );
        bibRef.setPubAccession( dbEntry );
    }

    private static void processBook( Node article, BibliographicReference bibRef ) {
        NodeList childNodes = article.getChildNodes();
        for ( int j = 0; j < childNodes.getLength(); j++ ) {
            Node item = childNodes.item( j );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String name = item.getNodeName();
            switch ( name ) {
                case "Publisher":
                    PubMedXMLParser.extractPublisher( bibRef, item );
                    break;
                case "PubDate":
                    PubMedXMLParser.processContributionDate( item, bibRef );
                    break;
                case "AuthorList":
                    if ( ( ( Element ) item ).hasAttribute( "Type" ) ) {
                        if ( ( ( Element ) item ).getAttribute( "Type" ).equals( "editors" ) ) {
                            bibRef.setEditor( extractAuthorList( item.getChildNodes() ) );
                        } else {
                            bibRef.setAuthorList( extractAuthorList( item.getChildNodes() ) );
                        }
                    }
                    break;
                case "BookTitle":
                    if ( bibRef.getTitle() == null )
                        bibRef.setTitle( XMLUtils.getTextValue( item ) );
                    bibRef.setPublication( XMLUtils.getTextValue( item ) );
                    break;
                case "Isbn":
                case "CollectionTitle":
                case "BeginningDate":
                case "Medium":
                    break;
                default:
                    PubMedXMLParser.log.warn( "Unrecognized node name " + name + " in Book" );
            }
        }
    }

    public static void processContributionDate( Node item, BibliographicReference bibRef ) {
        bibRef.setPublicationDate( extractDate( item ) );
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
            } else {
                PubMedXMLParser.log.warn( "Unrecognized node name " + a.getNodeName() + " in Publisher" );
            }
        }
    }

    /**
     * Fill in information about the book: Publisher, Editor(s), Publication year
     *
     * @param bibRef bib ref
     * @param record record
     * @param exDb
     */
    private static void processBookRecord( BibliographicReference bibRef, Node record, ExternalDatabase exDb ) {
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
                    processBook( item, bibRef );
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
                    processPMID( item, bibRef, exDb );
                    break;
                case "ContributionDate":
                    /*
                     * Unusual, but happens for books that are updated with new sections. We use this instead of the
                     * publication date.
                     */
                    processContributionDate( item, bibRef );
                    break;
                case "ArticleIdList":
                case "Language":
                case "Sections":
                case "DateRevised":
                    break;
                default:
                    log.warn( "Unrecognized node name " + name + " in BookDocument" );
            }
        }
    }

    private static void processJournal( Node journal, BibliographicReference bibRef ) {
        NodeList journalNodes = journal.getChildNodes();
        for ( int j = 0; j < journalNodes.getLength(); j++ ) {
            Node item = journalNodes.item( j );
            if ( !( item instanceof Element ) ) {
                continue;
            }
            String name = item.getNodeName();
            switch ( name ) {
                case "JournalIssue":
                    processJournalIssue( item, bibRef );
                    break;
                case "Title":
                case "ISSN":
                case "ISOAbbreviation":
                    break;
                default:
                    log.warn( "Unrecognized node name " + name + " in Journal" );
                    break;
            }
        }
    }

    private static void processJournalIssue( Node journalIssue, BibliographicReference bibRef ) {
        NodeList journalIssueNodes = journalIssue.getChildNodes();
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
                    bibRef.setPublicationDate( extractDate( jitem ) );
                    break;
                default:
                    log.warn( "Unrecognized node name " + jname + " in JournalIssue" );
            }
        }
    }

    private static void processMeshHeadingList( Node meshHeadings, BibliographicReference bibRef ) {
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
                if ( item.getNodeName().equals( "DescriptorName" ) ) {
                    String d = XMLUtils.getTextValue( item );
                    boolean dmajorB = isMajorHeading( item );
                    vc.setTerm( d );
                    vc.setIsMajorTopic( dmajorB );
                } else {
                    MedicalSubjectHeading qual = MedicalSubjectHeading.Factory.newInstance();
                    String q = XMLUtils.getTextValue( item );
                    boolean qmajorB = isMajorHeading( item );
                    qual.setIsMajorTopic( qmajorB );
                    qual.setTerm( q );
                    vc.getQualifiers().add( qual );
                }
            }

            bibRef.getMeshTerms().add( vc );
        }
    }

    private static void processChemicalList( Node chemNodes, BibliographicReference bibRef ) {
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
                if ( item.getNodeName().equals( "RegistryNumber" ) ) {
                    String regString = XMLUtils.getTextValue( item );
                    c.setRegistryNumber( regString );
                } else {
                    String txt = XMLUtils.getTextValue( item );
                    c.setName( txt );
                }
            }
            PubMedXMLParser.log.debug( c.getName() );
            bibRef.getChemicals().add( c );
        }
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
                if ( !( m instanceof Element ) ) {
                    continue;
                }
                String nodeName = m.getNodeName();
                switch ( nodeName ) {
                    case "LastName":
                        al.append( XMLUtils.getTextValue( m ) );
                        al.append( ", " );
                        break;
                    case "ForeName":
                    case "CollectiveName":
                        al.append( XMLUtils.getTextValue( m ) );
                        al.append( "; " );
                        break;
                    case "Initials":
                    case "AffiliationInfo":
                    case "Identifier":
                    case "Suffix":
                        break;
                    default:
                        log.warn( "Unrecognized node name " + nodeName + " in AuthorList" );
                }
            }
        }

        if ( al.length() == 0 )
            return "(No authors listed)";
        if ( al.length() < 3 )
            return al.toString();
        return al.substring( 0, al.length() - 2 ); // trim trailing semicolon + space.
    }

    private static Date extractDate( Node dateNode ) {
        String yearText = null;
        String medLineText = null;
        String monthText = null;
        String dayText = null;

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
                case "Season":
                    break;
                default:
                    log.warn( "Unrecognized node name " + c.getNodeName() + " in " + dateNode.getNodeName() );
            }
        }

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
            log.debug( "Month is missing, using January as default." );
            monthText = "Jan"; // arbitrary...
        }

        if ( dayText == null ) {
            log.debug( "Day is missing, using 1st as default." );
            dayText = "01";
        }

        if ( yearText == null ) {
            log.warn( "Year is missing, giving up on parsing date." );
            return null;
        }

        String dateString = monthText + " " + dayText + ", " + yearText;

        try {
            return DateUtils.parseDate( dateString, PUB_MED_LOCALE, PUB_MED_DATE_FORMATS );
        } catch ( ParseException e ) {
            PubMedXMLParser.log.warn( "Could not parse date " + dateString );
            return null;
        }
    }
}
