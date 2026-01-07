package ubic.gemma.web.taglib;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.util.Assert;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.ExternalDatabases;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * Render meta tags from a bibliographic reference.
 * <p>
 * We're following Google Scholar's guidelines available at <a href="https://scholar.google.com/intl/en/scholar/inclusion.html"> Inclusion Guidelines for Webmasters</a>.
 *
 * @author poirigui
 */
@CommonsLog
public class BibliographicReferenceMetaTag extends TagSupport {

    @Setter
    private BibliographicReference citation;

    @Override
    public int doStartTag() throws JspException {
        Assert.notNull( citation, "A citation must be set." );
        TagWriter writer = new TagWriter( pageContext );
        writeMetaTag( "citation_title", citation.getTitle(), writer );
        if ( citation.getAuthorList() != null ) {
            for ( String author : StringUtils.split( citation.getAuthorList(), ";" ) ) {
                writeMetaTag( "citation_author", StringUtils.strip( author ), writer );
            }
        }
        if ( citation.getPublicationDate() != null ) {
            writeMetaTag( "citation_publication_date", DateFormatUtils.format( citation.getPublicationDate(), "yyyy/MM/dd" ), writer );
        }
        writeMetaTag( "citation_journal_title", citation.getPublication(), writer );
        writeMetaTag( "citation_issue", citation.getIssue(), writer );
        writeMetaTag( "citation_volume", citation.getVolume(), writer );
        writeMetaTag( "citation_publisher", citation.getPublisher(), writer );
        if ( citation.getPubAccession() != null ) {
            // Not part of Google Scholar meta tags, but used by AltMetrics
            // https://help.altmetric.com/support/solutions/articles/6000240582-required-metadata-for-content-tracking
            if ( citation.getPubAccession().getExternalDatabase().getName().equalsIgnoreCase( ExternalDatabases.PUBMED ) ) {
                writeMetaTag( "citation_pmid", citation.getPubAccession().getAccession(), writer );
            } else if ( citation.getPubAccession().getExternalDatabase().getName().equalsIgnoreCase( ExternalDatabases.ARXIV ) ) {
                writeMetaTag( "citation_arxiv_id", citation.getPubAccession().getAccession(), writer );
            } else if ( citation.getPubAccession().getExternalDatabase().getName().equalsIgnoreCase( ExternalDatabases.BIORXIV ) ) {
                writeMetaTag( "citation_biorxiv_id", citation.getPubAccession().getAccession(), writer );
            } else {
                log.warn( "Unknown external database for bibliographic reference: " + citation.getPubAccession().getExternalDatabase() );
            }
        }
        return SKIP_BODY;
    }

    private void writeMetaTag( String name, String content, TagWriter writer ) throws JspException {
        if ( StringUtils.isBlank( content ) ) {
            return;
        }
        writer.startTag( "meta" );
        writer.writeAttribute( "name", name );
        writer.writeAttribute( "content", htmlEscape( content ) );
        writer.endTag();
    }
}
