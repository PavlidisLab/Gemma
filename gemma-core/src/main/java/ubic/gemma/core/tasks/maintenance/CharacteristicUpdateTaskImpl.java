/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.tasks.maintenance;

import gemma.gsec.SecurityService;
import gemma.gsec.model.Securable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * This handles characteristic updates from the client: experiment tags, characteristic browser
 *
 * @author paul
 */
@Component
@Scope("prototype")
public class CharacteristicUpdateTaskImpl extends AbstractTask<TaskResult, CharacteristicUpdateCommand>
        implements CharacteristicUpdateTask {

    private static final Log log = LogFactory.getLog( CharacteristicUpdateTask.class );

    private final BioMaterialService bioMaterialService;
    private final CharacteristicService characteristicService;
    private final ExpressionExperimentService expressionExperimentService;
    private final FactorValueService factorValueService;
    private final SecurityService securityService;

    @Autowired
    public CharacteristicUpdateTaskImpl( BioMaterialService bioMaterialService,
            CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
            FactorValueService factorValueService, SecurityService securityService ) {
        this.bioMaterialService = bioMaterialService;
        this.characteristicService = characteristicService;
        this.expressionExperimentService = expressionExperimentService;
        this.factorValueService = factorValueService;
        this.securityService = securityService;
    }

    @Override
    public TaskResult call() {
        if ( taskCommand.isRemove() ) {
            return this.doRemove();
        }
        return this.doUpdate();
    }

    @Override
    public void setTaskCommand( CharacteristicUpdateCommand command ) {
        assert command != null;
        super.setTaskCommand( command );
    }

    private void addToParent( Characteristic c, Object parent ) {
        if ( parent instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) parent;
            ee = expressionExperimentService.thawLite( ee );
            ee.getCharacteristics().add( c );
            expressionExperimentService.update( ee );
        } else if ( parent instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) parent;
            bm.getCharacteristics().add( c );
            bioMaterialService.update( bm );
        } else if ( parent instanceof FactorValue ) {
            FactorValue fv = ( FactorValue ) parent;
            fv.getCharacteristics().add( c );
            factorValueService.update( fv );
        }
    }

    private Characteristic convertAvo2Characteristic( AnnotationValueObject avo ) {
        Characteristic vc = Characteristic.Factory.newInstance();
        vc.setId( avo.getId() );
        vc.setCategory( avo.getClassName() );
        vc.setCategoryUri( StringUtils.stripToNull( avo.getClassUri() ) );
        vc.setValue( avo.getTermName() );
        vc.setValueUri( StringUtils.stripToNull( avo.getTermUri() ) );
        if ( StringUtils.isNotBlank( avo.getEvidenceCode() ) )
            vc.setEvidenceCode( GOEvidenceCode.valueOf( avo.getEvidenceCode() ) );
        return vc;
    }

    /**
     * Convert incoming AVOs into Characteristics (if the AVO objectClass is not FactorValue)
     */
    private Collection<Characteristic> convertToCharacteristic( Collection<AnnotationValueObject> avos ) {
        Collection<Characteristic> result = new HashSet<>();
        for ( AnnotationValueObject avo : avos ) {
            if ( avo.getObjectClass() != null && avo.getObjectClass().equals( FactorValue.class.getSimpleName() ) )
                continue;

            Characteristic vc = convertAvo2Characteristic( avo );

            result.add( vc );
        }
        return result;
    }

    /**
     * This is used to handle the special case of FactorValues that are being updated to have a characteristic.
     *
     * @return for each given AnnotationValueObject, the corresponding FactorValue with an
     * associated persistent
     * Characteristic.
     * @throws IllegalStateException if the corresponding FactorValue already has at least one Characteristic. This
     *                               method is just intended for filling that in if it's empty.
     */
    private Collection<FactorValue> convertToFactorValuesWithCharacteristics( Collection<AnnotationValueObject> avos ) {
        Collection<FactorValue> result = new HashSet<>();
        for ( AnnotationValueObject avo : avos ) {
            if ( avo.getObjectClass() == null || !avo.getObjectClass().equals( FactorValue.class.getSimpleName() ) )
                continue;

            if ( avo.getId() == null ) {
                log.warn( "No id" );
                continue;
            }

            /*
             * load the factor value, and create a characteristic
             */
            FactorValue fv = factorValueService.load( avo.getId() );
            if ( fv == null )
                continue;

            if ( !fv.getCharacteristics().isEmpty() ) {
                throw new IllegalStateException(
                        "Don't use the annotator to update factor values that already have characteristics" );
            }

            Characteristic vc = convertAvo2Characteristic( avo );
            vc.setId( null );

            if ( vc.getEvidenceCode() == null ) {
                vc.setEvidenceCode( GOEvidenceCode.IC );
            }

            vc = characteristicService.create( vc );

            fv.setValue( vc.getValue() );
            fv.getCharacteristics().add( vc );

            result.add( fv );

        }
        return result;
    }

    private TaskResult doRemove() {
        Collection<AnnotationValueObject> chars = taskCommand.getAnnotationValueObjects();
        log.info( "Delete " + chars.size() + " characteristics..." );

        Collection<Characteristic> asChars = convertToCharacteristic( chars );

        if ( asChars.size() == 0 ) {
            log.info( "No characteristic objects were received" );
            return new TaskResult( taskCommand, false );
        }

        Map<Characteristic, Object> charToParent = characteristicService.getParents( asChars );
        for ( Characteristic cFromClient : asChars ) {
            Characteristic cFromDatabase = characteristicService.load( cFromClient.getId() );
            Object parent = charToParent.get( cFromDatabase );
            removeFromParent( cFromDatabase, parent );
            characteristicService.remove( cFromDatabase );
            log.info( "Characteristic deleted: " + cFromDatabase + " (associated with " + parent + ")" );
        }
        return new TaskResult( taskCommand, true );

    }

    /**
     * Update characteristics.
     * <p>
     * This is used for Experiment tags and the Characteristic browser.
     * <p>
     * For experimental designs, see ExperimentalDesignController.
     *
     */
    private TaskResult doUpdate() {
        Collection<AnnotationValueObject> avos = taskCommand.getAnnotationValueObjects();
        if ( avos.size() == 0 )
            return new TaskResult( taskCommand, false );
        log.info( "Updating " + avos.size() + " characteristics or uncharacterized factor values..." );
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Characteristic> asChars = convertToCharacteristic( avos );
        Collection<FactorValue> factorValues = convertToFactorValuesWithCharacteristics( avos );

        if ( asChars.size() == 0 && factorValues.size() == 0 ) {
            log.info( "Nothing to update" );
            return new TaskResult( taskCommand, false );
        }

        for ( FactorValue factorValue : factorValues ) {
            factorValueService.update( factorValue );
        }

        if ( asChars.size() == 0 )
            return new TaskResult( taskCommand, true );

        Map<Characteristic, Object> charToParent = characteristicService.getParents( asChars );

        for ( Characteristic cFromClient : asChars ) {
            Long characteristicId = cFromClient.getId();
            if ( characteristicId == null ) {
                continue;
            }
            Characteristic cFromDatabase = characteristicService.load( characteristicId );

            if ( cFromDatabase == null ) {
                log.warn( "No such characteristic with id=" + characteristicId );
                continue;
            }

            Object parent = charToParent.get( cFromDatabase );

            /*
             * Check needed because Characteristics are not securable.
             */
            if ( parent != null && Securable.class.isAssignableFrom( parent.getClass() ) && !securityService
                    .isEditable( ( Securable ) parent ) ) {
                throw new AccessDeniedException( "Access is denied" );
            }

            assert cFromDatabase != null;

            // preserve original data (which might have been entered by us, but may be from GEO)
            if ( StringUtils.isBlank( cFromDatabase.getOriginalValue() ) ) {
                cFromDatabase.setOriginalValue( cFromDatabase.getValue() );
            }

            cFromDatabase.setValue( StringUtils.strip( cFromClient.getValue() ) ); // remove whitespace we added to force dirty check
            cFromDatabase.setCategory( StringUtils.strip( cFromClient.getCategory() ) );

            if ( ( cFromDatabase.getValueUri() == null && cFromClient.getValueUri() != null ) || ( cFromDatabase.getValueUri() != null
                    && !cFromDatabase
                    .getValueUri().equals( cFromClient.getValueUri() ) ) ) {
                log.info( "Characteristic value update: " + cFromDatabase + " " + cFromDatabase.getValueUri()
                        + " -> " + cFromClient.getValueUri() + " associated with " + parent );
                cFromDatabase.setValueUri( cFromClient.getValueUri() );
            }

            if ( ( cFromDatabase.getCategoryUri() == null && cFromClient.getCategoryUri() != null ) || ( cFromDatabase.getCategoryUri() != null
                    && !cFromDatabase.getCategoryUri().equals( cFromClient.getCategoryUri() ) ) ) {
                log.info( "Characteristic category update: " + cFromDatabase + " " + cFromDatabase
                        .getCategoryUri() + " -> " + cFromClient.getCategoryUri() + " associated with "
                        + parent );
                cFromDatabase.setCategoryUri( cFromClient.getCategoryUri() );
            }

            if ( cFromClient.getEvidenceCode() == null ) {
                cFromDatabase.setEvidenceCode( GOEvidenceCode.IC ); // characteristic has been manually updated
            } else {
                if ( !cFromDatabase.getEvidenceCode().equals( cFromClient.getEvidenceCode() ) ) {
                    log.info( "Characteristic evidence code update: " + cFromDatabase + " " + cFromDatabase
                            .getEvidenceCode() + " -> " + cFromClient.getEvidenceCode() );
                }
                cFromDatabase.setEvidenceCode( cFromClient.getEvidenceCode() ); // let them change it.
            }

            characteristicService.update( cFromDatabase );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Update took: " + timer.getTime() );
        }

        return new TaskResult( taskCommand, true );

    }

    private void removeFromParent( Characteristic c, Object parent ) {
        if ( parent instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) parent;
            ee = expressionExperimentService.thawLite( ee );
            ee.getCharacteristics().remove( c );
            expressionExperimentService.update( ee );
        } else if ( parent instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) parent;
            bm.getCharacteristics().remove( c );
            bioMaterialService.update( bm );
        } else if ( parent instanceof FactorValue ) {
            FactorValue fv = ( FactorValue ) parent;
            fv.getCharacteristics().remove( c );
            factorValueService.update( fv );
        }
    }
}
