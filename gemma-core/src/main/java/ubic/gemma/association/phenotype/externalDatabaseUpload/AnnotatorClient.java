package ubic.gemma.association.phenotype.externalDatabaseUpload;

import java.io.StringReader;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
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

            // These are the setting PP used before, DO only
            // method.addParameter("withDefaultStopWords","true"); // default is false
            // method.addParameter("ontologiesToExpand", "1009");
            // method.addParameter("ontologiesToKeepInResult", "1009");
            // method.addParameter("isVirtualOntologyId", "true"); // default is false, true is recommended
            // method.addParameter("levelMax", "10"); // expand to root, 10 is enough.
            // method.addParameter("mappingTypes", "null"); //null, Automatic, Manual
            // method.addParameter("textToAnnotate", text);
            // method.addParameter("format", "tabDelimited"); //Options are 'text', 'xml', 'tabDelimited'
            // method.addParameter("apikey", "7b14e900-1ba2-4e58-ac21-c5c4c74e7ece") // Paul's

            // PostMethod method = new PostMethod( annotatorUrl );
            Request request = Request.Post( annotatorUrl ).bodyForm(
                    Form.form().add( "textToAnnotate", ecodedTerm ).add( "ontologiesToKeepInResult", "1009,1125" )
                            .add( "withDefaultStopWords", "true" ).add( "levelMax", "0" ).add( "semanticTypes", "" )
                            .add( "mappingTypes", "" ).add( "wholeWordOnly", "true" )
                            .add( "isVirtualOntologyId", "true" ).add( "longestOnly", "false" )
                            .add( "filterNumber", "true" ).add( "isTopWordsCaseSensitive", "false" )
                            .add( "mintermSize", "3" ).add( "scored", "true" ).add( "withSynonyms", "true" )
                            .add( "ontologiesToExpand", "" )
                            // .config( "format", "xml" )
                            .add( "apikey", apiKey ).build() );

            // Execute the POST method
            String contents = request.execute().returnContent().asString();

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
