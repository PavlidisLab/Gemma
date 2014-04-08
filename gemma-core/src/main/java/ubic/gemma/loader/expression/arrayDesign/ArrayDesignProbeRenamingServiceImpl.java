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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Rename probes on array designs.
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated this should probably not be used. Instead we should add new designs with the required probe naming
 *             scheme.
 */
@Deprecated
@Component
public class ArrayDesignProbeRenamingServiceImpl implements ArrayDesignProbeRenamingService {

    private static Log log = LogFactory.getLog( ArrayDesignProbeRenamingServiceImpl.class.getName() );

    @Autowired
    ArrayDesignService arrayDesignService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeRenamingService#reName(ubic.gemma.model.expression.
     * arrayDesign.ArrayDesign, java.io.InputStream)
     */
    @Override
    public void reName( ArrayDesign arrayDesign, InputStream newIdFile ) {
        Map<String, String> old2new;
        try {
            old2new = parseIdFile( newIdFile );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        log.info( old2new.size() + " potential renaming items read" );

        int count = 0;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            if ( old2new.containsKey( cs.getName() ) ) {
                String descriptionAddendum = " [Renamed by Gemma from " + cs.getName() + "]";
                if ( StringUtils.isNotBlank( cs.getDescription() ) ) {
                    cs.setDescription( cs.getDescription() + descriptionAddendum );
                } else {
                    cs.setDescription( descriptionAddendum );
                }

                cs.setName( old2new.get( cs.getName() ) );

                if ( ++count % 2000 == 0 ) {
                    log.info( "Renamed " + count + " composite sequences, last to be renamed was " + cs );
                }
            }

        }

        arrayDesignService.update( arrayDesign );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeRenamingService#setArrayDesignService(ubic.gemma.model
     * .expression.arrayDesign.ArrayDesignService)
     */
    @Override
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param newIdFile
     * @returnk
     * @throws IOException
     */
    private Map<String, String> parseIdFile( InputStream newIdFile ) throws IOException {
        try (BufferedReader br = new BufferedReader( new InputStreamReader( newIdFile ) );) {

            String line = null;

            Map<String, String> old2new = new HashMap<String, String>();
            while ( ( line = br.readLine() ) != null ) {
                String[] fields = line.split( "\t" );
                if ( fields.length < 2 ) continue;
                String originalProbeName = fields[0];
                String newProbeName = fields[1];

                if ( old2new.containsKey( newProbeName ) ) {
                    log.warn( newProbeName + " is a duplicate, will mangle to make unique" );
                    String candidateNewProbeName = newProbeName;
                    int i = 1;
                    while ( old2new.containsKey( newProbeName ) ) {
                        newProbeName = candidateNewProbeName + DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR + "Dup" + i;
                        i++;
                        // just in case...
                        if ( i > 100 ) {
                            log.warn( "Was unable to create unique probe name for " + originalProbeName );
                            continue;
                        }
                    }
                }

                old2new.put( originalProbeName, newProbeName );
            }

            return old2new;
        }
    }

}
