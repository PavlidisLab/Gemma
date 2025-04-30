/*
 * The Gemma project
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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.persistence.persister.Persister;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Manage the loading of large numbers of pubmed entries into the database. Assumes that the XML files are locally
 * available.
 *
 * @author pavlidis
 */
@Component

public class PubMedService {

    private static final Log log = LogFactory.getLog( PubMedService.class.getName() );

    @Autowired
    private Persister persisterHelper;

    /**
     * @param directory of XML files
     */
    public void loadFromDirectory( File directory ) {

        File[] files = directory.listFiles();
        assert files != null;
        for ( File file : files ) {
            if ( !file.getAbsolutePath().contains( ".xml" ) )
                continue;

            PubMedService.log.info( "Loading: " + file );
            try (InputStream s = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() )) {
                this.loadFromFile( s );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    private void loadFromFile( InputStream f ) throws IOException {
        Collection<BibliographicReference> refs = PubMedXMLParser.parse( f );
        PubMedService.log.info( "Persisting " + refs.size() );
        persisterHelper.persist( refs );
    }

}
