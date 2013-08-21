/*
 * The gemma-mda project
 * 
 * Copyright (c) 2013 University of British Columbia
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

package ubic.gemma.util;

import ubic.gemma.model.genome.Chromosome;

/**
 * A utility class for {@link Chromosome}
 * 
 * @author ptan
 * @version $Id$
 */
public class ChromosomeUtil {

    /**
     * This method is only familiar with chromosomes as named in the UCSC GoldenPath database and would have to be
     * modified if the 'rules' are broken by some other source used.
     * 
     * @param chr
     * @return true if this is a regular chromosome, not a separate assembly for e.g. a haplotype (6_cox_hap2 for
     *         example).
     */
    public static boolean isCanonical( Chromosome chr ) {
        String name = chr.getName();

        /*
         * 6_cox_hap2
         */
        if ( name.contains( "_hap" ) ) return false;

        /*
         * chr1_gl000191_random
         */
        if ( name.endsWith( "_random" ) ) return false;

        /*
         * Un_gl000249
         */
        if ( name.startsWith( "Un_" ) ) return false;

        /*
         * X|Un, 1|2|3|4,
         */
        if ( name.contains( "|" ) ) return false;

        /*
         * ??
         */
        if ( name.equals( "-" ) ) return false;

        return true;

    }

}
