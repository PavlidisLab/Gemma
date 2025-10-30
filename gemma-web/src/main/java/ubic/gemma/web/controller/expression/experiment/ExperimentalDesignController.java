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
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
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
import ubic.gemma.model.common.IdentifiableValueObject;
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

import static org.apache.commons.lang3.StringUtils.stripToNull;
import static ubic.gemma.core.util.BeanWrapperUtils.setPropertyValue;

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

    @Value("${gemma.download.path}/userUploads")
    private Path uploadDir;

    @SuppressWarnings({ "unused" })
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
            // as a fail-safe.
            experimentalDesignImporter.importDesign( ee, is );
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to import the design: " + e.getMessage() );
        }

        evictExperimentFromCache( ee );
    }

    /**
     * Create an experimental factor.
     *
     * @param e    experimentalDesign to add the factor to
     * @param efvo non-null if we are pre-populating the factor values based on an existing set of BioMaterialCharacteristic,
     *             see <a href="https://github.com/PavlidisLab/Gemma/issues/987">#987</a>
     */
    @SuppressWarnings({ "unused" })
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

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.valueOf( efvo.getType() ) );
        ef.setExperimentalDesign( ed );
        ef.setName( efvo.getName() );
        ef.setDescription( efvo.getDescription() );
        ef.setCategory( this.createCategoryCharacteristic( efvo.getCategory(), efvo.getCategoryUri() ) );
        ef.setAutoGenerated( false );

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
                BioMaterial bm = bioMaterialService.loadOrFail( bmo.getId() );

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
                // we have to make an FV for each biomaterial, and we have to make a measurement for each biomaterial.
                // We update the experiment in batch to try to speed this up.
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
                expressionExperimentService.addFactorValues( ee, bmToFv, false );
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
                    s.setSubjectUri( stripToNull( cvo.getValueUri() ) ); // can be null
                    fv.getCharacteristics().add( s );
                    fv = expressionExperimentService.addFactorValue( ee, fv, false );

                    for ( BioMaterial bm : map.get( cvo ) ) {
                        bm.getFactorValues().add( fv );
                        toUpdate.add( bm );
                    }
                }
                bioMaterialService.update( toUpdate );
            }
        }

        evictExperimentFromCache( ee );
    }

    @SuppressWarnings({ "unused" })
    public void createFactorValue( EntityDelegator<ExperimentalFactor> e ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalFactor ef = experimentalFactorService.loadOrFail( e.getId(), EntityNotFoundException::new );
        ExpressionExperiment ee = expressionExperimentService.findByFactor( ef );
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
        expressionExperimentService.addFactorValue( ee, fv, false );

        // don't bother marking the factor as manually curated, as this is just a template.

        evictExperimentFromCache( ee );
    }

    @SuppressWarnings({ "unused" })
    public void createFactorValueCharacteristic( EntityDelegator<FactorValue> e, CharacteristicValueObject cvo ) {
        if ( e == null || e.getId() == null ) return;
        FactorValue fv = factorValueService.loadWithExperimentalFactorOrFail( e.getId(), EntityNotFoundException::new );

        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );

        if ( ee == null ) {
            throw new EntityNotFoundException( "No such experiment with " + fv );
        }

        Statement c = factorValueService.createStatement( fv, statementFromVo( cvo ) );
        log.debug( String.format( "Created %s", c ) );

        markFactorsAsManuallyCurated( Collections.singleton( fv.getExperimentalFactor() ) );

        // this.auditTrailService.addUpdateEvent( ee, ExperimentalDesignEvent.class,
        // "FactorValue characteristic added to: " + fv, c.toString() );
        evictExperimentFromCache( ee );
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
        c.setCategoryUri( stripToNull( vo.getCategoryUri() ) );
        c.setSubject( vo.getValue() );
        c.setSubjectUri( stripToNull( vo.getValueUri() ) );
        return c;
    }

    @SuppressWarnings({ "unused" })
    public void deleteExperimentalFactors( EntityDelegator<ExperimentalDesign> e, Long[] efIds ) {
        if ( e == null || e.getId() == null ) return;

        Collection<Long> efCol = Arrays.stream( efIds )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );

        Collection<ExperimentalFactor> toDelete = experimentalFactorService.loadOrFail( efCol, EntityNotFoundException::new );

        Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactors( toDelete );
        experimentalFactorService.remove( toDelete );
        evictExperimentsFromCache( ees );
    }

    @SuppressWarnings({ "unused" })
    public void deleteFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {
        Set<ExperimentalFactor> efs = new HashSet<>();
        Set<Long> fvIds = Arrays.stream( fvvos )
                .map( IdentifiableValueObject::getId )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );
        Map<Long, FactorValue> fvById = IdentifiableUtils.getIdMap( factorValueService.loadWithExperimentalFactorOrFail( fvIds, EntityNotFoundException::new ) );
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
            FactorValue fv = fvById.get( fvvo.getId() );
            efs.add( fv.getExperimentalFactor() );
            Statement c = fv.getCharacteristics().stream().filter( s -> s.getId().equals( fvvo.getCharId() ) ).findFirst().orElseThrow( () -> new EntityNotFoundException( String.format( "No statement with ID %d in FactorValue with ID %d", fvvo.getCharId(), fvvo.getId() ) ) );
            fvs[i] = fv;
            statements[i] = c;
        }
        for ( int i = 0; i < fvvos.length; i++ ) {
            factorValueService.removeStatement( fvs[i], statements[i] );
        }
        markFactorsAsManuallyCurated( efs );
        evictExperimentsFromCacheByFactorValues( fvById.values() );
    }

    /**
     * Make an exact copy of a factor value and add it to the experiment.
     * As per <a href="https://github.com/PavlidisLab/Gemma/issues/1160">#1160</a>
     * @param e the experimental factor
     * @param fvId the id of the FV to duplicate
     */
    @SuppressWarnings({ "unused" })
    public void duplicateFactorValue( EntityDelegator<ExperimentalFactor> e, Long fvId ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalFactor ef = experimentalFactorService.loadOrFail( e.getId(), EntityNotFoundException::new );
        FactorValue fv = factorValueService.loadOrFail( fvId, EntityNotFoundException::new );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );
        if ( ee == null ) {
            throw new EntityNotFoundException( "No experiment found with factor value ID " + fvId + "." );
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
        expressionExperimentService.addFactorValue( ee, newFv, false );

        // in case of duplication, we preserve the auto-generated flag on the factor until it is actually modified

        evictExperimentFromCache( ee );
    }

    @SuppressWarnings({ "unused" })
    public void deleteFactorValues( EntityDelegator<ExperimentalFactor> e, Long[] fvIds ) {
        if ( e == null || e.getId() == null ) return;
        Collection<Long> fvCol = Arrays.stream( fvIds )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactorValueIds( fvCol );
        factorValueDeletion.deleteFactorValues( fvCol );
        for ( ExpressionExperiment ee : ees ) {
            evictExperimentFromCache( ee );
        }
    }

    @SuppressWarnings({ "unused" })
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
    @SuppressWarnings({ "unused" })
    public Collection<CharacteristicValueObject> getBioMaterialCharacteristicCategories( Long experimentalDesignID ) {
        ExpressionExperiment ee = expressionExperimentService.findByDesignId( experimentalDesignID );
        if ( ee == null ) {
            throw new EntityNotFoundException( "No experiment for design with ID " + experimentalDesignID );
        }

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
     * Filter the characteristicValues to those that we want to display in columns in the biomaterial value table.
     *
     */
    private void filterCharacteristics( Collection<BioMaterialValueObject> result ) {
        Collection<String> toRemove = new HashSet<>();

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
                    if ( StringUtils.isNotBlank( value ) && value.matches( ".+[:=].+" ) ) { // note: GEO only allows ":" now, but we have "=" in the db for older entries.
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
                    toRemove.add( category );
                    continue;
                }

                map.get( category ).add( bmo );
            }
        }

        /*
        find ones that don't meet criteria for display e.g. are constant across all samples
         */

        for ( String category : map.keySet() ) {
            //log.info( ">>>>>>>>>> " + category + ", " + map.get( category ).size() + " items" );
            if ( map.get( category ).size() != result.size() ) {
                //  toRemove.add( category ); // this isn't really worth it and hides useful information.
                continue;
            }

            // TODO add more exclusions; see also ExpressionExperimentDao.getAnnotationsByBioMaterials
            if ( category.equals( "LabelCompound" ) || category.equals( "MaterialType" ) || category.equals( "molecular entity" ) ) {
                toRemove.add( category );
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
                toRemove.add( category );
            }
        }

        // finally, clean up the bmos.
        for ( BioMaterialValueObject bmo : result ) {
            for ( String lose : toRemove ) {
                bmo.getCharacteristicValues().remove( lose );
            }
        }

    }

    @SuppressWarnings({ "unused" })
    public Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator<?> e ) {
        if ( e == null || e.getId() == null ) return null;

        ExpressionExperiment ee;
        if ( e.holds( ExpressionExperiment.class ) ) {
            ee = expressionExperimentService.loadOrFail( e.getId(), EntityNotFoundException::new );
        } else if ( e.holds( ExperimentalDesign.class ) ) {
            ee = expressionExperimentService.findByDesignId( e.getId() );
            if ( ee == null ) {
                throw new EntityNotFoundException( "There is no experiment associated to the design with ID " + e.getId() + "." );
            }
        } else {
            throw new RuntimeException( "Don't know how to process a " + e.getClassDelegatingFor() );
        }

        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null ) {
            throw new EntityNotFoundException( "Experiment " + ee.getShortName() + " does not have an experimental design." );
        }

        Collection<ExperimentalFactorValueObject> result = new HashSet<>();
        for ( ExperimentalFactor factor : ee.getExperimentalDesign().getExperimentalFactors() ) {
            result.add( new ExperimentalFactorValueObject( factor ) );
        }
        return result;
    }

    @SuppressWarnings({ "unused" })
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

    @SuppressWarnings({ "unused" })
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

    @SuppressWarnings({ "unused" })
    public void updateBioMaterials( BioMaterialValueObject[] bmvos ) {

        if ( bmvos == null || bmvos.length == 0 ) return;


        StopWatch w = new StopWatch();
        w.start();

        Collection<BioMaterial> biomaterials = bioMaterialService.updateBioMaterials( Arrays.asList( bmvos ) );

        log.info( String.format( "Updating biomaterials took %.2f seconds", ( double ) w.getTime() / 1000.0 ) );

        if ( biomaterials.isEmpty() ) return;

        BioMaterial bm = biomaterials.iterator().next();
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByBioMaterial( bm );
        if ( ees.isEmpty() ) {
            throw new IllegalStateException( "No Experiment for biomaterial: " + bm );
        } else if ( ees.size() > 1 ) {
            throw new IllegalStateException( "There is more than one experiment for biomaterial: " + bm );
        }
        ExpressionExperiment ee = ees.iterator().next();

        ee = expressionExperimentService.thawLite( ee );

        if ( ee.getExperimentalDesign() == null ) {
            throw new EntityNotFoundException( "Experiment " + ee.getShortName() + " does not have an experimental design." );
        }

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {

                /*
                 * Check for unused factorValues
                 */
                Collection<FactorValue> usedFactorValues = new HashSet<>();
                DiffExAnalyzerUtils.populateFactorValuesFromBASet( ee, ef, usedFactorValues );

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
                    log.info( "Deleting " + toDelete.size() + " unused factor values for " + ef );
                    factorValueDeletion.deleteFactorValues( IdentifiableUtils.getIds( toDelete ) );
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
        evictExperimentFromCache( ee );
    }

    @SuppressWarnings({ "unused" })
    public void updateExperimentalFactors( ExperimentalFactorValueObject[] efvos ) {
        if ( efvos == null || efvos.length == 0 ) return;
        List<Long> efIds = Arrays.stream( efvos )
                .map( ExperimentalFactorValueObject::getId )
                .collect( Collectors.toList() );
        Map<Long, ExperimentalFactor> factors = IdentifiableUtils.getIdMap( experimentalFactorService.loadOrFail( efIds, EntityNotFoundException::new ) );
        Set<ExperimentalFactor> modifiedFactors = new HashSet<>();
        for ( ExperimentalFactorValueObject efvo : efvos ) {
            ExperimentalFactor ef = factors.get( efvo.getId() );
            if ( applyChanges( ef, efvo ) ) {
                if ( ef.isAutoGenerated() ) {
                    log.info( "Marking " + ef + " as manually curated." );
                    ef.setAutoGenerated( false );
                }
                modifiedFactors.add( ef );
            }
        }

        if ( !modifiedFactors.isEmpty() ) {
            experimentalFactorService.update( modifiedFactors );
            // not using markFactorsAsManuallyCurated() here because we don't want to duplicate the update() call
            evictExperimentsFromCacheByFactors( modifiedFactors );
        }
    }

    private boolean applyChanges( ExperimentalFactor ef, ExperimentalFactorValueObject efvo ) {
        BeanWrapper bw = new BeanWrapperImpl( ef );
        FactorType newType = efvo.getType() != null ? FactorType.valueOf( efvo.getType() ) : null;

        // we only allow this if there are no factors
        if ( !Objects.equals( ef.getType(), newType ) && !ef.getFactorValues().isEmpty() ) {
            throw new IllegalArgumentException( "You cannot change the 'type' of a factor once it has factor values. Delete the factor values first." );
        }

        boolean modified = false;
        modified |= setPropertyValue( bw, "name", efvo.getName() );
        modified |= setPropertyValue( bw, "description", efvo.getDescription() );
        modified |= setPropertyValue( bw, "type", newType );

        //  can be null if this was imported from GEO etc.
        if ( ef.getCategory() == null ) {
            ef.setCategory( Characteristic.Factory.newInstance() );
        }

        modified |= setPropertyValue( bw, "category", efvo.getCategory() );
        modified |= setPropertyValue( bw, "categoryUri", stripToNull( efvo.getCategoryUri() ) );
        modified |= setPropertyValue( bw, "value", efvo.getCategory() );
        modified |= setPropertyValue( bw, "valueUri", stripToNull( efvo.getCategoryUri() ) );

        return modified;
    }

    @SuppressWarnings({ "unused" })
    public void updateFactorValueCharacteristics( FactorValueValueObject[] fvvos ) {
        if ( fvvos == null || fvvos.length == 0 ) return;

        Set<Long> fvIds = Arrays.stream( fvvos )
                .map( IdentifiableValueObject::getId )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );
        Map<Long, FactorValue> fvById = IdentifiableUtils.getIdMap( factorValueService.loadWithExperimentalFactorOrFail( fvIds, EntityNotFoundException::new ) );

        // validate the VOs and apply the update
        FactorValue[] fvs = new FactorValue[fvvos.length];
        Statement[] statements = new Statement[fvvos.length];
        boolean[] modified = new boolean[fvvos.length];
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
                fvs[i] = fv;
                statements[i] = c;
                // updating the statement can alter its hashCode() and thus breaking the Set contract, we have to remove
                // it and add it back before saving
                fv.getCharacteristics().remove( c );
                try {
                    modified[i] = applyChanges( c, fvvo );
                } finally {
                    fv.getCharacteristics().add( c );
                }
            } else {
                fvs[i] = fv;
                statements[i] = Statement.Factory.newInstance();
                modified[i] = applyChanges( statements[i], fvvo );
            }
        }

        // now save!
        Set<ExperimentalFactor> modifiedFactors = new HashSet<>();
        for ( int i = 0; i < fvs.length; i++ ) {
            if ( !modified[i] ) {
                continue;
            }
            statements[i] = factorValueService.saveStatement( fvs[i], statements[i] );
            if ( fvs[i].getNeedsAttention() ) {
                factorValueService.clearNeedsAttentionFlag( fvs[i], "The dataset does not need attention and all of its factor values were fixed." );
                log.info( "Reverted needs attention flag for " + fvs[i] );
            }
            modifiedFactors.add( fvs[i].getExperimentalFactor() );
        }

        if ( !modifiedFactors.isEmpty() ) {
            markFactorsAsManuallyCurated( modifiedFactors );
            evictExperimentsFromCacheByFactorValues( fvById.values() );
        }
    }

    /**
     * For related code see CharacteristicUpdateTaskImpl
     */
    private boolean applyChanges( Statement c, FactorValueValueObject fvvo ) {
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

        boolean modified = false;
        BeanWrapperImpl bw = new BeanWrapperImpl( c );

        // preserve original data
        if ( StringUtils.isBlank( c.getOriginalValue() ) ) {
            c.setOriginalValue( c.getSubject() );
            modified = true;
        }

        modified |= setPropertyValue( bw, "category", fvvo.getCategory() );
        modified |= setPropertyValue( bw, "categoryUri", stripToNull( fvvo.getCategoryUri() ) );
        modified |= setPropertyValue( bw, "subject", fvvo.getValue() );
        modified |= setPropertyValue( bw, "subjectUri", stripToNull( fvvo.getValueUri() ) );
        modified |= setPropertyValue( bw, "evidenceCode", GOEvidenceCode.IC ); // characteristic has been manually updated

        if ( !StringUtils.isBlank( fvvo.getObject() ) ) {
            modified |= setPropertyValue( bw, "predicate", fvvo.getPredicate() );
            modified |= setPropertyValue( bw, "predicateUri", stripToNull( fvvo.getPredicateUri() ) );
            modified |= setPropertyValue( bw, "object", fvvo.getObject() );
            modified |= setPropertyValue( bw, "objectUri", stripToNull( fvvo.getObjectUri() ) );
        } else {
            modified |= setPropertyValue( bw, "predicate", null );
            modified |= setPropertyValue( bw, "predicateUri", null );
            modified |= setPropertyValue( bw, "object", null );
            modified |= setPropertyValue( bw, "objectUri", null );
        }

        if ( !StringUtils.isBlank( fvvo.getSecondObject() ) ) {
            modified |= setPropertyValue( bw, "secondPredicate", fvvo.getSecondPredicate() );
            modified |= setPropertyValue( bw, "secondPredicateUri", stripToNull( fvvo.getSecondPredicateUri() ) );
            modified |= setPropertyValue( bw, "secondObject", fvvo.getSecondObject() );
            modified |= setPropertyValue( bw, "secondObjectUri", stripToNull( fvvo.getSecondObjectUri() ) );
        } else {
            modified |= setPropertyValue( bw, "secondPredicate", null );
            modified |= setPropertyValue( bw, "secondPredicateUri", null );
            modified |= setPropertyValue( bw, "secondObject", null );
            modified |= setPropertyValue( bw, "secondObjectUri", null );
        }

        return modified;
    }

    @SuppressWarnings({ "unused" })
    public void markFactorValuesAsNeedsAttention( Long[] fvvos, String note ) {
        Set<Long> fvIds = Arrays.stream( fvvos )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );
        Collection<FactorValue> fvs = factorValueService.loadOrFail( fvIds, EntityNotFoundException::new );
        for ( FactorValue fv : fvs ) {
            if ( fv.getNeedsAttention() ) {
                if ( fvvos.length == 1 ) {
                    throw new IllegalArgumentException( String.format( "%s is already marked as needs attention.", fv ) );
                } else {
                    continue; // skip
                }
            }
            factorValueService.markAsNeedsAttention( fv, note );
        }
        log.info( String.format( "Marked %d factor values as needs attention: %s", fvs.size(), note ) );
        evictExperimentsFromCacheByFactorValues( fvs );
    }

    @SuppressWarnings({ "unused" })
    public void clearFactorValuesNeedsAttention( Long[] fvvos, String note ) {
        Set<Long> fvIds = Arrays.stream( fvvos )
                .map( Objects::requireNonNull )
                .collect( Collectors.toSet() );
        Collection<FactorValue> fvs = factorValueService.loadOrFail( fvIds, EntityNotFoundException::new );
        for ( FactorValue fv : fvs ) {
            if ( !fv.getNeedsAttention() ) {
                if ( fvvos.length == 1 ) {
                    throw new IllegalArgumentException( String.format( "%s does not need attention.", fv ) );
                } else {
                    continue; // skip
                }
            }
            factorValueService.clearNeedsAttentionFlag( fv, note );
        }
        log.info( String.format( "Cleared %d factor values' needs attention flags: %s", fvs.size(), note ) );
        evictExperimentsFromCacheByFactorValues( fvs );
    }

    private Characteristic createCategoryCharacteristic( String category, String categoryUri ) {
        Characteristic c = Characteristic.Factory.newInstance( category, stripToNull( categoryUri ), category, stripToNull( categoryUri ) );
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

    private void markFactorsAsManuallyCurated( Collection<ExperimentalFactor> efs ) {
        Set<ExperimentalFactor> toUpdate = new HashSet<>();
        for ( ExperimentalFactor factor : efs ) {
            if ( factor.isAutoGenerated() ) {
                log.info( "Marking " + factor + " as manually curated." );
                factor.setAutoGenerated( false );
                toUpdate.add( factor );
            }
        }
        experimentalFactorService.update( toUpdate );
    }

    private void evictExperimentsFromCacheByFactors( Set<ExperimentalFactor> factors ) {
        for ( ExpressionExperiment ee : expressionExperimentService.findByFactors( factors ) ) {
            evictExperimentFromCache( ee );
        }
    }

    private void evictExperimentsFromCacheByFactorValues( Collection<FactorValue> values ) {
        for ( ExpressionExperiment ee : expressionExperimentService.findByFactorValues( values ).keySet() ) {
            evictExperimentFromCache( ee );
        }
    }

    private void evictExperimentsFromCache( Collection<ExpressionExperiment> ees ) {
        for ( ExpressionExperiment ee : ees ) {
            evictExperimentFromCache( ee );
        }
    }

    private void evictExperimentFromCache( ExpressionExperiment ee ) {
        this.experimentReportService.evictFromCache( ee.getId() );
    }
}
