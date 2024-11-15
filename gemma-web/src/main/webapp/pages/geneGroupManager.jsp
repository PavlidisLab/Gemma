<%@ include file="/common/taglibs.jsp" %>


<head>
<title>Manage Gene Groups</title>
<Gemma:script src='/scripts/api/entities/gene/GeneGroupManager.js' />
</head>

<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true" />

<h2>Manage Gene Groups</h2>

<security:authorize access="!hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
    <script type="text/javascript">
    Gemma.AjaxLogin.showLoginWindowFn( true );
    </script>
    <p>
        Sorry, you must be logged in to use this tool.
    </p>
</security:authorize>


<security:authorize access="hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
    <script type="text/javascript">
    Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

    Ext.onReady( function() {

       Ext.QuickTips.init();

       /*new Gemma.GeneGroupManager({
                   renderTo : 'genesetCreation-div',
                   html:'You can create a group of genes to be used in searches and analyses. This interface allows you to create gene groups, modify them, and control who else can see them.'
               });
        */
       new Gemma.GemmaViewPort( {
          centerPanelConfig : new Gemma.GeneGroupManager()
       } );

    } );
    </script>
    <p>
        You can create a group of genes to be used in searches and analyses.
        This interface allows you to create gene groups, modify them, and
        control who else can see them.
    </p>

    <div id='genesetCreation-div'>
    </div>
</security:authorize>

<div id='errorMessage' style='width: 500px; margin-bottom: 1em;'></div>
