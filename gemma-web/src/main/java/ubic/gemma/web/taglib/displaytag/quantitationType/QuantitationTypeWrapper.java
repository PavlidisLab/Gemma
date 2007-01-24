/**
 * 
 */
package ubic.gemma.web.taglib.displaytag.quantitationType;

import java.util.Map;

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * @author jsantos
 *
 */
public class QuantitationTypeWrapper extends TableDecorator {
    /**
     * @return View for quantitation type name.
     */
    public String getQtName() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        return qt.getName();
    }
    
    /**
     * @return View for status of the QTs
     */
    public String getQtPreferredStatus() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsPreferred()) {
            return "<input type=checkbox checked disabled></input>";
        }
        else {
            return "<input type=checkbox disabled></input>";
        }
    }
    
    /**
     * @return View for background status of the QTs
     */
    public String getQtBackground() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsBackground()) {
            return "<input type=checkbox checked disabled></input>";
        }
        else {
            return "<input type=checkbox disabled></input>";
        }
    }
    
    /**
     * @return View for background subtracted status of the QTs
     */
    public String getQtBackgroundSubtracted() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsBackgroundSubtracted()) {
            return "<input type=checkbox checked disabled></input>";
        }
        else {
            return "<input type=checkbox disabled></input>";
        }
    }
    
    /**
     * @return View for normalized status of the QTs
     */
    public String getQtNormalized() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsNormalized()) {
            return "<input type=checkbox checked disabled></input>";
        }
        else {
            return "<input type=checkbox disabled></input>";
        }
    }
    
}
