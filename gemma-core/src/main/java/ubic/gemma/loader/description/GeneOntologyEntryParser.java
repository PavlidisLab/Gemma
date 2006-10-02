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
package ubic.gemma.loader.description;

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

import ubic.gemma.loader.util.parser.Parser;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.basecode.bio.geneset.GONames;

/**
 * Parses GO entries from GO XML.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="geneOntologyEntryParser"
 */
public class GeneOntologyEntryParser implements Parser {
    protected static final Log log = LogFactory.getLog( GeneOntologyEntryParser.class );
    Map<String, OntologyEntry> cache = new HashMap<String, OntologyEntry>();

    GONames goNames;

    ExternalDatabase goDB;

    public GeneOntologyEntryParser() {
        goDB = ExternalDatabase.Factory.newInstance();
        goDB.setName( "GO" );
        goDB.setType( DatabaseType.ONTOLOGY );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Parser#parse(java.io.InputStream)
     */
    @SuppressWarnings("unchecked")
    public void parse( InputStream is ) throws IOException {

        try {
            goNames = new GONames( is );
            Map<String, String> goidMap = goNames.getMap();
            int i = 0;
            for ( String goId : goidMap.keySet() ) {
                createNewOntologyEntry( goId );
                if ( log.isDebugEnabled() && i > 0 && i % 5000 == 0 ) {
                    log.debug( "Created " + i + " ontology entries from GO" );
                }
                i++;
            }

        } catch ( SAXException e ) {
            throw new RuntimeException( e );
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
        newOE.setCategory( goNames.getAspectForId( goId ) );

        Collection<OntologyEntry> oeChildren = new HashSet<OntologyEntry>();

        Collection<String> children = goNames.getChildren( goId );
        for ( String childId : children ) {
            if ( !cache.containsKey( childId ) ) {
                cache.put( childId, createNewOntologyEntry( childId ) );
            }
            oeChildren.add( cache.get( childId ) );
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
     * @see ubic.gemma.loader.loaderutils.Parser#parse(java.io.File)
     */
    public void parse( File f ) throws IOException {
        this.parse( new FileInputStream( f ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Parser#parse(java.lang.String)
     */
    public void parse( String filename ) throws IOException {
        this.parse( new FileInputStream( new File( filename ) ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Parser#getResults()
     */
    public Collection<OntologyEntry> getResults() {
        return cache.values();
    }

}
