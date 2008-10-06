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
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResult
 */
public class BlastResultDaoImpl extends ubic.gemma.model.genome.sequenceAnalysis.BlastResultDaoBase {

    @Override
    public ubic.gemma.model.genome.sequenceAnalysis.BlastResult findOrCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlastResult toFindOrCreate ) {
        if ( toFindOrCreate.getQuerySequence() == null )
            throw new IllegalArgumentException( "BlastResult must have a querrySequence associated with it." );

        BlastResult result = this.find( toFindOrCreate );
        if ( result != null ) return result;

        logger.debug( "Creating new BlatResult: " + toFindOrCreate.toString() );
        result = ( BlastResult ) create( toFindOrCreate );
        return result;
    }
}