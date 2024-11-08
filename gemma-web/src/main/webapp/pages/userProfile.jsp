<%@ include file="/common/taglibs.jsp" %>
<head>
<title>Edit your profile</title>
<jwr:script src='/scripts/app/editUser.js' useRandomParam='false' />
</head>
<body>
<div class="padded">
    <h2>User Profile</h2>
    <p>
        Change your email address or password. You must enter your current password.
    </p>
    <div id="editUser"></div>
    <div id="errorMessage" style="width: 500px; margin-bottom: 1em;"></div>
</div>
</body>