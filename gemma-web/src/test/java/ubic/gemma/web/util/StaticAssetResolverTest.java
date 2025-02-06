package ubic.gemma.web.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockPageContext;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.web.taglib.ImageTag;
import ubic.gemma.web.taglib.ScriptTag;
import ubic.gemma.web.taglib.StyleTag;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ContextConfiguration
public class StaticAssetResolverTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class StaticAssetServerTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer(
                    "gemma.staticAssetServer.enabled=true",
                    "gemma.staticAssetServer.baseUrl=http://localhost:8082",
                    "gemma.staticAssetServer.allowedDirs=/bundles/,/fonts/,/images/,/scripts/,/styles/" );
        }

        @Bean
        public StaticAssetResolver staticAssetServer() {
            return new StaticAssetResolver();
        }
    }

    @Autowired
    private StaticAssetResolver staticAssetResolver;

    @Autowired
    private ServletContext servletContext;

    @Test
    public void test() {
        assertThat( staticAssetResolver.resolveUrl( "/bundles/include.js" ) )
                .isEqualTo( "http://localhost:8082/bundles/include.js" );
        assertThatThrownBy( () -> staticAssetResolver.resolveUrl( "/include.js" ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testScriptTag() throws JspException, UnsupportedEncodingException {
        MockPageContext pageContext = new MockPageContext( servletContext );
        ScriptTag tag = new ScriptTag();
        tag.setPageContext( pageContext );
        tag.setSrc( "/bundles/include.js" );
        tag.doStartTag();
        tag.doEndTag();
        assertThat( pageContext.getContentAsString() ).isEqualTo( "<script src=\"http://localhost:8082/bundles/include.js\"></script>" );
    }

    @Test
    public void testStyleTag() throws JspException, UnsupportedEncodingException {
        MockPageContext pageContext = new MockPageContext( servletContext );
        StyleTag tag = new StyleTag();
        tag.setPageContext( pageContext );
        tag.setSrc( "/bundles/gemma-all.css" );
        tag.doStartTag();
        tag.doEndTag();
        assertThat( pageContext.getContentAsString() ).isEqualTo( "<link href=\"http://localhost:8082/bundles/gemma-all.css\" rel=\"stylesheet\"/>" );
    }

    @Test
    public void testImageTags() throws JspException, UnsupportedEncodingException {
        MockPageContext pageContext = new MockPageContext( servletContext );
        ImageTag tag = new ImageTag();
        tag.setPageContext( pageContext );
        tag.setSrc( "/images/test.png" );
        tag.doStartTag();
        tag.doEndTag();
        assertThat( pageContext.getContentAsString() ).isEqualTo( "<img src=\"http://localhost:8082/images/test.png\"/>" );
    }
}