<%@ include file="/common/taglibs.jsp" %>
<head>
<title><fmt:message key="generalSearch.title" /></title>
</head>

<input type="hidden" id="reloadOnLogout" value="false">
<div id="messages"></div>
<div style="height: 1em; margin-bottom: 10px;" id="validation-messages"></div>
<div style="margin-bottom: 10px" id="general-search-form"></div>
<div style="margin: 5px" id="search-bookmark"></div>
<div style="margin-top: 2px" id="search-results-grid"></div>

<script type="text/javascript">
Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
Ext.onReady( function() {
   Ext.QuickTips.init();

   new Gemma.GemmaViewPort( {
      centerPanelConfig : new Gemma.Search.GeneralSearch()
   } );
} );
</script>
