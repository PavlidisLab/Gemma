<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Cell Browser for ${fn:escapeXml(ee.shortName)} - ${fn:escapeXml(ee.name)}</title>
<meta name="description" content="${fn:escapeXml(ee.description)}" />
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
</head>

<!-- the header is exactly 60px high, but there's some extra pixels that I'm not sure how to account for -->
<iframe src="${cellBrowserUrl}" style="width: 100%; height: calc(100vh - 60px - 9px); border: 0"></iframe>