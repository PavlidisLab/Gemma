<%@ taglib uri="/WEB-INF/Gemma.tld" prefix="Gemma"%>
<%-- see https://twitter.com/about/resources/buttons#follow --%>
<a href="https://twitter.com/GemmaBioinfo" class="twitter-follow-button" data-show-count="false"
	data-show-screen-name="false">Follow @GemmaBioinfo</a>
<script>
   !function(d, s, id) {
      var js, fjs = d.getElementsByTagName(s)[0], p = /^http:/.test(d.location) ? 'http' : 'https';
      if ( !d.getElementById(id) ) {
         js = d.createElement(s);
         js.id = id;
         js.src = p + '://platform.twitter.com/widgets.js';
         fjs.parentNode.insertBefore(js, fjs);
      }
   }(document, 'script', 'twitter-wjs');
</script>

<a href="${pageContext.request.contextPath}/rssfeed"
	title="Subscribe to Gemma feeds" target="_blank"><Gemma:img src="/images/icons/rss_feed.gif"
	cssStyle="border: 0" /></a>
<br>
