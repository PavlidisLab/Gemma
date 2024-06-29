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

package ubic.gemma.model.genome.gene;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.genome.Taxon;

/**
 * @author tvrossum
 */
@CommonsLog
public class DatabaseBackedGeneSetValueObject extends GeneSetValueObject {

    private static final long serialVersionUID = -1360523793656012770L;

    public DatabaseBackedGeneSetValueObject() {
        super();
    }

    /**
     * default constructor to satisfy java bean contract
     */
    public DatabaseBackedGeneSetValueObject( GeneSet geneSet, @Nullable Taxon taxon, Long size ) {
        super( geneSet, taxon, size );
        if ( taxon == null ) {
            // NPE bug 60 - happens if we have leftover (empty) gene sets for taxa that were removed.
            log.warn( "No taxon found for gene set " + geneSet );
        }
    }
}

