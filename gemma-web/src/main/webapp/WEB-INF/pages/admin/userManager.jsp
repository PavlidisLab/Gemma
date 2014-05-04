<%-- 
author: keshav
version: $Id$
--%>
<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Users</title>
	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/userManager.js' useRandomParam='false' />
</head>

<p>
	Manage users (and groups) in Gemma.
</p>
<div align='left' id='userList'></div>
<div align='left' id='errorMessage' style='width: 700px; margin-bottom: 1em;'></div>
