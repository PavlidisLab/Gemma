package ubic.gemma.web.controller.expression.experiment;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.web.remote.EntityDelegator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * Note: do not use parametrized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 */
@SuppressWarnings("unused") // Used in front end
@RequestMapping("/experimentalDesign")
public interface ExperimentalDesignController {

    void createDesignFromFile( Long eeid, String filePath );

    /**
     * Creates a new ExperimentalFactor and adds it to the ExperimentalDesign specified by the EntityDelegator.
     *
     * @param e    an EntityDelegator representing an ExperimentalDesign
     * @param efvo an ExperimentalFactorValueObject representing the new ExperimentalFactor
     */
    void createExperimentalFactor( EntityDelegator<ExperimentalDesign> e, ExperimentalFactorValueObject efvo );

    /**
     * Creates a new FactorValue and adds it to the ExperimentalFactor specified by the EntityDelegator. The new
     * FactorValue may have some initial Characteristics created to match any previously existing FactorValues for the
     * same ExperimentalFactor. Note that this applies only to 'categorical' variables. For continuous variables, you
     * merely set the value.
     *
     * @param e an EntityDelegator representing an ExperimentalFactor
     */
    void createFactorValue( EntityDelegator<ExperimentalFactor> e );

    /**
     * Creates a new Characteristic and adds it to the FactorValue specified by the EntityDelegator.
     *
     * @param e an EntityDelegator representing a FactorValue
     */
    void createFactorValueCharacteristic( EntityDelegator<FactorValue> e, StatementValueObject c );

    /**
     * Deletes the specified ExperimentalFactors and removes them from the ExperimentalDesign specified by the
     * EntityDelegator.
     *
     * @param e     an EntityDelegator representing an ExperimentalDesign
     * @param efIds a collection of ExperimentalFactor ids
     */
    void deleteExperimentalFactors( EntityDelegator<ExperimentalDesign> e, Long[] efIds );

    /**
     * Deletes the specified Characteristics from their parent FactorValues.
     *
     * @param fvvos a collection of FactorValueValueObjects containing the Characteristics to remove
     */
    void deleteFactorValueCharacteristics( FactorValueValueObject[] fvvos );

    /**
     * Deletes the specified FactorValues and removes them from the ExperimentalFactor specified by the EntityDelegator.
     *
     * @param e     an EntityDelegator representing an ExperimentalFactor
     * @param fvIds a collection of FactorValue ids
     */
    void deleteFactorValues( EntityDelegator<ExperimentalFactor> e, Long[] fvIds );

    /**
     * Returns BioMaterialValueObjects for each BioMaterial in the ExpressionExperiment specified by the
     * EntityDelegator.
     *
     * @param e an EntityDelegator representing an ExpressionExperiment
     * @return a collection of BioMaterialValueObjects
     */
    Collection<BioMaterialValueObject> getBioMaterials( EntityDelegator<ExpressionExperiment> e );

    /**
     * Returns ExperimentalFactorValueObjects for each ExperimentalFactor in the ExperimentalDesign or
     * ExpressionExperiment specified by the EntityDelegator.
     *
     * @param e an EntityDelegator representing an ExperimentalDesign OR an ExpressionExperiment
     * @return a collection of ExperimentalFactorValueObjects
     */
    Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator<?> e );

    /**
     * Returns FactorValueValueObjects for each FactorValue in the ExperimentalFactor specified by the EntityDelegator.
     * There will be one row per FactorValue
     *
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @return a collection of FactorValueValueObjects
     */
    Collection<FactorValueValueObject> getFactorValues( EntityDelegator<ExperimentalFactor> e );

    /**
     * Returns FactorValueValueObjects for each Characteristic belonging to a FactorValue in the ExperimentalFactor
     * specified by the EntityDelegator. There will be one row per Characteristic.
     *
     * @param e an EntityDelegator representing an ExperimentalFactor
     * @return a collection of FactorValueValueObjects
     */
    Collection<FactorValueValueObject> getFactorValuesWithCharacteristics( EntityDelegator<ExperimentalFactor> e );

    /**
     * @param request  with either 'eeid' (expression experiment id) or 'edid' (experimental design id)
     * @param response response
     * @return ModelAndView
     */
    @RequestMapping("/showExperimentalDesign.html")
    ModelAndView show( HttpServletRequest request, HttpServletResponse response );

    /**
     * Updates the specified BioMaterials's factor values. This completely removes any pre-existing factor values.
     *
     * @param bmvos a collection of BioMaterialValueObjects containing the updated values
     */
    void updateBioMaterials( BioMaterialValueObject[] bmvos );

    /**
     * Updates the specified ExperimentalFactors.
     *
     * @param efvos a collection of ExperimentalFactorValueObjects containing the updated values
     */
    void updateExperimentalFactors( ExperimentalFactorValueObject[] efvos );

    /**
     * Updates the specified Characteristics.
     *
     * @param fvvos a collection of FactorValueValueObjects containing the updated values
     */
    void updateFactorValueCharacteristics( FactorValueValueObject[] fvvos );

    /**
     * Mark the given {@link FactorValue} as needs attention.
     */
    void markFactorValueAsNeedsAttention( EntityDelegator<ExpressionExperiment> ee, EntityDelegator<FactorValue> fv, String note );
}