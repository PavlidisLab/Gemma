<%@ include file="/common/taglibs.jsp"%>
<head>
<title>Home</title>

<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/AnalysisResultsSearchNonWidget.js' />
<jwr:script src='/scripts/app/generalSearchSimple.js' />

<script type="text/javascript" src="<c:url value='/scriptsnonjawr/arbor.js'/>"></script>


</head>


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


<div style="padding-left: 0px">
	<div align="center">

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

		<%--  div style="width: 900px">
		<br>

		<table id="frontPageContent" align="left" style="text-align: left">
			<tr>
				<td
					style="font-size: 0.9em; vertical-align: top; padding-right: 10px">

					<div>
						<p>
							<a href="https://twitter.com/GemmaSoftware"
								class="twitter-follow-button" data-show-count="false">Follow
								@GemmaSoftware</a>
							<script src="http://platform.twitter.com/widgets.js"
								type="text/javascript">
							</script>

						</p>
					</div>
			</tr>
		</table>--%>
	</div>
</div>
<div id="footer" class="clearfix">
	<div id="divider"></div>
	<div class="footer">
		<span class="left">Gemma &nbsp;&nbsp;${appConfig["version"]}&nbsp;&nbsp;&nbsp;Copyright &copy; 2007-2013 &nbsp;
			<a href='<c:url value="http://gemma-chibi-doc.sites.olt.ubc.ca/terms-and-conditions" />'>Terms and conditions</a>
		</span>
	</div>
</div>
<jsp:include page="/common/analytics.jsp" />