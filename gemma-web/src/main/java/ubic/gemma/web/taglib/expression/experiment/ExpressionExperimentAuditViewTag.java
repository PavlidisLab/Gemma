/**
 * 
 */
package ubic.gemma.web.taglib.expression.experiment;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Used to display the audit trail information for a EE.
 * 
 * @jsp.tag name="eeAudit" body-content="empty"
 * @author Paul
 * @version $Id$
 */
public class ExpressionExperimentAuditViewTag extends TagSupport {

    /**
     * 
     */
    private static final long serialVersionUID = 1478714878857705718L;
    private ExpressionExperiment expressionExperiment;

    /**
     * @param expressionExperiment
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    @Override
    public int doStartTag() throws JspException {
        StringBuilder buf = new StringBuilder();

        if ( expressionExperiment.getAuditTrail() == null ) {
            buf.append( "[No analysis info]" );
        } else {
            boolean hasAnalyses = false;
            buf.append( "<table><tr><th>Analysis</th><th>Date</th><th>Notes</th></tr>" );
            for ( AuditEvent event : expressionExperiment.getAuditTrail().getEvents() ) {
                if ( event == null ) continue; // legacy of ordered-list which could end up with gaps; should not be
                                               // needed any more
                if ( event.getEventType() != null && event.getEventType() instanceof ExpressionExperimentAnalysisEvent ) {
                    buf.append( "<td>" + event.getEventType() + "</td>" );
                    buf.append( "<td>" + event.getDate() + "</td>" );
                    buf.append( "<td>" + event.getNote() + "</td>" );
                    hasAnalyses = true;
                }
            }
            buf.append( "</table>" );

            if ( !hasAnalyses ) {
                buf = new StringBuilder();
                buf.append( "[No analysis info]" );
            }
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "audit trail view tag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}
