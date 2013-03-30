/*
 * The Gemma project
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
package ubic.gemma.web.controller.expression.experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.directwebremoting.extend.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.expression.experiment.FactorValueDeletion;
import ubic.gemma.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.AnchorTagUtil;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.EntityNotFoundException;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Main entry point to editing and viewing experimental designs. Note: do not use parameterized collections as
 * parameters for ajax methods in this class! Type information is lost during proxy creation so DWR can't figure out
 * what type of collection the method should take. See bug 2756. Use arrays instead.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/experimentalDesign")
public class ExperimentalDesignControllerImpl extends BaseController implements ExperimentalDesignController {

    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;
    @Autowired
    private ExperimentalDesignService experimentalDesignService;
    @Autowired
    private ExperimentalFactorService experimentalFactorService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentReportService experimentReportService;
    @Autowired
    private FactorValueDeletion factorValueDeletion;
    @Autowired
    private FactorValueService factorValueService;
    @Autowired
    private SecurityService securityService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#createDesignFromFile(java.lang.Long,
     * java.lang.String)
     */
    @Override
    public void createDesignFromFile( Long eeid, String filePath ) {
        ExpressionExperiment ee = expressionExperimentService.load( eeid );
        ee = expressionExperimentService.thaw( ee );

        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not access experiment with id=" + eeid );
        }

        if ( ee.getExperimentalDesign().getExperimentalFactors().size() > 0 ) {
            throw new IllegalArgumentException(
                    "Cannot import an experimental design for an experiment that already has design data populated." );
        }

        File f = new File( filePath );

        if ( !f.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from file:" + f );
        }

        try {
            // removed dry run code, validation and object creation is done before any commits to DB
            // So if validation fails no rollback needed. HWoever, this call is wrapped in a transaction
            // as a fail safe.
            InputStream is = new FileInputStream( f );
            experimentalDesignImporter.importDesign( ee, is );
            this.experimentReportService.evictFromCache( ee.getId() );
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to import the design: " + e.getMessage() );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#createExperimentalFactor(ubic.gemma
     * .web.remote.EntityDelegator, ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject)
     */
    @Override
    public void createExperimentalFactor( EntityDelegator e, ExperimentalFactorValueObject efvo ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalDesign ed = experimentalDesignService.load( e.getId() );

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.fromString( efvo.getType() ) );
        ef.setExperimentalDesign( ed );
        ef.setName( efvo.getName() );
        ef.setDescription( efvo.getDescription() );
        ef.setCategory( createCategoryCharacteristic( efvo.getCategory(), efvo.getCategoryUri() ) );

        /*
         * Note: this call should not be needed because of cascade behaviour.
         */
        // experimentalFactorService.create( ef );
        if ( ed.getExperimentalFactors() == null ) ed.setExperimentalFactors( new HashSet<ExperimentalFactor>() );
        ed.getExperimentalFactors().add( ef );

        experimentalDesignService.update( ed );

        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ed );
        this.experimentReportService.evictFromCache( ee.getId() );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#createFactorValue(ubic.gemma.web
     * .remote.EntityDelegator)
     */
    @Override
    public void createFactorValue( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalFactor ef = experimentalFactorService.load( e.getId() );

        Collection<Characteristic> chars = new HashSet<Characteristic>();
        for ( FactorValue fv : ef.getFactorValues() ) {
            for ( Characteristic c : fv.getCharacteristics() ) {
                chars.add( createTemplateCharacteristic( c ) );
                break;
            }
        }
        if ( chars.isEmpty() ) {
            if ( ef.getCategory() == null ) {
                throw new IllegalArgumentException(
                        "You cannot create new factor values on a experimental factor that is not defined by a formal Category" );
            }
            chars.add( createTemplateCharacteristic( ef.getCategory() ) );
        }

        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        fv.setCharacteristics( chars );
        factorValueService.create( fv ); // until the larger problem is fixed...

        if ( ef.getFactorValues() == null ) ef.setFactorValues( new HashSet<FactorValue>() );
        ef.getFactorValues().add( fv );

        experimentalFactorService.update( ef );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#createFactorValueCharacteristic(
     * ubic.gemma.web.remote.EntityDelegator, ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    public void createFactorValueCharacteristic( EntityDelegator e, Characteristic c ) {
        if ( e == null || e.getId() == null ) return;
        FactorValue fv = factorValueService.load( e.getId() );

        if ( fv == null ) {
            throw new EntityNotFoundException( "No such factor value with id=" + e.getId() );
        }

        if ( fv.getCharacteristics() == null ) {
            fv.setCharacteristics( new HashSet<Characteristic>() );
        }

        fv.getCharacteristics().add( c );

        factorValueService.update( fv );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#deleteExperimentalFactors(ubic.gemma
     * .web.remote.EntityDelegator, java.util.Collection)
     */
    @Override
    public void deleteExperimentalFactors( EntityDelegator e, Long[] efIds ) {

        if ( e == null || e.getId() == null ) return;

        Collection<Long> efCol = new LinkedList<Long>();
        Collections.addAll( efCol, efIds );

        Collection<ExperimentalFactor> toDelete = experimentalFactorService.load( efCol );

        delete( toDelete );

    }

    /**
     * @param toDelete
     */
    private void delete( Collection<ExperimentalFactor> toDelete ) {
        for ( ExperimentalFactor factorRemove : toDelete ) {
            experimentalFactorService.delete( factorRemove );
        }

        for ( ExperimentalFactor ef : toDelete ) {
            ExpressionExperiment ee = expressionExperimentService.findByFactor( ef );

            if ( ee != null ) {
                this.experimentReportService.evictFromCache( ee.getId() );
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#deleteFactorValueCharacteristics
     * (java.util.Collection)
     */
    @Override
    public void deleteFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {
        for ( FactorValueValueObject fvvo : fvvos ) {
            FactorValue fv = factorValueService.load( fvvo.getId() );

            if ( fv == null ) {
                log.warn( "No factorvalue with ID=" + fvvo.getId() );
                continue;
            }

            Characteristic c = characteristicService.load( fvvo.getCharId() );

            if ( c == null ) {
                log.warn( "Characteristic ID is null for FactorValueValueObject with id=" + fvvo.getId() );
                continue;
            }

            fv.getCharacteristics().remove( c );
            characteristicService.delete( c );
            factorValueService.update( fv );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#deleteFactorValues(ubic.gemma.web
     * .remote.EntityDelegator, java.util.Collection)
     */
    @Override
    public void deleteFactorValues( EntityDelegator e, Long[] fvIds ) {

        if ( e == null || e.getId() == null ) return;
        Collection<Long> fvCol = new LinkedList<Long>();
        Collections.addAll( fvCol, fvIds );

        for ( Long fvId : fvCol ) {
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fvId );
            this.experimentReportService.evictFromCache( ee.getId() );
        }

        factorValueDeletion.deleteFactorValues( fvCol );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#getBioMaterials(ubic.gemma.web.remote
     * .EntityDelegator)
     */
    @Override
    public Collection<BioMaterialValueObject> getBioMaterials( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;
        ExpressionExperiment ee = expressionExperimentService.load( e.getId() );
        ee = expressionExperimentService.thawLite( ee );
        Collection<BioMaterialValueObject> result = new HashSet<BioMaterialValueObject>();
        for ( BioAssay assay : ee.getBioAssays() ) {
            BioMaterial sample = assay.getSampleUsed();
            BioMaterialValueObject bmvo = new BioMaterialValueObject( sample, assay );
            result.add( bmvo );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#getExperimentalFactors(ubic.gemma
     * .web.remote.EntityDelegator)
     */
    @Override
    public Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;

        Collection<ExperimentalFactorValueObject> result = new HashSet<ExperimentalFactorValueObject>();
        Long designId = null;
        if ( e.getClassDelegatingFor().equalsIgnoreCase( "ExpressionExperiment" ) ) {
            ExpressionExperiment ee = this.expressionExperimentService.load( e.getId() );
            designId = ee.getExperimentalDesign().getId();
        } else if ( e.getClassDelegatingFor().equalsIgnoreCase( "ExperimentalDesign" ) ) {
            designId = e.getId();
        } else {
            throw new RuntimeException( "Don't know how to process a " + e.getClassDelegatingFor() );
        }
        ExperimentalDesign ed = this.experimentalDesignService.load( designId );

        for ( ExperimentalFactor factor : ed.getExperimentalFactors() ) {
            result.add( new ExperimentalFactorValueObject( factor ) );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#getFactorValues(ubic.gemma.web.remote
     * .EntityDelegator)
     */
    @Override
    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );

        Collection<FactorValueValueObject> result = new HashSet<FactorValueValueObject>();
        for ( FactorValue value : ef.getFactorValues() ) {
            Characteristic efCategory = value.getExperimentalFactor().getCategory();
            if ( efCategory == null ) {
                efCategory = Characteristic.Factory.newInstance();
                efCategory.setValue( value.getExperimentalFactor().getName() );
            }
            result.add( new FactorValueValueObject( value, efCategory ) );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#getFactorValuesWithCharacteristics
     * (ubic.gemma.web.remote.EntityDelegator)
     */
    @Override
    public Collection<FactorValueValueObject> getFactorValuesWithCharacteristics( EntityDelegator e ) {
        Collection<FactorValueValueObject> result = new HashSet<FactorValueValueObject>();
        if ( e == null || e.getId() == null ) return result;
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );

        for ( FactorValue value : ef.getFactorValues() ) {
            if ( value.getCharacteristics().size() > 0 ) {
                for ( Characteristic c : value.getCharacteristics() ) {
                    result.add( new FactorValueValueObject( value, c ) );
                }
            } else {
                // We just use the experimental factor's characteristic.
                Characteristic category = value.getExperimentalFactor().getCategory();
                if ( category == null ) {
                    category = Characteristic.Factory.newInstance();
                    category.setValue( value.getExperimentalFactor().getName() );
                }
                result.add( new FactorValueValueObject( value ) );
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#show(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/showExperimentalDesign.html")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        String idstr = request.getParameter( "eeid" );
        String edStr = request.getParameter( "edid" );
        if ( StringUtils.isBlank( idstr ) && StringUtils.isBlank( edStr ) ) {
            throw new IllegalArgumentException( "Must supply 'eeid' or 'edid' parameter" );
        }

        Long designId;
        ExpressionExperiment ee = null;
        ExperimentalDesign experimentalDesign = null;
        if ( StringUtils.isNotBlank( idstr ) ) {
            try {
                Long id = Long.parseLong( idstr );
                ee = expressionExperimentService.load( id );

                if ( ee == null ) {
                    throw new EntityNotFoundException( "Expression experiment with id=" + id + " cannot be accessed" );
                }

                designId = ee.getExperimentalDesign().getId();
                experimentalDesign = experimentalDesignService.load( designId );
                if ( experimentalDesign == null ) {
                    throw new EntityNotFoundException( designId + " not found" );
                }
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "eeid must be a number" );
            }
        } else {
            try {
                designId = Long.parseLong( edStr );
                experimentalDesign = experimentalDesignService.load( designId );
                if ( experimentalDesign == null ) {
                    throw new EntityNotFoundException( designId + " not found" );
                }
                ee = experimentalDesignService.getExpressionExperiment( experimentalDesign );

            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "edid must be a number" );
            }
        }

        request.setAttribute( "id", designId );

        ee = expressionExperimentService.thawLite( ee );

        ModelAndView mnv = new ModelAndView( "experimentalDesign.detail" );
        mnv.addObject( "hasPopulatedDesign", experimentalDesign.getExperimentalFactors().size() > 0 );
        mnv.addObject( "experimentalDesign", experimentalDesign );
        mnv.addObject( "expressionExperiment", ee );
        mnv.addObject( "currentUserCanEdit", securityService.isEditable( ee ) ? "true" : "" );
        mnv.addObject( "expressionExperimentUrl", AnchorTagUtil.getExpressionExperimentUrl( ee.getId() ) );

        return mnv;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#updateBioMaterials(java.util.Collection
     * )
     */
    @Override
    public void updateBioMaterials( BioMaterialValueObject[] bmvos ) {

        if ( bmvos == null || bmvos.length == 0 ) return;

        Collection<BioMaterial> biomaterials = bioMaterialService.updateBioMaterials( Arrays.asList( bmvos ) );

        if ( biomaterials.isEmpty() ) return;

        BioMaterial bm = biomaterials.iterator().next();
        ExpressionExperiment ee = expressionExperimentService.findByBioMaterial( bm );
        if ( ee == null ) throw new IllegalStateException( "No Experiment for biomaterial: " + bm );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#updateExperimentalFactors(java.util
     * .Collection)
     */
    @Override
    public void updateExperimentalFactors( ExperimentalFactorValueObject[] efvos ) {

        if ( efvos == null || efvos.length == 0 ) return;

        for ( ExperimentalFactorValueObject efvo : efvos ) {
            ExperimentalFactor ef = experimentalFactorService.load( efvo.getId() );
            ef.setName( efvo.getName() );
            ef.setDescription( efvo.getDescription() );

            FactorType newType = FactorType.fromString( efvo.getType() );
            if ( !newType.equals( ef.getType() ) ) {
                // we only allow this if there are no factors
                if ( ef.getFactorValues().isEmpty() ) {
                    ef.setType( newType );
                } else {
                    throw new IllegalArgumentException(
                            "You cannot change the 'type' of a factor once it has factor values. Delete the factor values first." );
                }
            }

            /*
             * at the moment, the characteristic is always going to be a VocabCharacteristic; if that changes, this will
             * have to...
             */
            VocabCharacteristic vc = ( VocabCharacteristic ) ef.getCategory();

            // VC can be null if this was imported from GEO etc.
            if ( vc == null ) {
                vc = VocabCharacteristic.Factory.newInstance();
            }

            // String originalCategoryUri = vc.getCategoryUri();

            vc.setCategory( efvo.getCategory() );
            vc.setCategoryUri( efvo.getCategoryUri() );
            vc.setValue( efvo.getCategory() );
            vc.setValueUri( efvo.getCategoryUri() );

            ef.setCategory( vc );

            experimentalFactorService.update( ef );

            /*
             * TODO: we might want to update the Category on the matching FactorValues (that use the original category).
             * The following code should do this, but is commented out until we evaluate the implications. See bug 1676.
             */
            // if ( !originalCategoryUri.equals( vc.getCategoryUri() ) ) {
            // for ( FactorValue fv : ef.getFactorValues() ) {
            // for ( Characteristic c : fv.getCharacteristics() ) {
            // if ( c instanceof VocabCharacteristic
            // && ( ( VocabCharacteristic ) c ).getCategoryUri().equals( originalCategoryUri ) ) {
            // c.setCategory( vc.getCategory() );
            // ( ( VocabCharacteristic ) c ).setCategoryUri( vc.getCategoryUri() );
            // characteristicService.update( c );
            // }
            // }
            // }
            // }
        }

        ExperimentalFactor ef = experimentalFactorService.load( efvos[0].getId() );
        ExpressionExperiment ee = expressionExperimentService.findByFactor( ef );
        if ( ee == null ) throw new IllegalArgumentException( "No experiment for factor: " + ef );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.expression.experiment.ExperimentalDesignController#updateFactorValueCharacteristics
     * (java.util.Collection)
     */
    @Override
    public void updateFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {

        if ( fvvos == null || fvvos.length == 0 ) return;

        /*
         * TODO have this use the same code in CharacteristicBrowserController.updateCharacteristics, probably moving
         * that code to CharacteristicService.
         */
        for ( FactorValueValueObject fvvo : fvvos ) {

            Long fvID = fvvo.getId();

            if ( fvID == null ) {
                throw new IllegalArgumentException( "Factor value id must be supplied" );
            }

            FactorValue fv = this.factorValueService.load( fvID );
            if ( fv == null ) {
                throw new IllegalArgumentException( "Could not load factorvalue with id=" + fvID );
            }

            if ( !securityService.isEditable( fv ) ) {
                /*
                 * We do this instead of the interceptor because Characteristics are not securable, and we really don't
                 * want them to be.
                 */
                throw new AccessDeniedException( "Access is denied" );
            }

            Long charId = fvvo.getCharId(); // this is optional. Maybe we're actually adding a characteristic for the
            // first time.

            Characteristic c;
            if ( charId != null ) {

                c = characteristicService.load( charId );

                if ( c == null ) {
                    /*
                     * This shouldn't happen but just in case...
                     */
                    throw new IllegalStateException( "No characteristic with id " + fvvo.getCharId() );
                }

                if ( !fv.getCharacteristics().contains( c ) ) {
                    throw new IllegalArgumentException( "Characteristic with id=" + charId
                            + " does not belong to factorvalue with id=" + fvID );
                }

            } else {

                if ( StringUtils.isNotBlank( fvvo.getValueUri() ) ) {
                    c = VocabCharacteristic.Factory.newInstance();
                } else {
                    c = Characteristic.Factory.newInstance();
                }
            }

            c.setCategory( fvvo.getCategory() );
            c.setValue( fvvo.getValue() );
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                vc.setCategoryUri( fvvo.getCategoryUri() );
                vc.setValueUri( fvvo.getValueUri() );
            }
            c.setEvidenceCode( GOEvidenceCode.IC ); // characteristic has been manually updated

            if ( c.getId() != null ) {
                characteristicService.update( c );
            } else {
                fv.getCharacteristics().add( c );
                factorValueService.update( fv );
            }

        }

        FactorValue fv = this.factorValueService.load( fvvos[0].getId() );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );
        this.experimentReportService.evictFromCache( ee.getId() );

    }

    private Characteristic createCategoryCharacteristic( String category, String categoryUri ) {
        Characteristic c;
        if ( categoryUri != null ) {
            VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
            vc.setCategoryUri( categoryUri );
            vc.setValueUri( categoryUri );
            c = vc;
        } else {
            c = Characteristic.Factory.newInstance();
        }
        c.setCategory( category );
        c.setValue( category );
        c.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        return c;
    }

    private Characteristic createTemplateCharacteristic( Characteristic source ) {
        Characteristic template = ( source instanceof VocabCharacteristic ) ? VocabCharacteristic.Factory.newInstance()
                : Characteristic.Factory.newInstance();
        template.setCategory( source.getCategory() );
        if ( source instanceof VocabCharacteristic ) {
            ( ( VocabCharacteristic ) template ).setCategoryUri( ( ( VocabCharacteristic ) source ).getCategoryUri() );
        }
        template.setEvidenceCode( GOEvidenceCode.IEA ); // automatically added characteristic
        return template;
    }

}
