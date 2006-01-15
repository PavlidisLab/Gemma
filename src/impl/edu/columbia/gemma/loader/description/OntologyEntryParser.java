package edu.columbia.gemma.loader.description;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import baseCode.bio.geneset.GONames;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.loader.loaderutils.Parser;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="ontologyEntryParser"
 */
public class OntologyEntryParser implements Parser {
    protected static final Log log = LogFactory.getLog( OntologyEntryParser.class );
    Map<String, Object> cache = new HashMap<String, Object>();

    GONames goNames;

    ExternalDatabase goDB;

    public OntologyEntryParser() {
        goDB = ExternalDatabase.Factory.newInstance();
        goDB.setName( "GO" );
        goDB.setType( DatabaseType.ONTOLOGY );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.io.InputStream)
     */
    @SuppressWarnings("unchecked")
    public void parse( InputStream is ) throws IOException {

        try {
            goNames = new GONames( is );
            Map<String, String> goidMap = goNames.getMap();
            for ( String goId : goidMap.keySet() ) {
                createNewOntologyEntry( goId );
            }

        } catch ( SAXException e ) {
            e.printStackTrace();
        }

    }

    /**
     * @param goId
     * @return
     */
    @SuppressWarnings("unchecked")
    private OntologyEntry createNewOntologyEntry( String goId ) {
        OntologyEntry newOE = OntologyEntry.Factory.newInstance();
        newOE.setAccession( goId );
        newOE.setValue( goNames.getNameForId( goId ) );
        newOE.setDescription( goNames.getDefinitionForId( goId ) );
        newOE.setExternalDatabase( goDB );

        Collection<OntologyEntry> oeChildren = new HashSet();

        Collection<String> children = goNames.getChildren( goId );
        for ( String childId : children ) {
            if ( !cache.containsKey( childId ) ) {
                cache.put( childId, createNewOntologyEntry( childId ) );

            }
            oeChildren.add( ( OntologyEntry ) cache.get( childId ) );
        }

        // Collection<OntologyEntry> oeParents = new HashSet();
        // Collection<String> parents = goNames.getParents( goId );
        // for ( String parentId : parents ) {
        // if ( !cache.containsKey( parentId ) ) {
        // cache.put( parentId, createNewOntologyEntry( parentId ) );
        //
        // }
        // oeParents.add( ( OntologyEntry ) cache.get( parentId ) );
        // }

        newOE.setAssociations( oeChildren );

        cache.put( goId, newOE );
        return newOE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.io.File)
     */
    public void parse( File f ) throws IOException {
        this.parse( new FileInputStream( f ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.lang.String)
     */
    public void parse( String filename ) throws IOException {
        this.parse( new FileInputStream( new File( filename ) ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#getResults()
     */
    public Collection<Object> getResults() {
        return cache.values();
    }

}
