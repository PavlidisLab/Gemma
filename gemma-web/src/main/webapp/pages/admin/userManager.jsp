<%-- 
author: keshav

--%>
<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Users</title>
<jwr:script src='/scripts/app/userManager.js' useRandomParam='false' />
</head>

<body>
<div class="padded">
    <h2>Manage Users</h2>

    <div align='left' id='userList'></div>
    <div align='left' id='errorMessage'
            style='width: 700px; margin-bottom: 1em;'></div>
</div>
</body>