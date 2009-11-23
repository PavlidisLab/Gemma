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
package ubic.gemma.model.association;

/**
 * Spring Service base class for <code>ubic.gemma.model.association.BioSequence2GeneProductService</code>, provides
 * access to all services and entities referenced by this service.
 * 
 * @see ubic.gemma.model.association.BioSequence2GeneProductService
 */
public abstract class BioSequence2GeneProductServiceBase implements
        ubic.gemma.model.association.BioSequence2GeneProductService {

    /**
     * @see ubic.gemma.model.association.BioSequence2GeneProductService#create(ubic.gemma.model.association.BioSequence2GeneProduct)
     */
    public ubic.gemma.model.association.BioSequence2GeneProduct create(
            final ubic.gemma.model.association.BioSequence2GeneProduct bioSequence2GeneProduct ) {
        try {
            return this.handleCreate( bioSequence2GeneProduct );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.BioSequence2GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.association.BioSequence2GeneProductService.create(ubic.gemma.model.association.BioSequence2GeneProduct bioSequence2GeneProduct)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.association.BioSequence2GeneProduct)}
     */
    protected abstract ubic.gemma.model.association.BioSequence2GeneProduct handleCreate(
            ubic.gemma.model.association.BioSequence2GeneProduct bioSequence2GeneProduct ) throws java.lang.Exception;

}