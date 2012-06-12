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
package ubic.gemma.web.taglib;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.displaytag.tags.el.ExpressionEvaluator;

import ubic.gemma.util.LabelValue;

/**
 * Tag for creating multiple &lt;select&gt; options for displaying a list of country names.
 * 
 * @author Jens Fischer, Matt Raible
 * @author pavlidis
 * @version $Id$
 * @deprecated I don't think we use this anywhere.
 */
@Deprecated
public class CountryTag extends TagSupport {
    /**
     * 
     */
    private static final long serialVersionUID = 1578861231763990108L;
    // private static final String COUNTRIES = CountryTag.class.getName() + ".COUNTRIES";
    private String name;
    private String prompt;
    private String scope;
    private String selected;

    /**
     * @param name The name to set.
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @param prompt The prompt to set.
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public void setPrompt( String prompt ) {
        this.prompt = prompt;
    }

    /**
     * @param selected The selected option.
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public void setDefault( String selected ) {
        this.selected = selected;
    }

    /**
     * Property used to simply stuff the list of countries into a specified scope.
     * 
     * @param scope
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public void setToScope( String scope ) {
        this.scope = scope;
    }

    /**
     * Process the start of this tag.
     * 
     * @return
     * @exception JspException if a JSP exception has occurred
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        ExpressionEvaluator eval = new ExpressionEvaluator( this, pageContext );

        if ( selected != null ) {
            selected = eval.evalString( "default", selected );
        }

        Locale userLocale = pageContext.getRequest().getLocale();
        List<LabelValue> countries = this.buildCountryList( userLocale );

        if ( scope != null ) {
            if ( scope.equals( "page" ) ) {
                pageContext.setAttribute( name, countries );
            } else if ( scope.equals( "request" ) ) {
                pageContext.getRequest().setAttribute( name, countries );
            } else if ( scope.equals( "session" ) ) {
                pageContext.getSession().setAttribute( name, countries );
            } else if ( scope.equals( "application" ) ) {
                pageContext.getServletContext().setAttribute( name, countries );
            } else {
                throw new JspException( "Attribute 'scope' must be: page, request, session or application" );
            }
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append( "<select name=\"" + name + "\" id=\"" + name + "\">\n" );

            if ( prompt != null ) {
                sb.append( "    <option value=\"\" selected=\"selected\">" );
                sb.append( eval.evalString( "prompt", prompt ) + "</option>\n" );
            }

            for ( Iterator<LabelValue> i = countries.iterator(); i.hasNext(); ) {
                LabelValue country = i.next();
                sb.append( "    <option value=\"" + country.getValue() + "\"" );

                if ( ( selected != null ) && selected.equals( country.getValue() ) ) {
                    sb.append( " selected=\"selected\"" );
                }

                sb.append( ">" + country.getLabel() + "</option>\n" );
            }

            sb.append( "</select>" );

            try {
                pageContext.getOut().write( sb.toString() );
            } catch ( IOException io ) {
                throw new JspException( io );
            }
        }

        return super.doStartTag();
    }

    /**
     * Release aquired resources to enable tag reusage.
     * 
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    @Override
    public void release() {
        super.release();
    }

    /**
     * Build a List of LabelValues for all the available countries. Uses the two letter uppercase ISO name of the
     * country as the value and the localized country name as the label.
     * 
     * @param locale The Locale used to localize the country names.
     * @return List of LabelValues for all available countries.
     */
    protected List<LabelValue> buildCountryList( Locale locale ) {
        final String EMPTY = "";
        final Locale[] available = Locale.getAvailableLocales();

        List<LabelValue> countries = new ArrayList<LabelValue>();

        for ( int i = 0; i < available.length; i++ ) {
            final String iso = available[i].getCountry();
            final String localName = available[i].getDisplayCountry( locale );

            if ( !EMPTY.equals( iso ) && !EMPTY.equals( localName ) ) {
                LabelValue country = new LabelValue( localName, iso );

                if ( !countries.contains( country ) ) {
                    countries.add( new LabelValue( localName, iso ) );
                }
            }
        }

        Collections.sort( countries, new LabelValueComparator( locale ) );

        return countries;
    }

    /**
     * Class to compare LabelValues using their labels with locale-sensitive behaviour.
     */
    public static class LabelValueComparator implements Comparator<LabelValue> {
        private Comparator<Object> c;

        /**
         * Creates a new LabelValueComparator object.
         * 
         * @param locale The Locale used for localized String comparison.
         */
        public LabelValueComparator( Locale locale ) {
            c = Collator.getInstance( locale );
        }

        /**
         * Compares the localized labels of two LabelValues.
         * 
         * @param o1 The first LabelValue to compare.
         * @param o2 The second LabelValue to compare.
         * @return The value returned by comparing the localized labels.
         */
        @Override
        public final int compare( LabelValue o1, LabelValue o2 ) {
            LabelValue lhs = o1;
            LabelValue rhs = o2;

            return c.compare( lhs.getLabel(), rhs.getLabel() );
        }
    }
}
