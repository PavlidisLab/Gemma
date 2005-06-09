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
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileDao;
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
 * @spring.bean id="ontologyEntryParser"
 * @spring.property name="externalDatabaseDao" ref="externalDatabaseDao"
 * @spring.property name="localFileDao" ref="localFileDao"
 */
public class OntologyEntryParserImpl implements Parser {
    protected static final Log log = LogFactory.getLog( OntologyEntryParserImpl.class );

    private ExternalDatabaseDao externalDatabaseDao;
    private GONames goNames;

    // private Map goTermsMap;

    private LocalFileDao localFileDao;

    /**
     * @param externalDatabaseName
     * @return
     */
    public ExternalDatabase createOrGetExternalDatabase( ExternalDatabase ed ) {

        if ( getExternalDatabaseEntries().size() == 0 )
            this.getExternalDatabaseDao().create( ed );
        else {
            Collection<ExternalDatabase> externalDatabases = getExternalDatabaseEntries();

            for ( ExternalDatabase externalDatabase : externalDatabases ) {
                if ( externalDatabase.getName().equalsIgnoreCase( ed.getName() ) ) {
                    log.info( "external database " + ed.getName() + " already exists" );
                    return externalDatabase;
                }

                this.getExternalDatabaseDao().create( ed );
                log.info( "external database with name: " + ed.getName() + " created." );
            }
        }
        return ed;
    }

    private LocalFile createOrGetLocalFile( LocalFile lf ) {
        if ( getLocalFileEntries().size() == 0 )
            this.getLocalFileDao().create( lf );
        else {
            Collection<LocalFile> localFiles = getLocalFileEntries();

            for ( LocalFile localFile : localFiles ) {
                if ( localFile.getLocalURI() == lf.getLocalURI() || localFile.getRemoteURI() == lf.getRemoteURI() ) {
                    log.info( "local file already exists" );
                    return localFile;
                }

                this.getLocalFileDao().create( lf );
                log.info( "local file created." );
            }
        }
        return lf;

    }

    public Method findParseLineMethod( String string ) throws NoSuchMethodException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return Returns the externalDatabaseDao.
     */
    public ExternalDatabaseDao getExternalDatabaseDao() {
        return externalDatabaseDao;
    }

    /**
     * @return Collection
     */
    public Collection getExternalDatabaseEntries() {

        return this.getExternalDatabaseDao().findAllExternalDb();
    }

    private Collection getLocalFileEntries() {
        return this.getLocalFileDao().findAllLocalFiles();
    }

    /**
     * @return Returns the localFileDao.
     */
    public LocalFileDao getLocalFileDao() {
        return localFileDao;
    }

    public Map parse( InputStream is, Method m ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map parseFile( String filename ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Parses an xml file at the specified url. This can be a zip file.
     * 
     * @param obj - obj will be an association (not a composition relationship)
     * @param url
     * @return
     * @throws IOException
     * @throws SAXException
     */
    public Collection parseXmlOntologyEntriesFromHttp( Object[] dependencies, String url ) throws IOException,
            SAXException {
        Map goTermsMap = null;
        InputStream is = LoaderTools.retrieveByHTTP( url );
        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        goNames = new GONames( gZipInputStream );

        goTermsMap = goNames.getMap();

        log.info( "number of ontology entries: " + goTermsMap.size() );

        return createGemmaObjects( dependencies, goTermsMap );
    }

    public Collection createGemmaObjects( Object[] dependencies, Map ontologyEntryTermsMap ) {
        Set goTermsKeysSet = null;
        ExternalDatabase externalDatabase = null;

        LocalFile localFile = null;

        Collection<LocalFile> flatFiles = new HashSet();
        for ( int i = 0; i < dependencies.length; i++ ) {
            Class c = dependencies[i].getClass();
            if ( c.getName().endsWith( "ExternalDatabaseImpl" ) )
                externalDatabase = createOrGetExternalDatabase( ( ExternalDatabase ) dependencies[i] );
            else if ( c.getName().endsWith( "LocalFileImpl" ) ) {
                localFile = createOrGetLocalFile( ( LocalFile ) dependencies[i] );
                flatFiles.add( localFile );
            } else {
                throw new IllegalArgumentException( "Make sure you have specified valid dependencies" );
            }
        }

        log.info( "creating Gemma objects ... " );

        goTermsKeysSet = ontologyEntryTermsMap.keySet();

        Collection<OntologyEntry> ontologyEntryCol = new HashSet();

        // create Gemma domain objects
        for ( Object key : goTermsKeysSet ) {
            OntologyEntry oe = OntologyEntry.Factory.newInstance();

            oe.setCategory( goNames.getAspectForId( ( String ) ontologyEntryTermsMap.get( key ) ) );
            oe.setValue( goNames.getNameForId( ( String ) ontologyEntryTermsMap.get( key ) ) );
            oe.setDescription( ( String ) ontologyEntryTermsMap.get( key ) );

            oe.setAccession( ( String ) key );

            externalDatabase.setFlatFiles( flatFiles );
            oe.setExternalDatabase( externalDatabase );

            ontologyEntryCol.add( oe );

        }
        return ontologyEntryCol;

    }

    /**
     * @param externalDatabaseDao The externalDatabaseDao to set.
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * @param localFileDao The localFileDao to set.
     */
    public void setLocalFileDao( LocalFileDao localFileDao ) {
        this.localFileDao = localFileDao;
    }

}
