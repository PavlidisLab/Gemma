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
package edu.columbia.gemma.genome.biosequence;

/**
 * <hr>
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.genome.biosequence.BioSequenceService
 */
public class BioSequenceServiceImpl extends edu.columbia.gemma.genome.biosequence.BioSequenceServiceBase {

    /**
     * @see edu.columbia.gemma.genome.biosequence.BioSequenceService#find(edu.columbia.gemma.genome.biosequence.BioSequence)
     */
    protected edu.columbia.gemma.genome.biosequence.BioSequence handleFind(
            edu.columbia.gemma.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception {
        return this.getBioSequenceDao().find( bioSequence );
    }

    /**
     * @see edu.columbia.gemma.genome.biosequence.BioSequenceService#remove(edu.columbia.gemma.genome.biosequence.BioSequence)
     */
    protected void handleRemove( edu.columbia.gemma.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception {
        this.getBioSequenceDao().remove( bioSequence );
    }

    /**
     * @see edu.columbia.gemma.genome.biosequence.BioSequenceService#update(edu.columbia.gemma.genome.biosequence.BioSequence)
     */
    protected void handleUpdate( edu.columbia.gemma.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception {
        this.getBioSequenceDao().update( bioSequence );
    }

    @Override
    protected BioSequence handleFindOrCreate( BioSequence bioSequence ) throws Exception {
        return this.getBioSequenceDao().findOrCreate( bioSequence );
    }

}