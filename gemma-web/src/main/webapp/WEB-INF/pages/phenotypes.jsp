<%@ include file="/common/taglibs.jsp"%>
<head>
    <title><fmt:message key="phenotypes.title" /></title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	
	<jwr:script src='/scripts/app/phenotypes.js' />
</head>

<body>
	<input type="hidden" name="phenotypeValue" id="phenotypeValue" value="${phenotypeValue}" />
	<input type="hidden" name="geneId" id="geneId" value="${geneId}" />
</body>