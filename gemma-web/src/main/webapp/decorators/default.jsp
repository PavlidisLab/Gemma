<%@ include file="/common/taglibs.jsp" %>
<%--@elvariable id="appConfig" type="java.util.Map"--%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width; initial-scale=1" />
<title><decorator:title /> | <fmt:message key="webapp.name" /></title>
<meta name="author" content="Gemma admin (${appConfig["gemma.support.email"]})" />
<link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" />
<Gemma:style src="/bundles/gemma-all.css" />
<script type="application/javascript">
window.gemmaHostUrl = "${appConfig["gemma.hosturl"]}";
window.ctxBasePath = "${pageContext.request.contextPath}";
window.recaptchaPublicKey = "${appConfig["gemma.recaptcha.publicKey"]}";
window.gemBrowUrl = "${appConfig["gemma.gemBrow.url"]}";
</script>
<%-- for registration, possible from any page--%>
<script src="https://www.google.com/recaptcha/api.js?render=explicit" async defer></script>
<jsp:include page="/common/analytics.jsp" />
<Gemma:script src="/bundles/include.js" />
<Gemma:script src="/bundles/gemma-lib.js" />
<decorator:head />
</head>
<body>
<jsp:include page="/common/userStatusVariables.jsp" />
<div id="page">
    <div id="header">
        <jsp:include page="/common/header.jsp" />
    </div>
    <div id="content">
        <div id="main">
            <%@ include file="/common/messages.jsp" %>
            <decorator:body />
        </div>
    </div>
</div>
</body>
</html>