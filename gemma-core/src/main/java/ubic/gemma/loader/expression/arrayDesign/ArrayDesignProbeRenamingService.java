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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Rename probes on array designs.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="arrayDesignProbeRenamingService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignProbeRenamingService {

    private static Log log = LogFactory.getLog( ArrayDesignProbeRenamingService.class.getName() );
    ArrayDesignService arrayDesignService;

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param arrayDesign
     * @param newIdFile Two columns, where first column is old id, second column is new id.
     */
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
            }
            if ( ++count % 2000 == 0 ) {
                log.info( "Renamed " + count + " composite sequences, last to be renamed was " + cs );
            }
        }

        arrayDesignService.update( arrayDesign );

    }

    /**
     * @param newIdFile
     * @return
     * @throws IOException
     */
    private Map<String, String> parseIdFile( InputStream newIdFile ) throws IOException {
        BufferedReader br = new BufferedReader( new InputStreamReader( newIdFile ) );

        String line = null;

        Map<String, String> old2new = new HashMap<String, String>();
        while ( ( line = br.readLine() ) != null ) {
            String[] fields = line.split( "\t" );
            if ( fields.length < 2 ) continue;
            String probeName = fields[0];
            String seqAcc = fields[1];

            old2new.put( probeName, seqAcc );
        }
        br.close();

        return old2new;
    }

}
