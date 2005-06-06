package edu.columbia.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import edu.columbia.gemma.loader.genome.gene.Parser;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;

public class GeneOntologyParserImpl extends BasicLineMapParser implements Parser {

    protected static final Log log = LogFactory.getLog( PubMedXMLFetcher.class );
    private String uri;
    private URL urlPattern;
    private GoMappings goMappings;

    /**
     * @throws ConfigurationException
     */
    public GeneOntologyParserImpl() throws ConfigurationException {
        Configuration config = new PropertiesConfiguration( "Gemma.properties" );
        String baseURL = ( String ) config.getProperty( "association.efetch.baseurl" );
        String db = ( String ) config.getProperty( "association.efetch.go.db" );
        String goProject = ( String ) config.getProperty( "association.efetch.goproject" );
        // String taxonType = ( String ) config.getProperty( "entrez.efetch.pubmed.rettype" );
        uri = baseURL + "/" + db + "/" + goProject + "/";
    }

    public InputStream retrieveByHTTP( String species ) throws IOException {
        String suffix = null;
        if (species.equalsIgnoreCase("human"))
            suffix = "HUMAN/gene_association.goa_human.gz";
        else if (species.equalsIgnoreCase("mouse"))
            suffix = "MOUSE/gene_association.goa_mouse.gz";
        else if (species.equalsIgnoreCase("rat"))
            suffix = "RAT/gene_association.goa_rat.gz";
        
        urlPattern = new URL( uri + suffix );

        // PubMedXMLParser pmxp = new PubMedXMLParser();
        try {
            return urlPattern.openStream();
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object parseOneLine( String line ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object getKey( Object newItem ) {
        // TODO Auto-generated method stub
        return null;
    }

    public Method findParseLineMethod( String species ) throws NoSuchMethodException {
        assert goMappings != null;
        Method[] methods = goMappings.getClass().getMethods();

        for ( int i = 0; i < methods.length; i++ ) {
            if ( methods[i].getName().toLowerCase().matches( ( "mapFrom" + species ).toLowerCase() ) ) {
                return methods[i];
            }
        }
        throw new NoSuchMethodException();
    }

    public Map parse( InputStream is, Method m ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map parseFile( String species ) throws IOException {
        log.info( "species: " + species );

        // File file = new File( filename );
        InputStream is = retrieveByHTTP( species );

        Method lineParseMethod = null;
        try {
            lineParseMethod = this.findParseLineMethod( species );
        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
            return null;
        }

        return this.parse( is, lineParseMethod );
    }

    /**
     * @return Returns the goMappings.
     */
    public GoMappings getGoMappings() {
        return goMappings;
    }

    /**
     * @param goMappings The goMappings to set.
     */
    public void setGoMappings( GoMappings goMappings ) {
        this.goMappings = goMappings;
    }

}
