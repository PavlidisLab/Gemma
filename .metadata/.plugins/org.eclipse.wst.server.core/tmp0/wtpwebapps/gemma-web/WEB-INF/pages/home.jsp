<%@ include file="/common/taglibs.jsp"%>
<head>
<title>Home</title>

<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/HomePageAnalysisSearch.js' />
<jwr:script src='/scripts/scriptsnonjawr/arbor.js' />

</head>
<%@ include file="/WEB-INF/pages/frontPageSlideShowShowOff.jsp"%>

<div id="main" style="text-align: left">
	<%@ include file="/common/messages.jsp"%>
</div>

<script type="text/javascript">
   Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
   Ext.onReady(function() {

      Ext.QuickTips.init();

      Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
      // Apply a set of config properties to the singleton
      /*Ext.apply(Ext.QuickTips.getQuickTip(), {
      	maxWidth : 200,
      	minWidth : 100,
      	showDelay : 0,
      	//trackMouse: true,
      	dismissDelay : 0,
      	hideDelay : 0
      });*/

      var generalSearchPanel = new Gemma.Search.GeneralSearchSimple();
      generalSearchPanel.render("generalSearchSimple-div");
   });
</script>


<input type="hidden" id="reloadOnLogout" value="false">


<div style="padding-left: 0px" align="center">


	<div style="width: 900px; padding-bottom: 20px;" id="generalSearchSimple-div" align="left"></div>

	<div style="width: 900px">
		<div align="center">
			<div id="analysis-results-search-form-warnings" align="left"></div>
			<div id="analysis-results-search-form" align="left"></div>
			<br>

			<div id="tutorial-control-div" align="left"></div>
			<div id="analysis-results-search-form-messages" align="left"></div>
			<div id="analysis-results-search-form-results" align="left"></div>
		</div>
	</div>

	<div id="meta-heatmap-div" align="left"></div>


</div>
<div id="footer" class="clearfix">
	<div id="divider"></div>
	<div class="footer">
		<span class="left">Gemma &nbsp;&nbsp;${appConfig["version"]}&nbsp;&nbsp;&nbsp;Copyright &copy; 2007-2016 &nbsp;
			<a href='<c:url value="http://gemma-chibi-doc.sites.olt.ubc.ca/terms-and-conditions" />'>Terms and conditions</a>
		</span> &nbsp; &nbsp;
		<jsp:include page="/common/social.jsp" />
	</div>
</div>
<jsp:include page="/common/analytics.jsp" />

