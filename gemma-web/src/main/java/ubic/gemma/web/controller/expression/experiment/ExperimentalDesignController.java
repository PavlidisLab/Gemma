package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @version $Id$
 */
@RequestMapping("/experimentalDesign")
public interface ExperimentalDesignController {

    /**
     * AJAX
     * 
     * @param eeid
     * @param filePath
     */
    public abstract void createDesignFromFile( Long eeid, String filePath );

    /**
     * Creates a new ExperimentalFactor and adds it to the ExperimentalDesign specified by the EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalDesign
     * @param efvo an ExperimentalFactorValueObject representing the new ExperimentalFactor
     */
    public abstract void createExperimentalFactor( EntityDelegator e, ExperimentalFactorValueObject efvo );

    /**
     * Creates a new FactorValue and adds it to the ExperimentalFactor specified by the EntityDelegator. The new
     * FactorValue may have some initial Characteristics created to match any previously existing FactorValues for the
     * same ExperimentalFactor. Note that this applies only to 'categorical' variables. For continuous variables, you
     * merely set the value.
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     */
    public abstract void createFactorValue( EntityDelegator e );

    /**
     * Creates a new Characteristic and adds it to the FactorValue specified by the EntityDelegator.
     * 
     * @param e an EntityDelegator representing a FactorValue
     */
    public abstract void createFactorValueCharacteristic( EntityDelegator e, Characteristic c );

    /**
     * Deletes the specified ExperimentalFactors and removes them from the ExperimentalDesign specified by the
     * EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalDesign
     * @param efIds a collection of ExperimentalFactor ids
     */
    public abstract void deleteExperimentalFactors( EntityDelegator e, Long[] efIds );

    /**
     * Deletes the specified Characteristics from their parent FactorValues.
     * 
     * @param fvvos a collection of FactorValueValueObjects containing the Characteristics to delete
     */
    public abstract void deleteFactorValueCharacteristics( FactorValueValueObject[] fvvos );

    /**
     * Deletes the specified FactorValues and removes them from the ExperimentalFactor specified by the EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @param efIds a collection of FactorValue ids
     */
    public abstract void deleteFactorValues( EntityDelegator e, Long[] fvIds );

    /**
     * Returns BioMaterialValueObjects for each BioMaterial in the ExpressionExperiment specified by the
     * EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExpressionExperiment
     * @return a collection of BioMaterialValueObjects
     */
    public abstract Collection<BioMaterialValueObject> getBioMaterials( EntityDelegator e );

    /**
     * Returns ExperimentalFactorValueObjects for each ExperimentalFactor in the ExperimentalDesign or
     * ExpressionExperiment specified by the EntityDelegator.
     * 
     * @param e an EntityDelegator representing an ExperimentalDesign OR an ExpressionExperiment
     * @return a collection of ExperimentalFactorValueObjects
     */
    public abstract Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator e );

    /**
     * Returns FactorValueValueObjects for each FactorValue in the ExperimentalFactor specified by the EntityDelegator.
     * There will be one row per FactorValue
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @return a collection of FactorValueValueObjects
     */
    public abstract Collection<FactorValueValueObject> getFactorValues( EntityDelegator e );

    /**
     * Returns FactorValueValueObjects for each Characteristic belonging to a FactorValue in the ExperimentalFactor
     * specified by the EntityDelegator. There will be one row per Characteristic.
     * 
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @return a collection of FactorValueValueObjects
     */
    public abstract Collection<FactorValueValueObject> getFactorValuesWithCharacteristics( EntityDelegator e );

    /**
     * @param request with either 'eeid' (expression experiment id) or 'edid' (experimental design id)
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showExperimentalDesign.html")
    public abstract ModelAndView show( HttpServletRequest request, HttpServletResponse response );

    /**
     * Updates the specified BioMaterials's factor values. This completely removes any pre-existing factor values.
     * 
     * @param bmvos a collection of BioMaterialValueObjects containing the updated values
     */
    public abstract void updateBioMaterials( BioMaterialValueObject[] bmvos );

    /**
     * Updates the specified ExperimentalFactors.
     * 
     * @param efvos a collection of ExperimentalFactorValueObjects containing the updated values
     */
    public abstract void updateExperimentalFactors( ExperimentalFactorValueObject[] efvos );

    /**
     * Updates the specified Characteristics.
     * 
     * @param efvos a collection of FactorValueValueObjects containing the updated values
     */
    public abstract void updateFactorValueCharacteristics( FactorValueValueObject[] fvvos );

    public String clearDesignCaches( Long eeId );

}