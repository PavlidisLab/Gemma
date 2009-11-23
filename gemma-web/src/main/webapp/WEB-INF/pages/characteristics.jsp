<%@ include file="/common/taglibs.jsp"%>

<head>
	<title><fmt:message key="characteristicBrowser.title" /></title>
	<security:authorize ifAnyGranted="GROUP_ADMIN">
		<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
		<jwr:script src='/scripts/app/CharacteristicBrowser.js' />
	</security:authorize>
</head>

<h1>
	<fmt:message key="characteristicBrowser.heading" />
</h1>

<security:authorize ifAnyGranted="GROUP_ADMIN">
	<div id="messages"></div>
	<div id="characteristicBrowser"></div>
</security:authorize>
<security:authorize ifNotGranted="GROUP_ADMIN">
	<p>
		Permission denied.
	</p>
</security:authorize>