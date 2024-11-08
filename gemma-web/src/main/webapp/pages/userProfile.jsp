<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Edit your profile</title>
	<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />
	<Gemma:script src='/scripts/app/editUser.js' />
</head>
<body>
	<p>
		Change your email address or password. You must enter your current password.
	</p>
	<div id='editUser'></div>
	<div id='errorMessage' style='width: 500px; margin-bottom: 1em;'></div>
</body>