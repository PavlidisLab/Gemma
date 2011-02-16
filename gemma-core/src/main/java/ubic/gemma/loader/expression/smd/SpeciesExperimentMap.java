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
package ubic.gemma.loader.expression.smd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A class to determine the species for all smdexperiments (bio assays). This is necessary because there is no foolproof
 * way to determine the species for an experiment from the SMD data files themselves.
 * <hr>
 * <p>
 * 
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SpeciesExperimentMap {

    Map<String, String> speciesExperimentMap;

    /**
     * @throws IOException
     */
    public SpeciesExperimentMap() throws IOException {
        speciesExperimentMap = new HashMap<String, String>();
        SMDSpeciesMapper smds = new SMDSpeciesMapper();
        String[] species = smds.getShortSpeciesNames();

        for ( int i = 0; i < species.length; i++ ) {
            String s = species[i];
            SpeciesBioAssayList list = new SpeciesBioAssayList();
            list.retrieveByFTP( s );
            Set<String> exps = list.getBioAssays();
            for ( String name : exps ) {
                speciesExperimentMap.put( name, s );
            }
        }
    }

    /**
     * For a given experiment ID, get the species.
     * 
     * @param experimentId
     * @return
     */
    public String getSpecies( int experimentId ) {
        String key = ( new Integer( experimentId ) ).toString();

        if ( !speciesExperimentMap.containsKey( key ) ) return null;
        return speciesExperimentMap.get( key );
    }

    public static void main( String[] args ) {
        try {
            SpeciesExperimentMap foo = new SpeciesExperimentMap();
            for ( int i = 0; i < 10000; i++ ) {
                System.err.println( foo.getSpecies( i ) );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}