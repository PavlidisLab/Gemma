/**
 * 
 */
package ubic.gemma.web.taglib.common.description;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;

/**
 * @jsp.tag name="citation" body-content="empty"
 * @author joseph
 * @version
 */
public class BibliographicReferenceTag extends TagSupport {


    /**
     * 
     */
    private static final long serialVersionUID = -7325678534991860679L;
    

    private Log log = LogFactory.getLog( this.getClass() );

    private BibliographicReference citation;

    /**
     * @jsp.attribute description="BibliographicReference record for citation" required="true" rtexprvalue="true"
     * @param citation
     */
    public void setCitation(BibliographicReference citation  ) {
        this.citation = citation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {

        log.debug( "end tag" );

        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        log.debug( "start tag" );

        StringBuilder buf = new StringBuilder();
        if ( this.citation == null ) {
            buf.append( "No accession" );
        } else {
            String[] authors = StringUtils.split( citation.getAuthorList(), ";");
            // if there are authors, only display the first author
            if (authors.length == 0) {
                
            }
            else if (authors.length == 1) {
                buf.append( authors[0] + " " );
            }
            else {
                buf.append( authors[0] + " et al. " );     
            }
            
            // display the publish year
            Calendar pubDate =  new GregorianCalendar();
            pubDate.setTime( citation.getPublicationDate() );
            buf.append( "(" + pubDate.get( Calendar.YEAR ) + ") ");
            
            // add pubmed link
            if (citation.getPubAccession() != null ) {
                String pubMedId = citation.getPubAccession().getAccession();
                if (StringUtils.isNotBlank( pubMedId) ) {
                    String link = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&dopt=AbstractPlus&list_uids="+ pubMedId + "&query_hl=2&itool=pubmed_docsum";
            
                    buf.append( "<a target='_blank' href='" + link + "' ><img src='/Gemma/images/pubmed.gif' /> </a>" );          
                }
            }

        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( this.getClass().getName() + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}