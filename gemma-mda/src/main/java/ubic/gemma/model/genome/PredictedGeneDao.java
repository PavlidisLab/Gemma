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
package ubic.gemma.model.genome;

/**
 * @see ubic.gemma.model.genome.PredictedGene
 */
public interface PredictedGeneDao extends ubic.gemma.model.genome.ChromosomeFeatureDao<PredictedGene> {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes entities must be transformed into objects of type
     * {@link ubic.gemma.model.genome.gene.GeneValueObject}.
     */
    public final static int TRANSFORM_GENEVALUEOBJECT = 1;

    /**
     * Copies the fields of {@link ubic.gemma.model.genome.gene.GeneValueObject} to the specified entity.
     * 
     * @param copyIfNull If FALSE, the value object's field will not be copied to the entity if the value is NULL. If
     *        TRUE, it will be copied regardless of its value.
     */
    public void geneValueObjectToEntity( ubic.gemma.model.genome.gene.GeneValueObject sourceVO,
            ubic.gemma.model.genome.PredictedGene targetEntity, boolean copyIfNull );

    /**
     * Converts a Collection of instances of type {@link ubic.gemma.model.genome.gene.GeneValueObject} to this DAO's
     * entity.
     */
    public void geneValueObjectToEntityCollection( java.util.Collection<PredictedGene> instances );

    /**
     * Converts this DAO's entity to an object of type {@link ubic.gemma.model.genome.gene.GeneValueObject}.
     */
    public ubic.gemma.model.genome.gene.GeneValueObject toGeneValueObject( ubic.gemma.model.genome.PredictedGene entity );

    /**
     * Copies the fields of the specified entity to the target value object. This method is similar to
     * toGeneValueObject(), but it does not handle any attributes in the target value object that are "read-only" (as
     * those do not have setter methods exposed).
     */
    public void toGeneValueObject( ubic.gemma.model.genome.PredictedGene sourceEntity,
            ubic.gemma.model.genome.gene.GeneValueObject targetVO );

    /**
     * Converts this DAO's entity to a Collection of instances of type
     * {@link ubic.gemma.model.genome.gene.GeneValueObject}.
     */
    public void toGeneValueObjectCollection( java.util.Collection<PredictedGene> entities );
}
