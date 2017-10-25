<%@ include file="/common/taglibs.jsp"%>

<c:if test='${ appConfig["ga.tracker"] != null}'>

    <script type="text/javascript">

        <!-- Google Analytics -->
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

        ga('create', '${appConfig["ga.tracker"]}', {
            cookieDomain: '${appConfig["ga.domain"]}'
        });
        ga('require', 'linkid', 'linkid.js');
        ga('send', 'pageview');

        <!-- End Google Analytics -->

        if (typeof googleAnalyticsTrackPageviewIfConfigured === 'undefined') {
            var googleAnalyticsTrackPageviewIfConfigured = googleAnalyticsTrackPageviewIfConfigured || function (pageURL) {
                    if (typeof ga !== 'undefined') {
                        ga('create', '${appConfig["ga.tracker"]}', {
                            cookieDomain: '${appConfig["ga.domain"]}'
                        });
                        ga('send', 'pageview', pageURL);
                    }
                };
        }
    </script>
</c:if>

<c:if test='${ appConfig["ga.tracker"] == null}'>
    <script type="text/javascript">
        if (typeof googleAnalyticsTrackPageviewIfConfigured === 'undefined') {
            var googleAnalyticsTrackPageviewIfConfigured = function (pageURL) {/* no op, for sandbox and development */
            };
        }
    </script>
</c:if>
