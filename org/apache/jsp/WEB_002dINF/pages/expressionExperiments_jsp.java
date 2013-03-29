package org.apache.jsp.WEB_002dINF.pages;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class expressionExperiments_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(2);
    _jspx_dependants.add("/common/taglibs.jsp");
    _jspx_dependants.add("/WEB-INF/Gemma.tld");
  }

  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.release();
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

      out.write('\n');
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
      out.write('\n');
      out.write("\n");
      out.write("<head>\n");
      out.write("\t<title>");
      if (_jspx_meth_fmt_005fmessage_005f0(_jspx_page_context))
        return;
      out.write("\n");
      out.write("\t</title>\n");
      out.write("</head>\n");
      out.write("\n");
      out.write("<div id=\"messages\" style=\"margin: 10px; width: 400px\">\n");
      out.write("\t");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${message}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("\n");
      out.write("</div>\n");
      out.write("<div id=\"taskId\" style=\"display: none;\"></div>\n");
      out.write("<div id=\"progress-area\" style=\"padding: 15px;\"></div>\n");
      out.write("\n");
      out.write("<script type=\"text/javascript\">\n");
      out.write("\tExt.state.Manager.setProvider(new Ext.state.CookieProvider( ));\n");
      out.write("\tExt.onReady(function(){\n");
      out.write("\t\tExt.QuickTips.init();\n");
      out.write("\t\t// this is to overcome a vbox-collapse bug\n");
      out.write("\t\t// see overrides.js for more\n");
      out.write("\t\tfunction onExpandCollapse(c) {\n");
      out.write("\t\t\t\t//Horrible Ext 2.* collapse handling has to be defeated...\n");
      out.write("\t\t        if (c.queuedBodySize) {\n");
      out.write("\t\t            delete c.queuedBodySize.width;\n");
      out.write("\t\t            delete c.queuedBodySize.height;\n");
      out.write("\t\t        }\n");
      out.write("\t\t        var parent =c.findParentByType('panel');\n");
      out.write("\t\t        if(parent) {parent.doLayout();}\n");
      out.write("\t\t}\n");
      out.write("\t\t\n");
      out.write("\t\tvar summaryPanel = new Gemma.ExpressionExperimentsSummaryPanel({\n");
      out.write("\t\t\theight:280,\n");
      out.write("\t\t\tflex:'0',\n");
      out.write("\t\t\tlisteners: {\n");
      out.write("\t\t\t\texpand: onExpandCollapse,\n");
      out.write("\t\t\t\tcollapse: onExpandCollapse\n");
      out.write("\t\t\t}\n");
      out.write("\t\t});\n");
      out.write("\n");
      out.write("\t\tvar eeGrid = new Gemma.ExperimentPagingGrid({\n");
      out.write("\t\t \tflex:1,\n");
      out.write("\t\t\tlisteners: {\n");
      out.write("\t\t\t\texpand: onExpandCollapse,\n");
      out.write("\t\t\t\tcollapse: onExpandCollapse\n");
      out.write("\t\t\t}\n");
      out.write("\t\t});\n");
      out.write("\t\n");
      out.write("\t\tsummaryPanel.on('showExperimentsByIds', function(ids){\n");
      out.write("\t\t\teeGrid.loadExperimentsById(ids);\n");
      out.write("\t\t});\n");
      out.write("\t\tsummaryPanel.on('showExperimentsByTaxon', function(ids){\n");
      out.write("\t\t\teeGrid.loadExperimentsByTaxon(ids);\n");
      out.write("\t\t});\n");
      out.write("\t\n");
      out.write("\t\tvar mainPanel = new Ext.Panel({\n");
      out.write("\t\t\t\tlayout:'vbox',\n");
      out.write("\t\t\t\tlayoutConfig:{\n");
      out.write("\t\t\t\t\talign: 'stretch'\n");
      out.write("\t\t\t\t},\n");
      out.write("\t\t \t\talign: 'stretch',\n");
      out.write("\t\t \t\t\titems:[ summaryPanel, eeGrid]\n");
      out.write("\t\t\t \t});\n");
      out.write("\t\t\n");
      out.write("\t\tvar viewPort = new Gemma.GemmaViewPort({\n");
      out.write("\t\t \tcenterPanelConfig: mainPanel\n");
      out.write("\t\t});\n");
      out.write("\t\t\n");
      out.write("\t});\n");
      out.write("\n");
      out.write("\n");
      out.write("</script>\n");
      out.write("\n");
      out.write("<input type=\"hidden\" id=\"reloadOnLogout\" value=\"false\">");
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
    // /WEB-INF/pages/expressionExperiments.jsp(9,8) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f0.setKey("expressionExperiments.title");
    int _jspx_eval_fmt_005fmessage_005f0 = _jspx_th_fmt_005fmessage_005f0.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
    return false;
  }
}
