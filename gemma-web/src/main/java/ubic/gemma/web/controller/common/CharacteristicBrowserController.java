/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AnchorTagUtil;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.expression.experiment.AnnotationValueObject;

/**
 * NOTE: Logging messages from this service are important for tracking changes to annotations.
 * 
 * @author luke
 * @author paul
 * @spring.bean id="characteristicBrowserController"
 * @spring.property name="formView" value="characteristics"
 * @spring.property name="characteristicService" ref="characteristicService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioMaterialService" ref="bioMaterialService"
 * @spring.property name="factorValueService" ref="factorValueService"
 * @spring.property name="ontologyService" ref="ontologyService"
 * @spring.property name="experimentalDesignService" ref="experimentalDesignService"
 */
public class CharacteristicBrowserController extends BaseFormController {

    private static Log specialLogger = LogFactory.getLog( CharacteristicBrowserController.class.getName() );

    CharacteristicService characteristicService;
    ExpressionExperimentService expressionExperimentService;
    BioMaterialService bioMaterialService;
    FactorValueService factorValueService;
    OntologyService ontologyService;
    ExperimentalDesignService experimentalDesignService;

    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        return new ModelAndView( this.getFormView() );
    }

    /**
     * @param valuePrefix
     * @return
     */
    public Collection<AnnotationValueObject> findCharacteristics( String valuePrefix ) {
        return findCharacteristics( valuePrefix, true, true, true, true, true );
    }

    /**
     * @param valuePrefix
     * @param searchNos
     * @param searchEEs
     * @param searchBMs
     * @param searchFVs
     * @param searchFFVs Search factor values that lack characteristics -- that is, search the factorValue.value.
     * @return
     */
    public Collection<AnnotationValueObject> findCharacteristics( String valuePrefix, boolean searchNos,
            boolean searchEEs, boolean searchBMs, boolean searchFVs, boolean searchFVVs ) {

        Collection<AnnotationValueObject> results = new HashSet<AnnotationValueObject>();
        if ( StringUtils.isBlank( valuePrefix ) ) {
            return results;
        }
        Collection<Characteristic> chars = characteristicService.findByValue( valuePrefix );
        Map<Characteristic, Object> charToParent = characteristicService.getParents( chars );
        for ( Object o : chars ) {
            Characteristic c = ( Characteristic ) o;
            Object parent = charToParent.get( c );
            if ( ( searchEEs && parent instanceof ExpressionExperiment )
                    || ( searchBMs && parent instanceof BioMaterial )
                    || ( searchFVs && ( parent instanceof FactorValue || parent instanceof ExperimentalFactor ) )
                    || ( searchNos && parent == null ) ) {
                AnnotationValueObject avo = new AnnotationValueObject();
                avo.setId( c.getId() );
                avo.setClassName( c.getCategory() );
                avo.setTermName( c.getValue() );

                if ( c.getEvidenceCode() != null ) avo.setEvidenceCode( c.getEvidenceCode().toString() );

                if ( c instanceof VocabCharacteristic ) {
                    VocabCharacteristic vc = ( VocabCharacteristic ) c;
                    avo.setClassUri( vc.getCategoryUri() );
                    avo.setTermUri( vc.getValueUri() );
                    avo.setObjectClass( VocabCharacteristic.class.getSimpleName() );
                } else {
                    avo.setObjectClass( Characteristic.class.getSimpleName() );
                }

                populateParentInformation( avo, parent );
                results.add( avo );
            }
        }

        if ( searchFVVs ) {
            Collection<FactorValue> factorValues = factorValueService.findByValue( valuePrefix );
            for ( FactorValue factorValue : factorValues ) {
                if ( factorValue.getCharacteristics().size() > 0 ) continue;
                if ( StringUtils.isBlank( factorValue.getValue() ) ) continue;

                AnnotationValueObject avo = new AnnotationValueObject();

                avo.setId( factorValue.getId() );
                avo.setTermName( factorValue.getValue() );

                avo.setObjectClass( FactorValue.class.getSimpleName() );

                populateParentInformation( avo, factorValue );

                results.add( avo );
            }
        }

        log.info( "Characteristic search for: '" + valuePrefix + "*': " + results.size() + " results" );
        return results;
    }

    /**
     * @param chars
     */
    public void removeCharacteristics( Collection<AnnotationValueObject> chars ) {
        specialLogger.info( "Delete " + chars.size() + " characteristics..." );

        Collection<Characteristic> asChars = convertToCharacteristic( chars );

        if ( asChars.size() == 0 ) {
            log.info( "No characteristic objects were received" );
            return;
        }

        Map<Characteristic, Object> charToParent = characteristicService.getParents( asChars );
        for ( Characteristic cFromClient : asChars ) {
            Characteristic cFromDatabase = characteristicService.load( cFromClient.getId() );
            Object parent = charToParent.get( cFromDatabase );
            removeFromParent( cFromDatabase, parent );
            characteristicService.delete( cFromDatabase );
            specialLogger.info( "Characteristic deleted: " + cFromDatabase + " (associated with " + parent + ")" );
        }
    }

    /**
     * Update characteristics associated with entities. This allows for the case of factor values that we are adding
     * characteristics to for the first time, but the most common case is altering existing characteristics.
     * 
     * @param avos
     */
    public void updateCharacteristics( Collection<AnnotationValueObject> avos ) {
        if ( avos.size() == 0 ) return;
        specialLogger.info( "Updating " + avos.size() + " characteristics or uncharacterized factor values..." );
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Characteristic> asChars = convertToCharacteristic( avos );
        Collection<FactorValue> factorValues = convertToFactorValuesWithCharacteristics( avos );

        if ( asChars.size() == 0 && factorValues.size() == 0 ) {
            log.info( "Nothing to update" );
            return;
        }

        for ( FactorValue factorValue : factorValues ) {
            factorValueService.update( factorValue );
        }

        if ( asChars.size() == 0 ) return;

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
            if ( vcFromClient != null && vcFromDatabase == null ) {
                vcFromDatabase = ( VocabCharacteristic ) characteristicService.create( VocabCharacteristic.Factory
                        .newInstance( null, null, cFromDatabase.getValue(), cFromDatabase.getCategory(), cFromDatabase
                                .getEvidenceCode(), cFromDatabase.getName(), cFromDatabase.getDescription(), null, null
                        /*
                         * don'tcopy AuditTrail to avoid cascade error...
                         */
                        // cFromDatabase.getAuditTrail()
                        ) );

                removeFromParent( cFromDatabase, parent );
                characteristicService.delete( cFromDatabase );
                addToParent( vcFromDatabase, parent );
                cFromDatabase = vcFromDatabase;
            } else if ( vcFromClient == null && vcFromDatabase != null ) {
                cFromDatabase = characteristicService.create( Characteristic.Factory.newInstance( vcFromDatabase
                        .getValue(), vcFromDatabase.getCategory(), vcFromDatabase.getEvidenceCode(), vcFromDatabase
                        .getName(), vcFromDatabase.getDescription(), null // don't copy AuditTrail to avoid cascade
                        // error... vcFromDatabase.getAuditTrail()
                        ) );
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
                        specialLogger.info( "Characteristic value update: " + vcFromDatabase + " "
                                + vcFromDatabase.getValueUri() + " -> " + vcFromClient.getValueUri()
                                + " associated with " + parent );
                        vcFromDatabase.setValueUri( vcFromClient.getValueUri() );
                    }

                    if ( vcFromDatabase.getCategory() == null || vcFromDatabase.getCategoryUri() == null
                            || !vcFromDatabase.getCategoryUri().equals( vcFromClient.getCategoryUri() ) ) {
                        specialLogger.info( "Characteristic category update: " + vcFromDatabase + " "
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
                    specialLogger.info( "Characteristic evidence code update: " + cFromDatabase + " "
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
            assert avo.getObjectClass() != null;

            if ( !avo.getObjectClass().equals( FactorValue.class.getSimpleName() ) ) continue;

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

    /**
     * Convert incombing AVOs into Characteristics (if the AVO objectClass is not FactorValue)
     * 
     * @param avos
     * @return
     */
    private Collection<Characteristic> convertToCharacteristic( Collection<AnnotationValueObject> avos ) {
        Collection<Characteristic> result = new HashSet<Characteristic>();
        for ( AnnotationValueObject avo : avos ) {
            if ( avo.getObjectClass() == null ) {
                // This should NOT happen...
                log.warn( "Null object class for object with id=" + avo.getId() + " (probably a characteristic)" );
                continue;
            }

            if ( avo.getObjectClass().equals( FactorValue.class.getSimpleName() ) ) continue;

            VocabCharacteristic vc = convertAvo2Characteristic( avo );

            result.add( vc );
        }
        return result;
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
     * @param c
     * @param parent
     */
    private void removeFromParent( Characteristic c, Object parent ) {
        if ( parent instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) parent;
            expressionExperimentService.thawLite( ee );
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

    private void addToParent( Characteristic c, Object parent ) {
        if ( parent instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) parent;
            expressionExperimentService.thawLite( ee );
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

    private void populateParentInformation( AnnotationValueObject avo, Object parent ) {
        if ( parent == null ) {
            avo.setParentLink( "[Orphan, " + avo.getObjectClass() + " ID=" + avo.getId() + "]" );
        } else if ( parent instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) parent;
            avo.setParentName( String.format( "Experiment: %s", ee.getName() ) );
            // avo.setParentDescription( ee.getDescription() );
            avo.setParentLink( AnchorTagUtil.getExpressionExperimentLink( ee.getId(), avo.getParentName() ) );
        } else if ( parent instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) parent;
            avo.setParentName( String.format( "BioMat: %s", bm.getName() ) );
            // avo.setParentDescription( bm.getDescription() );
            avo.setParentLink( AnchorTagUtil.getBioMaterialLink( bm.getId(), avo.getParentName() ) );
            ExpressionExperiment ee = expressionExperimentService.findByBioMaterial( bm );

            if ( ee != null ) {
                avo.setParentOfParentName( String.format( "%s", ee.getName() ) );
                // avo.setParentOfParentDescription( ee.getDescription() );
                avo.setParentOfParentLink( AnchorTagUtil.getExpressionExperimentLink( ee.getId(), avo
                        .getParentOfParentName() ) );
            } else {
                log.warn( "Expression experiment for " + bm + " was null" );
            }
        } else if ( parent instanceof FactorValue ) {
            FactorValue fv = ( FactorValue ) parent;
            avo.setParentDescription( String.format( "FactorValue: %s &laquo; Exp.Factor: %s",
                    ( fv.getValue() == null ? "" : ": " + fv.getValue() ), fv.getExperimentalFactor().getName() ) );
            // avo.setParentLink( AnchorTagUtil.getExperimentalDesignLink( fv.getExperimentalFactor()
            // .getExperimentalDesign().getId(), avo.getParentName() ) );
            ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( fv.getExperimentalFactor()
                    .getExperimentalDesign() );
            avo.setParentOfParentName( String.format( "Experimental Design for: %s", ee.getName() ) );
            // avo.setParentOfParentDescription( ee.getDescription() );
            avo.setParentOfParentLink( AnchorTagUtil.getExperimentalDesignLink( fv.getExperimentalFactor()
                    .getExperimentalDesign().getId(), avo.getParentName() )
                    + "&nbsp;&laquo;&nbsp;" + AnchorTagUtil.getExpressionExperimentLink( ee.getId(), ee.getName() ) );
        } else if ( parent instanceof ExperimentalFactor ) {
            ExperimentalFactor ef = ( ExperimentalFactor ) parent;
            // avo.setParentDescription( String.format( "ExperimentalFactor: %s  ", ef.getName() ) );
            avo.setParentLink( AnchorTagUtil.getExperimentalDesignLink( ef.getExperimentalDesign().getId(),
                    "Exp. Factor: " + ef.getName() ) );
            ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ef.getExperimentalDesign() );
            avo.setParentOfParentName( String.format( "%s", ee.getName() ) );
            // avo.setParentOfParentDescription( ee.getDescription() );
            avo.setParentOfParentLink( AnchorTagUtil.getExpressionExperimentLink( ee.getId(), avo
                    .getParentOfParentName() ) );
        }
    }

    /**
     * @param characteristicService the characteristicService to set
     */
    public void setCharacteristicService( CharacteristicService characteristicService ) {
        this.characteristicService = characteristicService;
    }

    /**
     * @param ontologyService the ontologyService to set
     */
    public void setOntologyService( OntologyService ontologyService ) {
        this.ontologyService = ontologyService;
    }

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param factorValueService the factorValueService to set
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

    /**
     * @param experimentalDesignService the experimentalDesignService to set
     */
    public void setExperimentalDesignService( ExperimentalDesignService experimentalDesignService ) {
        this.experimentalDesignService = experimentalDesignService;
    }

}
