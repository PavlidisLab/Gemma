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
package ubic.gemma.model.expression.biomaterial;

/**
 * 
 */
public interface BioMaterialService extends ubic.gemma.model.common.AuditableService {

    /**
     * <p>
     * Copies a bioMaterial.
     * </p>
     */
    public ubic.gemma.model.expression.biomaterial.BioMaterial copy(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    public ubic.gemma.model.expression.biomaterial.BioMaterial create(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * 
     */
    public ubic.gemma.model.expression.biomaterial.BioMaterial findOrCreate(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * 
     */
    public ubic.gemma.model.expression.biomaterial.BioMaterial load( java.lang.Long id );

    /**
     * 
     */
    public java.util.Collection<BioMaterial> loadAll();

    /**
     * 
     */
    public java.util.Collection<BioMaterial> loadMultiple( java.util.Collection<Long> ids );

    /**
     * 
     */
    public void remove( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * <p>
     * Updates the given biomaterial to the database.
     * </p>
     */
    public void update( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

}
