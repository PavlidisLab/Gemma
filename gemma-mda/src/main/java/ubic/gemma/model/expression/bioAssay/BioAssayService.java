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
package ubic.gemma.model.expression.bioAssay;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

/**
 * @author kelsey
 * @version $Id$
 */
public interface BioAssayService {

    /**
     * <p>
     * Associates a bioMaterial with a specified bioAssay.
     * </p>
     */
    @PreAuthorize("hasPermission(#bioAssay, 'write') or hasPermission(#bioAssay, 'administration')")
    public void addBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial );

    /**
     * 
     */
    public Integer countAll();

    @Secured( { "GROUP_USER" })
    public BioAssay create( BioAssay bioAssay );

    /**
     * <p>
     * Locate all BioAssayDimensions in which the selected BioAssay occurs
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public BioAssay findOrCreate( BioAssay bioAssay );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public BioAssay load( java.lang.Long id );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<BioAssay> loadAll();

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void remove( BioAssay bioAssay );

    /**
     * <p>
     * Removes the association between a specific bioMaterial and a bioAssay.
     * </p>
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void removeBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public void thaw( BioAssay bioAssay );

    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( BioAssay bioAssay );

    /**
     * @param accession eg GSM12345.
     * @return BioAssays that match based on the plain accession (unconstrained by ExternalDatabase).
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<BioAssay> findByAccession( String accession );

}
