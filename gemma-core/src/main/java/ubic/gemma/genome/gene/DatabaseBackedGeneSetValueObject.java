/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.genome.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ubic.gemma.model.genome.gene.GeneSet;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public class DatabaseBackedGeneSetValueObject extends GeneSetValueObject {

    /**
     * 
     */
    private static final long serialVersionUID = -1360523793656012770L;

    /**
     * sorts results by size
     * 
     * @param genesets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be included.
     * @return
     */
    public static Collection<DatabaseBackedGeneSetValueObject> convert2ValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {
        List<DatabaseBackedGeneSetValueObject> results = new ArrayList<DatabaseBackedGeneSetValueObject>();

        for ( GeneSet gs : genesets ) {
            if ( !includeOnesWithoutGenes && gs.getMembers().isEmpty() ) {
                continue;
            }
            
            results.add( new DatabaseBackedGeneSetValueObject( gs ) );
        }

        Collections.sort( results, new Comparator<GeneSetValueObject>() {
            @Override
            public int compare( GeneSetValueObject o1, GeneSetValueObject o2 ) {
                return -o1.getSize().compareTo( o2.getSize() );
            }
        } );
        return results;
    }
    /**
     * default constructor to satisfy java bean contract
     */
    public DatabaseBackedGeneSetValueObject() {
        super();
    }

    /**
     * Constructor to build value object from GeneSet
     * 
     * @param gs
     */
    public DatabaseBackedGeneSetValueObject( GeneSet gs ) {
        super(gs);
    }

}
