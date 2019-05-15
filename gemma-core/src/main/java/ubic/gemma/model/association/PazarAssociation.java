/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;

/**
 * A TF - target association from Pazar (www.pazar.info)
 * @deprecated
 */
@SuppressWarnings("unused") // Possible external use
public abstract class PazarAssociation extends TfGeneAssociation {
    private static final long serialVersionUID = 765189108667614057L;
    final private String pazarTfId = null;
    final private String pazarTargetGeneId = null;

    public String getPazarTargetGeneId() {
        return this.pazarTargetGeneId;
    }

    public String getPazarTfId() {
        return this.pazarTfId;
    }

    public static final class Factory {

        public static PazarAssociation newInstance( Analysis sourceAnalysis, Gene secondGene, Gene firstGene,
                DatabaseEntry databaseEntry, java.lang.String pazarTfId, java.lang.String pazarTargetGeneId ) {
            final PazarAssociation entity = new PazarAssociationImpl();

            try {
                FieldUtils.writeField( entity, "sourceAnalysis", sourceAnalysis, true );
                FieldUtils.writeField( entity, "secondGene", secondGene, true );
                FieldUtils.writeField( entity, "firstGene", firstGene, true );
                FieldUtils.writeField( entity, "databaseEntry", databaseEntry, true );
                FieldUtils.writeField( entity, "pazarTfId", pazarTfId, true );
                FieldUtils.writeField( entity, "pazarTargetGeneId", pazarTargetGeneId, true );

            } catch ( IllegalAccessException e ) {
                LogFactory.getLog( PazarAssociation.class ).error( e );
            }
            return entity;

        }
    }

}