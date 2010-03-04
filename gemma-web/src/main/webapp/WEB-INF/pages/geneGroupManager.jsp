<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Manage Gene Groups</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/GeneGroupManager.js' useRandomParam='false' />
</head>
<body>
	<h2>
		Manage Gene Groups
	</h2>

	<p>
		A 'Group' is a set of Gemma users who have a common set of permissions. This page allows you to see what Groups you
		belong to, create groups, and change who is in groups you control.
	</p>

	
	<div id='genesetCreation-div'> </div>
	
	<div id='errorMessage' style='width: 500px; margin-bottom: 1em;'></div>
	
	
	
	
</body>