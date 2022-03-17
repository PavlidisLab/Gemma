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

import gemma.gsec.SecurityService;
import org.apache.commons.lang3.StringUtils;
import org.directwebremoting.extend.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.expression.diff.LinearModelAnalyzer;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.expression.experiment.FactorValueDeletion;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.util.AnchorTagUtil;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExperimentalDesignUpdatedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Main entry point to editing and viewing experimental designs. Note: do not use parametrized collections as
 * parameters for ajax methods in this class! Type information is lost during proxy creation so DWR can't figure out
 * what type of collection the method should take. See bug 2756. Use arrays instead.
 *
 * @author keshav
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
    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    public void createDesignFromFile( Long eeid, String filePath ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThaw( eeid );

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

        try ( InputStream is = new FileInputStream( f ) ) {
            // removed dry run code, validation and object creation is done before any commits to DB
            // So if validation fails no rollback needed. However, this call is wrapped in a transaction
            // as a fail safe.
            experimentalDesignImporter.importDesign( ee, is );
            this.experimentReportService.evictFromCache( ee.getId() );

        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to import the design: " + e.getMessage() );
        }

    }

    @Override
    public void createExperimentalFactor( EntityDelegator e, ExperimentalFactorValueObject efvo ) {
        if ( e == null || e.getId() == null )
            return;
        ExperimentalDesign ed = experimentalDesignService.loadWithExperimentalFactors( e.getId() );

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.fromString( efvo.getType() ) );
        ef.setExperimentalDesign( ed );
        ef.setName( efvo.getName() );
        ef.setDescription( efvo.getDescription() );
        ef.setCategory( this.createCategoryCharacteristic( efvo.getCategory(), efvo.getCategoryUri() ) );

        /*
         * Note: this call should not be needed because of cascade behaviour.
         */
        // experimentalFactorService.create( ef );
        if ( ed.getExperimentalFactors() == null )
            ed.setExperimentalFactors( new HashSet<ExperimentalFactor>() );
        ed.getExperimentalFactors().add( ef );

        experimentalDesignService.update( ed );

        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ed );

        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "ExperimentalFactor added: " + efvo.getName(), efvo.toString() );
        this.experimentReportService.evictFromCache( ee.getId() );

    }

    @Override
    public void createFactorValue( EntityDelegator e ) {
        if ( e == null || e.getId() == null )
            return;
        ExperimentalFactor ef = experimentalFactorService.load( e.getId() );

        if ( ef == null ) {
            throw new EntityNotFoundException(
                    "Experimental factor with ID=" + e.getId() + " could not be accessed for editing" );
        }

        Set<Characteristic> chars = new HashSet<>();
        for ( FactorValue fv : ef.getFactorValues() ) {
            //noinspection LoopStatementThatDoesntLoop // No, but its an effective way of doing this
            for ( Characteristic c : fv.getCharacteristics() ) {
                chars.add( this.createTemplateCharacteristic( c ) );
                break;
            }
        }
        if ( chars.isEmpty() ) {
            if ( ef.getCategory() == null ) {
                throw new IllegalArgumentException(
                        "You cannot create new factor values on a experimental factor that is not defined by a formal Category" );
            }
            chars.add( this.createTemplateCharacteristic( ef.getCategory() ) );
        }

        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        fv.setCharacteristics( chars );

        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ef.getExperimentalDesign() );

        // this is just a placeholder factor value; use has to edit it.
        expressionExperimentService.addFactorValue( ee, fv );
    }

    @Override
    public void createFactorValueCharacteristic( EntityDelegator e, Characteristic c ) {
        if ( e == null || e.getId() == null )
            return;
        FactorValue fv = factorValueService.load( e.getId() );

        if ( fv == null ) {
            throw new EntityNotFoundException( "No such factor value with id=" + e.getId() );
        }

        if ( StringUtils.isBlank( c.getCategory() ) ) {
            throw new IllegalArgumentException( "The category cannot be blank for " + c );
        }

        if ( fv.getCharacteristics() == null ) {
            fv.setCharacteristics( new HashSet<Characteristic>() );
        }

        fv.getCharacteristics().add( c );

        factorValueService.update( fv );

        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );
        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "FactorValue characteristic added to: " + fv, c.toString() );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    @Override
    public void deleteExperimentalFactors( EntityDelegator e, Long[] efIds ) {

        if ( e == null || e.getId() == null )
            return;

        Collection<Long> efCol = new LinkedList<>();
        Collections.addAll( efCol, efIds );

        Collection<ExperimentalFactor> toDelete = experimentalFactorService.load( efCol );

        this.delete( toDelete );

    }

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
            characteristicService.remove( c );
            factorValueService.update( fv );
        }
    }

    @Override
    public void deleteFactorValues( EntityDelegator e, Long[] fvIds ) {

        if ( e == null || e.getId() == null )
            return;
        Collection<Long> fvCol = new LinkedList<>();
        Collections.addAll( fvCol, fvIds );

        for ( Long fvId : fvCol ) {
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fvId );
            this.experimentReportService.evictFromCache( ee.getId() );
        }

        factorValueDeletion.deleteFactorValues( fvCol );

    }

    @Override
    public Collection<BioMaterialValueObject> getBioMaterials( EntityDelegator e ) {
        if ( e == null || e.getId() == null )
            return null;
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLite( e.getId() );
        Collection<BioMaterialValueObject> result = new HashSet<>();
        for ( BioAssay assay : ee.getBioAssays() ) {
            BioMaterial sample = assay.getSampleUsed();
            BioMaterialValueObject bmvo = new BioMaterialValueObject( sample, assay );
            result.add( bmvo );

        }

        filterCharacteristics( result );

        return result;
    }

    /**
     * Filter the characteristicValues to those that we want to display in columns in the biomaterialvalue table.
     *
     * @param result
     */
    private void filterCharacteristics( Collection<BioMaterialValueObject> result ) {

        int c = result.size();

        // build map of categories to bmos. No category: can't use.
        Map<String, Collection<BioMaterialValueObject>> map = new HashMap<>();
        for ( BioMaterialValueObject bmo : result ) {
            for ( CharacteristicValueObject ch : bmo.getCharacteristics() ) {

                String category = ch.getCategory();
                if ( StringUtils.isBlank( category ) ) {

                    /*
                    Experimental: split on ":", use first part as the category.
                     */
                    if ( StringUtils.isNotBlank( ch.getValue() ) && ch.getValue().contains( ":" ) ) {
                        String[] split = ch.getValue().split( ":" );
                        category = StringUtils.strip( split[0] );
                    } else {
                        continue;
                    }
                }

                if ( !map.containsKey( category ) ) {
                    map.put( category, new HashSet<BioMaterialValueObject>() );
                }
                map.get( category ).add( bmo );
            }
        }

        /*
        find ones that don't have a value for each sample, or which have more values than samples, or which are constants
         */
        Collection<String> toremove = new HashSet<>();
        for ( String category : map.keySet() ) {
            // log.info( ">>>>>>>>>> " + category + ", " + map.get( category ).size() + " items" );
            if ( map.get( category ).size() != result.size() ) {
                toremove.add( category );
                continue;
            }

            // TODO add more exclusions; see also ExpresionExperimentDao.getAnnotationsByBioMaterials
            if ( category.equals( "LabelCompound" ) || category.equals( "MaterialType" ) || category.equals( "molecular entity" ) ) {
                toremove.add( category );
                continue;
            }

            Collection<String> vals = new HashSet<>();
            boolean keeper = false;
            bms:
            for ( BioMaterialValueObject bm : map.get( category ) ) {
                // log.info( "inspecting " + bm );
                // Find the characteristic that had this category
                for ( CharacteristicValueObject ch : bm.getCharacteristics() ) {
                    String mappedCategory = ch.getCategory();
                    String mappedValue = ch.getValue();

                    if ( StringUtils.isBlank( mappedCategory ) ) {
                        // redo split (will refactor later)
                        if ( StringUtils.isNotBlank( mappedValue ) && mappedValue.contains( ":" ) ) {
                            String[] split = mappedValue.split( ":" );
                            mappedCategory = StringUtils.strip( split[0] );
                        } else {
                            continue bms;
                        }
                    }

                    if ( mappedCategory.equals( category ) ) {
                        if ( !vals.contains( mappedValue ) ) {
                            if ( log.isDebugEnabled() )
                                log.debug( category + " -> " + mappedValue );
                            vals.add( mappedValue );
                        }

                        //  populate this into the biomaterial
                        //  log.info( category + " -> " + mappedValue );
                        bm.getCharacteristicValues().put( mappedCategory, mappedValue );
                    }
                }

                //                if ( vals.size() > 1 ) {
                //                    if ( log.isDebugEnabled() )
                //                        log.debug( category + " -- Keeper with " + vals.size() + " values" );
                //
                //                    keeper = true;
                //                }
            }

            if ( vals.size() < 2 ) {
                toremove.add( category );
            }
        }

        // finally, clean up the bmos.
        for ( BioMaterialValueObject bmo : result ) {
            for ( String lose : toremove ) {
                bmo.getCharacteristicValues().remove( lose );
            }
        }

    }

    @Override
    public Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator e ) {
        if ( e == null || e.getId() == null )
            return null;

        Collection<ExperimentalFactorValueObject> result = new HashSet<>();
        Long designId;
        if ( e.getClassDelegatingFor().equalsIgnoreCase( "ExpressionExperiment" ) || e.getClassDelegatingFor()
                .endsWith( ".ExpressionExperiment" ) ) {
            ExpressionExperiment ee = this.expressionExperimentService.load( e.getId() );
            designId = ee.getExperimentalDesign().getId();
        } else if ( e.getClassDelegatingFor().equalsIgnoreCase( "ExperimentalDesign" ) || e.getClassDelegatingFor()
                .equalsIgnoreCase( "ExperimentalDesign" )
                || e.getClassDelegatingFor()
                .endsWith( ".ExperimentalDesign" ) ) {
            designId = e.getId();
        } else {
            throw new RuntimeException( "Don't know how to process a " + e.getClassDelegatingFor() );
        }
        // ugly fix for bug 3746
        ExpressionExperiment ee = experimentalDesignService
                .getExpressionExperiment( this.experimentalDesignService.load( designId ) );
        ee = expressionExperimentService.thawLite( ee );
        ExperimentalDesign ed = ee.getExperimentalDesign();

        for ( ExperimentalFactor factor : ed.getExperimentalFactors() ) {
            result.add( new ExperimentalFactorValueObject( factor ) );
        }

        return result;
    }

    @Override
    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator e ) {
        // FIXME I'm not sure why this keeps getting called with empty fields.
        if ( e == null || e.getId() == null )
            return new HashSet<>();
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );
        if ( ef == null )
            return new HashSet<>();

        Collection<FactorValueValueObject> result = new HashSet<>();
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

    @Override
    public Collection<FactorValueValueObject> getFactorValuesWithCharacteristics( EntityDelegator e ) {
        Collection<FactorValueValueObject> result = new HashSet<>();
        if ( e == null || e.getId() == null ) {
            return result;
        }
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );
        if ( ef == null ) {
            return result;
        }

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

    @Override
    @RequestMapping("/showExperimentalDesign.html")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        String idstr = request.getParameter( "eeid" );
        String edStr = request.getParameter( "edid" );
        if ( StringUtils.isBlank( idstr ) && StringUtils.isBlank( edStr ) ) {
            throw new IllegalArgumentException( "Must supply 'eeid' or 'edid' parameter" );
        }

        Long designId;
        ExpressionExperiment ee;
        ExperimentalDesign experimentalDesign;
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

        // strip white spaces
        String desc = ee.getDescription();
        ee.setDescription( StringUtils.strip( desc ) );

        ModelAndView mnv = new ModelAndView( "experimentalDesign.detail" );
        mnv.addObject( "taxonId", expressionExperimentService.getTaxon( ee ).getId() );
        mnv.addObject( "hasPopulatedDesign", ee.getExperimentalDesign().getExperimentalFactors().size() > 0 );
        mnv.addObject( "experimentalDesign", ee.getExperimentalDesign() );
        mnv.addObject( "expressionExperiment", ee );
        mnv.addObject( "currentUserCanEdit", securityService.isEditable( ee ) ? "true" : "" );
        mnv.addObject( "expressionExperimentUrl", AnchorTagUtil.getExpressionExperimentUrl( ee.getId() ) );

        return mnv;
    }

    @Override
    public void updateBioMaterials( BioMaterialValueObject[] bmvos ) {

        if ( bmvos == null || bmvos.length == 0 )
            return;

        Collection<BioMaterial> biomaterials = bioMaterialService.updateBioMaterials( Arrays.asList( bmvos ) );

        if ( biomaterials.isEmpty() )
            return;

        BioMaterial bm = biomaterials.iterator().next();
        ExpressionExperiment ee = expressionExperimentService.findByBioMaterial( bm );
        if ( ee == null )
            throw new IllegalStateException( "No Experiment for biomaterial: " + bm );

        ee = expressionExperimentService.thawLite( ee );
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {

                /*
                 * Check for unused factorValues
                 */
                Collection<FactorValue> usedFactorValues = new HashSet<>();
                LinearModelAnalyzer.populateFactorValuesFromBASet( ee, ef, usedFactorValues );

                Collection<FactorValue> toDelete = new HashSet<>();
                for ( FactorValue fv : ef.getFactorValues() ) {
                    if ( !usedFactorValues.contains( fv ) ) {
                        /*
                         * remove it.
                         */
                        toDelete.add( fv );

                    }
                }

                if ( !toDelete.isEmpty() ) {
                    log.info( "Deleting " + toDelete.size() + " unused factorvalues for " + ef );
                    factorValueDeletion.deleteFactorValues( EntityUtils.getIds( toDelete ) );
                }

            }
        }
        StringBuilder details = new StringBuilder( "Updated bio materials:\n" );
        for ( BioMaterialValueObject vo : bmvos ) {
            if ( vo == null ) {
                continue;
            }
            BioMaterial ba = bioMaterialService.load( vo.getId() );
            if ( ba != null ) {
                details.append( "id: " ).append( ba.getId() ).append( " - " ).append( ba.getName() ).append( "\n" );
            }
        }
        this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class,
                "BioMaterials updated (" + bmvos.length + " items)", details.toString() );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    @Override
    public void updateExperimentalFactors( ExperimentalFactorValueObject[] efvos ) {

        if ( efvos == null || efvos.length == 0 )
            return;

        for ( ExperimentalFactorValueObject efvo : efvos ) {
            ExperimentalFactor ef = experimentalFactorService.load( efvo.getId() );
            ef.setName( efvo.getName() );
            ef.setDescription( efvo.getDescription() );

            FactorType newType = FactorType.fromString( efvo.getType() );
            if ( newType == null || !newType.equals( ef.getType() ) ) {
                // we only allow this if there are no factors
                if ( ef.getFactorValues().isEmpty() ) {
                    ef.setType( newType );
                } else {
                    throw new IllegalArgumentException(
                            "You cannot change the 'type' of a factor once it has factor values. Delete the factor values first." );
                }
            }

            Characteristic vc = ef.getCategory();

            //  can be null if this was imported from GEO etc.
            if ( vc == null ) {
                vc = Characteristic.Factory.newInstance();
            }

            // String originalCategoryUri = vc.getCategoryUri();

            vc.setCategory( efvo.getCategory() );
            vc.setCategoryUri( efvo.getCategoryUri() );
            vc.setValue( efvo.getCategory() );
            vc.setValueUri( efvo.getCategoryUri() );

            ef.setCategory( vc );

            experimentalFactorService.update( ef );
        }

        ExperimentalFactor ef = experimentalFactorService.load( efvos[0].getId() );
        ExpressionExperiment ee = expressionExperimentService.findByFactor( ef );
        if ( ee == null )
            throw new IllegalArgumentException( "No experiment for factor: " + ef );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    @Override
    public void updateFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {

        if ( fvvos == null || fvvos.length == 0 )
            return;

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
                    throw new IllegalArgumentException(
                            "Characteristic with id=" + charId + " does not belong to factorvalue with id=" + fvID );
                }

            } else {

                c = Characteristic.Factory.newInstance();

            }

            // For related code see CharacteristicUpdateTaskImpl

            // preserve original data
            if ( StringUtils.isBlank( c.getOriginalValue() ) ) {
                c.setOriginalValue( c.getValue() );
            }

            c.setCategory( fvvo.getCategory() );
            c.setValue( fvvo.getValue() );
            c.setCategoryUri( fvvo.getCategoryUri() );
            c.setValueUri( fvvo.getValueUri() );

            c.setEvidenceCode( GOEvidenceCode.IC ); // characteristic has been manually updated

            if ( c.getId() != null ) {
                characteristicService.update( c );
            } else {
                fv.getCharacteristics().add( c );
                factorValueService.update( fv ); // cascade
            }

        }

        FactorValue fv = this.factorValueService.load( fvvos[0].getId() );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );
        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "FactorValue characteristics updated", StringUtils.join( fvvos, "\n" ) );
        this.experimentReportService.evictFromCache( ee.getId() );

    }

    private Characteristic createCategoryCharacteristic( String category, String categoryUri ) {
        Characteristic c;
        if ( categoryUri != null ) {
            Characteristic vc = Characteristic.Factory.newInstance();
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
        Characteristic template = Characteristic.Factory.newInstance();
        template.setCategory( source.getCategory() );
        template.setCategoryUri( source.getCategoryUri() );
        template.setEvidenceCode( GOEvidenceCode.IEA ); // automatically added characteristic
        return template;
    }

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

}
