<%@ include file="/common/taglibs.jsp"%>
<head>
    <title><fmt:message key="phenotypes.title" /></title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	
	<jwr:script src='/scripts/app/phenotypes.js' />
</head>

<body>
	<input type="hidden" name="phenotypeUrlId" id="phenotypeUrlId" value="${phenotypeUrlId}" />
	<input type="hidden" name="geneId" id="geneId" value="${geneId}" />
	<input type="hidden" id="reloadOnLogin" value="false"/>  
	<input type="hidden" id="reloadOnLogout" value="false" />
</body>