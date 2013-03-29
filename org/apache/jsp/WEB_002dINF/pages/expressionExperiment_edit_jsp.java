package org.apache.jsp.WEB_002dINF.pages;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.commons.lang.StringUtils;

public final class expressionExperiment_edit_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(2);
    _jspx_dependants.add("/common/taglibs.jsp");
    _jspx_dependants.add("/WEB-INF/Gemma.tld");
  }

  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fescapeXml_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fchoose;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fotherwise;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fstep_005fend_005fbegin;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fspring_005fnestedPath_0026_005fpath;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fGemma_005fassayView_0026_005fexpressionExperiment_005fedit_005fnobody;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fescapeXml_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fchoose = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fotherwise = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fstep_005fend_005fbegin = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fspring_005fnestedPath_0026_005fpath = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fGemma_005fassayView_0026_005fexpressionExperiment_005fedit_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.release();
    _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.release();
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
    _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.release();
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.release();
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.release();
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fescapeXml_005fnobody.release();
    _005fjspx_005ftagPool_005fc_005fchoose.release();
    _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.release();
    _005fjspx_005ftagPool_005fc_005fotherwise.release();
    _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fstep_005fend_005fbegin.release();
    _005fjspx_005ftagPool_005fspring_005fnestedPath_0026_005fpath.release();
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.release();
    _005fjspx_005ftagPool_005fGemma_005fassayView_0026_005fexpressionExperiment_005fedit_005fnobody.release();
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
      out.write("<head>\n");
      out.write("\t");
      if (_jspx_meth_jwr_005fscript_005f0(_jspx_page_context))
        return;
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\t");
      if (_jspx_meth_jwr_005fscript_005f1(_jspx_page_context))
        return;
      out.write("\n");
      out.write("\n");
      out.write("\t<script type=\"text/javascript\">\n");
      out.write("\tExt.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';\n");
      out.write("\n");
      out.write("\tExt.onReady( function() {\n");
      out.write("\n");
      out.write("\t\tExt.QuickTips.init();\n");
      out.write("\t\tExt.state.Manager.setProvider(new Ext.state.CookieProvider());\n");
      out.write("\n");
      out.write("\t\tvar manager = new Gemma.EEManager( {\n");
      out.write("\t\t\teditable : true,\n");
      out.write("\t\t\tid : \"eemanager\"\n");
      out.write("\t\t});\n");
      out.write("\n");
      out.write("\t\tmanager.on('done', function() {\n");
      out.write("\t\t\twindow.location.reload(true);\n");
      out.write("\t\t});\n");
      out.write("\n");
      out.write("\t});\n");
      out.write("</script>\n");
      out.write("\n");
      out.write("\n");
      out.write("</head>\n");
      out.write("\n");
      ubic.gemma.web.controller.expression.experiment.ExpressionExperimentEditCommand expressionExperiment = null;
      synchronized (request) {
        expressionExperiment = (ubic.gemma.web.controller.expression.experiment.ExpressionExperimentEditCommand) _jspx_page_context.getAttribute("expressionExperiment", PageContext.REQUEST_SCOPE);
        if (expressionExperiment == null){
          expressionExperiment = new ubic.gemma.web.controller.expression.experiment.ExpressionExperimentEditCommand();
          _jspx_page_context.setAttribute("expressionExperiment", expressionExperiment, PageContext.REQUEST_SCOPE);
        }
      }
      out.write('\n');
      //  spring:bind
      org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f0 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
      _jspx_th_spring_005fbind_005f0.setPageContext(_jspx_page_context);
      _jspx_th_spring_005fbind_005f0.setParent(null);
      // /WEB-INF/pages/expressionExperiment.edit.jsp(33,0) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
      _jspx_th_spring_005fbind_005f0.setPath("expressionExperiment.*");
      int[] _jspx_push_body_count_spring_005fbind_005f0 = new int[] { 0 };
      try {
        int _jspx_eval_spring_005fbind_005f0 = _jspx_th_spring_005fbind_005f0.doStartTag();
        if (_jspx_eval_spring_005fbind_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
          org.springframework.web.servlet.support.BindStatus status = null;
          status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
          do {
            out.write('\n');
            out.write('	');
            if (_jspx_meth_c_005fif_005f0(_jspx_th_spring_005fbind_005f0, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f0))
              return;
            out.write('\n');
            int evalDoAfterBody = _jspx_th_spring_005fbind_005f0.doAfterBody();
            status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
            if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
              break;
          } while (true);
        }
        if (_jspx_th_spring_005fbind_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
          return;
        }
      } catch (Throwable _jspx_exception) {
        while (_jspx_push_body_count_spring_005fbind_005f0[0]-- > 0)
          out = _jspx_page_context.popBody();
        _jspx_th_spring_005fbind_005f0.doCatch(_jspx_exception);
      } finally {
        _jspx_th_spring_005fbind_005f0.doFinally();
        _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f0);
      }
      out.write("\n");
      out.write("\n");
      out.write("<title>");
      if (_jspx_meth_fmt_005fmessage_005f1(_jspx_page_context))
        return;
      out.write('\n');
      out.write('	');
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${expressionExperiment.shortName}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</title>\n");
      out.write("<form method=\"post\"\n");
      out.write("\taction=\"");
      if (_jspx_meth_c_005furl_005f1(_jspx_page_context))
        return;
      out.write("\">\n");
      out.write("\n");
      out.write("\n");
      out.write("\t<h2>\n");
      out.write("\t\tEditing:\n");
      out.write("\t\t<a\n");
      out.write("\t\t\thref=\"");
      if (_jspx_meth_c_005furl_005f2(_jspx_page_context))
        return;
      out.write('"');
      out.write('>');
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${expressionExperiment.shortName}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</a>\n");
      out.write("\t</h2>\n");
      out.write("\t<h3>\n");
      out.write("\t\tQuantitation Types\n");
      out.write("\t</h3>\n");
      out.write("\t");
      //  c:choose
      org.apache.taglibs.standard.tag.common.core.ChooseTag _jspx_th_c_005fchoose_005f0 = (org.apache.taglibs.standard.tag.common.core.ChooseTag) _005fjspx_005ftagPool_005fc_005fchoose.get(org.apache.taglibs.standard.tag.common.core.ChooseTag.class);
      _jspx_th_c_005fchoose_005f0.setPageContext(_jspx_page_context);
      _jspx_th_c_005fchoose_005f0.setParent(null);
      int _jspx_eval_c_005fchoose_005f0 = _jspx_th_c_005fchoose_005f0.doStartTag();
      if (_jspx_eval_c_005fchoose_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write('\n');
          out.write('	');
          out.write('	');
          //  c:when
          org.apache.taglibs.standard.tag.rt.core.WhenTag _jspx_th_c_005fwhen_005f0 = (org.apache.taglibs.standard.tag.rt.core.WhenTag) _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.WhenTag.class);
          _jspx_th_c_005fwhen_005f0.setPageContext(_jspx_page_context);
          _jspx_th_c_005fwhen_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f0);
          // /WEB-INF/pages/expressionExperiment.edit.jsp(61,2) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
          _jspx_th_c_005fwhen_005f0.setTest(expressionExperiment.getQuantitationTypes()
									.size() == 0);
          int _jspx_eval_c_005fwhen_005f0 = _jspx_th_c_005fwhen_005f0.doStartTag();
          if (_jspx_eval_c_005fwhen_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\n");
              out.write("\t\t\t\t\t\t\t\t\tNo quantitation types! Data may be corrupted (likely data import error)\n");
              out.write("\t\t");
              int evalDoAfterBody = _jspx_th_c_005fwhen_005f0.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fwhen_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f0);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fwhen_0026_005ftest.reuse(_jspx_th_c_005fwhen_005f0);
          out.write('\n');
          out.write('	');
          out.write('	');
          //  c:otherwise
          org.apache.taglibs.standard.tag.common.core.OtherwiseTag _jspx_th_c_005fotherwise_005f0 = (org.apache.taglibs.standard.tag.common.core.OtherwiseTag) _005fjspx_005ftagPool_005fc_005fotherwise.get(org.apache.taglibs.standard.tag.common.core.OtherwiseTag.class);
          _jspx_th_c_005fotherwise_005f0.setPageContext(_jspx_page_context);
          _jspx_th_c_005fotherwise_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fchoose_005f0);
          int _jspx_eval_c_005fotherwise_005f0 = _jspx_th_c_005fotherwise_005f0.doStartTag();
          if (_jspx_eval_c_005fotherwise_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
              out.write("\n");
              out.write("\t\t\t<table>\n");
              out.write("\t\t\t\t<tr>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tName\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tDesc\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tPref?\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tRatio?\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tBkg?\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tBkgSub?\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tNorm?\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tType\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tSpec.Type\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tScale\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t\t<th>\n");
              out.write("\t\t\t\t\t\tRep.\n");
              out.write("\t\t\t\t\t</th>\n");
              out.write("\t\t\t\t</tr>\n");
              out.write("\n");
              out.write("\n");
              out.write("\n");
              out.write("\t\t\t\t");
              //  c:forEach
              org.apache.taglibs.standard.tag.rt.core.ForEachTag _jspx_th_c_005fforEach_005f1 = (org.apache.taglibs.standard.tag.rt.core.ForEachTag) _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fstep_005fend_005fbegin.get(org.apache.taglibs.standard.tag.rt.core.ForEachTag.class);
              _jspx_th_c_005fforEach_005f1.setPageContext(_jspx_page_context);
              _jspx_th_c_005fforEach_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fotherwise_005f0);
              // /WEB-INF/pages/expressionExperiment.edit.jsp(106,4) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_c_005fforEach_005f1.setVar("index");
              // /WEB-INF/pages/expressionExperiment.edit.jsp(106,4) name = begin type = int reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_c_005fforEach_005f1.setBegin(0);
              // /WEB-INF/pages/expressionExperiment.edit.jsp(106,4) name = end type = int reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_c_005fforEach_005f1.setEnd(expressionExperiment.getQuantitationTypes()
										.size() - 1);
              // /WEB-INF/pages/expressionExperiment.edit.jsp(106,4) name = step type = int reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
              _jspx_th_c_005fforEach_005f1.setStep(1);
              int[] _jspx_push_body_count_c_005fforEach_005f1 = new int[] { 0 };
              try {
                int _jspx_eval_c_005fforEach_005f1 = _jspx_th_c_005fforEach_005f1.doStartTag();
                if (_jspx_eval_c_005fforEach_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                  do {
                    out.write("\n");
                    out.write("\t\t\t\t\t");
                    //  spring:nestedPath
                    org.springframework.web.servlet.tags.NestedPathTag _jspx_th_spring_005fnestedPath_005f0 = (org.springframework.web.servlet.tags.NestedPathTag) _005fjspx_005ftagPool_005fspring_005fnestedPath_0026_005fpath.get(org.springframework.web.servlet.tags.NestedPathTag.class);
                    _jspx_th_spring_005fnestedPath_005f0.setPageContext(_jspx_page_context);
                    _jspx_th_spring_005fnestedPath_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f1);
                    // /WEB-INF/pages/expressionExperiment.edit.jsp(110,5) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                    _jspx_th_spring_005fnestedPath_005f0.setPath((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("expressionExperiment.quantitationTypes[${index}]", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                    int[] _jspx_push_body_count_spring_005fnestedPath_005f0 = new int[] { 0 };
                    try {
                      int _jspx_eval_spring_005fnestedPath_005f0 = _jspx_th_spring_005fnestedPath_005f0.doStartTag();
                      if (_jspx_eval_spring_005fnestedPath_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                        java.lang.String nestedPath = null;
                        nestedPath = (java.lang.String) _jspx_page_context.findAttribute("nestedPath");
                        do {
                          out.write("\n");
                          out.write("\t\t\t\t\t\t<tr>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f1 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f1.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(114,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f1.setPath("name");
                          int[] _jspx_push_body_count_spring_005fbind_005f1 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f1 = _jspx_th_spring_005fbind_005f1.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input type=\"text\" size=\"20\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"");
                              if (_jspx_meth_c_005fout_005f1(_jspx_th_spring_005fbind_005f1, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f1))
                              return;
                              out.write("\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tvalue=\"");
                              if (_jspx_meth_c_005fout_005f2(_jspx_th_spring_005fbind_005f1, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f1))
                              return;
                              out.write("\" />\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f1.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f1[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f1.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f1.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f1);
                          }
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f2 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f2.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(121,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f2.setPath("description");
                          int[] _jspx_push_body_count_spring_005fbind_005f2 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f2 = _jspx_th_spring_005fbind_005f2.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input type=\"text\" size=\"35\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"");
                              if (_jspx_meth_c_005fout_005f3(_jspx_th_spring_005fbind_005f2, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f2))
                              return;
                              out.write("\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tvalue=\"");
                              if (_jspx_meth_c_005fout_005f4(_jspx_th_spring_005fbind_005f2, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f2))
                              return;
                              out.write("\" />\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f2.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f2[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f2.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f2.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f2);
                          }
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f3 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f3.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(128,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f3.setPath("isPreferred");
                          int[] _jspx_push_body_count_spring_005fbind_005f3 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f3 = _jspx_th_spring_005fbind_005f3.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input id=\"preferredCheckbox\" type=\"checkbox\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fif_005f1(_jspx_th_spring_005fbind_005f3, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f3))
                              return;
                              out.write(" />\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input type=\"hidden\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"_");
                              if (_jspx_meth_c_005fout_005f5(_jspx_th_spring_005fbind_005f3, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f3))
                              return;
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f3.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f3[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f3.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f3.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f3);
                          }
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f4 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f4.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f4.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(137,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f4.setPath("isRatio");
                          int[] _jspx_push_body_count_spring_005fbind_005f4 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f4 = _jspx_th_spring_005fbind_005f4.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input id=\"ratioCheckbox\" type=\"checkbox\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fif_005f2(_jspx_th_spring_005fbind_005f4, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f4))
                              return;
                              out.write(" />\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input type=\"hidden\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"_");
                              if (_jspx_meth_c_005fout_005f6(_jspx_th_spring_005fbind_005f4, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f4))
                              return;
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f4.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f4[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f4.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f4.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f4);
                          }
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f5 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f5.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f5.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(146,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f5.setPath("isBackground");
                          int[] _jspx_push_body_count_spring_005fbind_005f5 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f5 = _jspx_th_spring_005fbind_005f5.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f5 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input id=\"backgroundCheckbox\" type=\"checkbox\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fif_005f3(_jspx_th_spring_005fbind_005f5, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f5))
                              return;
                              out.write(" />\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input type=\"hidden\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"_");
                              if (_jspx_meth_c_005fout_005f7(_jspx_th_spring_005fbind_005f5, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f5))
                              return;
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f5.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f5[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f5.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f5.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f5);
                          }
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f6 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f6.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f6.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(155,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f6.setPath("isBackgroundSubtracted");
                          int[] _jspx_push_body_count_spring_005fbind_005f6 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f6 = _jspx_th_spring_005fbind_005f6.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f6 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input id=\"bkgsubCheckbox\" type=\"checkbox\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fif_005f4(_jspx_th_spring_005fbind_005f6, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f6))
                              return;
                              out.write(" />\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input type=\"hidden\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"_");
                              if (_jspx_meth_c_005fout_005f8(_jspx_th_spring_005fbind_005f6, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f6))
                              return;
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f6.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f6.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f6[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f6.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f6.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f6);
                          }
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f7 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f7.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f7.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(164,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f7.setPath("isNormalized");
                          int[] _jspx_push_body_count_spring_005fbind_005f7 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f7 = _jspx_th_spring_005fbind_005f7.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f7 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input id=\"normCheckbox\" type=\"checkbox\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fif_005f5(_jspx_th_spring_005fbind_005f7, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f7))
                              return;
                              out.write(" />\n");
                              out.write("\t\t\t\t\t\t\t\t\t<input type=\"hidden\"\n");
                              out.write("\t\t\t\t\t\t\t\t\t\tname=\"_");
                              if (_jspx_meth_c_005fout_005f9(_jspx_th_spring_005fbind_005f7, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f7))
                              return;
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f7.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f7.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f7[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f7.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f7.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f7);
                          }
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f8 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f8.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f8.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(173,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f8.setPath("generalType");
                          int[] _jspx_push_body_count_spring_005fbind_005f8 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f8 = _jspx_th_spring_005fbind_005f8.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f8 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<select name=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fforEach_005f2(_jspx_th_spring_005fbind_005f8, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f8))
                              return;
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t</select>\n");
                              out.write("\t\t\t\t\t\t\t\t\t<span class=\"fieldError\">");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.errorMessage}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("</span>\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f8.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f8.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f8[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f8.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f8.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f8);
                          }
                          out.write("\n");
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f9 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f9.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f9.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(187,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f9.setPath("type");
                          int[] _jspx_push_body_count_spring_005fbind_005f9 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f9 = _jspx_th_spring_005fbind_005f9.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f9 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<select name=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fforEach_005f3(_jspx_th_spring_005fbind_005f9, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f9))
                              return;
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t</select>\n");
                              out.write("\t\t\t\t\t\t\t\t\t<span class=\"fieldError\">");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.errorMessage}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("</span>\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f9.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f9.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f9[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f9.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f9.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f9);
                          }
                          out.write("\n");
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f10 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f10.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f10.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(201,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f10.setPath("scale");
                          int[] _jspx_push_body_count_spring_005fbind_005f10 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f10 = _jspx_th_spring_005fbind_005f10.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f10 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<select name=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fforEach_005f4(_jspx_th_spring_005fbind_005f10, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f10))
                              return;
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t</select>\n");
                              out.write("\t\t\t\t\t\t\t\t\t<span class=\"fieldError\">");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.errorMessage}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("</span>\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f10.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f10.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f10[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f10.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f10.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f10);
                          }
                          out.write("\n");
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t\t<td>\n");
                          out.write("\t\t\t\t\t\t\t\t");
                          //  spring:bind
                          org.springframework.web.servlet.tags.BindTag _jspx_th_spring_005fbind_005f11 = (org.springframework.web.servlet.tags.BindTag) _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.get(org.springframework.web.servlet.tags.BindTag.class);
                          _jspx_th_spring_005fbind_005f11.setPageContext(_jspx_page_context);
                          _jspx_th_spring_005fbind_005f11.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fnestedPath_005f0);
                          // /WEB-INF/pages/expressionExperiment.edit.jsp(215,8) name = path type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
                          _jspx_th_spring_005fbind_005f11.setPath("representation");
                          int[] _jspx_push_body_count_spring_005fbind_005f11 = new int[] { 0 };
                          try {
                            int _jspx_eval_spring_005fbind_005f11 = _jspx_th_spring_005fbind_005f11.doStartTag();
                            if (_jspx_eval_spring_005fbind_005f11 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                              org.springframework.web.servlet.support.BindStatus status = null;
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              do {
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t<select name=\"");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("\">\n");
                              out.write("\t\t\t\t\t\t\t\t\t\t");
                              if (_jspx_meth_c_005fforEach_005f5(_jspx_th_spring_005fbind_005f11, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f11))
                              return;
                              out.write("\n");
                              out.write("\t\t\t\t\t\t\t\t\t</select>\n");
                              out.write("\t\t\t\t\t\t\t\t\t<span class=\"fieldError\">");
                              out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.errorMessage}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
                              out.write("</span>\n");
                              out.write("\t\t\t\t\t\t\t\t");
                              int evalDoAfterBody = _jspx_th_spring_005fbind_005f11.doAfterBody();
                              status = (org.springframework.web.servlet.support.BindStatus) _jspx_page_context.findAttribute("status");
                              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                              break;
                              } while (true);
                            }
                            if (_jspx_th_spring_005fbind_005f11.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                              return;
                            }
                          } catch (Throwable _jspx_exception) {
                            while (_jspx_push_body_count_spring_005fbind_005f11[0]-- > 0)
                              out = _jspx_page_context.popBody();
                            _jspx_th_spring_005fbind_005f11.doCatch(_jspx_exception);
                          } finally {
                            _jspx_th_spring_005fbind_005f11.doFinally();
                            _005fjspx_005ftagPool_005fspring_005fbind_0026_005fpath.reuse(_jspx_th_spring_005fbind_005f11);
                          }
                          out.write("\n");
                          out.write("\n");
                          out.write("\t\t\t\t\t\t\t</td>\n");
                          out.write("\t\t\t\t\t\t</tr>\n");
                          out.write("\t\t\t\t\t");
                          int evalDoAfterBody = _jspx_th_spring_005fnestedPath_005f0.doAfterBody();
                          nestedPath = (java.lang.String) _jspx_page_context.findAttribute("nestedPath");
                          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                            break;
                        } while (true);
                      }
                      if (_jspx_th_spring_005fnestedPath_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                        return;
                      }
                    } catch (Throwable _jspx_exception) {
                      while (_jspx_push_body_count_spring_005fnestedPath_005f0[0]-- > 0)
                        out = _jspx_page_context.popBody();
                      _jspx_th_spring_005fnestedPath_005f0.doCatch(_jspx_exception);
                    } finally {
                      _jspx_th_spring_005fnestedPath_005f0.doFinally();
                      _005fjspx_005ftagPool_005fspring_005fnestedPath_0026_005fpath.reuse(_jspx_th_spring_005fnestedPath_005f0);
                    }
                    out.write("\n");
                    out.write("\t\t\t\t");
                    int evalDoAfterBody = _jspx_th_c_005fforEach_005f1.doAfterBody();
                    if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                      break;
                  } while (true);
                }
                if (_jspx_th_c_005fforEach_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                  return;
                }
              } catch (Throwable _jspx_exception) {
                while (_jspx_push_body_count_c_005fforEach_005f1[0]-- > 0)
                  out = _jspx_page_context.popBody();
                _jspx_th_c_005fforEach_005f1.doCatch(_jspx_exception);
              } finally {
                _jspx_th_c_005fforEach_005f1.doFinally();
                _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fstep_005fend_005fbegin.reuse(_jspx_th_c_005fforEach_005f1);
              }
              out.write("\n");
              out.write("\n");
              out.write("\t\t\t</table>\n");
              out.write("\t\t");
              int evalDoAfterBody = _jspx_th_c_005fotherwise_005f0.doAfterBody();
              if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
                break;
            } while (true);
          }
          if (_jspx_th_c_005fotherwise_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f0);
            return;
          }
          _005fjspx_005ftagPool_005fc_005fotherwise.reuse(_jspx_th_c_005fotherwise_005f0);
          out.write('\n');
          out.write('	');
          int evalDoAfterBody = _jspx_th_c_005fchoose_005f0.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fchoose_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f0);
        return;
      }
      _005fjspx_005ftagPool_005fc_005fchoose.reuse(_jspx_th_c_005fchoose_005f0);
      out.write("\n");
      out.write("\n");
      out.write("\t<h3>\n");
      out.write("\t\tBiomaterials and Assays\n");
      out.write("\t</h3>\n");
      out.write("\n");
      out.write("\t<p>\n");
      out.write("\t\t<a\n");
      out.write("\t\t\thref=\"");
      if (_jspx_meth_c_005furl_005f3(_jspx_page_context))
        return;
      out.write("\">\n");
      out.write("\t\t\tClick for details and QC</a>\n");
      out.write("\t</p>\n");
      out.write("\n");
      out.write("\t<p>\n");
      out.write("\t\t<input type=\"button\"\n");
      out.write("\t\t\tonClick=\"Ext.getCmp('eemanager').unmatchBioAssays(");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${expressionExperiment.id}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write(")\"\n");
      out.write("\t\t\tvalue=\"Unmatch all bioassays\" />\n");
      out.write("\t</p>\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\t");
      if (_jspx_meth_Gemma_005fassayView_005f0(_jspx_page_context))
        return;
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\t<script language=\"JavaScript\" type=\"text/javascript\">\n");
      out.write("\t// all the bioassays\n");
      out.write("\tvar dragItems = document.getElementsByClassName('dragItem');\n");
      out.write("\n");
      out.write("\tvar windowIdArray = new Array(dragItems.length);\n");
      out.write("\tfor (j = 0; j < dragItems.length; j++) {\n");
      out.write("\t\twindowIdArray[j] = dragItems[j].id;\n");
      out.write("\t}\n");
      out.write("\n");
      out.write("\tfor (i = 0; i < windowIdArray.length; i++) {\n");
      out.write("\t\tvar windowId = windowIdArray[i];\n");
      out.write("\t\t//set to be draggable\n");
      out.write("\t\tnew Draggable(windowId, {\n");
      out.write("\t\t\trevert :true,\n");
      out.write("\t\t\tghosting :true\n");
      out.write("\t\t});\n");
      out.write("\n");
      out.write("\t\t//set to be droppable, using scriptaculous framework, see dragdrop.js\n");
      out.write("\t\tDroppables\n");
      out.write("\t\t\t\t.add(\n");
      out.write("\t\t\t\t\t\twindowId,\n");
      out.write("\t\t\t\t\t\t{\n");
      out.write("\t\t\t\t\t\t\toverlap :'vertical',\n");
      out.write("\t\t\t\t\t\t\taccept :'dragItem',\n");
      out.write("\t\t\t\t\t\t\thoverclass :'drophover',\n");
      out.write("\t\t\t\t\t\t\tonDrop : function(element, droppableElement) {\n");
      out.write("\t\t\t\t\t\t\t\t// error check\n");
      out.write("\t\t\t\t\t\t\t// if between columns (ArrayDesigns), do not allow\n");
      out.write("\t\t\t\t\t\t\tif (element.getAttribute('arrayDesign') == droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t.getAttribute('arrayDesign')) {\n");
      out.write("\t\t\t\t\t\t\t\t// initialize variables\n");
      out.write("\t\t\t\t\t\t\t\tvar removeFromElement = element\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getAttribute('material');\n");
      out.write("\t\t\t\t\t\t\t\tvar removeFromDroppable = droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getAttribute('material');\n");
      out.write("\t\t\t\t\t\t\t\t// swap the assays\n");
      out.write("\t\t\t\t\t\t\t\tvar temp = element.getAttribute('assay');\n");
      out.write("\t\t\t\t\t\t\t\telement.setAttribute('assay', droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getAttribute('assay'));\n");
      out.write("\t\t\t\t\t\t\t\tdroppableElement.setAttribute('assay', temp);\n");
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\t// retrieve the JSON object and parse it\n");
      out.write("\t\t\t\t\t\t\t\tvar materialString = document\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getElementById('assayToMaterialMap').value;\n");
      out.write("\t\t\t\t\t\t\t\tvar materialMap = Ext.util.JSON\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.decode(materialString);\n");
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\t// write the new values into the materialMap\n");
      out.write("\t\t\t\t\t\t\t\tmaterialMap[element.getAttribute('assay')]\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.push(element.getAttribute('material'));\n");
      out.write("\t\t\t\t\t\t\t\tmaterialMap[droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getAttribute('assay')]\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.push(droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t\t\t\t.getAttribute('material'));\n");
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\t// remove the old values from the materialMap\n");
      out.write("\t\t\t\t\t\t\t\tvar elementToRemove;\n");
      out.write("\t\t\t\t\t\t\t\tfor (k = 0; k < materialMap[element\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getAttribute('assay')].length; k++) {\n");
      out.write("\t\t\t\t\t\t\t\t\tif (materialMap[element\n");
      out.write("\t\t\t\t\t\t\t\t\t\t\t.getAttribute('assay')][k] = removeFromElement) {\n");
      out.write("\t\t\t\t\t\t\t\t\t\telementToRemove = k;\n");
      out.write("\t\t\t\t\t\t\t\t\t\tbreak;\n");
      out.write("\t\t\t\t\t\t\t\t\t}\n");
      out.write("\t\t\t\t\t\t\t\t}\n");
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\tmaterialMap[element.getAttribute('assay')]\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.splice(k, 1);\n");
      out.write("\t\t\t\t\t\t\t\tfor (k = 0; k < materialMap[droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getAttribute('assay')].length; k++) {\n");
      out.write("\t\t\t\t\t\t\t\t\tif (materialMap[droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t\t\t.getAttribute('assay')][k] = removeFromDroppable) {\n");
      out.write("\t\t\t\t\t\t\t\t\t\telementToRemove = k;\n");
      out.write("\t\t\t\t\t\t\t\t\t\tbreak;\n");
      out.write("\t\t\t\t\t\t\t\t\t}\n");
      out.write("\t\t\t\t\t\t\t\t}\n");
      out.write("\t\t\t\t\t\t\t\tmaterialMap[droppableElement\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.getAttribute('assay')].splice(k, 1);\n");
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\t// serialize the JSON object\n");
      out.write("\t\t\t\t\t\t\t\tdocument.getElementById('assayToMaterialMap').value = Ext.util.JSON\n");
      out.write("\t\t\t\t\t\t\t\t\t\t.encode(materialMap);\n");
      out.write("\n");
      out.write("\t\t\t\t\t\t\t\t// swap inner HTML\n");
      out.write("\t\t\t\t\t\t\t\tvar content1 = element.innerHTML;\n");
      out.write("\t\t\t\t\t\t\t\tvar content2 = droppableElement.innerHTML;\n");
      out.write("\t\t\t\t\t\t\t\tdroppableElement.innerHTML = content1;\n");
      out.write("\t\t\t\t\t\t\t\telement.innerHTML = content2;\n");
      out.write("\t\t\t\t\t\t\t} else {\n");
      out.write("\t\t\t\t\t\t\t\tnew Effect.Highlight(droppableElement.id, {\n");
      out.write("\t\t\t\t\t\t\t\t\tdelay :0,\n");
      out.write("\t\t\t\t\t\t\t\t\tduration :0.25,\n");
      out.write("\t\t\t\t\t\t\t\t\tstartcolor :'#ff0000',\n");
      out.write("\t\t\t\t\t\t\t\t\tendcolor :'#ff0000'\n");
      out.write("\t\t\t\t\t\t\t\t});\n");
      out.write("\t\t\t\t\t\t\t\tnew Effect.Highlight(droppableElement.id, {\n");
      out.write("\t\t\t\t\t\t\t\t\tdelay :0.5,\n");
      out.write("\t\t\t\t\t\t\t\t\tduration :0.25,\n");
      out.write("\t\t\t\t\t\t\t\t\tstartcolor :'#ff0000',\n");
      out.write("\t\t\t\t\t\t\t\t\tendcolor :'#ff0000'\n");
      out.write("\t\t\t\t\t\t\t\t});\n");
      out.write("\t\t\t\t\t\t\t}\n");
      out.write("\t\t\t\t\t\t}\n");
      out.write("\t\t\t\t\t\t});\n");
      out.write("\t}\n");
      out.write("</script>\n");
      out.write("\n");
      out.write("\t<table>\n");
      out.write("\t\t<tr>\n");
      out.write("\t\t\t<td>\n");
      out.write("\t\t\t\t<input type=\"submit\" class=\"button\" name=\"save\"\n");
      out.write("\t\t\t\t\tvalue=\"");
      if (_jspx_meth_fmt_005fmessage_005f2(_jspx_page_context))
        return;
      out.write("\" />\n");
      out.write("\t\t\t\t<input type=\"submit\" class=\"button\" name=\"cancel\"\n");
      out.write("\t\t\t\t\tvalue=\"");
      if (_jspx_meth_fmt_005fmessage_005f3(_jspx_page_context))
        return;
      out.write("\" />\n");
      out.write("\t\t\t</td>\n");
      out.write("\t\t</tr>\n");
      out.write("\t</table>\n");
      out.write("\n");
      out.write("</form>");
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

  private boolean _jspx_meth_jwr_005fscript_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  jwr:script
    net.jawr.web.taglib.JavascriptBundleTag _jspx_th_jwr_005fscript_005f0 = (net.jawr.web.taglib.JavascriptBundleTag) _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.get(net.jawr.web.taglib.JavascriptBundleTag.class);
    _jspx_th_jwr_005fscript_005f0.setPageContext(_jspx_page_context);
    _jspx_th_jwr_005fscript_005f0.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(3,1) name = src type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_jwr_005fscript_005f0.setSrc("/scripts/json.js");
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
    // /WEB-INF/pages/expressionExperiment.edit.jsp(6,1) name = src type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_jwr_005fscript_005f1.setSrc("/scripts/ajax/ext/data/DwrProxy.js");
    int _jspx_eval_jwr_005fscript_005f1 = _jspx_th_jwr_005fscript_005f1.doStartTag();
    if (_jspx_th_jwr_005fscript_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.reuse(_jspx_th_jwr_005fscript_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005fjwr_005fscript_0026_005fsrc_005fnobody.reuse(_jspx_th_jwr_005fscript_005f1);
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f0, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f0)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f0);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(34,1) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f0.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${not empty status.errorMessages}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
    if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("\n");
        out.write("\t\t<div class=\"error\">\n");
        out.write("\t\t\t");
        if (_jspx_meth_c_005fforEach_005f0(_jspx_th_c_005fif_005f0, _jspx_page_context, _jspx_push_body_count_spring_005fbind_005f0))
          return true;
        out.write("\n");
        out.write("\t\t</div>\n");
        out.write("\t");
        int evalDoAfterBody = _jspx_th_c_005fif_005f0.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
    return false;
  }

  private boolean _jspx_meth_c_005fforEach_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fif_005f0, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f0)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:forEach
    org.apache.taglibs.standard.tag.rt.core.ForEachTag _jspx_th_c_005fforEach_005f0 = (org.apache.taglibs.standard.tag.rt.core.ForEachTag) _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.get(org.apache.taglibs.standard.tag.rt.core.ForEachTag.class);
    _jspx_th_c_005fforEach_005f0.setPageContext(_jspx_page_context);
    _jspx_th_c_005fforEach_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f0);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(36,3) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f0.setVar("error");
    // /WEB-INF/pages/expressionExperiment.edit.jsp(36,3) name = items type = java.lang.Object reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f0.setItems((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.errorMessages}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int[] _jspx_push_body_count_c_005fforEach_005f0 = new int[] { 0 };
    try {
      int _jspx_eval_c_005fforEach_005f0 = _jspx_th_c_005fforEach_005f0.doStartTag();
      if (_jspx_eval_c_005fforEach_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\n");
          out.write("\t\t\t\t<img src=\"");
          if (_jspx_meth_c_005furl_005f0(_jspx_th_c_005fforEach_005f0, _jspx_page_context, _jspx_push_body_count_c_005fforEach_005f0))
            return true;
          out.write("\"\n");
          out.write("\t\t\t\t\talt=\"");
          if (_jspx_meth_fmt_005fmessage_005f0(_jspx_th_c_005fforEach_005f0, _jspx_page_context, _jspx_push_body_count_c_005fforEach_005f0))
            return true;
          out.write("\" class=\"icon\" />\n");
          out.write("\t\t\t\t");
          if (_jspx_meth_c_005fout_005f0(_jspx_th_c_005fforEach_005f0, _jspx_page_context, _jspx_push_body_count_c_005fforEach_005f0))
            return true;
          out.write("\n");
          out.write("\t\t\t\t<br />\n");
          out.write("\t\t\t");
          int evalDoAfterBody = _jspx_th_c_005fforEach_005f0.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fforEach_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        return true;
      }
    } catch (Throwable _jspx_exception) {
      while (_jspx_push_body_count_c_005fforEach_005f0[0]-- > 0)
        out = _jspx_page_context.popBody();
      _jspx_th_c_005fforEach_005f0.doCatch(_jspx_exception);
    } finally {
      _jspx_th_c_005fforEach_005f0.doFinally();
      _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.reuse(_jspx_th_c_005fforEach_005f0);
    }
    return false;
  }

  private boolean _jspx_meth_c_005furl_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fforEach_005f0, PageContext _jspx_page_context, int[] _jspx_push_body_count_c_005fforEach_005f0)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:url
    org.apache.taglibs.standard.tag.rt.core.UrlTag _jspx_th_c_005furl_005f0 = (org.apache.taglibs.standard.tag.rt.core.UrlTag) _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.UrlTag.class);
    _jspx_th_c_005furl_005f0.setPageContext(_jspx_page_context);
    _jspx_th_c_005furl_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f0);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(37,14) name = value type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005furl_005f0.setValue("/images/iconWarning.gif");
    int _jspx_eval_c_005furl_005f0 = _jspx_th_c_005furl_005f0.doStartTag();
    if (_jspx_th_c_005furl_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fforEach_005f0, PageContext _jspx_page_context, int[] _jspx_push_body_count_c_005fforEach_005f0)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f0 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f0.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f0);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(38,10) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f0.setKey("icon.warning");
    int _jspx_eval_fmt_005fmessage_005f0 = _jspx_th_fmt_005fmessage_005f0.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f0);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fforEach_005f0, PageContext _jspx_page_context, int[] _jspx_push_body_count_c_005fforEach_005f0)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f0 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fescapeXml_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f0.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f0);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(39,4) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f0.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${error}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    // /WEB-INF/pages/expressionExperiment.edit.jsp(39,4) name = escapeXml type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f0.setEscapeXml(false);
    int _jspx_eval_c_005fout_005f0 = _jspx_th_c_005fout_005f0.doStartTag();
    if (_jspx_th_c_005fout_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fescapeXml_005fnobody.reuse(_jspx_th_c_005fout_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fescapeXml_005fnobody.reuse(_jspx_th_c_005fout_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f1(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f1 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f1.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f1.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(46,7) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f1.setKey("expressionExperiment.title");
    int _jspx_eval_fmt_005fmessage_005f1 = _jspx_th_fmt_005fmessage_005f1.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f1);
    return false;
  }

  private boolean _jspx_meth_c_005furl_005f1(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:url
    org.apache.taglibs.standard.tag.rt.core.UrlTag _jspx_th_c_005furl_005f1 = (org.apache.taglibs.standard.tag.rt.core.UrlTag) _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.UrlTag.class);
    _jspx_th_c_005furl_005f1.setPageContext(_jspx_page_context);
    _jspx_th_c_005furl_005f1.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(49,9) name = value type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005furl_005f1.setValue("/expressionExperiment/editExpressionExperiment.html");
    int _jspx_eval_c_005furl_005f1 = _jspx_th_c_005furl_005f1.doStartTag();
    if (_jspx_th_c_005furl_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f1);
    return false;
  }

  private boolean _jspx_meth_c_005furl_005f2(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:url
    org.apache.taglibs.standard.tag.rt.core.UrlTag _jspx_th_c_005furl_005f2 = (org.apache.taglibs.standard.tag.rt.core.UrlTag) _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.UrlTag.class);
    _jspx_th_c_005furl_005f2.setPageContext(_jspx_page_context);
    _jspx_th_c_005furl_005f2.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(55,9) name = value type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005furl_005f2.setValue((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005furl_005f2 = _jspx_th_c_005furl_005f2.doStartTag();
    if (_jspx_th_c_005furl_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f2);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f2);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f1(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f1, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f1)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f1 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f1.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f1);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(116,16) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f1.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f1 = _jspx_th_c_005fout_005f1.doStartTag();
    if (_jspx_th_c_005fout_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f1);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f2(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f1, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f1)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f2 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f2.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f1);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(117,17) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f2.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f2 = _jspx_th_c_005fout_005f2.doStartTag();
    if (_jspx_th_c_005fout_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f2);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f2);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f3(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f2, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f2)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f3 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f3.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f2);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(123,16) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f3.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f3 = _jspx_th_c_005fout_005f3.doStartTag();
    if (_jspx_th_c_005fout_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f3);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f3);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f4(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f2, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f2)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f4 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f4.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f4.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f2);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(124,17) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f4.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f4 = _jspx_th_c_005fout_005f4.doStartTag();
    if (_jspx_th_c_005fout_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f4);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f4);
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f1(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f3, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f3)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f1 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f1.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f3);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(131,10) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f1.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == true}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f1 = _jspx_th_c_005fif_005f1.doStartTag();
    if (_jspx_eval_c_005fif_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("checked=\"checked\"");
        int evalDoAfterBody = _jspx_th_c_005fif_005f1.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f1);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f1);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f5(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f3, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f3)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f5 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f5.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f5.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f3);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(133,17) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f5.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f5 = _jspx_th_c_005fout_005f5.doStartTag();
    if (_jspx_th_c_005fout_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f5);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f5);
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f2(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f4, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f4)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f2 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f2.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f4);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(140,10) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f2.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == true}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f2 = _jspx_th_c_005fif_005f2.doStartTag();
    if (_jspx_eval_c_005fif_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("checked=\"checked\"");
        int evalDoAfterBody = _jspx_th_c_005fif_005f2.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f2);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f2);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f6(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f4, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f4)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f6 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f6.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f6.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f4);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(142,17) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f6.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f6 = _jspx_th_c_005fout_005f6.doStartTag();
    if (_jspx_th_c_005fout_005f6.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f6);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f6);
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f3(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f5, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f5)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f3 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f3.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f5);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(149,10) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f3.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == true}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f3 = _jspx_th_c_005fif_005f3.doStartTag();
    if (_jspx_eval_c_005fif_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("checked=\"checked\"");
        int evalDoAfterBody = _jspx_th_c_005fif_005f3.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f3);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f3);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f7(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f5, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f5)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f7 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f7.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f7.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f5);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(151,17) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f7.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f7 = _jspx_th_c_005fout_005f7.doStartTag();
    if (_jspx_th_c_005fout_005f7.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f7);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f7);
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f4(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f6, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f6)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f4 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f4.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f4.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f6);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(158,10) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f4.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == true}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f4 = _jspx_th_c_005fif_005f4.doStartTag();
    if (_jspx_eval_c_005fif_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("checked=\"checked\"");
        int evalDoAfterBody = _jspx_th_c_005fif_005f4.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f4);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f4);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f8(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f6, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f6)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f8 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f8.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f8.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f6);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(160,17) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f8.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f8 = _jspx_th_c_005fout_005f8.doStartTag();
    if (_jspx_th_c_005fout_005f8.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f8);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f8);
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f5(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f7, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f7)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f5 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f5.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f5.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f7);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(167,10) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f5.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == true}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f5 = _jspx_th_c_005fif_005f5.doStartTag();
    if (_jspx_eval_c_005fif_005f5 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("checked=\"checked\"");
        int evalDoAfterBody = _jspx_th_c_005fif_005f5.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f5);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f5);
    return false;
  }

  private boolean _jspx_meth_c_005fout_005f9(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f7, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f7)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:out
    org.apache.taglibs.standard.tag.rt.core.OutTag _jspx_th_c_005fout_005f9 = (org.apache.taglibs.standard.tag.rt.core.OutTag) _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.OutTag.class);
    _jspx_th_c_005fout_005f9.setPageContext(_jspx_page_context);
    _jspx_th_c_005fout_005f9.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f7);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(169,17) name = value type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fout_005f9.setValue((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.expression}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005fout_005f9 = _jspx_th_c_005fout_005f9.doStartTag();
    if (_jspx_th_c_005fout_005f9.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f9);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fout_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005fout_005f9);
    return false;
  }

  private boolean _jspx_meth_c_005fforEach_005f2(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f8, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f8)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:forEach
    org.apache.taglibs.standard.tag.rt.core.ForEachTag _jspx_th_c_005fforEach_005f2 = (org.apache.taglibs.standard.tag.rt.core.ForEachTag) _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.get(org.apache.taglibs.standard.tag.rt.core.ForEachTag.class);
    _jspx_th_c_005fforEach_005f2.setPageContext(_jspx_page_context);
    _jspx_th_c_005fforEach_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f8);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(175,10) name = items type = java.lang.Object reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f2.setItems((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${generalQuantitationTypes}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    // /WEB-INF/pages/expressionExperiment.edit.jsp(175,10) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f2.setVar("type");
    int[] _jspx_push_body_count_c_005fforEach_005f2 = new int[] { 0 };
    try {
      int _jspx_eval_c_005fforEach_005f2 = _jspx_th_c_005fforEach_005f2.doStartTag();
      if (_jspx_eval_c_005fforEach_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t<option value=\"");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\"\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          if (_jspx_meth_c_005fif_005f6(_jspx_th_c_005fforEach_005f2, _jspx_page_context, _jspx_push_body_count_c_005fforEach_005f2))
            return true;
          out.write(">\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t</option>\n");
          out.write("\t\t\t\t\t\t\t\t\t\t");
          int evalDoAfterBody = _jspx_th_c_005fforEach_005f2.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fforEach_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        return true;
      }
    } catch (Throwable _jspx_exception) {
      while (_jspx_push_body_count_c_005fforEach_005f2[0]-- > 0)
        out = _jspx_page_context.popBody();
      _jspx_th_c_005fforEach_005f2.doCatch(_jspx_exception);
    } finally {
      _jspx_th_c_005fforEach_005f2.doFinally();
      _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.reuse(_jspx_th_c_005fforEach_005f2);
    }
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f6(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fforEach_005f2, PageContext _jspx_page_context, int[] _jspx_push_body_count_c_005fforEach_005f2)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f6 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f6.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f6.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f2);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(177,12) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f6.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == type}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f6 = _jspx_th_c_005fif_005f6.doStartTag();
    if (_jspx_eval_c_005fif_005f6 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("selected");
        int evalDoAfterBody = _jspx_th_c_005fif_005f6.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f6.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f6);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f6);
    return false;
  }

  private boolean _jspx_meth_c_005fforEach_005f3(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f9, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f9)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:forEach
    org.apache.taglibs.standard.tag.rt.core.ForEachTag _jspx_th_c_005fforEach_005f3 = (org.apache.taglibs.standard.tag.rt.core.ForEachTag) _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.get(org.apache.taglibs.standard.tag.rt.core.ForEachTag.class);
    _jspx_th_c_005fforEach_005f3.setPageContext(_jspx_page_context);
    _jspx_th_c_005fforEach_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f9);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(189,10) name = items type = java.lang.Object reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f3.setItems((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${standardQuantitationTypes}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    // /WEB-INF/pages/expressionExperiment.edit.jsp(189,10) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f3.setVar("type");
    int[] _jspx_push_body_count_c_005fforEach_005f3 = new int[] { 0 };
    try {
      int _jspx_eval_c_005fforEach_005f3 = _jspx_th_c_005fforEach_005f3.doStartTag();
      if (_jspx_eval_c_005fforEach_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t<option value=\"");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\"\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          if (_jspx_meth_c_005fif_005f7(_jspx_th_c_005fforEach_005f3, _jspx_page_context, _jspx_push_body_count_c_005fforEach_005f3))
            return true;
          out.write(">\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t</option>\n");
          out.write("\t\t\t\t\t\t\t\t\t\t");
          int evalDoAfterBody = _jspx_th_c_005fforEach_005f3.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fforEach_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        return true;
      }
    } catch (Throwable _jspx_exception) {
      while (_jspx_push_body_count_c_005fforEach_005f3[0]-- > 0)
        out = _jspx_page_context.popBody();
      _jspx_th_c_005fforEach_005f3.doCatch(_jspx_exception);
    } finally {
      _jspx_th_c_005fforEach_005f3.doFinally();
      _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.reuse(_jspx_th_c_005fforEach_005f3);
    }
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f7(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fforEach_005f3, PageContext _jspx_page_context, int[] _jspx_push_body_count_c_005fforEach_005f3)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f7 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f7.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f7.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f3);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(191,12) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f7.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == type}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f7 = _jspx_th_c_005fif_005f7.doStartTag();
    if (_jspx_eval_c_005fif_005f7 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("selected");
        int evalDoAfterBody = _jspx_th_c_005fif_005f7.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f7.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f7);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f7);
    return false;
  }

  private boolean _jspx_meth_c_005fforEach_005f4(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f10, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f10)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:forEach
    org.apache.taglibs.standard.tag.rt.core.ForEachTag _jspx_th_c_005fforEach_005f4 = (org.apache.taglibs.standard.tag.rt.core.ForEachTag) _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.get(org.apache.taglibs.standard.tag.rt.core.ForEachTag.class);
    _jspx_th_c_005fforEach_005f4.setPageContext(_jspx_page_context);
    _jspx_th_c_005fforEach_005f4.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f10);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(203,10) name = items type = java.lang.Object reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f4.setItems((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${scaleTypes}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    // /WEB-INF/pages/expressionExperiment.edit.jsp(203,10) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f4.setVar("type");
    int[] _jspx_push_body_count_c_005fforEach_005f4 = new int[] { 0 };
    try {
      int _jspx_eval_c_005fforEach_005f4 = _jspx_th_c_005fforEach_005f4.doStartTag();
      if (_jspx_eval_c_005fforEach_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t<option value=\"");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\"\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          if (_jspx_meth_c_005fif_005f8(_jspx_th_c_005fforEach_005f4, _jspx_page_context, _jspx_push_body_count_c_005fforEach_005f4))
            return true;
          out.write(">\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t</option>\n");
          out.write("\t\t\t\t\t\t\t\t\t\t");
          int evalDoAfterBody = _jspx_th_c_005fforEach_005f4.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fforEach_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        return true;
      }
    } catch (Throwable _jspx_exception) {
      while (_jspx_push_body_count_c_005fforEach_005f4[0]-- > 0)
        out = _jspx_page_context.popBody();
      _jspx_th_c_005fforEach_005f4.doCatch(_jspx_exception);
    } finally {
      _jspx_th_c_005fforEach_005f4.doFinally();
      _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.reuse(_jspx_th_c_005fforEach_005f4);
    }
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f8(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fforEach_005f4, PageContext _jspx_page_context, int[] _jspx_push_body_count_c_005fforEach_005f4)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f8 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f8.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f8.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f4);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(205,12) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f8.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == type}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f8 = _jspx_th_c_005fif_005f8.doStartTag();
    if (_jspx_eval_c_005fif_005f8 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("selected");
        int evalDoAfterBody = _jspx_th_c_005fif_005f8.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f8.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f8);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f8);
    return false;
  }

  private boolean _jspx_meth_c_005fforEach_005f5(javax.servlet.jsp.tagext.JspTag _jspx_th_spring_005fbind_005f11, PageContext _jspx_page_context, int[] _jspx_push_body_count_spring_005fbind_005f11)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:forEach
    org.apache.taglibs.standard.tag.rt.core.ForEachTag _jspx_th_c_005fforEach_005f5 = (org.apache.taglibs.standard.tag.rt.core.ForEachTag) _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.get(org.apache.taglibs.standard.tag.rt.core.ForEachTag.class);
    _jspx_th_c_005fforEach_005f5.setPageContext(_jspx_page_context);
    _jspx_th_c_005fforEach_005f5.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_spring_005fbind_005f11);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(217,10) name = items type = java.lang.Object reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f5.setItems((java.lang.Object) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${representations}", java.lang.Object.class, (PageContext)_jspx_page_context, null, false));
    // /WEB-INF/pages/expressionExperiment.edit.jsp(217,10) name = var type = java.lang.String reqTime = false required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fforEach_005f5.setVar("type");
    int[] _jspx_push_body_count_c_005fforEach_005f5 = new int[] { 0 };
    try {
      int _jspx_eval_c_005fforEach_005f5 = _jspx_th_c_005fforEach_005f5.doStartTag();
      if (_jspx_eval_c_005fforEach_005f5 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
        do {
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t<option value=\"");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\"\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          if (_jspx_meth_c_005fif_005f9(_jspx_th_c_005fforEach_005f5, _jspx_page_context, _jspx_push_body_count_c_005fforEach_005f5))
            return true;
          out.write(">\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t\t");
          out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${type}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
          out.write("\n");
          out.write("\t\t\t\t\t\t\t\t\t\t\t</option>\n");
          out.write("\t\t\t\t\t\t\t\t\t\t");
          int evalDoAfterBody = _jspx_th_c_005fforEach_005f5.doAfterBody();
          if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
            break;
        } while (true);
      }
      if (_jspx_th_c_005fforEach_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
        return true;
      }
    } catch (Throwable _jspx_exception) {
      while (_jspx_push_body_count_c_005fforEach_005f5[0]-- > 0)
        out = _jspx_page_context.popBody();
      _jspx_th_c_005fforEach_005f5.doCatch(_jspx_exception);
    } finally {
      _jspx_th_c_005fforEach_005f5.doFinally();
      _005fjspx_005ftagPool_005fc_005fforEach_0026_005fvar_005fitems.reuse(_jspx_th_c_005fforEach_005f5);
    }
    return false;
  }

  private boolean _jspx_meth_c_005fif_005f9(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fforEach_005f5, PageContext _jspx_page_context, int[] _jspx_push_body_count_c_005fforEach_005f5)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f9 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f9.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f9.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fforEach_005f5);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(219,12) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f9.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${status.value == type}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f9 = _jspx_th_c_005fif_005f9.doStartTag();
    if (_jspx_eval_c_005fif_005f9 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("selected");
        int evalDoAfterBody = _jspx_th_c_005fif_005f9.doAfterBody();
        if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)
          break;
      } while (true);
    }
    if (_jspx_th_c_005fif_005f9.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f9);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f9);
    return false;
  }

  private boolean _jspx_meth_c_005furl_005f3(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:url
    org.apache.taglibs.standard.tag.rt.core.UrlTag _jspx_th_c_005furl_005f3 = (org.apache.taglibs.standard.tag.rt.core.UrlTag) _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.UrlTag.class);
    _jspx_th_c_005furl_005f3.setPageContext(_jspx_page_context);
    _jspx_th_c_005furl_005f3.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(242,9) name = value type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005furl_005f3.setValue((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=${expressionExperiment.id}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
    int _jspx_eval_c_005furl_005f3 = _jspx_th_c_005furl_005f3.doStartTag();
    if (_jspx_th_c_005furl_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f3);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f3);
    return false;
  }

  private boolean _jspx_meth_Gemma_005fassayView_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  Gemma:assayView
    ubic.gemma.web.taglib.expression.experiment.AssayViewTag _jspx_th_Gemma_005fassayView_005f0 = (ubic.gemma.web.taglib.expression.experiment.AssayViewTag) _005fjspx_005ftagPool_005fGemma_005fassayView_0026_005fexpressionExperiment_005fedit_005fnobody.get(ubic.gemma.web.taglib.expression.experiment.AssayViewTag.class);
    _jspx_th_Gemma_005fassayView_005f0.setPageContext(_jspx_page_context);
    _jspx_th_Gemma_005fassayView_005f0.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(254,1) name = expressionExperiment type = null reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_Gemma_005fassayView_005f0.setExpressionExperiment((ubic.gemma.model.expression.experiment.ExpressionExperiment) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${expressionExperiment}", ubic.gemma.model.expression.experiment.ExpressionExperiment.class, (PageContext)_jspx_page_context, null, false));
    // /WEB-INF/pages/expressionExperiment.edit.jsp(254,1) name = edit type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_Gemma_005fassayView_005f0.setEdit("true");
    int _jspx_eval_Gemma_005fassayView_005f0 = _jspx_th_Gemma_005fassayView_005f0.doStartTag();
    if (_jspx_th_Gemma_005fassayView_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fGemma_005fassayView_0026_005fexpressionExperiment_005fedit_005fnobody.reuse(_jspx_th_Gemma_005fassayView_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fGemma_005fassayView_0026_005fexpressionExperiment_005fedit_005fnobody.reuse(_jspx_th_Gemma_005fassayView_005f0);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f2(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f2 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f2.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f2.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(369,12) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f2.setKey("button.save");
    int _jspx_eval_fmt_005fmessage_005f2 = _jspx_th_fmt_005fmessage_005f2.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f2);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f2);
    return false;
  }

  private boolean _jspx_meth_fmt_005fmessage_005f3(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  fmt:message
    org.apache.taglibs.standard.tag.rt.fmt.MessageTag _jspx_th_fmt_005fmessage_005f3 = (org.apache.taglibs.standard.tag.rt.fmt.MessageTag) _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.MessageTag.class);
    _jspx_th_fmt_005fmessage_005f3.setPageContext(_jspx_page_context);
    _jspx_th_fmt_005fmessage_005f3.setParent(null);
    // /WEB-INF/pages/expressionExperiment.edit.jsp(371,12) name = key type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_fmt_005fmessage_005f3.setKey("button.cancel");
    int _jspx_eval_fmt_005fmessage_005f3 = _jspx_th_fmt_005fmessage_005f3.doStartTag();
    if (_jspx_th_fmt_005fmessage_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f3);
      return true;
    }
    _005fjspx_005ftagPool_005ffmt_005fmessage_0026_005fkey_005fnobody.reuse(_jspx_th_fmt_005fmessage_005f3);
    return false;
  }
}
