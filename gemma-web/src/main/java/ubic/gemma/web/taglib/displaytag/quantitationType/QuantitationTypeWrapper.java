/**
 * 
 */
package ubic.gemma.web.taglib.displaytag.quantitationType;

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * @author jsantos
 * @author paul
 * @version $Id$
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
        if ( qt.getIsPreferred() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";

    }

    /**
     * @return View for ratio of the QTs
     */
    public String getQtRatioStatus() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsRatio() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    /**
     * @return View for background status of the QTs
     */
    public String getQtBackground() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsBackground() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    /**
     * @return View for background subtracted status of the QTs
     */
    public String getQtBackgroundSubtracted() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsBackgroundSubtracted() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    /**
     * @return View for normalized status of the QTs
     */
    public String getQtNormalized() {
        QuantitationType qt = ( QuantitationType ) getCurrentRowObject();
        if ( qt.getIsNormalized() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    public String getData() {
        QuantitationType object = ( QuantitationType ) getCurrentRowObject();
        if ( object == null ) {
            return "-";
        }
        return "<a href=\"/Gemma/getData.html?qt=" + object.getId() + "\">Data</a>";
    }

}
