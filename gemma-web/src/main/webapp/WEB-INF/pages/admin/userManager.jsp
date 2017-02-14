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

<div class="padded">
	<div class="v-padded">
		<h2>Manage users (and groups) in Gemma.</h2>
	</div>
	
	<div align='left' id='userList'></div>
	<div align='left' id='errorMessage'
		style='width: 700px; margin-bottom: 1em;'></div>
</div>
