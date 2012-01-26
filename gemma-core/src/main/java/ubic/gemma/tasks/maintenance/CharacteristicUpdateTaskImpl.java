/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.tasks.maintenance;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.security.SecurityService;

/**
 * @author paul
 * @version $Id$
 */
@Component
public class CharacteristicUpdateTaskImpl implements CharacteristicUpdateTask {

    private static Log log = LogFactory.getLog( CharacteristicUpdateTaskImpl.class );

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private SecurityService securityService;

    @TaskMethod
    public TaskResult execute( CharacteristicUpdateCommand command ) {
        if ( command.isRemove() ) {
            return this.doRemove( command );
        }
        return this.doUpdate( command );
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

    /**
     * @param avo
     * @return
     */
    private VocabCharacteristic convertAvo2Characteristic( AnnotationValueObject avo ) {
        VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
        vc.setId( avo.getId() );
        vc.setCategory( avo.getClassName() );
        vc.setCategoryUri( avo.getClassUri() );
        vc.setValue( avo.getTermName() );
        vc.setValueUri( avo.getTermUri() );
        if ( StringUtils.isNotBlank( avo.getEvidenceCode() ) )
            vc.setEvidenceCode( GOEvidenceCode.fromString( avo.getEvidenceCode() ) );
        return vc;
    }

    /**
     * Convert incoming AVOs into Characteristics (if the AVO objectClass is not FactorValue)
     * 
     * @param avos
     * @return
     */
    private Collection<Characteristic> convertToCharacteristic( Collection<AnnotationValueObject> avos ) {
        Collection<Characteristic> result = new HashSet<Characteristic>();
        for ( AnnotationValueObject avo : avos ) {
            if ( avo.getObjectClass() != null && avo.getObjectClass().equals( FactorValue.class.getSimpleName() ) )
                continue;

            VocabCharacteristic vc = convertAvo2Characteristic( avo );

            result.add( vc );
        }
        return result;
    }

    /**
     * This is used to handle the special case of FactorValues that are being updated to have a characteristic.
     * 
     * @param avos
     * @return for each given AnnotationValueObject, the corresponding FactorValue with an associated persistent
     *         Characteristic.
     * @throws IllegalStateException if the corresponding FactorValue already has at least one Characteristic. This
     *         method is just intended for filling that in if it's empty.
     */
    private Collection<FactorValue> convertToFactorValuesWithCharacteristics( Collection<AnnotationValueObject> avos ) {
        Collection<FactorValue> result = new HashSet<FactorValue>();
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
            if ( fv == null ) continue;

            if ( !fv.getCharacteristics().isEmpty() ) {
                throw new IllegalStateException(
                        "Don't use the annotator to update factor values that already have characteristics" );
            }

            VocabCharacteristic vc = convertAvo2Characteristic( avo );
            vc.setId( null );

            if ( vc.getEvidenceCode() == null ) {
                vc.setEvidenceCode( GOEvidenceCode.IC );
            }

            vc = ( VocabCharacteristic ) characteristicService.create( vc );

            fv.setValue( vc.getValue() );
            fv.getCharacteristics().add( vc );

            result.add( fv );

        }
        return result;
    }

    private TaskResult doRemove( CharacteristicUpdateCommand c ) {
        Collection<AnnotationValueObject> chars = c.getAnnotationValueObjects();
        log.info( "Delete " + chars.size() + " characteristics..." );

        Collection<Characteristic> asChars = convertToCharacteristic( chars );

        if ( asChars.size() == 0 ) {
            log.info( "No characteristic objects were received" );
            return new TaskResult( c, false );
        }

        Map<Characteristic, Object> charToParent = characteristicService.getParents( asChars );
        for ( Characteristic cFromClient : asChars ) {
            Characteristic cFromDatabase = characteristicService.load( cFromClient.getId() );
            Object parent = charToParent.get( cFromDatabase );
            removeFromParent( cFromDatabase, parent );
            characteristicService.delete( cFromDatabase );
            log.info( "Characteristic deleted: " + cFromDatabase + " (associated with " + parent + ")" );
        }
        return new TaskResult( c, true );

    }

    /**
     * @param command
     * @return
     */
    private TaskResult doUpdate( CharacteristicUpdateCommand command ) {
        Collection<AnnotationValueObject> avos = command.getAnnotationValueObjects();
        if ( avos.size() == 0 ) return new TaskResult( command, false );
        log.info( "Updating " + avos.size() + " characteristics or uncharacterized factor values..." );
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Characteristic> asChars = convertToCharacteristic( avos );
        Collection<FactorValue> factorValues = convertToFactorValuesWithCharacteristics( avos );

        if ( asChars.size() == 0 && factorValues.size() == 0 ) {
            log.info( "Nothing to update" );
            return new TaskResult( command, false );
        }

        for ( FactorValue factorValue : factorValues ) {
            factorValueService.update( factorValue );
        }

        if ( asChars.size() == 0 ) return new TaskResult( command, true );

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

            VocabCharacteristic vcFromClient = ( cFromClient instanceof VocabCharacteristic ) ? ( VocabCharacteristic ) cFromClient
                    : null;
            VocabCharacteristic vcFromDatabase = ( cFromDatabase instanceof VocabCharacteristic ) ? ( VocabCharacteristic ) cFromDatabase
                    : null;

            /*
             * if one of the characteristics is a VocabCharacteristic and the other is not, we have to change the
             * characteristic in the database so that it matches the one from the client; since we can't change the
             * class of the object, we have to delete the old characteristic and make a new one of the appropriate
             * class.
             */
            Object parent = charToParent.get( cFromDatabase );

            /*
             * Check needed because Characteristics are not securable.
             */
            if ( parent != null && Securable.class.isAssignableFrom( parent.getClass() )
                    && !securityService.isEditable( ( Securable ) parent ) ) {
                throw new AccessDeniedException( "Access is denied" );
            }

            if ( vcFromClient != null && vcFromDatabase == null ) {
                VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
                c.setValue( cFromDatabase.getValue() );
                c.setEvidenceCode( cFromDatabase.getEvidenceCode() );
                c.setDescription( cFromDatabase.getDescription() );
                c.setCategory( cFromDatabase.getCategory() );
                c.setName( cFromDatabase.getName() );

                vcFromDatabase = ( VocabCharacteristic ) characteristicService.create( c );

                removeFromParent( cFromDatabase, parent );
                characteristicService.delete( cFromDatabase );
                addToParent( vcFromDatabase, parent );
                cFromDatabase = vcFromDatabase;
            } else if ( vcFromClient == null && vcFromDatabase != null ) {
                cFromDatabase = characteristicService.create( Characteristic.Factory.newInstance(
                        vcFromDatabase.getValue(), vcFromDatabase.getCategory(), null, null, vcFromDatabase.getName(),
                        vcFromDatabase.getDescription() // don't copy AuditTrail or Status to avoid
                        // cascade
                        // error... vcFromDatabase.getAuditTrail()
                        , vcFromDatabase.getEvidenceCode() ) );
                removeFromParent( vcFromDatabase, parent );
                characteristicService.delete( vcFromDatabase );
                addToParent( cFromDatabase, parent );
            }

            /*
             * at this point, cFromDatabase points to the class-corrected characteristic in the database that must be
             * updated with the information coming from the client.
             */
            cFromDatabase.setValue( cFromClient.getValue() );
            cFromDatabase.setCategory( cFromClient.getCategory() );
            if ( cFromDatabase instanceof VocabCharacteristic ) {
                vcFromDatabase = ( VocabCharacteristic ) cFromDatabase;

                if ( vcFromClient != null ) {
                    if ( vcFromDatabase.getValueUri() == null || vcFromDatabase.getValueUri() == null
                            || !vcFromDatabase.getValueUri().equals( vcFromClient.getValueUri() ) ) {
                        log.info( "Characteristic value update: " + vcFromDatabase + " " + vcFromDatabase.getValueUri()
                                + " -> " + vcFromClient.getValueUri() + " associated with " + parent );
                        vcFromDatabase.setValueUri( vcFromClient.getValueUri() );
                    }

                    if ( vcFromDatabase.getCategory() == null || vcFromDatabase.getCategoryUri() == null
                            || !vcFromDatabase.getCategoryUri().equals( vcFromClient.getCategoryUri() ) ) {
                        log.info( "Characteristic category update: " + vcFromDatabase + " "
                                + vcFromDatabase.getCategoryUri() + " -> " + vcFromClient.getCategoryUri()
                                + " associated with " + parent );
                        vcFromDatabase.setCategoryUri( vcFromClient.getCategoryUri() );
                    }
                }
            }

            if ( cFromClient.getEvidenceCode() == null ) {
                cFromDatabase.setEvidenceCode( GOEvidenceCode.IC ); // characteristic has been manually updated
            } else {
                if ( !cFromDatabase.getEvidenceCode().equals( cFromClient.getEvidenceCode() ) ) {
                    log.info( "Characteristic evidence code update: " + cFromDatabase + " "
                            + cFromDatabase.getEvidenceCode() + " -> " + cFromClient.getEvidenceCode() );
                }
                cFromDatabase.setEvidenceCode( cFromClient.getEvidenceCode() ); // let them change it.
            }

            characteristicService.update( cFromDatabase );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Update took: " + timer.getTime() );
        }

        return new TaskResult( command, true );

    }

    /**
     * @param c
     * @param parent
     */
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
