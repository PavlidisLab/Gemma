package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class login_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(2);
    _jspx_dependants.add("/common/taglibs.jsp");
    _jspx_dependants.add("/WEB-INF/Gemma.tld");
  }

  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;
  private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
    _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.release();
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
      out.write('\n');
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("<html>\n");
      out.write("\t<head>\n");
      out.write("\t\t<title>Login</title>\n");
      out.write("\n");
      out.write("\t\t<script type=\"text/javascript\">\n");
      out.write("\t\t\n");
      out.write("\tExt.namespace('Gemma');\n");
      out.write("\tExt.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';\n");
      out.write("\n");
      out.write("\tExt.onReady( function()\n");
      out.write("\t{\n");
      out.write("\t\tExt.QuickTips.init();\n");
      out.write("\n");
      out.write("\t\tvar error = Ext.get('login_error_msg') ? Ext.get('login_error_msg').getValue() : \"\";\n");
      out.write("\t\t\n");
      out.write("\t\tvar login = new Ext.Panel({frame :false, title :'Login', width : 350, items:[new Ext.FormPanel(\n");
      out.write("\t\t{\n");
      out.write("\t\t\tlabelWidth :90,\n");
      out.write("\t\t\turl :'/Gemma/j_spring_security_check',\n");
      out.write("\t\t\tmethod :'POST',\n");
      out.write("\t\t\tid :'_loginForm',\n");
      out.write("\t\t\tstandardSubmit :true,\n");
      out.write("\t\t\tframe : true,\n");
      out.write("\t\t\tbodyStyle :'padding:5px 5px 0',\n");
      out.write("\t\t\ticonCls :'user-suit',\n");
      out.write("\t\t\twidth :350,\n");
      out.write("\t\t\tmonitorValid:true,\n");
      out.write("\t\t\tkeys:\n");
      out.write("\t\t\t[\n");
      out.write("\t\t\t\t{\n");
      out.write("\t\t\t\t\tkey: Ext.EventObject.ENTER,\n");
      out.write("\t\t\t\t\tfn: function()\n");
      out.write("\t\t\t        {\n");
      out.write("\t\t\t\t\t\tvar sb = Ext.getCmp('my-status');\n");
      out.write("\t\t\t\t\t\tsb.showBusy();\n");
      out.write("\t\t\t\t\t\tExt.getCmp(\"_loginForm\").getForm().submit();\n");
      out.write("\t\t\t        }\n");
      out.write("\t\t\t\t}\n");
      out.write("\t\t\t],\n");
      out.write("\t\t\tdefaults :\n");
      out.write("\t\t    {\n");
      out.write("\t\t\t\t//enter defaults here\n");
      out.write("\t\t\t\t//width :230 \n");
      out.write("\t\t\t\t\n");
      out.write("\t\t\t},\n");
      out.write("\t\t\tdefaultType :'textfield',\n");
      out.write("\t\t\titems :\n");
      out.write("\t\t\t[\n");
      out.write("\t\t\t\t{\n");
      out.write("\t\t\t\t\tfieldLabel :'Username',\n");
      out.write("\t\t\t\t\tname :'j_username',\n");
      out.write("\t\t\t\t\tid :'j_username',\n");
      out.write("\t\t\t\t\tallowBlank :false\n");
      out.write("\t\t\t\t},\n");
      out.write("\t\t\t\t{\n");
      out.write("\t\t\t\t\tfieldLabel :'Password',\n");
      out.write("\t\t\t\t\tname :'j_password',\n");
      out.write("\t\t\t\t\tid :'j_password',\n");
      out.write("\t\t\t\t\tallowBlank :false,\n");
      out.write("\t\t\t\t\tinputType :'password'\n");
      out.write("\t\t\t\t},{\n");
      out.write("\t\t\t\t\tfieldLabel : '<a href=\"/Gemma/passwordHint.html\">Forgot your password?</a>',\n");
      out.write("\t\t\t\t\tname :'passwordHint',\n");
      out.write("\t\t\t\t\tid :'passwordHint',\n");
      out.write("\t\t\t\t\tlabelSeparator:'',\n");
      out.write("\t\t\t\t\thidden : true\n");
      out.write("\t\t\t\t},{\n");
      out.write("\t\t\t\t\tfieldLabel: 'Remember Me',\n");
      out.write("\t\t\t\t\tboxLabel : 'rememberMe',\n");
      out.write("\t\t\t\t\t// defined in AbstractRememberMeServices.\n");
      out.write("\t\t\t\t\tid : '_spring_security_remember_me',\n");
      out.write("\t\t\t\t\tname : '_spring_security_remember_me',\n");
      out.write("\t\t\t\t\tinputType: 'checkbox'\n");
      out.write("\t\t\t\t}\n");
      out.write("\t\t\t\t\t\n");
      out.write("\t\t\t],\n");
      out.write("\t\t\t\n");
      out.write("\t\t\tbuttons :\n");
      out.write("\t\t\t\t\t[ {\n");
      out.write("\t\t\t\t\t\t\ttext :'Login',\n");
      out.write("\t\t\t\t\t\t\tformBind:true,\n");
      out.write("\t\t\t\t\t\t\ttype :'submit',\n");
      out.write("\t\t\t\t\t\t\tminWidth: 75,\n");
      out.write("\t\t\t\t\t\t\thandler :function()\n");
      out.write("\t\t\t        \t\t{\n");
      out.write("\t\t\t\t\t\t\t\tvar sb = Ext.getCmp('my-status');\n");
      out.write("\t\t\t\t\t\t\t\tsb.showBusy();\n");
      out.write("\t\t\t\t\t\t\t\tExt.getCmp(\"_loginForm\").getForm().submit();\n");
      out.write("\t\t\t        \t\t}\t\t\t\t\t\n");
      out.write("\t\t\t\t\t\t}\n");
      out.write("\t\t\t\t\t]})], // end of items for outer panel.\n");
      out.write("\t\t\t\t\t\n");
      out.write("\t\t   bbar: new Ext.ux.StatusBar(\n");
      out.write("\t\t\t{\n");
      out.write("\t\t\t\tid: 'my-status',\n");
      out.write("\t\t\t    text: '',\n");
      out.write("\t\t\t    iconCls: 'default-icon',\n");
      out.write("\t\t\t    busyText: 'Logging you in...',\n");
      out.write("\t\t\t    items:\n");
      out.write("\t\t\t\t\t[\n");
      out.write("\t\t\t\t\t\t'<div style=\"color: red; vertical-align: top; padding-right: 5px;\">' +error + '<br/></div>'\n");
      out.write("\t\t\t\t\t]\n");
      out.write("\t\t\t})\n");
      out.write("\t\t});\n");
      out.write("\t\tlogin.render(document.getElementById('_login'));\n");
      out.write("\t\t\n");
      out.write("\t});\n");
      out.write("\n");
      out.write("</script>\n");
      out.write("\t</head>\n");
      out.write("\t<body>\n");
      out.write("\t\t");
      if (_jspx_meth_c_005fif_005f0(_jspx_page_context))
        return;
      out.write("\n");
      out.write("\n");
      out.write("\t\t<p style='margin-left: 200px; width: 500; padding: 10px'>\n");
      out.write("\t\t\tUsers do not need to log on or register for many uses of Gemma. An\n");
      out.write("\t\t\taccount is only needed if you want to take advantage of data upload\n");
      out.write("\t\t\tor 'favorite search' and similar functionality.\n");
      out.write("\t\t\t<strong>Need an account? <a\n");
      out.write("\t\t\t\thref=\"");
      if (_jspx_meth_c_005furl_005f0(_jspx_page_context))
        return;
      out.write("\">Register</a> </strong>\n");
      out.write("\t\t</p>\n");
      out.write("\n");
      out.write("\t\t<div id=\"login-mask\" style=\"\"></div>\n");
      out.write("\t\t<div align=\"center\" id=\"login\">\n");
      out.write("\t\t\t<div id=\"_login\" class=\"login-indicator\"></div>\n");
      out.write("\t\t</div>\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\t</body>\n");
      out.write("</html>");
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

  private boolean _jspx_meth_c_005fif_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:if
    org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
    _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
    _jspx_th_c_005fif_005f0.setParent(null);
    // /login.jsp(119,2) name = test type = boolean reqTime = true required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005fif_005f0.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${not empty sessionScope[\"SPRING_SECURITY_LAST_EXCEPTION\"]}", java.lang.Boolean.class, (PageContext)_jspx_page_context, null, false)).booleanValue());
    int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
    if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
      do {
        out.write("\n");
        out.write("\t\t\t<input id=\"login_error_msg\" type=\"hidden\"\n");
        out.write("\t\t\t\tvalue='Error: ");
        out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${sessionScope[\"SPRING_SECURITY_LAST_EXCEPTION\"].message}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
        out.write("' />\n");
        out.write("\t\t");
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

  private boolean _jspx_meth_c_005furl_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  c:url
    org.apache.taglibs.standard.tag.rt.core.UrlTag _jspx_th_c_005furl_005f0 = (org.apache.taglibs.standard.tag.rt.core.UrlTag) _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.UrlTag.class);
    _jspx_th_c_005furl_005f0.setPageContext(_jspx_page_context);
    _jspx_th_c_005furl_005f0.setParent(null);
    // /login.jsp(130,10) name = value type = null reqTime = true required = false fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_c_005furl_005f0.setValue("register.html");
    int _jspx_eval_c_005furl_005f0 = _jspx_th_c_005furl_005f0.doStartTag();
    if (_jspx_th_c_005furl_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
      _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f0);
      return true;
    }
    _005fjspx_005ftagPool_005fc_005furl_0026_005fvalue_005fnobody.reuse(_jspx_th_c_005furl_005f0);
    return false;
  }
}
