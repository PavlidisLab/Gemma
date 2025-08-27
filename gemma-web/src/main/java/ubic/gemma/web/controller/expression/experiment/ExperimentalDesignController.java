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
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
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
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.web.controller.util.EntityDelegator;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.controller.util.MessageUtil;
import ubic.gemma.web.controller.util.upload.FileUploadUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main entry point to editing and viewing experimental designs. Note: do not use parametrized collections as
 * parameters for ajax methods in this class! Type information is lost during proxy creation so DWR can't figure out
 * what type of collection the method should take. See bug 2756. Use arrays instead.
 *
 * @author keshav
 */
@Controller
@RequestMapping("/experimentalDesign")
public class ExperimentalDesignController {

    protected final Log log = LogFactory.getLog( getClass().getName() );
    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageUtil messageUtil;
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
    private TableMaintenanceUtil tableMaintenanceUtil;
    @Autowired
    private TaskExecutor taskExecutor;

    @Value("${gemma.download.path}/userUploads")
    private Path uploadDir;

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void createDesignFromFile( Long eeid, String filename ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawOrFail( eeid, EntityNotFoundException::new );

        if ( ee.getExperimentalDesign() != null && !ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot import an experimental design for an experiment that already has design data populated." );
        }

        Path f = FileUploadUtil.getUploadedFile( filename, uploadDir );
        if ( !Files.isReadable( f ) ) {
            throw new IllegalArgumentException( "Cannot read from file:" + f );
        }

        try ( InputStream is = Files.newInputStream( f ) ) {
            // removed dry run code, validation and object creation is done before any commits to DB
            // So if validation fails no rollback needed. However, this call is wrapped in a transaction
            // as a fail safe.
            experimentalDesignImporter.importDesign( ee, is );
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to import the design: " + e.getMessage() );
        }

        evictFromCache( ee );
    }

    /**
     * Create an experimental factor.
     * <p>
     * AJAX
     *
     * @param e    experimentalDesign to add the factor to
     * @param efvo non-null if we are pre-populating the factor values based on an existing set of BioMaterialCharacteristic,
     *             see <a href="https://github.com/PavlidisLab/Gemma/issues/987">#987</a>
     */
    public void createExperimentalFactor( EntityDelegator<ExperimentalDesign> e, ExperimentalFactorValueWebUIObject efvo ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalDesign ed = experimentalDesignService.loadWithExperimentalFactors( e.getId() );
        if ( ed == null ) {
            throw new EntityNotFoundException( "No ExperimentalDesign with ID " + e.getId() + "." );
        }
        ExpressionExperiment ee = expressionExperimentService.findByDesign( ed );

        if ( ee == null ) {
            throw new EntityNotFoundException( "No experiment for design with ID " + e.getId() );
        }

        ee = expressionExperimentService.thawLite( ee );

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
            Collection<BioMaterialValueObject> bmvos = getBioMaterialValueObjects( ee );

            Map<CharacteristicValueObject, Collection<BioMaterial>> map = new HashMap<>();

            for ( BioMaterialValueObject bmo : bmvos ) {
                BioMaterial bm = bioMaterialService.loadOrFail( bmo.getId(), EntityNotFoundException::new );

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
                    s.setSubjectUri( StringUtils.stripToNull( cvo.getValueUri() ) ); // can be null
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

        evictFromCache( ee );
    }

    /**
     * AJAX
     */
    public void createFactorValue( EntityDelegator<ExperimentalFactor> e ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalFactor ef = experimentalFactorService.loadOrFail( e.getId(), EntityNotFoundException::new,
                "Experimental factor with ID=" + e.getId() + " could not be accessed for editing" );
        ExpressionExperiment ee = expressionExperimentService.findByDesign( ef.getExperimentalDesign() );
        if ( ee == null ) {
            throw new EntityNotFoundException( "No experiment for factor: " + ef );
        }

        Set<Statement> chars = new HashSet<>();
        for ( FactorValue fv : ef.getFactorValues() ) {
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

        // this is just a placeholder factor value; use has to edit it.
        expressionExperimentService.addFactorValue( ee, fv );
        evictFromCache( ee );
    }

    /**
     * AJAX
     */
    public void createFactorValueCharacteristic( EntityDelegator<FactorValue> e, CharacteristicValueObject cvo ) {
        if ( e == null || e.getId() == null ) return;
        FactorValue fv = factorValueService.loadOrFail( e.getId(), EntityNotFoundException::new );

        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );

        if ( ee == null ) {
            throw new EntityNotFoundException( "No such experiment with " + fv );
        }

        Statement c = factorValueService.createStatement( fv, statementFromVo( cvo ) );
        log.debug( String.format( "Created %s", c ) );

        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "FactorValue characteristic added to: " + fv, c.toString() );
        evictFromCache( ee );
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

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void deleteExperimentalFactors( EntityDelegator<ExperimentalDesign> e, Long[] efIds ) {
        if ( e == null || e.getId() == null ) return;
        Collection<ExperimentalFactor> toDelete = experimentalFactorService.loadOrFail( getIds( efIds ), EntityNotFoundException::new );
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactors( toDelete );
        experimentalFactorService.remove( toDelete );
        ees.forEach( this::evictFromCache );
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
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
            FactorValue fv = factorValueService.loadOrFail( fvvo.getId(), EntityNotFoundException::new );
            Statement c = fv.getCharacteristics().stream().filter( s -> s.getId().equals( fvvo.getCharId() ) ).findFirst().orElseThrow( () -> new EntityNotFoundException( String.format( "No statement with ID %d in FactorVlaue with ID %d", fvvo.getCharId(), fvvo.getId() ) ) );
            fvs[i] = fv;
            statements[i] = c;
        }
        Collection<ExpressionExperiment> ees = new HashSet<>();
        for ( int i = 0; i < fvvos.length; i++ ) {
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fvs[i] );
            if ( ee != null ) {
                ees.add( ee );
            } else {
                log.warn( "No experiment found for " + fvs[i] + "." );
            }
            factorValueService.removeStatement( fvs[i], statements[i] );
        }

        ees.forEach( this::evictFromCache );
    }

    /**
     * Make an exact copy of a factorvalue and add it to the experiment.
     * As per <a href="https://github.com/PavlidisLab/Gemma/issues/1160">#1160</a>
     * <p>
     * AJAX
     * @param e the experimental factor
     * @param fvId the id of the FV to duplicate
     */
    @SuppressWarnings("unused")
    public void duplicateFactorValue( EntityDelegator<ExperimentalFactor> e, Long fvId ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalFactor ef = experimentalFactorService.loadOrFail( e.getId(), EntityNotFoundException::new );
        FactorValue fv = factorValueService.loadOrFail( fvId, EntityNotFoundException::new );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );
        if ( ee == null ) {
            throw new EntityNotFoundException( "No ExpressionExperiment with for FactorValue with ID " + fvId + "." );
        }

        FactorValue newFv = FactorValue.Factory.newInstance();
        newFv.setExperimentalFactor( ef );
        Set<Statement> chars = new HashSet<>();

        for ( Statement c : fv.getCharacteristics() ) {
            Statement newC = new Statement();
            newC.setCategory( c.getCategory() );
            newC.setCategoryUri( c.getCategoryUri() );
            newC.setSubject( c.getSubject() );
            newC.setSubjectUri( c.getSubjectUri() );
            newC.setPredicate( c.getPredicate() );
            newC.setObject( c.getObject() );
            newC.setObjectUri( c.getObjectUri() );
            newC.setPredicateUri( c.getPredicateUri() );
            newC.setSecondObject( c.getSecondObject() );
            newC.setSecondObjectUri( c.getSecondObjectUri() );
            newC.setSecondPredicate( c.getSecondPredicate() );
            newC.setSecondPredicateUri( c.getSecondPredicateUri() );
            newC.setEvidenceCode( c.getEvidenceCode() );
            newC.setOriginalValue( c.getOriginalValue() );
            chars.add( newC );
        }

        newFv.setCharacteristics( chars );
        expressionExperimentService.addFactorValue( ee, newFv );

        evictFromCache( ee );
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void deleteFactorValues( EntityDelegator<ExperimentalFactor> e, Long[] fvIds ) {
        if ( e == null || e.getId() == null ) return;
        Collection<Long> fvCol = getIds( fvIds );
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactorValueIds( fvCol );
        factorValueDeletion.deleteFactorValues( fvCol );
        ees.forEach( this::evictFromCache );
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public Collection<BioMaterialValueObject> getBioMaterials( EntityDelegator<ExpressionExperiment> e ) {
        if ( e == null || e.getId() == null ) return null;
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( e.getId(), EntityNotFoundException::new );
        return getBioMaterialValueObjects( ee );
    }

    /**
     * This filters the characteristics through filterCharacteristics()
     *
     * @param ee experiment to get biomaterials for
     */
    private Collection<BioMaterialValueObject> getBioMaterialValueObjects( ExpressionExperiment ee ) {
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


    /**
     * Extract just the categories from the biomaterial's characteristics.
     * <p>
     * AJAX
     * @return Collection of CharacteristicValueObjects but all we care about is the category
     */
    @SuppressWarnings("unused")
    public Collection<CharacteristicValueObject> getBioMaterialCharacteristicCategories( Long experimentalDesignID ) {
        ExpressionExperiment ee = expressionExperimentService.findByDesignId( experimentalDesignID );
        if ( ee == null ) {
            throw new EntityNotFoundException( "No experiment for design with ID " + experimentalDesignID );
        }
        ee = expressionExperimentService.thawLite( ee );
        return getBioMaterialValueObjects( ee ).stream()
                .flatMap( bmvo -> bmvo.getCharacteristicValues().keySet().stream() )
                .map( cvo -> new CharacteristicValueObject( null, null, cvo, null ) )
                .collect( Collectors.toSet() );
    }

    /**
     * AJAX
     */
    public Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator<?> e ) {
        if ( e == null || e.getId() == null ) return null;

        ExpressionExperiment ee;
        if ( e.holds( ExpressionExperiment.class ) ) {
            ee = expressionExperimentService.loadAndThawLiteOrFail( e.getId(), EntityNotFoundException::new );
        } else if ( e.holds( ExperimentalDesign.class ) ) {
            ee = expressionExperimentService.findByDesignId( e.getId() );
            if ( ee == null ) {
                throw new EntityNotFoundException( "There is no experiment associated to the design with ID " + e.getId() + "." );
            }
            ee = expressionExperimentService.thawLite( ee );
        } else {
            throw new RuntimeException( "Don't know how to process a " + e.getClassDelegatingFor() );
        }

        if ( ee.getExperimentalDesign() == null ) {
            throw new EntityNotFoundException( "Experiment " + ee.getShortName() + " does not have an experimental design." );
        }

        return ee.getExperimentalDesign().getExperimentalFactors().stream()
                .map( ExperimentalFactorValueObject::new )
                .collect( Collectors.toSet() );
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator<ExperimentalFactor> e ) {
        // FIXME I'm not sure why this keeps getting called with empty fields.
        if ( e == null || e.getId() == null ) return new HashSet<>();
        ExperimentalFactor ef = this.experimentalFactorService.loadOrFail( e.getId(), EntityNotFoundException::new );
        return ef.getFactorValues().stream().map( FactorValueValueObject::new ).collect( Collectors.toSet() );
    }

    /**
     * AJAX
     */
    public Collection<FactorValueValueObject> getFactorValuesWithCharacteristics( EntityDelegator<ExperimentalFactor> e ) {
        Collection<FactorValueValueObject> result = new HashSet<>();
        if ( e == null || e.getId() == null ) {
            return result;
        }
        ExperimentalFactor ef = this.experimentalFactorService.loadOrFail( e.getId(), EntityNotFoundException::new );

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

    @RequestMapping(value = "/showExperimentalDesign.html", params = { "edid" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showById( @RequestParam("edid") Long edId ) {
        ExpressionExperiment ee = expressionExperimentService.findByDesignId( edId );
        if ( ee == null ) {
            throw new EntityNotFoundException( String.format( "No ExpressionExperiment for design with ID %d.", edId ) );
        }
        return show( ee );
    }

    @RequestMapping(value = "/showExperimentalDesign.html", params = { "eeid" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showByExperimentId( @RequestParam("eeid") Long eeId ) {
        return show( expressionExperimentService.loadOrFail( eeId, EntityNotFoundException::new ) );
    }

    @RequestMapping(value = "/showExperimentalDesign.html", params = { "shortName" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showByExperimentShortName( @RequestParam("shortName") String shortName ) {
        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            throw new EntityNotFoundException( String.format( "No ExpressionExperiment with short name %s.", shortName ) );
        }
        return show( ee );
    }

    private ModelAndView show( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null ) {
            throw new EntityNotFoundException( "Experiment " + ee.getShortName() + " does not have an experimental design." );
        }
        return new ModelAndView( "experimentalDesign.detail" )
                .addObject( "taxon", expressionExperimentService.getTaxon( ee ) )
                .addObject( "hasPopulatedDesign", !ee.getExperimentalDesign().getExperimentalFactors().isEmpty() )
                .addObject( "experimentalDesign", ee.getExperimentalDesign() )
                .addObject( "expressionExperiment", ee )
                .addObject( "currentUserCanEdit", securityService.isEditableByCurrentUser( ee ) )
                .addAllObjects( getNeedsAttentionDetails( ee.getExperimentalDesign() ) );
    }

    private Map<String, ?> getNeedsAttentionDetails( ExperimentalDesign ed ) {
        Map<String, Object> result = new HashMap<>();
        boolean needsAttention = ed.getExperimentalFactors().stream().flatMap( ef -> ef.getFactorValues().stream() ).anyMatch( FactorValue::getNeedsAttention );
        result.put( "needsAttention", needsAttention );
        try {
            ExperimentalDesign randomEd = experimentalDesignService.getRandomExperimentalDesignThatNeedsAttention( ed );
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

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void updateBioMaterials( BioMaterialValueObject[] bmvos ) {

        if ( bmvos == null || bmvos.length == 0 ) return;


        StopWatch w = new StopWatch();
        w.start();

        Collection<BioMaterial> biomaterials = bioMaterialService.updateBioMaterials( Arrays.asList( bmvos ) );

        log.info( String.format( "Updating biomaterials took %.2f seconds", ( double ) w.getTime() / 1000.0 ) );

        if ( biomaterials.isEmpty() ) return;

        Map<ExpressionExperiment, Collection<BioMaterial>> ees = expressionExperimentService.findByBioMaterials( biomaterials );
        for ( Map.Entry<ExpressionExperiment, Collection<BioMaterial>> entry : ees.entrySet() ) {
            updateBioMaterials( entry.getKey(), entry.getValue() );
        }
    }

    private void updateBioMaterials( ExpressionExperiment ee, Collection<BioMaterial> bms ) {
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null ) {
            throw new EntityNotFoundException( "Experiment " + ee.getShortName() + " does not have an experimental design." );
        }

        /*
         * Check for unused factorValues
         */
        Collection<FactorValue> unusedFactorValues = new HashSet<>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
                Collection<FactorValue> usedFactorValues = new HashSet<>();
                DiffExAnalyzerUtils.populateFactorValuesFromBASet( ee, ef, usedFactorValues );

                for ( FactorValue fv : ef.getFactorValues() ) {
                    if ( !usedFactorValues.contains( fv ) ) {
                        unusedFactorValues.add( fv );
                    }
                }
            }
        }

        if ( !unusedFactorValues.isEmpty() ) {
            log.info( "Deleting " + unusedFactorValues.size() + " unused factor values in " + ee + "..." );
            factorValueDeletion.deleteFactorValues( IdentifiableUtils.getIds( unusedFactorValues ) );
        }

        StringBuilder details = new StringBuilder( "Updated bio materials:\n" );
        for ( BioMaterial bm : bms ) {
            details.append( "id: " ).append( bm.getId() ).append( " - " ).append( bm.getName() ).append( "\n" );
        }
        this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, "BioMaterials updated (" + bms.size() + " items)", details.toString() );
        evictFromCache( ee );
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void updateExperimentalFactors( ExperimentalFactorValueObject[] efvos ) {

        if ( efvos == null || efvos.length == 0 ) return;

        Collection<Long> factorIds = Arrays.stream( efvos )
                .map( ExperimentalFactorValueObject::getId )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );
        Map<Long, ExperimentalFactor> factorById = IdentifiableUtils.getIdMap( experimentalFactorService.loadOrFail( factorIds, EntityNotFoundException::new ) );
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactors( factorById.values() );

        for ( ExperimentalFactorValueObject efvo : efvos ) {
            ExperimentalFactor ef = factorById.get( efvo.getId() );

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
        }

        experimentalFactorService.update( factorById.values() );

        ees.forEach( this::evictFromCache );
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void updateFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {
        if ( fvvos == null || fvvos.length == 0 ) return;

        // validate the VOs
        Collection<Long> fvIds = Arrays.stream( fvvos )
                .map( FactorValueValueObject::getId )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );
        Collection<FactorValue> fvs2 = factorValueService.loadOrFail( fvIds, EntityNotFoundException::new );
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactorValues( fvs2 );
        Map<Long, FactorValue> fvById = IdentifiableUtils.getIdMap( fvs2 );

        FactorValue[] fvs = new FactorValue[fvvos.length];
        Statement[] statements = new Statement[fvvos.length];
        for ( int i = 0; i < fvvos.length; i++ ) {
            FactorValueValueObject fvvo = fvvos[i];
            FactorValue fv = fvById.get( fvvo.getId() );
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
                try {
                    updateFactorValueCharacteristic( c, fvvo );
                } finally {
                    fv.getCharacteristics().add( c );
                }
            } else {
                c = Statement.Factory.newInstance();
                updateFactorValueCharacteristic( c, fvvo );
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

        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "FactorValue characteristics updated", StringUtils.join( fvvos, "\n" ) );
        ees.forEach( this::evictFromCache );
    }

    private void updateFactorValueCharacteristic( Statement c, FactorValueValueObject fvvo ) {
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
            c.setObjectUri( StringUtils.stripToNull( fvvo.getObjectUri() ) );
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
            c.setSecondObjectUri( StringUtils.stripToNull( fvvo.getSecondObjectUri() ) );
        } else {
            c.setSecondPredicate( null );
            c.setSecondPredicateUri( null );
            c.setSecondObject( null );
            c.setSecondObjectUri( null );
        }
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void markFactorValuesAsNeedsAttention( Long[] fvvos, String note ) {
        Collection<FactorValue> fvs = factorValueService.loadOrFail( getIds( fvvos ), EntityNotFoundException::new );
        int marked = 0;
        for ( FactorValue fv : fvs ) {
            if ( fv.getNeedsAttention() ) {
                if ( fvvos.length == 1 ) {
                    throw new IllegalArgumentException( String.format( "%s is already marked as needs attention.", fv ) );
                }
            } else {
                factorValueService.markAsNeedsAttention( fv, note );
                marked++;
            }
        }
        log.info( String.format( "Marked %d factor values as needs attention: %s", marked, note ) );
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public void clearFactorValuesNeedsAttention( Long[] fvvos, String note ) {
        Collection<FactorValue> fvs = factorValueService.loadOrFail( getIds( fvvos ), EntityNotFoundException::new );
        int cleared = 0;
        for ( FactorValue fv : fvs ) {
            if ( !fv.getNeedsAttention() ) {
                if ( fvvos.length == 1 ) {
                    throw new IllegalArgumentException( String.format( "%s does not need attention.", fv ) );
                }
            } else {
                factorValueService.clearNeedsAttentionFlag( fv, note );
                cleared++;
            }
        }
        log.info( String.format( "Cleared %d factor values' needs attention flags: %s", cleared, note ) );
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

    private Collection<Long> getIds( Long[] ids ) {
        return Arrays.stream( ids ).map( Objects::requireNonNull ).collect( Collectors.toSet() );
    }

    private void evictFromCache( ExpressionExperiment ee ) {
        // defer evictions
        taskExecutor.execute( () -> {
            this.tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ee, ExperimentalDesign.class );
            this.experimentReportService.evictFromCache( ee.getId() );
        } );
    }
}
