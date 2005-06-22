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
import edu.columbia.gemma.loader.loaderutils.ParserTools;
import edu.columbia.gemma.loader.loaderutils.Parser;

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
     * Parses an xml file at the specified url. This can be a zip file. TODO candidate for Parser interface
     * 
     * @param obj - obj will be an association (not a composition relationship)
     * @param url
     * @return
     * @throws IOException
     * @throws SAXException
     */
    public Map parseFromHttp( String url ) throws IOException {
        InputStream is = ParserTools.retrieveByHTTP( url );
        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        try {
            goNames = new GONames( gZipInputStream );
        } catch ( SAXException e ) {
            e.printStackTrace();
        }

        log.info( "number of ontology entries: " + goNames.getMap().size() );

        return goNames.getMap();
    }

    /**
     * @param dependencies
     * @param ontologyEntryTermsMap
     * @return Collection
     */
    public Collection createOrGetDependencies( Object[] dependencies, Map ontologyEntryTermsMap ) {
        Set goTermsKeysSet = null;
        ExternalDatabase externalDatabase = null;

        LocalFile localFile = null;

        Collection<LocalFile> flatFiles = new HashSet<LocalFile>();

        for ( Object obj : dependencies ) {
            Class c = obj.getClass();
            if ( c.getName().endsWith( "ExternalDatabaseImpl" ) )
                externalDatabase = createOrGetExternalDatabase( ( ExternalDatabase ) obj );
            else if ( c.getName().endsWith( "LocalFileImpl" ) ) {
                localFile = createOrGetLocalFile( ( LocalFile ) obj );
                flatFiles.add( localFile );
            } else {
                throw new IllegalArgumentException( "Make sure you have specified valid dependencies" );
            }
        }

        log.info( "creating Gemma objects ... " );

        goTermsKeysSet = ontologyEntryTermsMap.keySet();

        Collection<OntologyEntry> ontologyEntryCol = new HashSet<OntologyEntry>();

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
     * @param externalDatabaseName
     * @return ExternalDatabase
     */
    public ExternalDatabase createOrGetExternalDatabase( ExternalDatabase ed ) {

        if ( getExternalDatabaseEntries().size() == 0 )
            this.getExternalDatabaseDao().create( ed );
        else {
            Collection<ExternalDatabase> externalDatabases = getExternalDatabaseEntries();

            for ( ExternalDatabase externalDatabase : externalDatabases ) {
                if ( externalDatabase.getName().equalsIgnoreCase( ed.getName() ) ) {
                    log.info( "external database " + externalDatabase.getName() + " already exists" );
                    return externalDatabase;
                }
            }
            this.getExternalDatabaseDao().create( ed );
            log.info( "external database with name: " + ed.getName() + " created." );

        }
        return ed;
    }

    /**
     * @param lf
     * @return LocalFile
     */
    private LocalFile createOrGetLocalFile( LocalFile lf ) {

        if ( getLocalFileEntries().size() == 0 )
            this.getLocalFileDao().create( lf );
        else {
            Collection<LocalFile> localFiles = getLocalFileEntries();
            for ( LocalFile localFile : localFiles ) {
                if ( localFile.getLocalURI().equals( lf.getLocalURI() ) ) {
                    log.info( "local file already exists" );
                    return localFile;
                }
            }
            this.getLocalFileDao().create( lf );
            log.info( "local file created." );
        }
        return lf;

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

    /**
     * @return Returns the externalDatabaseDao.
     */
    public ExternalDatabaseDao getExternalDatabaseDao() {
        return externalDatabaseDao;
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

    public Map parse( InputStream is, Method lineParseMethod ) throws IOException {

        throw new UnsupportedOperationException();
    }

    public Map parseFile( String filename ) throws IOException {

        throw new UnsupportedOperationException();
    }

}
