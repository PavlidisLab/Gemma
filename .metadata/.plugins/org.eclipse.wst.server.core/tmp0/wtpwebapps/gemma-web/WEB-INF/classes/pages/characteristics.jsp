<%@ include file="/common/taglibs.jsp"%>

<head>
	<title><fmt:message key="characteristicBrowser.title" /></title>
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
		<jwr:script src='/scripts/app/CharacteristicBrowser.js' />
	</security:authorize>
</head>

<h1>
	<fmt:message key="characteristicBrowser.heading" />
</h1>

<div id="messages"></div>
<div id="characteristicBrowser"></div>



