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
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.expression.diff.LinearModelAnalyzer;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.expression.experiment.FactorValueDeletion;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExperimentalDesignUpdatedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.AnchorTagUtil;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.ServletContext;
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
@SuppressWarnings("unused")
public class ExperimentalDesignController extends BaseController {

    @Autowired
    private BioMaterialService bioMaterialService;
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
    @Autowired
    private ServletContext servletContext;

    public void createDesignFromFile( Long eeid, String filePath ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawOrFail( eeid, EntityNotFoundException::new, "Could not access experiment with id=" + eeid );

        if ( !ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot import an experimental design for an experiment that already has design data populated." );
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

    /**
     * Create an experimental factor.
     *
     * @param e    experimentalDesign to add the factor to
     * @param efvo non-null if we are pre-populating the factor values based on an existing set of BioMaterialCharacteristic,
     *             see <a href="https://github.com/PavlidisLab/Gemma/issues/987">#987</a>
     */
    public void createExperimentalFactor( EntityDelegator<ExperimentalDesign> e, ExperimentalFactorValueWebUIObject efvo ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalDesign ed = experimentalDesignService.loadWithExperimentalFactors( e.getId() );
        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ed );

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.valueOf( efvo.getType() ) );
        ef.setExperimentalDesign( ed );
        ef.setName( efvo.getName() );
        ef.setDescription( efvo.getDescription() );
        ef.setCategory( this.createCategoryCharacteristic( efvo.getCategory(), efvo.getCategoryUri() ) );

        /*
         * Note: this call should not be needed because of cascade behaviour when we call update.
         */
        // experimentalFactorService.create( ef );

        if ( ed.getExperimentalFactors() == null ) ed.setExperimentalFactors( new HashSet<>() );
        ed.getExperimentalFactors().add( ef );

        experimentalDesignService.update( ed );

        if ( StringUtils.isNotBlank( efvo.getBioMaterialCharacteristicCategoryToUse() ) ) {

            log.info( "Creating factor values based on existing BioMaterial Characteristics: " + efvo.getBioMaterialCharacteristicCategoryToUse() + " in " + ee.getShortName() );

            /*
             * get the biomaterials, pull the relevant characteristic keeping track of which biomaterial had which value, then create a factor value for each unique value. Then associate the factor values with the biomaterial according to the value it had for the characteristic.
             */
            Collection<BioMaterialValueObject> bmvos = getBioMaterialValueObjects( experimentalDesignService.getExpressionExperiment( ed ) );

            Map<CharacteristicValueObject, Collection<BioMaterial>> map = new HashMap<>();

            for ( BioMaterialValueObject bmo : bmvos ) {
                BioMaterial bm = bioMaterialService.load( bmo.getId() );

                // biomaterials that are missing the characteristic will just be ignored. Curator would have to fill in.
                for ( CharacteristicValueObject cvo : bmo.getCharacteristics() ) {
                    cvo.setId( null ); // we just want to compare the values, not the IDs.
                    if ( cvo.getCategory().equals( efvo.getBioMaterialCharacteristicCategoryToUse() ) ) {
                        if ( !map.containsKey( cvo ) ) {
                            map.put( cvo, new HashSet<>() );
                        }
                        map.get( cvo ).add( bm );
                    }
                }
            }

            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
                // we have to make an fv for each biomaterial, and we have to make a measurement for each biomaterial.
                // We udpate the experiment in batch to try to speed this up.
                Map<BioMaterial, FactorValue> bmToFv = new HashMap<>();
                for ( CharacteristicValueObject cvo : map.keySet() ) {
                    for ( BioMaterial bm : map.get( cvo ) ) {
                        FactorValue fv = FactorValue.Factory.newInstance();
                        fv.setExperimentalFactor( ef );

                        if ( cvo.getValue() == null || cvo.getValue().isEmpty() ) {
                            cvo.setValue( null );
                        } else {
                            try {
                                Double.parseDouble( cvo.getValue() );
                            } catch ( NumberFormatException err ) {
                                // try to handle missing data reasonably.
                                if ( cvo.getValue().equalsIgnoreCase( "NA" ) || cvo.getValue().equalsIgnoreCase( "N/A" ) ) {
                                    cvo.setValue( null );
                                } else {
                                    // clean up after ourselves.
                                    experimentalFactorService.remove( ef );
                                    throw new IllegalArgumentException( "Factor type is continuous but the value is not parseable as a number or missing data: " + cvo.getValue() );
                                }
                            }
                        }
                        Measurement m = Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, cvo.getValue(), PrimitiveType.DOUBLE );
                        fv.setMeasurement( m );
                        bmToFv.put( bm, fv );
                    }
                }
                expressionExperimentService.addFactorValues( ee, bmToFv );
            } else {
                Collection<BioMaterial> toUpdate = new HashSet<>();
                for ( CharacteristicValueObject cvo : map.keySet() ) {
                    FactorValue fv = FactorValue.Factory.newInstance();
                    fv.setExperimentalFactor( ef );

                    Statement s = Statement.Factory.newInstance();
                    if ( ef.getCategory() != null ) {
                        s.setCategory( ef.getCategory().getCategory() );
                        s.setCategoryUri( ef.getCategory().getCategoryUri() );
                    }
                    s.setSubject( cvo.getValue() );
                    s.setSubjectUri( cvo.getValueUri() ); // can be null
                    fv.getCharacteristics().add( s );
                    fv = expressionExperimentService.addFactorValue( ee, fv );

                    for ( BioMaterial bm : map.get( cvo ) ) {
                        bm.getFactorValues().add( fv );
                        toUpdate.add( bm );
                    }
                }
                bioMaterialService.update( toUpdate );
            }
        }
        this.experimentReportService.evictFromCache( ee.getId() );

    }

    public void createFactorValue( EntityDelegator<ExperimentalFactor> e ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalFactor ef = experimentalFactorService.load( e.getId() );

        if ( ef == null ) {
            throw new EntityNotFoundException( "Experimental factor with ID=" + e.getId() + " could not be accessed for editing" );
        }

        Set<Statement> chars = new HashSet<>();
        for ( FactorValue fv : ef.getFactorValues() ) {
            //noinspection LoopStatementThatDoesntLoop // No, but its an effective way of doing this
            for ( Statement c : fv.getCharacteristics() ) {
                chars.add( this.createTemplateStatement( c ) );
                break;
            }
        }
        if ( chars.isEmpty() ) {
            if ( ef.getCategory() == null ) {
                throw new IllegalArgumentException( "You cannot create new factor values on a experimental factor that is not defined by a formal Category" );
            }
            chars.add( this.createTemplateStatement( ef.getCategory() ) );
        }

        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        fv.setCharacteristics( chars );

        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ef.getExperimentalDesign() );

        // this is just a placeholder factor value; use has to edit it.
        expressionExperimentService.addFactorValue( ee, fv );
    }

    public void createFactorValueCharacteristic( EntityDelegator<FactorValue> e, CharacteristicValueObject cvo ) {
        if ( e == null || e.getId() == null ) return;
        FactorValue fv = factorValueService.load( e.getId() );

        if ( fv == null ) {
            throw new EntityNotFoundException( "No such factor value with id=" + e.getId() );
        }

        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );

        if ( ee == null ) {
            throw new EntityNotFoundException( "No such experiment with " + fv );
        }

        Statement c = factorValueService.createStatement( fv, statementFromVo( cvo ) );
        log.debug( String.format( "Created %s", c ) );

        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "FactorValue characteristic added to: " + fv, c.toString() );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    private Statement statementFromVo( CharacteristicValueObject vo ) {
        if ( StringUtils.isBlank( vo.getCategory() ) ) {
            throw new IllegalArgumentException( "The category cannot be blank for " + vo );
        }
        if ( StringUtils.isBlank( vo.getValue() ) ) {
            throw new IllegalArgumentException( "The value cannot be blank for " + vo );
        }
        Statement c = new Statement();
        c.setCategory( vo.getCategory() );
        c.setCategoryUri( StringUtils.stripToNull( vo.getCategoryUri() ) );
        c.setSubject( vo.getValue() );
        c.setSubjectUri( StringUtils.stripToNull( vo.getValueUri() ) );
        return c;
    }

    public void deleteExperimentalFactors( EntityDelegator<ExperimentalDesign> e, Long[] efIds ) {

        if ( e == null || e.getId() == null ) return;

        Collection<Long> efCol = new LinkedList<>();
        Collections.addAll( efCol, efIds );

        Collection<ExperimentalFactor> toDelete = experimentalFactorService.load( efCol );

        this.delete( toDelete );

    }

    public void deleteFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {
        FactorValue[] fvs = new FactorValue[fvvos.length];
        Statement[] statements = new Statement[fvvos.length];
        for ( int i = 0; i < fvvos.length; i++ ) {
            FactorValueValueObject fvvo = fvvos[i];
            if ( fvvo.getId() == null ) {
                throw new IllegalArgumentException( "A factor value ID must be supplied." );
            }
            if ( fvvo.getCharId() == null ) {
                throw new IllegalArgumentException( "A characteristic ID must be supplied." );
            }
            FactorValue fv = factorValueService.loadOrFail( fvvo.getId() );
            Statement c = fv.getCharacteristics().stream().filter( s -> s.getId().equals( fvvo.getCharId() ) ).findFirst().orElseThrow( () -> new EntityNotFoundException( String.format( "No statement with ID %d in FactorVlaue with ID %d", fvvo.getCharId(), fvvo.getId() ) ) );
            fvs[i] = fv;
            statements[i] = c;
        }
        for ( int i = 0; i < fvvos.length; i++ ) {
            factorValueService.removeStatement( fvs[i], statements[i] );
        }
    }

    public void deleteFactorValues( EntityDelegator<ExperimentalFactor> e, Long[] fvIds ) {

        if ( e == null || e.getId() == null ) return;
        Collection<Long> fvCol = new LinkedList<>();
        Collections.addAll( fvCol, fvIds );

        for ( Long fvId : fvCol ) {
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fvId );
            this.experimentReportService.evictFromCache( ee.getId() );
        }

        factorValueDeletion.deleteFactorValues( fvCol );

    }

    public Collection<BioMaterialValueObject> getBioMaterials( EntityDelegator<ExpressionExperiment> e ) {
        if ( e == null || e.getId() == null ) return null;
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( e.getId() );
        return getBioMaterialValueObjects( ee );
    }

    /**
     * This filters the characteristics through filterCharacteristics()
     *
     * @param ee experiment to get biomaterials for
     */
    private Collection<BioMaterialValueObject> getBioMaterialValueObjects( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
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
     * Extract just the categories from the biomaterial's characteristics.
     * @return Collection of CharacteristicValueObjects but all we care about is the category
     */
    public Collection<CharacteristicValueObject> getBioMaterialCharacteristicCategories( Long experimentalDesignID ) {
        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( experimentalDesignService.loadOrFail( experimentalDesignID ) );

        Collection<BioMaterialValueObject> bmvos = getBioMaterialValueObjects( ee );
        if ( bmvos.isEmpty() ) {
            return Collections.emptyList();
        }

        Set<CharacteristicValueObject> categories = new HashSet<>();
        for ( BioMaterialValueObject bmvo : bmvos ) {
            for ( String cvo : bmvo.getCharacteristicValues().keySet() ) {
                //  if ( StringUtils.isNotBlank( cvo.getCategory() ) ) { // this shouldn't happen; also duplicates are already filtered out
                categories.add( new CharacteristicValueObject( null, null, cvo, null ) );
                //  }
            }
        }
        return categories;
    }

    /**
     * Filter the characteristicValues to those that we want to display in columns in the biomaterialvalue table.
     *
     */
    private void filterCharacteristics( Collection<BioMaterialValueObject> result ) {
        Collection<String> toremove = new HashSet<>();

        // build map of categories to bmos. No category: can't use.
        Map<String, Collection<BioMaterialValueObject>> map = new HashMap<>();
        for ( BioMaterialValueObject bmo : result ) {
            for ( CharacteristicValueObject ch : bmo.getCharacteristics() ) {

                String category = ch.getCategory();
                String value = ch.getValue();
                if ( StringUtils.isBlank( category ) ) {
                    /*
                    Experimental: split on ":" or "=", use first part as the category. This should no longer be necessary
                     */
                    if ( StringUtils.isNotBlank( value ) && value.matches( ".+[:=].+" ) ) { // note: GEO only allows ":" now but we have "=" in the db for older entries.
                        String[] split = value.split( "[:=]", 2 );
                        category = StringUtils.strip( split[0] );
                    } else {
                        continue;
                    }
                }

                if ( !map.containsKey( category ) ) {
                    map.put( category, new HashSet<>() );
                }

                if ( map.get( category ).contains( bmo ) ) {
                    // See issue 999. We have to hide these duplicated categories entirely, as they can't be reliably "lined up" across samples.
                    toremove.add( category );
                    continue;
                }

                map.get( category ).add( bmo );
            }
        }

        /*
        find ones that don't meet criteria for display e.g are constant across all samples
         */

        for ( String category : map.keySet() ) {
            //log.info( ">>>>>>>>>> " + category + ", " + map.get( category ).size() + " items" );
            if ( map.get( category ).size() != result.size() ) {
                //  toremove.add( category ); // this isn't really worth it and hides useful information.
                continue;
            }

            // TODO add more exclusions; see also ExpresionExperimentDao.getAnnotationsByBioMaterials
            if ( category.equals( "LabelCompound" ) || category.equals( "MaterialType" ) || category.equals( "molecular entity" ) ) {
                toremove.add( category );
                continue;
            }

            Collection<String> vals = new HashSet<>();
            bms:
            for ( BioMaterialValueObject bm : map.get( category ) ) {
                // log.info( "inspecting " + bm );
                // Find the characteristic that had this category
                for ( CharacteristicValueObject ch : bm.getCharacteristics() ) {
                    String mappedCategory = ch.getCategory();
                    String mappedValue = ch.getValue();

                    if ( StringUtils.isBlank( mappedCategory ) ) { // this should no longer be needed
                        // redo split (will refactor later)
                        if ( StringUtils.isNotBlank( mappedValue ) && mappedValue.matches( ".+[:=].+" ) ) {
                            String[] split = mappedValue.split( "[:=]", 2 );
                            mappedCategory = StringUtils.strip( split[0] );
                            mappedValue = StringUtils.strip( split[1] ); // to show the trimmed up value.
                        } else {
                            continue bms;
                        }
                    }

                    if ( mappedCategory.equals( category ) ) {
                        if ( !vals.contains( mappedValue ) ) {
                            if ( log.isDebugEnabled() ) log.debug( category + " -> " + mappedValue );
                            vals.add( mappedValue );
                        }

                        //  populate this into the biomaterial
                        //  log.info( category + " -> " + mappedValue );
                        bm.getCharacteristicValues().put( mappedCategory, mappedValue );
                    }
                }
            }

            if ( vals.size() < 2 ) { // constant
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

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator<?> e ) {
        if ( e == null || e.getId() == null ) return null;

        Collection<ExperimentalFactorValueObject> result = new HashSet<>();
        Long designId;
        if ( e.holds( ExpressionExperiment.class ) ) {
            ExpressionExperiment ee = this.expressionExperimentService.loadOrFail( e.getId() );
            designId = ee.getExperimentalDesign().getId();
        } else if ( e.holds( ExperimentalDesign.class ) ) {
            designId = e.getId();
        } else {
            throw new RuntimeException( "Don't know how to process a " + e.getClassDelegatingFor() );
        }
        // ugly fix for bug 3746
        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( this.experimentalDesignService.loadOrFail( designId ) );
        ee = expressionExperimentService.thawLite( ee );
        ExperimentalDesign ed = ee.getExperimentalDesign();

        for ( ExperimentalFactor factor : ed.getExperimentalFactors() ) {
            result.add( new ExperimentalFactorValueObject( factor ) );
        }

        return result;
    }

    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator<ExperimentalFactor> e ) {
        // FIXME I'm not sure why this keeps getting called with empty fields.
        if ( e == null || e.getId() == null ) return new HashSet<>();
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );
        if ( ef == null ) return new HashSet<>();

        Collection<FactorValueValueObject> result = new HashSet<>();
        for ( FactorValue value : ef.getFactorValues() ) {
            result.add( new FactorValueValueObject( value ) );
        }
        return result;
    }

    public Collection<FactorValueValueObject> getFactorValuesWithCharacteristics
            ( EntityDelegator<ExperimentalFactor> e ) {
        Collection<FactorValueValueObject> result = new HashSet<>();
        if ( e == null || e.getId() == null ) {
            return result;
        }
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );
        if ( ef == null ) {
            return result;
        }

        for ( FactorValue value : ef.getFactorValues() ) {
            if ( !value.getCharacteristics().isEmpty() ) {
                for ( Statement c : value.getCharacteristics() ) {
                    result.add( new FactorValueValueObject( value, c ) );
                }
            } else {
                result.add( new FactorValueValueObject( value ) );
            }
        }
        return result;
    }

    @RequestMapping(value = "/showExperimentalDesign.html", params = { "edid" }, method = RequestMethod.GET)
    public ModelAndView showById( @RequestParam("edid") Long edId ) {
        ExperimentalDesign ed = experimentalDesignService.loadOrFail( edId, EntityNotFoundException::new );
        return show( experimentalDesignService.getExpressionExperiment( ed ) );
    }

    @RequestMapping(value = "/showExperimentalDesign.html", params = { "eeid" }, method = RequestMethod.GET)
    public ModelAndView showByExperimentId( @RequestParam("eeid") Long eeId ) {
        return show( expressionExperimentService.loadOrFail( eeId, EntityNotFoundException::new ) );
    }

    @RequestMapping(value = "/showExperimentalDesign.html", params = { "shortName" }, method = RequestMethod.GET)
    public ModelAndView showByExperimentShortName( @RequestParam("shortName") String shortName ) {
        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            throw new EntityNotFoundException( String.format( "No ExpressionExperiment with short name %s.", shortName ) );
        }
        return show( ee );
    }

    private ModelAndView show( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
        // strip white spaces
        String desc = ee.getDescription();
        ee.setDescription( StringUtils.strip( desc ) );
        RequestContextHolder.getRequestAttributes().setAttribute( "id", ee.getExperimentalDesign().getId(), RequestAttributes.SCOPE_REQUEST );
        return new ModelAndView( "experimentalDesign.detail" ).addObject( "taxonId", expressionExperimentService.getTaxon( ee ).getId() ).addObject( "hasPopulatedDesign", !ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ).addObject( "experimentalDesign", ee.getExperimentalDesign() ).addObject( "expressionExperiment", ee ).addObject( "currentUserCanEdit", securityService.isEditable( ee ) ? "true" : "" ).addAllObjects( getNeedsAttentionDetails( ee ) ).addObject( "expressionExperimentUrl", AnchorTagUtil.getExpressionExperimentUrl( ee, servletContext ) );
    }

    private Map<String, ?> getNeedsAttentionDetails( ExpressionExperiment ee ) {
        Map<String, Object> result = new HashMap<>();
        boolean needsAttention = ee.getExperimentalDesign().getExperimentalFactors().stream().flatMap( ef -> ef.getFactorValues().stream() ).anyMatch( FactorValue::getNeedsAttention );
        result.put( "needsAttention", needsAttention );
        try {
            ExperimentalDesign randomEd = experimentalDesignService.getRandomExperimentalDesignThatNeedsAttention( ee.getExperimentalDesign() );
            result.put( "randomExperimentalDesignThatNeedsAttention", randomEd );
            if ( randomEd != null ) {
                ExpressionExperiment randomEe = expressionExperimentService.findByDesign( randomEd );
                if ( randomEe != null ) {
                    result.put( "randomExperimentalDesignThatNeedsAttentionShortName", randomEe.getShortName() );
                } else {
                    log.warn( String.format( "%s does not belong to any experiment.", randomEd ) );
                    result.put( "randomExperimentalDesignThatNeedsAttentionShortName", "<detached>" );
                }
            }
        } catch ( AccessDeniedException ignored ) {
        }
        return result;
    }

    public void updateBioMaterials( BioMaterialValueObject[] bmvos ) {

        if ( bmvos == null || bmvos.length == 0 ) return;


        StopWatch w = new StopWatch();
        w.start();

        Collection<BioMaterial> biomaterials = bioMaterialService.updateBioMaterials( Arrays.asList( bmvos ) );

        log.info( String.format( "Updating biomaterials took %.2f seconds", ( double ) w.getTime() / 1000.0 ) );

        if ( biomaterials.isEmpty() ) return;

        BioMaterial bm = biomaterials.iterator().next();
        ExpressionExperiment ee = expressionExperimentService.findByBioMaterial( bm );
        if ( ee == null ) throw new IllegalStateException( "No Experiment for biomaterial: " + bm );

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
        this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, "BioMaterials updated (" + bmvos.length + " items)", details.toString() );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    public void updateExperimentalFactors( ExperimentalFactorValueObject[] efvos ) {

        if ( efvos == null || efvos.length == 0 ) return;

        for ( ExperimentalFactorValueObject efvo : efvos ) {
            ExperimentalFactor ef = experimentalFactorService.loadOrFail( efvo.getId() );
            ef.setName( efvo.getName() );
            ef.setDescription( efvo.getDescription() );

            FactorType newType = efvo.getType() != null ? FactorType.valueOf( efvo.getType() ) : null;
            if ( newType == null || !newType.equals( ef.getType() ) ) {
                // we only allow this if there are no factors
                if ( ef.getFactorValues().isEmpty() ) {
                    ef.setType( newType );
                } else {
                    throw new IllegalArgumentException( "You cannot change the 'type' of a factor once it has factor values. Delete the factor values first." );
                }
            }

            Characteristic vc = ef.getCategory();

            //  can be null if this was imported from GEO etc.
            if ( vc == null ) {
                vc = Characteristic.Factory.newInstance();
            }

            // String originalCategoryUri = vc.getCategoryUri();

            vc.setCategory( efvo.getCategory() );
            vc.setCategoryUri( StringUtils.stripToNull( efvo.getCategoryUri() ) );
            vc.setValue( efvo.getCategory() );
            vc.setValueUri( StringUtils.stripToNull( efvo.getCategoryUri() ) );

            ef.setCategory( vc );

            experimentalFactorService.update( ef );
        }

        ExperimentalFactor ef = experimentalFactorService.loadOrFail( efvos[0].getId() );
        ExpressionExperiment ee = expressionExperimentService.findByFactor( ef );
        if ( ee == null ) throw new EntityNotFoundException( "No experiment for factor: " + ef );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    public void updateFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {

        /*
         * TODO: support Characteristic extensions (predicate-object)
         */

        if ( fvvos == null || fvvos.length == 0 ) return;

        // validate the VOs
        FactorValue[] fvs = new FactorValue[fvvos.length];
        Statement[] statements = new Statement[fvvos.length];
        for ( int i = 0; i < fvvos.length; i++ ) {
            FactorValueValueObject fvvo = fvvos[i];
            if ( fvvo.getId() == null ) {
                throw new IllegalArgumentException( "Factor value ID must be supplied" );
            }
            if ( StringUtils.isBlank( fvvo.getCategory() ) ) {
                throw new IllegalArgumentException( "A statement must have a category" );
            }
            if ( StringUtils.isBlank( fvvo.getValue() ) ) {
                throw new IllegalArgumentException( "A statement must have a subject." );
            }
            if ( StringUtils.isBlank( fvvo.getPredicate() ) ^ StringUtils.isBlank( fvvo.getObject() ) ) {
                throw new IllegalArgumentException( "Either provide both predicate and object or neither." );
            }
            if ( StringUtils.isBlank( fvvo.getSecondPredicate() ) ^ StringUtils.isBlank( fvvo.getSecondObject() ) ) {
                throw new IllegalArgumentException( "Either provide both second predicate and second object or neither." );
            }
            FactorValue fv = null;
            // reuse a previous FV, otherwise changes may be overwritten
            for ( int j = 0; j < i; j++ ) {
                if ( fvvo.getId().equals( fvs[j].getId() ) ) {
                    fv = fvs[j];
                }
            }
            if ( fv == null ) {
                fv = this.factorValueService.loadOrFail( fvvo.getId(), EntityNotFoundException::new );
            }
            Long charId = fvvo.getCharId(); // this is optional. Maybe we're actually adding a characteristic for the
            Statement c;
            if ( charId != null ) {
                c = fv.getCharacteristics().stream()
                        .filter( s -> s.getId().equals( charId ) )
                        .findFirst()
                        .orElseThrow( () -> new EntityNotFoundException( String.format( "No characteristic with ID %d in FactorValue with ID %d", charId, fvvo.getId() ) ) );
                // updating the statement can alter its hashCode() and thus breaking the Set contract, we have to remove
                // it and add it back before saving
                fv.getCharacteristics().remove( c );
            } else {
                c = Statement.Factory.newInstance();
            }

            // For related code see CharacteristicUpdateTaskImpl

            // preserve original data
            if ( StringUtils.isBlank( c.getOriginalValue() ) ) {
                c.setOriginalValue( c.getSubject() );
            }

            c.setCategory( fvvo.getCategory() );
            c.setCategoryUri( StringUtils.stripToNull( fvvo.getCategoryUri() ) );
            c.setSubject( fvvo.getValue() );
            c.setSubjectUri( StringUtils.stripToNull( fvvo.getValueUri() ) );
            c.setEvidenceCode( GOEvidenceCode.IC ); // characteristic has been manually updated

            if ( !StringUtils.isBlank( fvvo.getObject() ) ) {
                c.setPredicate( fvvo.getPredicate() );
                c.setPredicateUri( fvvo.getPredicateUri() );
                c.setObject( fvvo.getObject() );
                c.setObjectUri( fvvo.getObjectUri() );
            } else {
                c.setPredicate( null );
                c.setPredicateUri( null );
                c.setObject( null );
                c.setObjectUri( null );
            }

            if ( !StringUtils.isBlank( fvvo.getSecondObject() ) ) {
                c.setSecondPredicate( fvvo.getSecondPredicate() );
                c.setSecondPredicateUri( fvvo.getSecondPredicateUri() );
                c.setSecondObject( fvvo.getSecondObject() );
                c.setSecondObjectUri( fvvo.getSecondObjectUri() );
            } else {
                c.setSecondPredicate( null );
                c.setSecondPredicateUri( null );
                c.setSecondObject( null );
                c.setSecondObjectUri( null );
            }

            if ( charId != null ) {
                fv.getCharacteristics().add( c );
            }

            fvs[i] = fv;
            statements[i] = c;
        }

        // now save!
        for ( int i = 0; i < fvs.length; i++ ) {
            statements[i] = factorValueService.saveStatement( fvs[i], statements[i] );
            if ( fvs[i].getNeedsAttention() ) {
                factorValueService.clearNeedsAttentionFlag( fvs[i], "The dataset does not need attention and all of its factor values were fixed." );
                log.info( "Reverted needs attention flag for " + fvs[i] );
            }
        }

        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fvs[0] );
        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "FactorValue characteristics updated", StringUtils.join( fvvos, "\n" ) );
        this.experimentReportService.evictFromCache( ee.getId() );
    }

    public void markFactorValuesAsNeedsAttention( Long[] fvvos, String note ) {
        Set<FactorValue> fvs = new HashSet<>( fvvos.length );
        for ( Long fvo : fvvos ) {
            FactorValue fv = factorValueService.loadOrFail( fvo, EntityNotFoundException::new, String.format( "No FactorValue with ID %d", fvo ) );
            if ( fv.getNeedsAttention() ) {
                if ( fvvos.length == 1 ) {
                    throw new IllegalArgumentException( String.format( "%s is already marked as needs attention.", fv ) );
                } else {
                    continue; // skip
                }
            }
            fvs.add( fv );
        }
        for ( FactorValue fv : fvs ) {
            factorValueService.markAsNeedsAttention( fv, note );
        }
        log.info( String.format( "Marked %d factor values as needs attention: %s", fvs.size(), note ) );
    }

    public void clearFactorValuesNeedsAttention( Long[] fvvos, String note ) {
        Set<FactorValue> fvs = new HashSet<>( fvvos.length );
        for ( Long fvo : fvvos ) {
            FactorValue fv = factorValueService.loadOrFail( fvo, EntityNotFoundException::new, String.format( "No FactorValue with ID %d", fvo ) );
            if ( !fv.getNeedsAttention() ) {
                if ( fvvos.length == 1 ) {
                    throw new IllegalArgumentException( String.format( "%s does not need attention.", fv ) );
                } else {
                    continue; // skip
                }
            }
            fvs.add( fv );
        }
        for ( FactorValue fv : fvs ) {
            factorValueService.clearNeedsAttentionFlag( fv, note );
        }
        log.info( String.format( "Cleared %d factor values' needs attention flags: %s", fvs.size(), note ) );
    }

    private Characteristic createCategoryCharacteristic( String category, String categoryUri ) {
        Characteristic c;
        if ( categoryUri != null ) {
            Characteristic vc = Characteristic.Factory.newInstance();
            vc.setCategoryUri( StringUtils.stripToNull( categoryUri ) );
            vc.setValueUri( StringUtils.stripToNull( categoryUri ) );
            c = vc;
        } else {
            c = Characteristic.Factory.newInstance();
        }
        c.setCategory( category );
        c.setValue( category );
        c.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        return c;
    }

    private Statement createTemplateStatement( Characteristic source ) {
        Statement template = Statement.Factory.newInstance();
        template.setCategory( source.getCategory() );
        template.setCategoryUri( source.getCategoryUri() );
        template.setEvidenceCode( GOEvidenceCode.IEA ); // automatically added characteristic
        return template;
    }

    private void delete( Collection<ExperimentalFactor> toDelete ) {
        for ( ExperimentalFactor factorRemove : toDelete ) {
            experimentalFactorService.remove( factorRemove );
        }

        for ( ExperimentalFactor ef : toDelete ) {
            ExpressionExperiment ee = expressionExperimentService.findByFactor( ef );

            if ( ee != null ) {
                this.experimentReportService.evictFromCache( ee.getId() );
            }
        }
    }

}
