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

import java.util.HashMap;
import java.util.Map;

/**
 * Convert species code for a species name for SMD.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SMDSpeciesMapper {

    // private final String[] speciesCodes = {"AL" , "AT" , "BS" , "CE" , "CJ" , "CR" , "DM" , "EC" , "HP" , "HS" , "MM"
    // , "SC" , "SE" , "SP" , "SS" , "ST" , "TB" , "TG" , "VC"};

    // private final String[] speciesNames = {"AL" , "AT" , "BS" , "CE" , "CJ" , "CR" , "Drosophila melanogaster" , "EC"
    // , "HP" , "Homo sapiens" , "Mus musculus" , "SC" , "SE" , "SP" , "SS" , "ST" , "TB" , "TG" , "VC"};

    // private final String[] shortSpeciesNames = {"AL" , "AT" , "BS" , "CE" , "CJ" , "CR" , "fly" , "EC" , "HP" ,
    // "human" , "mouse" , "SC" , "SE" , "SP" , "SS" , "ST" , "TB" , "TG" , "VC"};

    // trimmed down for testing.
    private final String[] speciesCodes = { "CE", "DM", "EC", "HS", "MM", "SC" };

    private final String[] speciesNames = { "Caenorhabditis elegans", "Drosophila melanogaster", "Escherichia coli",
            "Homo sapiens", "Mus musculus", "Saccharomyces cerevisiae" };

    private final String[] shortSpeciesNames = { "worm", "fly", "ecoli", "human", "mouse", "yeast" };

    private Map<String, String> speciesMap;

    /**
     * 
     *
     */
    public SMDSpeciesMapper() {
        speciesMap = new HashMap<String, String>();
        for ( int i = 0; i < speciesCodes.length; i++ ) {
            String speciesName = speciesNames[i];
            speciesMap.put( speciesName, speciesCodes[i] );
            speciesMap.put( shortSpeciesNames[i], speciesCodes[i] );
        }
    }

    /**
     * @param speciesName
     * @return
     */
    public String getCode( String speciesName ) {
        return speciesMap.get( speciesName );
    }

    /**
     * @return
     */
    public String[] getShortSpeciesNames() {
        return shortSpeciesNames;
    }
}