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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.genome.sequenceAnalysis;


/**
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultService
 */
public class BlastResultServiceImpl extends ubic.gemma.model.genome.sequenceAnalysis.BlastResultServiceBase {

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultService#create(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    @Override
    protected ubic.gemma.model.genome.sequenceAnalysis.BlastResult handleCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult ) throws java.lang.Exception {
        return ( BlastResult ) this.getBlastResultDao().create( blastResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultService#remove(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult )
            throws java.lang.Exception {
        this.getBlastResultDao().remove( blastResult );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultServiceBase#handleFindOrCreate(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    @Override
    protected ubic.gemma.model.genome.sequenceAnalysis.BlastResult handleFindOrCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlastResult resultToFindOrCreate ) throws java.lang.Exception {

        return this.getBlastResultDao().findOrCreate( resultToFindOrCreate );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultServiceBase#handleFind(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    @Override
    protected ubic.gemma.model.genome.sequenceAnalysis.BlastResult handleFind(
            ubic.gemma.model.genome.sequenceAnalysis.BlastResult resultToFind ) {

        return this.getBlastResultDao().find( resultToFind );

    }

}