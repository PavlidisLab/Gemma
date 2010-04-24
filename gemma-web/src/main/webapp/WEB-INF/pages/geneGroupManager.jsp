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
		You can create a group of genes to be used in searches and analyses. This interface allows you to create gene groups,
		modify them, and control who else can see them.
	</p>

	<div id='genesetCreation-div'>
	</div>

	<div id='errorMessage' style='width: 500px; margin-bottom: 1em;'></div>

</body>