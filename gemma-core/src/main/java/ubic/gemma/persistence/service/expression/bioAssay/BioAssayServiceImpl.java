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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.Thaws.thawBioAssay;

/**
 * @author pavlidis
 * @author keshav
 * @author joseph
 * @see BioAssayService
 */
@Service
public class BioAssayServiceImpl extends AbstractFilteringVoEnabledService<BioAssay, BioAssayValueObject>
        implements BioAssayService {

    private final BioAssayDao bioAssayDao;

    private final BioMaterialDao bioMaterialDao;

    private final ArrayDesignDao arrayDesignDao;

    @Autowired
    public BioAssayServiceImpl( BioAssayDao bioAssayDao, BioMaterialDao bioMaterialDao, ArrayDesignDao arrayDesignDao ) {
        super( bioAssayDao );
        this.bioAssayDao = bioAssayDao;
        this.bioMaterialDao = bioMaterialDao;
        this.arrayDesignDao = arrayDesignDao;
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
    public BioAssay findByShortName( String shortName ) {
        return bioAssayDao.findByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> findByAccession( String accession ) {
        return this.bioAssayDao.findByAccession( accession );
    }

    @Autowired
    private BioMaterialService bioMaterialService;

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> findSubBioAssays( BioAssay bioAssay, boolean direct ) {
        Collection<BioMaterial> bms = bioMaterialService.findSubBioMaterials( bioAssay.getSampleUsed(), direct );
        return bms.stream().map( BioMaterial::getBioAssaysUsedIn ).flatMap( Collection::stream ).collect( Collectors.toSet() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> findSiblings( BioAssay bioAssay ) {
        Collection<BioMaterial> bms = bioMaterialService.findSiblings( bioAssay.getSampleUsed() );
        return bms.stream().map( BioMaterial::getBioAssaysUsedIn ).flatMap( Collection::stream ).collect( Collectors.toSet() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay ) {
        return bioAssayDao.getBioAssaySets( bioAssay );
    }

    /**
     * @see BioAssayService#removeBioMaterialAssociation(BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional
    public void removeBioMaterialAssociation( final BioAssay bioAssay, final BioMaterial bioMaterial ) {
        this.handleRemoveBioMaterialAssociation( bioAssay, bioMaterial );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssay thaw( BioAssay ba ) {
        ba = ensureInSession( ba );
        thawBioAssay( ba );
        return ba;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        bioAssays = ensureInSession( bioAssays );
        for ( BioAssay ba : bioAssays ) {
            thawBioAssay( ba );
        }
        return bioAssays;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap, boolean basic, boolean allFactorValues ) {
        Map<Long, ArrayDesign> arrayDesigns = new HashMap<>();
        for ( BioAssay ba : entities ) {
            arrayDesigns.put( ba.getArrayDesignUsed().getId(), ba.getArrayDesignUsed() );
            if ( ba.getOriginalPlatform() != null ) {
                arrayDesigns.put( ba.getOriginalPlatform().getId(), ba.getOriginalPlatform() );
            }
        }
        Map<ArrayDesign, ArrayDesignValueObject> ba2vo = arrayDesignDao.loadValueObjects( arrayDesigns.values() )
                .stream()
                .collect( Collectors.toMap( vo -> arrayDesigns.get( vo.getId() ), Function.identity() ) );
        return bioAssayDao.loadValueObjects( entities, ba2vo, assay2sourceAssayMap, basic, allFactorValues );
    }

    private void handleAddBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) {
        // add bioMaterial to bioAssay
        bioAssay.setSampleUsed( bioMaterial );

        // add bioAssay to bioMaterial
        Set<BioAssay> currentBioAssays = bioMaterial.getBioAssaysUsedIn();
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
        BioAssay bioAssayTemp = Objects.requireNonNull( this.bioAssayDao.load( bioAssay.getId() ),
                String.format( "No BioAssay with ID %d.", bioAssay.getId() ) );
        BioMaterial biomaterialToBeRemoved = Objects.requireNonNull( this.bioMaterialDao.load( bioMaterial.getId() ),
                String.format( "No BioMaterial with ID %d.", bioMaterial.getId() ) );

        BioMaterial currentBioMaterials = bioAssayTemp.getSampleUsed();
        bioAssayTemp.setSampleUsed( currentBioMaterials );

        // Remove bioAssay from bioMaterial
        Set<BioAssay> currentBioAssays = biomaterialToBeRemoved.getBioAssaysUsedIn();
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