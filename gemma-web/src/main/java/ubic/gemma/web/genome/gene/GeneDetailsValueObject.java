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

package ubic.gemma.web.genome.gene;

import java.util.Collection;

import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Used for gene page
 * 
 * @author tvrossum
 * @version $Id$
 */
public class GeneDetailsValueObject extends ubic.gemma.model.genome.gene.GeneValueObject {

    private static final long serialVersionUID = -8145779822182567113L;
    private Collection<GeneValueObject> homologues;
    private Collection<GeneSetValueObject> geneSets;
    private Long compositeSequenceCount; // number of probes
    

    public GeneDetailsValueObject() {
        super();
    }

    public GeneDetailsValueObject( GeneValueObject otherBean ) {
        super( otherBean );
    }

    /**
     * @return the homologues
     */
    public Collection<GeneValueObject> getHomologues() {
        return homologues;
    }
    /**
     * @param homologues the homologues to set
     */
    public void setHomologues( Collection<GeneValueObject> homologues ) {
        this.homologues = homologues;
    }
    /**
     * @return the geneSets
     */
    public Collection<GeneSetValueObject> getGeneSets() {
        return geneSets;
    }
    /**
     * @param geneSets the geneSets to set
     */
    public void setGeneSets( Collection<GeneSetValueObject> geneSets ) {
        this.geneSets = geneSets;
    }
    /**
     * @return the compositeSequenceCount
     */
    public Long getCompositeSequenceCount() {
        return compositeSequenceCount;
    }
    /**
     * @param compositeSequenceCount the compositeSequenceCount to set
     */
    public void setCompositeSequenceCount( Long compositeSequenceCount ) {
        this.compositeSequenceCount = compositeSequenceCount;
    }
    
}
