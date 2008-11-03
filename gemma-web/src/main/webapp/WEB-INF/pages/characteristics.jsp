<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<head>
	<title><fmt:message key="characteristicBrowser.title" />
	</title>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
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