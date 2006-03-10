<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="ubic.gemma.model.expression.experiment.ExperimentalDesign"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!DOCTYPE HTML PUBLIC "-//W3C//Dtd HTML 4.01 transitional//EN">
<HTML>
    <HEAD>
    </HEAD>
    <BODY>

        <DIV align="left">
            <table width="100%">
                <tr>
                    <td>
                        <DIV align="left">
                            <b>Search Results</b>
                        </DIV>
                    </td>
                </tr>
                <tr>
                    <td>
                        <HR>
                    </td>
                </tr>
                <tr>
                    <display:table name="experimentalDesigns" class="list" requestURI="" id="experimentalDesignList"
                        export="true">
                        <display:column property="name" sort="true" titleKey="experimentalDesign.name" />
                        <display:column property="description" sort="true" titleKey="experimentalDesign.description" />
                        <display:column property="replicateDescription" sort="true"
                            titleKey="experimentalDesign.replicateDescription" />
                        <display:column property="qualityControlDescription" sort="true"
                            titleKey="experimentalDesign.qualityControlDescription" />
                        <display:column property="normalizationDescription" sort="true"
                            titleKey="experimentalDesign.normalizationDescription" />
                        <display:column property="name" sort="true" href="expressionExperimentdetails.htm"
                            paramId="name" paramProperty="name" titleKey="owning.expressionExperiment.name" />
                        <display:setProperty name="basic.empty.showtable" value="true" />
                    </display:table>
                </tr>
                <tr>
                    <td>
                        <HR>
                    </td>
                </tr>
            </table>

        </DIV>
    </BODY>
</HTML>
