package org.apache.jsp.WEB_002dINF.pages;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class generalSearch_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(2);
    _jspx_dependants.add("/common/taglibs.jsp");
    _jspx_dependants.add("/WEB-INF/Gemma.tld");
  }

  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.release();
    _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.release();
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=utf-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			"/WEB-INF/pages/error.jsp", true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write(" \n");
      out.write("\n");
      out.write(" \n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("<head>\n");
      out.write("\n");
      out.write("<title>");
      if (_jspx_meth_fmt_005fmessage_005f0(_jspx_page_context))
        return;
      out.write("</title>\n");
      out.write("\n");
      if (_jspx_meth_jwr_005fscript_005f0(_jspx_page_context))
        return;
      out.write('\n');
      if (_jspx_meth_jwr_005fscript_005f1(_jspx_page_context))
        return;
      out.write("\n");
      out.write("\n");
      out.write("<script type=\"text/javascript\">\n");
      out.write("\tExt.state.Manager.setProvider(new Ext.state.CookieProvider( ));\n");
      out.write("\tExt.onReady(function(){\n");
      out.write("\t\tExt.QuickTips.init();\n");
      out.write("\t\t\n");
      out.write("\t\tnew Gemma.GemmaViewPort({\n");
      out.write("\t\t \tcenterPanelConfig: new Gemma.Search.GeneralSearch()\n");
      out.write("\t\t});\n");
      out.write("\t});\n");
      out.write("\t</script>\n");
      out.write("</head>\n");
      out.write("<input type=\"hidden\" id=\"reloadOnLogout\" value=\"false\">\n");
      out.write("<div id=\"messages\"></div>\n");
      out.write("<div style=\"height: 1em; margin-bottom:\" id=\"validation-messages\"></div>\n");
      out.write("<div style=\"margin-bottom: 10px\" id=\"general-search-form\"></div>\n");
      out.write("<div style=\"margin: 5px\" id=\"search-bookmark\"></div>\n");
      out.write("<div style=\"margin-top: 2px\" id=\"search-results-grid\"></div>\n");
      out.write("\n");
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try { out.clearBuffer(); } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
        else log(t.getMessage(), t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }

  private boolean _jspx_meth_fmt_005fmessage_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f0 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f0.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f0.setParent(null);
    // /WEB-INF/pages/generalSearch.jsp(5,7) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f0.setKey("generalSearch.title");
    int _jspx_eval_fmt_005fmessage_005f0 = _jspx_th_fmt_005fmessage_005f0.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
    return false;
  }

  private boolean _jspx_meth_jwr_005fscript_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  jwr:script
    net.jawr.web.taglib.JavascriptBundleTag _jspx_th_jwr_005fscript_005f0 = (net.jawr.web.taglib.JavascriptBundleTag) _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.get(net.jawr.web.taglib.JavascriptBundleTag.class);
    _jspx_th_jwr_005fscript_005f0.setPageContext(_jspx_page_context);
    _jspx_th_jwr_005fscript_005f0.setParent(null);
    // /WEB-INF/pages/generalSearch.jsp(7,0) name = src type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_jwr_005fscript_005f0.setSrc("/scripts/ajax/ext/data/DwrProxy.js");
    int _jspx_eval_jwr_005fscript_005f0 = _jspx_th_jwr_005fscript_005f0.doStartTag();
    if (_jspx_th_jwr_005fscript_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.reuse(_jspx_th_jwr_005fscript_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.reuse(_jspx_th_jwr_005fscript_005f0);
    return false;
  }

  private boolean _jspx_meth_jwr_005fscript_005f1(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  jwr:script
    net.jawr.web.taglib.JavascriptBundleTag _jspx_th_jwr_005fscript_005f1 = (net.jawr.web.taglib.JavascriptBundleTag) _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.get(net.jawr.web.taglib.JavascriptBundleTag.class);
    _jspx_th_jwr_005fscript_005f1.setPageContext(_jspx_page_context);
    _jspx_th_jwr_005fscript_005f1.setParent(null);
    // /WEB-INF/pages/generalSearch.jsp(8,0) name = src type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_jwr_005fscript_005f1.setSrc("/scripts/app/search.js");
    int _jspx_eval_jwr_005fscript_005f1 = _jspx_th_jwr_005fscript_005f1.doStartTag();
    if (_jspx_th_jwr_005fscript_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.reuse(_jspx_th_jwr_005fscript_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.reuse(_jspx_th_jwr_005fscript_005f1);
    return false;
  }
}
