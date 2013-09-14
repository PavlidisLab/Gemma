/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.pazar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.loader.pazar.model.PazarRecord;
import ubic.gemma.model.association.PazarAssociation;
import ubic.gemma.persistence.Persister;

/**
 * Load Pazar records from a text file...
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class PazarLoaderImpl implements PazarLoader {

    @Autowired
    private PazarConverter pazarConverter;

    @Autowired
    private Persister persisterHelper;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.pazar.PazarLoader#load(java.io.File)
     */
    @Override
    public int load( File file ) throws IOException {
        FileInputStream i = new FileInputStream( file );
        return this.load( i );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.pazar.PazarLoader#load(java.io.InputStream)
     */
    @Override
    public int load( InputStream is ) throws IOException {
        PazarParser p = new PazarParser();
        p.parse( is );

        Collection<PazarRecord> results = p.getResults();

        Collection<PazarAssociation> convertedResults = pazarConverter.convert( results );

        Collection<?> persisted = persisterHelper.persist( convertedResults );

        return persisted.size();
    }

}
