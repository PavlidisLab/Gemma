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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * @author kelsey
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface BioMaterialService extends BaseService<BioMaterial>, BaseVoEnabledService<BioMaterial, BioMaterialValueObject> {

    /**
     * Copies a bioMaterial.
     *
     * @param bioMaterial ba to copy
     * @return the copy
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    BioMaterial copy( BioMaterial bioMaterial );

    /**
     * @see BioMaterialDao#findSubBioMaterials(BioMaterial, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioMaterial> findSubBioMaterials( BioMaterial bioMaterial, boolean direct );

    /**
     * Find the siblings of a given biomaterial.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioMaterial> findSiblings( BioMaterial bioMaterial );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    BioMaterial findOrCreate( BioMaterial bioMaterial );

    @Override
    @Secured({ "GROUP_USER" })
    BioMaterial create( BioMaterial bioMaterial );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioMaterial> load( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    BioMaterial load( Long id );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioMaterial> loadAll();

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( BioMaterial bioMaterial );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( BioMaterial bioMaterial );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> getExpressionExperiments( BioMaterial bm );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE__READ" })
    BioMaterial thaw( BioMaterial bioMaterial );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials );

    /**
     * Update the biomaterials that are described by the given valueObjects. This is used to update experimental designs
     * in particular.
     *
     * @param valueObjects VOs
     * @return the biomaterials that were modified.
     */
    @Secured({ "GROUP_USER" })
    Collection<BioMaterial> updateBioMaterials( Collection<BioMaterialValueObject> valueObjects );

    /**
     * Associate dates with bioassays and any new factors with the biomaterials. Note we can have missing values.
     *
     * @param d2fv  map of dates to factor values
     */
    @Secured({ "GROUP_ADMIN" })
    <T> void associateBatchFactor( Map<BioMaterial, T> descriptors, Map<T, FactorValue> d2fv );

    String getBioMaterialIdList( Collection<BioMaterial> bioMaterials );

    /**
     * Will persist the give vocab characteristic to the given biomaterial
     * @see ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#addCharacteristic(ExpressionExperiment, Characteristic)
     */
    void addCharacteristic( BioMaterial bm, Characteristic vc );


    /**
     * Remove the given characteristic from the given biomaterial
     * @throws IllegalArgumentException if the characteristic does not belong to the biomaterial
     */
    void removeCharacteristic( BioMaterial bm, Characteristic vc );
}
