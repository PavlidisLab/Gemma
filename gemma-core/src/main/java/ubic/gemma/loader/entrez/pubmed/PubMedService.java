/*
 * The Gemma-ONT_REV project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.loader.entrez.pubmed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Manage the loading of large numbers of pubmed entries into the database. Assumes that the XML files are locally
 * available.
 * 
 * @spring.bean id="pubMedService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @author pavlidis
 * @version $Id$
 */
public class PubMedService {

    private static Log log = LogFactory.getLog( PubMedService.class.getName() );

    private PersisterHelper persisterHelper;

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param f
     */
    public void loadFromFile( InputStream f ) {
        PubMedXMLParser p = new PubMedXMLParser();
        Collection<BibliographicReference> refs = p.parse( f );
        log.info( "Persisting " + refs.size() );
        persisterHelper.persist( refs );
    }

    /**
     * @param directory of XML files
     */
    public void loadFromDirectory( File directory ) {
        try {
            File[] files = directory.listFiles();
            for ( File file : files ) {
                log.info( "Loading: " + file );
                InputStream s = new FileInputStream( file );
                this.loadFromFile( s );
            }
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        }
    }

}
