<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!-- HTTP 1.1 -->
<meta http-equiv="Cache-Control" content="no-store" />

<!-- HTTP 1.0 -->
<meta http-equiv="Pragma" content="no-cache" />

<!-- Prevents caching at the Proxy Server -->
<meta http-equiv="Expires" content="0" />

<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

<meta name="description"
	content="Database of gene expression data and other genomics data in a meta-analysis framework, running under open source software." />

<meta name="keywords"
	content="genomics,bioinformatics,genetics,transcriptomes,rnaseq,microarrays,biotechnology,medicine,biomedical,meta-analysis,statistics,search,open source,database,software" />

<c:set var="ctxPath" value="${pageContext.request.contextPath}" scope="request" />

<meta name="author" content="Gemma admin (gemma@chibi.ubc.ca)" />

<c:if test="${fn:contains(header['User-Agent'],'chromeframe') }">
	<meta http-equiv="X-UA-Compatible" content="chrome=1">
</c:if>


<link rel="icon" href="<c:url value="/images/logo/gemOnlyTiny.ico"/>" />
