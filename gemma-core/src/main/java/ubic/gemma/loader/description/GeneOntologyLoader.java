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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.persistence.PersisterHelper;

/**
 * A service to load OntologyEntries.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class GeneOntologyLoader {

    protected static final Log log = LogFactory.getLog( GeneOntologyLoader.class );

    private PersisterHelper persisterHelper;

    /**
     * @param is
     * @return
     */
    public Collection<OntologyEntry> load( InputStream is ) {
        GeneOntologyEntryParser parser = new GeneOntologyEntryParser();
        try {
            parser.parse( is );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return this.load( parser.getResults() );
    }

    /**
     * @param is
     * @return
     */
    public Collection<OntologyEntry> load( File file ) throws IOException {
        return this.load( FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() ) );
    }

    /**
     * @param oeCol
     * @return
     */
    public Collection<OntologyEntry> load( Collection<OntologyEntry> oeCol ) {
        int count = 0;
        for ( Object oe : oeCol ) {
            persisterHelper.persist( oe );
            if ( ++count % 1000 == 0 ) {
                log.info( "Persisted " + count + " ontology entries from GO" );
            }
        }
        return oeCol;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
