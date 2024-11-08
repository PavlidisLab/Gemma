<%@ page errorPage="/pages/error/500.jsp" pageEncoding="UTF-8" contentType="text/html; charset=utf-8"
        trimDirectiveWhitespaces="true" %>
<%@ include file="/common/taglibs.jsp" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width; initial-scale=1" />
<title><decorator:title /> | <fmt:message key="webapp.name" /></title>
<meta name="author" content="Gemma admin (${appConfig["gemma.support.email"]})" />
<meta name="description"
        content="<decorator:getProperty property="meta.description" default="Database of curated, reanalyzed genomics datasets for meta-analysis"/>" />
<meta name="keywords"
        content="<decorator:getProperty property="meta.keywords" default="genomics,bioinformatics,genetics,transcriptomes,rnaseq,microarrays,biotechnology,medicine,biomedical,meta-analysis,statistics,search,open source,database,software"/>" />
<link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" />
<jwr:style src="/bundles/gemma-all.css" />
<script type="application/javascript">
const ctxBasePath = "${pageContext.request.contextPath}";
const recaptchaPublicKey = "${appConfig["gemma.recaptcha.publicKey"]}";
const gemBrowUrl = "${appConfig["gemma.gemBrow.url"]}";
</script>
<%-- for registration, possible from any page--%>
<script src="https://www.google.com/recaptcha/api.js?render=explicit" async defer></script>
<jsp:include page="/common/analytics.jsp" />
<!-- DWR -->
<jsp:include page="/common/dwr.jsp" />
<jwr:script src="/bundles/include.js" />
<jwr:script src="/bundles/gemma-lib.js" />
<decorator:head />
</head>

<body>
<!-- variables -->
<jsp:include page="/common/variables.jsp" />
<div id="page">
    <div id="headerclearnopadding" class="clearfix">
        <jsp:include page="/common/header.inner.jsp" />
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

