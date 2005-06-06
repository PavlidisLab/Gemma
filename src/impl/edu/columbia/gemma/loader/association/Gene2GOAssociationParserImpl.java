package edu.columbia.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import edu.columbia.gemma.loader.genome.gene.Parser;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;
import edu.columbia.gemma.loader.loaderutils.LoaderTools;

public class Gene2GOAssociationParserImpl extends BasicLineMapParser implements Parser {

    private static String uri;
    protected static final Log log = LogFactory.getLog( PubMedXMLFetcher.class );
    private String goaHuman = null;
    private String goaMouse = null;
    private String goaRat = null;
    private Map goMap = null;
    private GoMappings goMappings;
    private Method methodToInvoke = null;

    /**
     * @throws ConfigurationException
     */
    public Gene2GOAssociationParserImpl() throws ConfigurationException {

        goMap = new HashMap();

        Configuration config = new PropertiesConfiguration( "Gemma.properties" );
        String baseURL = ( String ) config.getProperty( "association.efetch.baseurl" );
        String db = ( String ) config.getProperty( "association.efetch.go.db" );
        String goProject = ( String ) config.getProperty( "association.efetch.goproject" );

        goaHuman = ( String ) config.getProperty( "association.efetch.url.suffix.human" );
        goaMouse = ( String ) config.getProperty( "association.efetch.url.suffix.mouse" );
        goaRat = ( String ) config.getProperty( "association.efetch.url.suffix.rat" );

        uri = baseURL + "/" + db + "/" + goProject + "/";
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

    /**
     * @return Returns the goMappings.
     */
    public GoMappings getGoMappings() {
        return goMappings;
    }

    public Map parse( InputStream is, Method m ) throws IOException {
        methodToInvoke = m;
        parse( is );

        return goMap;
    }

    public Map parseFile( String species ) throws IOException {
        log.info( "species: " + species );

        InputStream is = readGoTermsViaHTTP( species );

        Method lineParseMethod = null;
        try {
            lineParseMethod = this.findParseLineMethod( species );
        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
            return null;
        }

        // return this.parse( is, lineParseMethod);
        this.parseMethod( is, lineParseMethod );
        return goMap;
    }

    public void parseMethod( InputStream is, Method m ) throws IOException {
        methodToInvoke = m;
        parse( is );
    }

    @Override
    public Object parseOneLine( String line ) {
        assert goMappings != null;
        assert goMap != null;
        OntologyEntry oe = null;

        try {
            Object obj = methodToInvoke.invoke( goMappings, new Object[] { line } );
            if ( obj == null ) return obj;
            oe = ( OntologyEntry ) obj;
            // goMap.put( oe.getNcbiId(), oe );
            return oe;

        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public InputStream readGoTermsViaHTTP( String name ) throws IOException {
        String suffix = null;

        if ( name.equalsIgnoreCase( "human" ) )
            suffix = goaHuman;

        else if ( name.equalsIgnoreCase( "mouse" ) )
            suffix = goaMouse;

        else if ( name.equalsIgnoreCase( "rat" ) ) suffix = goaRat;

        try {
            return LoaderTools.retrieveByHTTP( uri + suffix );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param goMappings The goMappings to set.
     */
    public void setGoMappings( GoMappings goMappings ) {
        this.goMappings = goMappings;
    }

    @Override
    protected Object getKey( Object newItem ) {
        // TODO Auto-generated method stub
        return null;
    }

}
