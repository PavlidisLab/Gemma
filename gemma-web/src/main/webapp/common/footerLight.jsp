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
	  var _gaq = _gaq || [];
	  _gaq.push(['_setAccount', '${appConfig["ga.tracker"]}']);
	  _gaq.push(['_setDomainName', '${appConfig["ga.domain"]}']);
	  _gaq.push(['_trackPageview']);

	  (function() {
	    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
	    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
	    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
	  })();
</script>
</c:if>