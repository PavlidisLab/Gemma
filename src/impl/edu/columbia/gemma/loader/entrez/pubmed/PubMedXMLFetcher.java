package edu.columbia.gemma.loader.entrez.pubmed;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import edu.columbia.gemma.common.description.BibliographicReference;

/**
 * Class that can retrieve pubmed records (in XML format) via HTTP. The url used is configured via a resource.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLFetcher {

    protected static final Log log = LogFactory.getLog( PubMedXMLFetcher.class );
    private String uri;

    public PubMedXMLFetcher() throws ConfigurationException {
        Configuration config = new PropertiesConfiguration( "entrez.properties" );
        String baseURL = ( String ) config.getProperty( "entrez.efetch.baseurl" );
        String db = ( String ) config.getProperty( "entrez.efetch.pubmed.db" );
        String idtag = ( String ) config.getProperty( "entrez.efetch.pubmed.idtag" );
        String retmode = ( String ) config.getProperty( "entrez.efetch.pubmed.retmode" );
        String rettype = ( String ) config.getProperty( "entrez.efetch.pubmed.rettype" );
        uri = baseURL + "&" + db + "&" + retmode + "&" + rettype + "&" + idtag;
    }

    public BibliographicReference retrieveByHTTP( int pubMedId ) throws IOException {
        URL toBeGotten = new URL( uri + pubMedId );

        PubMedXMLParser pmxp = new PubMedXMLParser();
        try {
            return pmxp.parse( toBeGotten.openStream() );
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( SAXException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( ParserConfigurationException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
