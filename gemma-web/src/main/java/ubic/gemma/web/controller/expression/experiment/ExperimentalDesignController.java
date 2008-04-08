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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.util.GemmaLinkUtils;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="experimentalDesignController"
 * @spring.property name = "experimentalDesignService" ref="experimentalDesignService"
 * @spring.property name = "methodNameResolver" ref="experimentalDesignActions"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "bioMaterialService" ref="bioMaterialService"
 * @spring.property name = "experimentalFactorService" ref="experimentalFactorService"
 * @spring.property name = "factorValueService" ref="factorValueService"
 * @spring.property name = "characteristicService" ref="characteristicService"
 */
public class ExperimentalDesignController extends BaseMultiActionController {

    private ExperimentalDesignService experimentalDesignService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private BioMaterialService bioMaterialService = null;
    private ExperimentalFactorService experimentalFactorService = null;
    private FactorValueService factorValueService = null;
    private CharacteristicService characteristicService = null;

    private final String identifierNotFound = "Must provide a valid ExperimentalDesign identifier";

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExperimentalDesign experimentalDesign = experimentalDesignService.load( id );
        if ( experimentalDesign == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( experimentalDesign );

        if ( ee == null ) {
            throw new EntityNotFoundException( "Expression experiment  for design " + experimentalDesign + " not found" );
        }

        request.setAttribute( "id", id );

        ModelAndView mnv = new ModelAndView( "experimentalDesign.detail" );
        mnv.addObject( "experimentalDesign", experimentalDesign );
        mnv.addObject( "expressionExperiment", ee );
        mnv.addObject( "expressionExperimentUrl", GemmaLinkUtils.getExpressionExperimentUrl( ee.getId() ) );

        return mnv;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "experimentalDesigns" ).addObject( "experimentalDesigns", experimentalDesignService
                .loadAll() );
    }

    /**
     * Returns ExperimentalFactorValueObjects for each ExperimentalFactor in the ExperimentalDesign specified by the
     * EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalDesign
     * @return a collection of ExperimentalFactorValueObjects
     */
    public Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;
        ExperimentalDesign ed = this.experimentalDesignService.load( e.getId() );

        Collection<ExperimentalFactorValueObject> result = new HashSet<ExperimentalFactorValueObject>();
        for ( ExperimentalFactor factor : ed.getExperimentalFactors() ) {
            result.add( new ExperimentalFactorValueObject( factor ) );
        }
        return result;
    }

    /**
     * Creates a new ExperimentalFactor and adds it to the ExperimentalDesign specified by the EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalDesign
     * @param efvo an ExperimentalFactorValueObject representing the new ExperimentalFactor
     */
    public void createExperimentalFactor( EntityDelegator e, ExperimentalFactorValueObject efvo ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalDesign ed = experimentalDesignService.load( e.getId() );

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setExperimentalDesign( ed );
        ef.setName( efvo.getName() );
        ef.setDescription( efvo.getDescription() );
        ef.setCategory( createCategoryCharacteristic( efvo.getCategory(), efvo.getCategoryUri() ) );
        experimentalFactorService.create( ef ); // until the larger problem is fixed...

        if ( ed.getExperimentalFactors() == null ) ed.setExperimentalFactors( new HashSet<ExperimentalFactor>() );
        ed.getExperimentalFactors().add( ef );

        experimentalDesignService.update( ed );
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

    /**
     * Deletes the specified ExperimentalFactors and removes them from the ExperimentalDesign specified by the
     * EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalDesign
     * @param efIds a collection of ExperimentalFactor ids
     */
    public void deleteExperimentalFactors( EntityDelegator e, Collection<Long> efIds ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalDesign ed = this.experimentalDesignService.load( e.getId() );

        // First, remove the factorValues from the bioassays.
        ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ed );

        if ( ee == null ) {
            throw new IllegalArgumentException( "No expression experiment for experimental design " + ed );
        }

        expressionExperimentService.thawLite( ee );

        for ( Long efId : efIds ) {
            ExperimentalFactor ef = experimentalFactorService.load( efId );
            for ( BioAssay ba : ee.getBioAssays() ) {
                for ( BioMaterial bm : ba.getSamplesUsed() ) {
                    boolean removed = false;
                    for ( Iterator<FactorValue> fIt = bm.getFactorValues().iterator(); fIt.hasNext(); ) {
                        if ( fIt.next().getExperimentalFactor().equals( ef ) ) {
                            fIt.remove();
                            removed = true;
                        }
                    }
                    if ( removed ) {
                        bioMaterialService.update( bm );
                    }
                }
            }

            ed.getExperimentalFactors().remove( ef );
            experimentalFactorService.delete( ef );
        }
        experimentalDesignService.update( ed );
    }

    /**
     * Updates the specified ExperimentalFactors.
     * 
     * @param efvos a collection of ExperimentalFactorValueObjects containing the updated values
     */
    public void updateExperimentalFactors( Collection<ExperimentalFactorValueObject> efvos ) {
        for ( ExperimentalFactorValueObject efvo : efvos ) {
            ExperimentalFactor ef = experimentalFactorService.load( efvo.getId() );
            ef.setName( efvo.getName() );
            ef.setDescription( efvo.getDescription() );

            /*
             * at the moment, the characteristic is always going to be a VocabCharacteristic; if that changes, this will
             * have to...
             */
            VocabCharacteristic vc = ( VocabCharacteristic ) ef.getCategory();

            // VC can be null if this was imported from GEO etc.
            if ( vc == null ) {
                vc = VocabCharacteristic.Factory.newInstance();
                ef.setCategory( vc );
            }

            vc.setCategory( efvo.getCategory() );
            vc.setCategoryUri( efvo.getCategoryUri() );
            vc.setValue( efvo.getCategory() );
            vc.setValueUri( efvo.getCategoryUri() );

            experimentalFactorService.update( ef );
        }
    }

    /**
     * Returns FactorValueValueObjects for each FactorValue in the ExperimentalFactor specified by the EntityDelegator.
     * There will be one row per FactorValue.
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @return a collection of FactorValueValueObjects
     */
    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );

        Collection<FactorValueValueObject> result = new HashSet<FactorValueValueObject>();
        for ( FactorValue value : ef.getFactorValues() ) {
            Characteristic category = value.getExperimentalFactor().getCategory();
            if ( category == null ) {
                category = Characteristic.Factory.newInstance();
                category.setValue( value.getExperimentalFactor().getName() );
            }
            result.add( new FactorValueValueObject( value, category ) );
        }
        return result;
    }

    /**
     * Creates a new FactorValue and adds it to the ExperimentalFactor specified by the EntityDelegator. The new
     * FactorValue may have some initial Characteristics created to match any previously existing FactorValues for the
     * same ExperimentalFactor.
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     */
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
            } else {
                chars.add( createTemplateCharacteristic( ef.getCategory() ) );
            }
        }

        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        fv.setCharacteristics( chars );
        factorValueService.create( fv ); // until the larger problem is fixed...

        if ( ef.getFactorValues() == null ) ef.setFactorValues( new HashSet<FactorValue>() );
        ef.getFactorValues().add( fv );

        experimentalFactorService.update( ef );
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

    /**
     * Deletes the specified FactorValues and removes them from the ExperimentalFactor specified by the EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @param efIds a collection of FactorValue ids
     */
    public void deleteFactorValues( EntityDelegator e, Collection<Long> fvIds ) {
        if ( e == null || e.getId() == null ) return;
        ExperimentalFactor ef = experimentalFactorService.load( e.getId() );

        for ( Long fvId : fvIds ) {
            FactorValue fv = factorValueService.load( fvId );
            ef.getFactorValues().remove( fv );
            factorValueService.delete( fv );
        }
        experimentalFactorService.update( ef );
    }

    /**
     * Returns FactorValueValueObjects for each Characteristic belonging to a FactorValue in the ExperimentalFactor
     * specified by the EntityDelegator. There will be one row per Characteristic.
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @return a collection of FactorValueValueObjects
     */
    public Collection<FactorValueValueObject> getFactorValuesWithCharacteristics( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;
        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );

        Collection<FactorValueValueObject> result = new HashSet<FactorValueValueObject>();
        for ( FactorValue value : ef.getFactorValues() ) {
            for ( Characteristic c : value.getCharacteristics() ) {
                result.add( new FactorValueValueObject( value, c ) );
            }
        }
        return result;
    }

    /**
     * Creates a new Characteristic and adds it to the FactorValue specified by the EntityDelegator.
     * 
     * @param e an EntityDelegator representing a FactorValue
     */
    public void createFactorValueCharacteristic( EntityDelegator e, Characteristic c ) {
        if ( e == null || e.getId() == null ) return;
        FactorValue fv = factorValueService.load( e.getId() );

        if ( fv.getCharacteristics() == null ) fv.setCharacteristics( new HashSet<Characteristic>() );
        fv.getCharacteristics().add( c );

        factorValueService.update( fv );
    }

    /**
     * Deletes the specified Characteristics from their parent FactorValues.
     * 
     * @param fvvos a collection of FactorValueValueObjects containing the Characteristics to delete
     */
    public void deleteFactorValueCharacteristics( Collection<FactorValueValueObject> fvvos ) {
        for ( FactorValueValueObject fvvo : fvvos ) {
            FactorValue fv = factorValueService.load( fvvo.getFactorValueId() );
            Characteristic c = characteristicService.load( fvvo.getCharId() );
            fv.getCharacteristics().remove( c );
            characteristicService.delete( c );
            factorValueService.update( fv );
        }
    }

    /**
     * Updates the specified Characteristics.
     * 
     * @param efvos a collection of FactorValueValueObjects containing the updated values
     */
    public void updateFactorValueCharacteristics( Collection<FactorValueValueObject> fvvos ) {
        /*
         * TODO have this use the same code in CharacteristicBrowserController.updateCharacteristics, probably moving
         * that code to CharacteristicService.
         */
        for ( FactorValueValueObject fvvo : fvvos ) {
            Characteristic c = characteristicService.load( fvvo.getCharId() );
            c.setCategory( fvvo.getCategory() );
            c.setValue( fvvo.getValue() );
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                vc.setCategoryUri( fvvo.getCategoryUri() );
                vc.setValueUri( fvvo.getValueUri() );
            }
            c.setEvidenceCode( GOEvidenceCode.IC ); // characteristic has been manually updated
            characteristicService.update( c );
        }
    }

    /**
     * Returns BioMaterialValueObjects for each BioMaterial in the ExpressionExperiment specified by the
     * EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExpressionExperiment
     * @return a collection of BioMaterialValueObjects
     */
    public Collection<BioMaterialValueObject> getBioMaterials( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;
        ExpressionExperiment ee = expressionExperimentService.load( e.getId() );

        /*
         * TODO to get this information in a less redundant way requires two asynchronous DWR calls, so it's a bit of a
         * pain; definitely cleaner, though.
         */
        Map<String, String> factors = new HashMap<String, String>();
        Map<String, String> factorValues = new HashMap<String, String>();
        for ( ExperimentalFactor factor : ee.getExperimentalDesign().getExperimentalFactors() ) {
            factors.put( String.format( "factor%d", factor.getId() ), getExperimentalFactorString( factor ) );
            for ( FactorValue value : factor.getFactorValues() ) {
                factorValues.put( String.format( "fv%d", value.getId() ), getFactorValueString( value ) );
            }
        }

        Collection<BioMaterialValueObject> result = new HashSet<BioMaterialValueObject>();
        for ( BioAssay assay : ee.getBioAssays() ) {
            for ( BioMaterial sample : assay.getSamplesUsed() ) {
                BioMaterialValueObject bmvo = new BioMaterialValueObject( sample, assay );
                bmvo.setFactors( factors );
                bmvo.setFactorValues( factorValues );
                result.add( bmvo );
            }
        }
        return result;
    }

    /**
     * Updates the specified BioMaterials.
     * 
     * @param efvos a collection of BioMaterialValueObjects containing the updated values
     */
    public void updateBioMaterials( Collection<BioMaterialValueObject> bmvos ) {
        for ( BioMaterialValueObject bmvo : bmvos ) {
            BioMaterial bm = bioMaterialService.load( bmvo.getId() );
            Collection<FactorValue> values = new HashSet<FactorValue>();
            for ( String fvIdString : bmvo.getFactorIdToFactorValueId().values() ) {
                if ( fvIdString.matches( "fv\\d+" ) ) {
                    long fvId = Long.parseLong( fvIdString.substring( 2 ) );
                    values.add( factorValueService.load( fvId ) );
                }
            }
            bm.setFactorValues( values );
            bioMaterialService.update( bm );
        }
    }

    /**
     * @param factor
     * @return
     */
    private String getExperimentalFactorString( ExperimentalFactor factor ) {
        return factor.getName();
    }

    /**
     * @param value
     * @return
     */
    private String getFactorValueString( FactorValue value ) {
        /*
         * Note that normally we should not have 'blanks' in the factor values; this is just to make sure something
         * shows up if that happens.
         */
        StringBuffer buf = new StringBuffer();
        if ( value.getMeasurement() != null ) {
            if ( StringUtils.isBlank( value.getMeasurement().getValue() ) ) {
                return "[NA]";
            } else {
                return value.getMeasurement().getValue();
            }
        } else if ( value.getCharacteristics().size() > 0 ) {
            for ( Iterator<Characteristic> iter = value.getCharacteristics().iterator(); iter.hasNext(); ) {
                Characteristic c = iter.next();
                String category = c.getCategory();
                if ( category != null ) {
                    buf.append( category );
                    buf.append( ": " );
                }
                buf.append( StringUtils.isBlank( c.getValue() ) ? "[NA]" : c.getValue() );
                if ( iter.hasNext() ) buf.append( ", " );
            }
            return buf.length() > 0 ? buf.toString() : value.getValue() + " [Non-CSC]";
        } else {
            if ( StringUtils.isBlank( value.getValue() ) ) {
                return "[NA]";
            } else {
                return value.getValue();
            }
        }
    }

    /**
     * TODO add delete to the model
     * 
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView delete(HttpServletRequest request,
    // HttpServletResponse response) {
    // String name = request.getParameter("name");
    //
    // if (name == null) {
    // // should be a validation error.
    // throw new EntityNotFoundException("Must provide a name");
    // }
    //
    // ExperimentalDesign experimentalDesign = experimentalDesignService
    // .findByName(name);
    // if (experimentalDesign == null) {
    // throw new EntityNotFoundException(experimentalDesign
    // + " not found");
    // }
    //
    // return doDelete(request, experimentalDesign);
    // }
    /**
     * TODO add doDelete to the model
     * 
     * @param request
     * @param experimentalDesign
     * @return
     */
    // private ModelAndView doDelete(HttpServletRequest request,
    // ExperimentalDesign experimentalDesign) {
    // experimentalDesignService.delete(experimentalDesign);
    // log.info("Expression Experiment with name: "
    // + experimentalDesign.getName() + " deleted");
    // addMessage(request, "experimentalDesign.deleted",
    // new Object[] { experimentalDesign.getName() });
    // return new ModelAndView("experimentalDesigns",
    // "experimentalDesign", experimentalDesign);
    // }
    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param experimentalFactorService the experimentalFactorService to set
     */
    public void setExperimentalFactorService( ExperimentalFactorService experimentalFactorService ) {
        this.experimentalFactorService = experimentalFactorService;
    }

    /**
     * @param factorValueService the factorValueService to set
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

    /**
     * @param experimentalDesignService
     */
    public void setExperimentalDesignService( ExperimentalDesignService experimentalDesignService ) {
        this.experimentalDesignService = experimentalDesignService;
    }

    /**
     * @param characteristicService
     */
    public void setCharacteristicService( CharacteristicService characteristicService ) {
        this.characteristicService = characteristicService;
    }

}
