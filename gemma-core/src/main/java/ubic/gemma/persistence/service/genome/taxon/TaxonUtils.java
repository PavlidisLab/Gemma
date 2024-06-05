/*
 * The Gemma project.
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

package ubic.gemma.persistence.service.genome.taxon;

import ubic.gemma.model.genome.Taxon;

/**
 * A utility class for taxon.
 *
 * @author klc
 */
@SuppressWarnings("SimplifiableIfStatement") // Better readability
public class TaxonUtils {

    public static boolean isHuman( Taxon tax ) {
        assert tax != null;
        if ( ( tax.getNcbiId() != null ) && ( tax.getNcbiId() == 9606 ) )
            return true;
        if ( ( tax.getScientificName() != null ) && ( tax.getScientificName().equalsIgnoreCase( "homo sapiens" ) ) )
            return true;
        return ( tax.getCommonName() != null ) && ( tax.getCommonName().equalsIgnoreCase( "human" ) );

    }

    public static boolean isMouse( Taxon tax ) {
        assert tax != null;

        if ( ( tax.getNcbiId() != null ) && ( tax.getNcbiId() == 10090 ) )
            return true;
        if ( ( tax.getScientificName() != null ) && ( tax.getScientificName().equalsIgnoreCase( "mus musculus" ) ) )
            return true;
        return ( tax.getCommonName() != null ) && ( tax.getCommonName().equalsIgnoreCase( "mouse" ) );

    }

    public static boolean isRat( Taxon tax ) {
        assert tax != null;

        if ( ( tax.getNcbiId() != null ) && ( tax.getNcbiId() == 10116 ) )
            return true;
        if ( ( tax.getScientificName() != null ) && ( tax.getScientificName()
                .equalsIgnoreCase( "Rattus norvegicus" ) ) )
            return true;
        return ( tax.getCommonName() != null ) && ( tax.getCommonName().equalsIgnoreCase( "rat" ) );

    }

}
