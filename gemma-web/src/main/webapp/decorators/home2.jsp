
<%-- Decorator for the home page --%>
<%-- $Id$ --%>

<%-- Include common set of tag library declarations for each layout --%>
<%@ include file="/common/taglibs.jsp"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
		<%-- Include common set of meta tags for each layout --%>
		<%@ include file="/common/meta.jsp"%>
		<title><decorator:title /> | <fmt:message key="webapp.name" /></title>

		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

		<jwr:style src="/bundles/gemma-all.css" />
		<jwr:script src="/bundles/include.js" />
		<jwr:script src="/bundles/gemma-lib.js" />	


		<decorator:head />
	</head>
	<body <decorator:getProperty property="body.id" writeEntireProperty="true"/>
		<decorator:getProperty property="body.class" writeEntireProperty="true"/>>

		<div id="page">

			<div id="homeheaderclear" class="clearfix">
				<jsp:include page="/common/header2.jsp" />
			</div>

			<div id="content" class="clearfix">

<div id="main" style="text-align:left">
	<%@ include file="/common/messages.jsp"%>
</div>
	
<%@ include file="/WEB-INF/pages/frontPageContent.jsp"%>

				<div id="nav" style="top: -105px; left: 150px; width:85%">
					<div class="wrapper" style="float:right; position:relative; right:35px; top:0px">
						<h2 class="accessibility">
							Navigation
						</h2>
						<jsp:include page="/WEB-INF/pages/menu.jsp" />
					</div>
					<hr />
				</div>


				<%-- end nav --%>



			</div>
			<%-- end content --%>

			<div id="footer" class="clearfix">
				<jsp:include page="/common/footer.jsp" />
			</div>
		</div>


<script type="text/javascript">
  var uservoiceOptions = {
    key: 'gemma',
    host: 'gemma.uservoice.com', 
    forum: '108909',
    alignment: 'left',
    background_color:'#3677a8', 
    text_color: 'white',
    hover_color: '#0066CC',
    lang: 'en',
    showTab: true
  };
  function _loadUserVoice() {
    var s = document.createElement('script');
    s.src = ("https:" == document.location.protocol ? "https://" : "http://") + "cdn.uservoice.com/javascripts/widgets/tab.js";
    document.getElementsByTagName('head')[0].appendChild(s);
  }
  _loadSuper = window.onload;
  window.onload = (typeof window.onload != 'function') ? _loadUserVoice : function() { _loadSuper(); _loadUserVoice(); };
</script>

	</body>
</html>
