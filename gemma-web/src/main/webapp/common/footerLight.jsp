<%@ include file="/common/taglibs.jsp"%>

<div id="divider">
	<div></div>
</div>
<div class="footer">

	<span class="left">Gemma  &nbsp;&nbsp;${appConfig["version"]}&nbsp;&nbsp;&nbsp;Copyright &copy; 2007-2012 &nbsp;
	<a href='<c:url value="http://gemma-chibi-doc.sites.olt.ubc.ca/terms-and-conditions" />'>Terms and conditions</a></span>
</div>

<c:if test='${ appConfig["ga.tracker"] != null}'>
	<script type="text/javascript">
	var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
	document.write(unescape("%3Cscript src='" + gaJsHost
			+ "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
	<script type="text/javascript">
	try {
		var pageTracker = _gat._getTracker('${appConfig["ga.tracker"]}');
		pageTracker._trackPageview();
	} catch (err) {
	}
</script>


</c:if>