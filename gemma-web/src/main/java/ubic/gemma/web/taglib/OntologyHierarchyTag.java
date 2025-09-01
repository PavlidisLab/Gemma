package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.basecode.ontology.model.OntologyTerm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class OntologyHierarchyTag extends AbstractHtmlElementTag {

    @Setter
    private transient OntologyTerm term;

    private final OntologyResourceTag ontologyResourceTag = new OntologyResourceTag();

    @Override
    protected int doStartTagInternal() throws Exception {
        Assert.notNull( term, "An ontology term must be set." );

        ontologyResourceTag.setParent( this );
        ontologyResourceTag.setPageContext( this.pageContext );
        ontologyResourceTag.setHtmlEscape( isHtmlEscape() ? "true" : "false" );

        // this is one of the rare case where a linked list is actually useful since we're going to be adding elements
        // in reverse.
        List<Collection<OntologyTerm>> hierarchy = new LinkedList<>();
        Collection<OntologyTerm> p = term.getParents( true );
        if ( !p.isEmpty() ) {
            hierarchy.add( p );
            while ( true ) {
                Collection<OntologyTerm> terms = hierarchy.get( 0 );
                Collection<OntologyTerm> parents = terms.stream()
                        .map( t -> t.getParents( true ) )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
                if ( parents.isEmpty() ) {
                    break;
                }
                hierarchy.add( 0, parents );
            }
        }

        TagWriter tw = new TagWriter( this.pageContext );
        tw.startTag( "div" );

        writeOptionalAttributes( tw );

        for ( Collection<OntologyTerm> parents : hierarchy ) {
            tw.startTag( "ul" );
            tw.writeAttribute( "class", "list-styled" );
            for ( OntologyTerm parent : parents ) {
                tw.startTag( "li" );
                ontologyResourceTag.writeResource( parent, getRequestContext().getContextPath(), tw );
            }
        }

        tw.startTag( "ul" );
        tw.writeAttribute( "class", "list-styled" );
        tw.startTag( "li" );
        writeTerm( term, true, tw );
        tw.endTag(); // li
        tw.endTag(); // ul

        for ( Collection<OntologyTerm> parents : hierarchy ) {
            for ( OntologyTerm ignored : parents ) {
                tw.endTag(); // li
            }
            tw.endTag(); // ul
        }

        tw.endTag(); // div

        return SKIP_BODY;
    }

    private void writeTerm( OntologyTerm term, boolean bold, TagWriter writer ) throws Exception {
        if ( bold ) {
            writer.startTag( "strong" );
        } else {
            writer.startTag( "span" );
        }
        ontologyResourceTag.writeResource( term, getRequestContext().getContextPath(), writer );
        writer.endTag();
        Collection<OntologyTerm> children = term.getChildren( true );
        if ( !children.isEmpty() ) {
            writer.startTag( "ul" );
            writer.writeAttribute( "class", "list-styled" );
            for ( OntologyTerm c : term.getChildren( true ) ) {
                writer.startTag( "li" );
                writeTerm( c, false, writer );
                writer.endTag(); // li
            }
            writer.endTag(); // ul
        }
    }
}
