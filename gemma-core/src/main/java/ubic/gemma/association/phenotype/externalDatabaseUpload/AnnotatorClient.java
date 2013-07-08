package ubic.gemma.association.phenotype.externalDatabaseUpload;

import java.io.StringReader;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class AnnotatorClient {

    public final String annotatorUrl = "http://rest.bioontology.org/obs/annotator";

    public TreeSet<AnnotatorResponse> findTerm( String term ) {

        // lets not encode it
        String ecodedTerm = removeSymbol( term );

        TreeSet<AnnotatorResponse> scoresFound = new TreeSet<AnnotatorResponse>();

        try {

            String apiKey = "68835db8-b142-4c7d-9509-3c843849ad67";

            String contents = "";

            HttpClient client = new HttpClient();
            client.getParams().setParameter( HttpMethodParams.USER_AGENT, "Annotator Client Example - Annotator" );

            PostMethod method = new PostMethod( annotatorUrl );

            method.addParameter( "textToAnnotate", ecodedTerm );
            method.addParameter( "ontologiesToKeepInResult", "1009,1125" );
            method.addParameter( "withDefaultStopWords", "true" );
            method.addParameter( "levelMax", "0" );
            method.addParameter( "semanticTypes", "" );
            method.addParameter( "mappingTypes", "" );
            method.addParameter( "wholeWordOnly", "true" );
            method.addParameter( "isVirtualOntologyId", "true" );

            // Configure the form parameters
            method.addParameter( "longestOnly", "false" );
            // lets try to change that

            method.addParameter( "filterNumber", "true" );

            method.addParameter( "isTopWordsCaseSensitive", "false" );
            method.addParameter( "mintermSize", "3" );
            method.addParameter( "scored", "true" );
            method.addParameter( "withSynonyms", "true" );
            method.addParameter( "ontologiesToExpand", "" );

            // method.addParameter( "format", "xml" );
            method.addParameter( "apikey", apiKey );

            // Execute the POST method
            int statusCode = client.executeMethod( method );

            if ( statusCode != -1 ) {
                try {
                    contents = method.getResponseBodyAsString();
                    // System.out.println( contents );

                    method.releaseConnection();

                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource( new StringReader( contents ) );
            Document document = builder.parse( is );

            NodeList nodes = document.getElementsByTagName( "annotationBean" );

            for ( int temp = 0; temp < nodes.getLength(); temp++ ) {

                Node nNode = nodes.item( temp );

                Element eElement = ( Element ) nNode;

                String localOntologyId = eElement.getElementsByTagName( "localOntologyId" ).item( 0 ).getTextContent();
                String ontologyUsed = "";

                if ( localOntologyId.equalsIgnoreCase( "50173" ) ) {
                    ontologyUsed = "HP";
                } else if ( localOntologyId.equalsIgnoreCase( "50310" ) ) {
                    ontologyUsed = "DOID";

                }

                Integer score = Integer.valueOf( eElement.getElementsByTagName( "score" ).item( 0 ).getTextContent() );
                String preferredName = eElement.getElementsByTagName( "preferredName" ).item( 0 ).getTextContent();

                String fullId = eElement.getElementsByTagName( "fullId" ).item( 0 ).getTextContent();

                AnnotatorResponse AnnotatorResponse = new AnnotatorResponse( score, preferredName, fullId, ecodedTerm,
                        ontologyUsed );

                for ( int i = 0; i < eElement.getElementsByTagName( "synonyms" ).getLength(); i++ ) {
                    AnnotatorResponse.addSynonyms( java.net.URLDecoder.decode(
                            eElement.getElementsByTagName( "synonyms" ).item( i ).getTextContent(), "UTF-8" ) );
                }

                if ( AnnotatorResponse.getSynonyms().contains( ecodedTerm ) ) {
                    AnnotatorResponse.setSynonym( true );
                }

                scoresFound.add( AnnotatorResponse );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return scoresFound;
    }

    private String removeSymbol( String txt ) {

        String newTxt = txt.replaceAll( "\\{", "" );
        newTxt = newTxt.replaceAll( "\\}", "" );
        newTxt = newTxt.replaceAll( "\\[", "" );
        newTxt = newTxt.replaceAll( "\\]", "" );
        newTxt = newTxt.replaceAll( "\\?", "" );

        return newTxt;
    }

}
