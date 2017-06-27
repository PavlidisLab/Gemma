/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.bioAssay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pavlidis
 * @author keshav
 * @author joseph
 * @see BioAssayService
 */
@Service
public class BioAssayServiceImpl extends VoEnabledService<BioAssay, BioAssayValueObject> implements BioAssayService {

    private final BioAssayDao bioAssayDao;

    private final BioMaterialDao bioMaterialDao;

    @Autowired
    public BioAssayServiceImpl( BioAssayDao bioAssayDao, BioMaterialDao bioMaterialDao ) {
        super(bioAssayDao);
        this.bioAssayDao = bioAssayDao;
        this.bioMaterialDao = bioMaterialDao;
    }

    @Override
    @Transactional
    public void addBioMaterialAssociation( final BioAssay bioAssay,
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        this.handleAddBioMaterialAssociation( bioAssay, bioMaterial );

    }

    /**
     * @see BioAssayService#findBioAssayDimensions(BioAssay)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions( final BioAssay bioAssay ) {
        return this.handleFindBioAssayDimensions( bioAssay );

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> findByAccession( String accession ) {
        return this.bioAssayDao.findByAccession( accession );
    }

    /**
     * @see BioAssayService#removeBioMaterialAssociation(BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional
    public void removeBioMaterialAssociation( final BioAssay bioAssay, final BioMaterial bioMaterial ) {
        this.handleRemoveBioMaterialAssociation( bioAssay, bioMaterial );
    }

    /**
     * @see BioAssayService#thaw(BioAssay)
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( final BioAssay bioAssay ) {
        this.bioAssayDao.thaw( bioAssay );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        return this.bioAssayDao.thaw( bioAssays );
    }

    private void handleAddBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) {
        // add bioMaterial to bioAssay
        bioAssay.setSampleUsed( bioMaterial );

        // add bioAssay to bioMaterial
        Collection<BioAssay> currentBioAssays = bioMaterial.getBioAssaysUsedIn();
        currentBioAssays.add( bioAssay );
        bioMaterial.setBioAssaysUsedIn( currentBioAssays );

        // update bioMaterial name - remove text after pipes
        // this should not be necessary going forward

        // build regular expression - match only text before the first pipe
        Pattern pattern = Pattern.compile( "^(.+)|" );
        String bmName = bioMaterial.getName();
        Matcher matcher = pattern.matcher( bmName );
        if ( matcher.find() ) {
            String shortName = matcher.group();
            bioMaterial.setName( shortName );
        }

        this.update( bioAssay );
        this.bioMaterialDao.update( bioMaterial );
    }

    private Collection<BioAssayDimension> handleFindBioAssayDimensions( BioAssay bioAssay ) {
        if ( bioAssay.getId() == null )
            throw new IllegalArgumentException( "BioAssay must be persistent" );
        return this.bioAssayDao.findBioAssayDimensions( bioAssay );
    }

    // TODO: Refactor so that it accepts ids and does security check later.
    private void handleRemoveBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) {
        BioAssay bioAssayTemp = this.bioAssayDao.load( bioAssay.getId() );
        BioMaterial biomaterialToBeRemoved = this.bioMaterialDao.load( bioMaterial.getId() );

        BioMaterial currentBioMaterials = bioAssayTemp.getSampleUsed();
        bioAssayTemp.setSampleUsed( currentBioMaterials );

        // Remove bioAssay from bioMaterial
        Collection<BioAssay> currentBioAssays = biomaterialToBeRemoved.getBioAssaysUsedIn();
        currentBioAssays.remove( bioAssayTemp );
        biomaterialToBeRemoved.setBioAssaysUsedIn( currentBioAssays );

        this.bioMaterialDao.update( biomaterialToBeRemoved );
        this.update( bioAssayTemp );

        // Check to see if the bioMaterial is now orphaned.
        // If it is, remove it; if not, update it.
        if ( currentBioAssays.size() == 0 ) {
            this.bioMaterialDao.remove( biomaterialToBeRemoved );
        }

    }

}