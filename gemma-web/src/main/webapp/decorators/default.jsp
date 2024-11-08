<!DOCTYPE html>
<%-- Include common set of tag library declarations for each layout --%>
<%@ include file="/common/taglibs.jsp"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<%-- Include common set of meta tags for each layout --%>
<%@ include file="/common/meta.jsp"%>

<link rel="shortcut icon" href="${pageContext.request.contextPath}/images/logo/gemmaTinyTrans.gif" />
<title><decorator:title /> | <fmt:message key="webapp.name" /></title>

<Gemma:style href="/bundles/gemma-all.css" />
<script type='text/javascript' src='${pageContext.request.contextPath}/dwr/engine.js'></script>
<script type='text/javascript' src='${pageContext.request.contextPath}/dwr/util.js'></script>

<Gemma:script src="/bundles/include.js" />
<Gemma:script src="/bundles/gemma-lib.js" />

<%-- for dwr creation of javascript objects to mirror java value objects; including one of these causes all the objects to be exposed.--%>
<script type='text/javascript' src='${pageContext.request.contextPath}/dwr/interface/EmptyController.js'></script>
<%-- We should use this as soon as we figure out how to set generateDtoClasses option to dtoall 
		script type='text/javascript' src='/Gemma/dwr/dtoall.js'></script>	--%>

<%-- for registration, possible from any page--%>
<script src="https://www.google.com/recaptcha/api.js?render=explicit" async defer></script>

<%-- log javascript errors --%>
<script type="text/javascript">
			window.onerror = function(errorMessage, url, line){		
				// message == text-based error description
			    // url     == url which exhibited the script error
			    // line    == the line number being executed when the error occurred
				JavascriptLogger.writeToErrorLog(errorMessage, url, line, document.location.href, navigator.userAgent);				
			};
		</script>

<decorator:head />


</head>
<body <decorator:getProperty property="body.id" writeEntireProperty="true"/>
	<decorator:getProperty property="body.class" writeEntireProperty="true"/>>

	<div id="page">

		<div id="headerclearnopadding" class="clearfix">
			<jsp:include page="/common/header.inner.jsp" />
		</div>


		<div id="content">



			<div id="main">
				<%@ include file="/common/messages.jsp"%>

				<h2>
					<decorator:getProperty property="page.heading" />
				</h2>


				<decorator:body />
			</div>


		</div>

		<jsp:include page="/common/userStatusVariables.jsp" />

		<jsp:include page="/common/analytics.jsp" />

	</div>

</body>
</html>

