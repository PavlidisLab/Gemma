<!--  Default header for any page but the main gemma home page -->
<%@ include file="/common/taglibs.jsp"%>
<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<c:if test="${pageContext.request.locale.language != 'en'}">
	<div id="switchLocale">
		<a href="<c:url value='/home.html?locale=en'/>"><fmt:message key="webapp.name" /> in English</a>
	</div>
</c:if>

<c:if test="${appConfig['maintenanceMode']}">
	<div style="font-weight: bold; color: #AA4444; font-size: 1.3em">
		Gemma is undergoing maintenance! Some functions may not be available.
	</div>
</c:if>

<div id="extheaderandnavigation" class="clearfix"></div>
				
<script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();

	new Gemma.GemmaNavigationHeader({
		 	renderTo: 'extheaderandnavigation'
		 });
		 
});
</script>

<%-- Put constants into request scope --%>
<Gemma:constants scope="request" />
<script type="text/javascript">
// IE throws an error when loading the bookmarked page because it doesn't support the createContextualFragment method
		// also throws this error when opening any visualisationWidget pop-up window
			if ((typeof Range !== "undefined") && !Range.prototype.createContextualFragment){
				Range.prototype.createContextualFragment = function(html){
					var frag = document.createDocumentFragment(),
					div = document.createElement("div");
					frag.appendChild(div);
					div.outerHTML = html;
					return frag;
				};
			}
</script>