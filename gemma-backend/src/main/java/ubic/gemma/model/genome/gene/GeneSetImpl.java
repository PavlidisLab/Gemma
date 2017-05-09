/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.genome.gene;

import ubic.gemma.model.genome.Gene;

/**
 * @see ubic.gemma.model.genome.gene.GeneSet
 */
public class GeneSetImpl extends ubic.gemma.model.genome.gene.GeneSet {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 7069729200662464958L;

    /**
     * @param g
     * @param gs
     * @return null if g is not in gs. Returns the geneSetMember if g is in gs.
     */
    static public GeneSetMember containsGene( Gene g, GeneSet gs ) {

        for ( GeneSetMember gm : gs.getMembers() ) {
            if ( gm.getGene().equals( g ) ) return gm;
        }

        return null;

    }

}