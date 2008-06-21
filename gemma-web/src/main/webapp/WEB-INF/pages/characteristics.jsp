<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<head>
	<title><fmt:message key="characteristicBrowser.title" />
	</title>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
	<authz:authorize ifAnyGranted="admin">
		<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
		<jwr:script src='/scripts/app/CharacteristicBrowser.js' />
	</authz:authorize>
</head>

<h1>
	<fmt:message key="characteristicBrowser.heading" />
</h1>

<authz:authorize ifAnyGranted="admin">
	<div id="messages"></div>
	<div id="characteristicBrowser"></div>
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<p>
		Permission denied.
	</p>
</authz:authorize>