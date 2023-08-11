<%@ include file="/common/taglibs.jsp" %>
<head>
    <title>Home</title>

    <jwr:script src='/scripts/api/ext/data/DwrProxy.js'/>
    <%-- <jwr:script src='/scripts/app/HomePageAnalysisSearch.js' /> --%>
    <jwr:script src='/scripts/scriptsnonjawr/arbor.js'/>

</head>
<%@ include file="/pages/frontPageSlideShowShowOff.jsp" %>

<div id="main" style="text-align: left">
    <%@ include file="/common/messages.jsp" %>
</div>

<script type="text/javascript">
   Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
   Ext.onReady( function() {

      Ext.QuickTips.init();

      Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
      // Apply a set of config properties to the singleton
      /*Ext.apply(Ext.QuickTips.getQuickTip(), {
      	maxWidth : 200,
      	minWidth : 100,
      	showDelay : 0,
      	//trackMouse: true,
      	dismissDelay : 0,
      	hideDelay : 0
      });*/

      var generalSearchPanel = new Gemma.Search.GeneralSearchSimple();
      generalSearchPanel.render( "generalSearchSimple-div" );
   } );
</script>


<input type="hidden" id="reloadOnLogout" value="false">


<div style="margin: 0 auto; width: 900px;">
    <div style="margin-bottom: 30px;" id="generalSearchSimple-div"></div>

    <div id="news-updates" style="margin:20px; justify-content: center">
        <div style="display:flex; margin-bottom: 40px;max-width: 800px">
            <p style="margin-bottom: 10px;">
                Convenient programmatic access to Gemma's data and analyses is available via the
                software packages <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">gemma.R</a> (R/Bioconductor)
                and <a href="https://github.com/PavlidisLab/gemmapy">gemmapy</a> (Python).
            </p>
            <img src="${pageContext.request.contextPath}/images/slideShow/bioconductor-logo.png" style="width:200px; height:auto; padding: 5px; margin: 5px">
        </div>

        <hr/>
        <div style="display:flex; margin-bottom: 40px;max-width: 800px">
            <p>
                We invite you to try out the new <a href="${pageContext.request.contextPath}/browse">Gemma Browser</a>, our
                new interface for exploring and searching Gemma's data holdings. It's still in beta,
                and more features and improvements are planned, but we'd love to hear your feedback.
            </p>
            <a style="align:right" href="${pageContext.request.contextPath}/browse"><img
                    src="${pageContext.request.contextPath}/images/slideShow/gemma-browser-preview.png" style="width:350px;height:auto; margin:5px; padding:5px"></a>
        </div>
    </div>

    <p>Questions? Feel free to <a href="mailto:pavlab-support@msl.ubc.ca?subject=Gemma">reach out</a>.</p>

</div>

<div id="footer" class="clearfix">
    <div id="divider"></div>
    <div class="footer" style="display: flex; align-items: baseline; padding-left: 10px; padding-right: 10px;">
        <div>Gemma ${appConfig["version"]}&nbsp;Copyright &copy; 2007-2023</div>
        <div style="margin-left: 10px;">
            Our <a href='<c:url value="https://pavlidislab.github.io/Gemma/terms.html" />'>Terms and conditions</a> have
            been updated!
        </div>
        <!-- <div style="margin-left: 10px;"><jsp:include page="/common/social.jsp"/></div> -->
        <div style="flex-grow: 1"></div>
        <a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/" style="align-self: center;">
            <img alt="Creative Commons License"
                 src="https://i.creativecommons.org/l/by-nc/4.0/80x15.png"/>
        </a>
    </div>
</div>
<jsp:include page="/common/analytics.jsp"/>

