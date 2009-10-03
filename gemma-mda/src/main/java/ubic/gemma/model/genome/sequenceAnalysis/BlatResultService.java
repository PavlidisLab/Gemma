/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.genome.sequenceAnalysis;

/**
 * 
 */
public interface BlatResultService {

    /**
     * 
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult create(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * 
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult find(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * 
     */
    public java.util.Collection findByBioSequence( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult findOrCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * <p>
     * loads all BlatResults specified by the given ids.
     * </p>
     */
    public java.util.Collection load( java.util.Collection ids );

    /**
     * 
     */
    public void remove( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * 
     */
    public void update( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

}
