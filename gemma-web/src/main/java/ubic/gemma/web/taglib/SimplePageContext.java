package ubic.gemma.web.taglib;

import javax.el.ELContext;
import javax.servlet.*;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import java.io.IOException;
import java.util.Enumeration;

@Deprecated
public class SimplePageContext extends PageContext {

    private final ServletContext servletContext;

    public SimplePageContext( ServletContext servletContext ) {
        this.servletContext = servletContext;
    }

    @Override
    public void initialize( Servlet servlet, ServletRequest request, ServletResponse response, String s, boolean b, int i, boolean b1 ) throws IOException, IllegalStateException, IllegalArgumentException {
    }

    @Override
    public void release() {
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public Object getPage() {
        return null;
    }

    @Override
    public ServletRequest getRequest() {
        return null;
    }

    @Override
    public ServletResponse getResponse() {
        return null;
    }

    @Override
    public Exception getException() {
        return null;
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void forward( String s ) throws ServletException, IOException {

    }

    @Override
    public void include( String s ) throws ServletException, IOException {

    }

    @Override
    public void include( String s, boolean b ) throws ServletException, IOException {

    }

    @Override
    public void handlePageException( Exception e ) {

    }

    @Override
    public void handlePageException( Throwable throwable ) {

    }

    @Override
    public void setAttribute( String s, Object o ) {

    }

    @Override
    public void setAttribute( String s, Object o, int i ) {

    }

    @Override
    public Object getAttribute( String s ) {
        return null;
    }

    @Override
    public Object getAttribute( String s, int i ) {
        return null;
    }

    @Override
    public Object findAttribute( String s ) {
        return null;
    }

    @Override
    public void removeAttribute( String s ) {

    }

    @Override
    public void removeAttribute( String s, int i ) {

    }

    @Override
    public int getAttributesScope( String s ) {
        return 0;
    }

    @Override
    public Enumeration<String> getAttributeNamesInScope( int i ) {
        return null;
    }

    @Override
    public JspWriter getOut() {
        return null;
    }

    @Override
    @Deprecated
    public ExpressionEvaluator getExpressionEvaluator() {
        return null;
    }

    @Override
    public ELContext getELContext() {
        return null;
    }

    @Override
    @Deprecated
    public VariableResolver getVariableResolver() {
        return null;
    }
}
