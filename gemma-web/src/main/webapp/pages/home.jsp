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


<div style="margin: 0 auto; width: 900px; padding-top: 30px; padding-bottom: 30px;">
    <div style="margin-bottom: 30px;" id="generalSearchSimple-div"></div>

    <div id="news-updates" style="padding-left: 15px; padding-right: 15px;">
        <div style="display:flex; justify-content: space-between; margin-bottom: 40px;">
            <p style="margin-right: 15px;">
                Convenient programmatic access to Gemma's data and analyses is available via the
                software packages <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">gemma.R</a> (R/Bioconductor)
                and <a href="https://github.com/PavlidisLab/gemmapy">gemmapy</a> (Python).
            </p>
            <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">
                <img src="${pageContext.request.contextPath}/images/slideShow/bioconductor-logo.png"
                     alt="Bioconductor Logo"
                     width="200"
                     style="margin-right: 15px;"/>
            </a>
            <a href="https://pypi.org/project/gemmapy/">
                <img src="${pageContext.request.contextPath}/images/slideShow/pypi-logo.svg"
                     alt="PyPi Logo"
                     width="100"/>
            </a>
        </div>
        <div style="display:flex; justify-content: space-between; margin-bottom: 40px;">
            <p style="margin-right: 15px;">
                We invite you to try out the new <a href="${pageContext.request.contextPath}/browse">Gemma Browser</a>,
                our
                new interface for exploring and searching Gemma's data holdings. It's still in beta,
                and more features and improvements are planned, but we'd love to hear your feedback.
            </p>
            <a href="${appConfig['gemma.gemBrow.url']}" style="align-self: center;">
                <img
                        src="${pageContext.request.contextPath}/images/slideShow/gemma-browser-preview.png"
                        alt="A snapshot of the new Gemma Browser."
                        width="350"/>
            </a>
        </div>

        <p>Questions? Feel free to <a href="mailto:pavlab-support@msl.ubc.ca?subject=Gemma">reach out</a>.</p>
    </div>

</div>

<div style="margin-bottom: 24px;"></div>
<div id="footer" style="position: fixed; bottom: 0; height: 24px; background: white;">
    <div id="divider"></div>
    <div class="footer" style="display: flex; align-items: baseline; padding-left: 10px; padding-right: 10px;">
        <div>Gemma ${appConfig["version"]}</div>
        <div style="margin-left: 10px;">Copyright &copy; 2007-2023</div>
        <div style="margin-left: 10px;">
            Our <a href='<c:url value="https://pavlidislab.github.io/Gemma/terms.html" />'>Terms and conditions</a> have
            been updated!
        </div>
        <!-- <div style="margin-left: 10px;"><jsp:include page="/common/social.jsp"/></div> -->
        <div style="flex-grow: 1"></div>
        <a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/" style="align-self: center;">
            <img alt="Creative Commons License"
                 src="https://i.creativecommons.org/l/by-nc/4.0/80x15.png" width="80" height="15"/>
        </a>
    </div>
</div>
<jsp:include page="/common/analytics.jsp"/>

