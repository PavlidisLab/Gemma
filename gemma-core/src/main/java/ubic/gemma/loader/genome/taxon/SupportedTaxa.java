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
package ubic.gemma.loader.genome.taxon;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;

/**
 * Maintains a list of taxa that Gemma supports; this is used to filter unwanted data/results, pull-downs etc.
 * @author pavlidis
 * @version $Id$
 * @deprecated
 */
@SuppressWarnings("unchecked")
public class SupportedTaxa {

    private static Collection<String> supportedTaxa = new HashSet<String>();

    static {
        // See project.properties for definitions.
        supportedTaxa.addAll( ConfigUtils.getList( "supported.taxon" ) );
    }

    /**
     * Test whether a taxon is supported by Gemma
     * 
     * @param taxon
     * @return true if supported, false otherwise.
     */
    public static boolean contains( Taxon taxon ) {

        return supportedTaxa.contains( taxon.getScientificName() )
                || ( taxon.getNcbiId() != null && supportedTaxa.contains( taxon.getNcbiId().toString() ) );
    }
}
