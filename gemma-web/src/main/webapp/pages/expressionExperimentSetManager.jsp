<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Dataset Group Manager</title>
</head>

<body>
<security:authorize access="!hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
    <script type="text/javascript">
    Gemma.AjaxLogin.showLoginWindowFn( true );
    </script>
    <p>Sorry, you must be logged in to use this tool.</p>
</security:authorize>

<security:authorize access="hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
    <div id='messages' style='width: 600px; height: 1.6em; margin: 0.2em; padding-bottom: 0.4em;'></div>

    <!-- p>Use this tool to create and edit groups of datasets. You can modify a built-in set by making a copy ("clone")
    and editing the copy.</p-->

    <script type="text/javascript">
    Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

    Ext.onReady( function() {

       Ext.QuickTips.init();

       new Gemma.GemmaViewPort( {
          centerPanelConfig : new Gemma.DatasetGroupEditor( {
             modal : true
          } )
       } );

    } );

    </script>


</security:authorize>

</body>