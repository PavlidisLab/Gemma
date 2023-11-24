<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!-- HTTP 1.1 -->
<meta http-equiv="Cache-Control" content="no-store"/>

<!-- HTTP 1.0 -->
<meta http-equiv="Pragma" content="no-cache"/>

<!-- Prevents caching at the Proxy Server -->
<meta http-equiv="Expires" content="0"/>

<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>

<meta name="description"
      content="Database of curated, reanalyzed genomics datasets for meta-analysis"/>

<meta name="keywords"
      content="genomics,bioinformatics,genetics,transcriptomes,rnaseq,microarrays,biotechnology,medicine,biomedical,meta-analysis,statistics,search,open source,database,software"/>

<c:set var="ctxPath" value="${pageContext.request.contextPath}" scope="request"/>
<c:set var="recaptchaPublicKey" value="${appConfig['gemma.recaptcha.publicKey']}" scope="request"/>
<c:set var="gemBrowUrl" value="${appConfig['gemma.gemBrow.url']}" scope="request"/>



<meta name="author" content="Gemma admin (pavlab-support@msl.ubc.ca)"/>

<c:if test="${fn:contains(header['User-Agent'],'chromeframe') }">
    <meta http-equiv="X-UA-Compatible" content="chrome=1">
</c:if>

<link rel="shortcut icon" href="<c:url value="/favicon.ico"/>"/>

<script>
    var ctxBasePath = '${ctxPath}';
    var recaptchaPublicKey = '${recaptchaPublicKey}';
    var gemBrowUrl = '${gemBrowUrl}';
</script>

