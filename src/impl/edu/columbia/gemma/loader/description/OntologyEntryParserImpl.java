package edu.columbia.gemma.loader.description;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import baseCode.bio.geneset.GONames;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.loader.genome.gene.Parser;
import edu.columbia.gemma.loader.loaderutils.LoaderTools;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class OntologyEntryParserImpl implements Parser {
    protected static final Log log = LogFactory.getLog( OntologyEntryParserImpl.class );

    private GONames goNames;

    private Set goTermsKeysSet;

    private Map goTermsMap;

    public Method findParseLineMethod( String string ) throws NoSuchMethodException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return
     * @throws IOException
     * @throws SAXException
     */
    public Collection parse() throws IOException, SAXException {
        InputStream is = LoaderTools
                .retrieveByHTTP( "http://archive.godatabase.org/latest/go_200505-termdb.rdf-xml.gz" );
        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        goNames = new GONames( gZipInputStream );

        goTermsMap = goNames.getMap();

        log.info( "number of ontology entries: " + goTermsMap.size() );

        log.info( "creating Gemma objects ... " );

        goTermsKeysSet = goTermsMap.keySet();

        Collection<OntologyEntry> ontologyEntryCol = new HashSet();

        // create Gemma domain objects
        for ( Object key : goTermsKeysSet ) {
            OntologyEntry oe = OntologyEntry.Factory.newInstance();

            oe.setAccession( ( String ) key );

            ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
            ed.setLocalInstallDbName( "GO" );
            ed.setName( "GO" );
            ed.setType( DatabaseType.ONTOLOGY );
            oe.setExternalDatabase( ed );

            oe.setDescription( ( String ) goTermsMap.get( key ) );

            ontologyEntryCol.add( oe );

        }
        return ontologyEntryCol;
    }

    public Map parse( InputStream is, Method m ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map parseFile( String filename ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
}
