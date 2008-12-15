<%@ include file="/common/taglibs.jsp"%>

<head>
	<title><fmt:message key="characteristicBrowser.title" /></title>
	<security:authorize ifAnyGranted="admin">
		<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
		<jwr:script src='/scripts/app/CharacteristicBrowser.js' />
	</security:authorize>
</head>

<h1>
	<fmt:message key="characteristicBrowser.heading" />
</h1>

<security:authorize ifAnyGranted="admin">
	<div id="messages"></div>
	<div id="characteristicBrowser"></div>
</security:authorize>
<security:authorize ifNotGranted="admin">
	<p>
		Permission denied.
	</p>
</security:authorize>