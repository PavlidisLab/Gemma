<!--

This XSL can be used to transform a MAGE-ML document containing OntologyEntry instances into something easier to deal with. 
However, this is only necessary if the 'full' MGED Ontology is used (Age has_measurement Measurement has_unit etc). 
ArrayExpress no longer uses that and annotates experiment only with the "no sub-element case". As of 8/2007 this transformation is
not being used by Gemma's MageMLConverter.

-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- $Id$ -->
	<xsl:output method="xml" indent="yes"/>
	<xsl:template match="/">
		<BioList>
			<xsl:apply-templates select="//MAGE-ML/BioMaterial_package/BioMaterial_assnlist/BioSource"/>
			<xsl:apply-templates select="//MAGE-ML/BioMaterial_package/BioMaterial_assnlist/BioSample"/>
			<xsl:apply-templates select="//MAGE-ML/BioMaterial_package/BioMaterial_assnlist/LabeledExtract"/>
		</BioList>
	</xsl:template>
	<!-- Main elements to match in MAGE -->
	<xsl:template match="BioSource|BioSample|LabeledExtract">
		<BioMaterial>
			<xsl:attribute name="identifier"><xsl:value-of select="@identifier"/></xsl:attribute>
			<xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
			<Characteristics>
				<xsl:apply-templates select="Characteristics_assnlist/OntologyEntry[@category != @value]"/>
				<xsl:apply-templates select="Characteristics_assnlist/OntologyEntry[@category = @value]"/>
			</Characteristics>
		</BioMaterial>
	</xsl:template>
	<!-- ******** Covers simplest case, where there are no sub-elements ********  -->
	<xsl:template match="OntologyEntry[@category != @value]">
		<xsl:variable name="c" select="@category"/>
		<xsl:variable name="d" select="@description"/>
		<xsl:choose>
			<xsl:when test="$c = 'has_value'">
				<xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
				<xsl:if test="string-length($d)>0">
					<xsl:attribute name="description"><xsl:value-of select="@description"/></xsl:attribute>
				</xsl:if>
			</xsl:when>
			<xsl:when test="starts-with($c, 'has_')">
				<xsl:variable name="el" select="substring-after($c, 'has_')"/>
				<xsl:variable name="elname" select="translate($el, '_', '')"/>
				<xsl:element name="{$elname}">
					<xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
					<xsl:if test="string-length($d)>0">
						<xsl:attribute name="description"><xsl:value-of select="@description"/></xsl:attribute>
					</xsl:if>
					<xsl:apply-templates select="../../OntologyReference_assn/DatabaseEntry"/>
					<xsl:apply-templates select="../../OntologyReference_assn/DatabaseEntry/Database_assnref/Database_ref"/>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="elname">
					<xsl:value-of select="translate($c,'/ ', '_')"/>
				</xsl:variable>
				<xsl:element name="{$elname}">
					<xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
					<xsl:if test="string-length($d)>0">
						<xsl:attribute name="description"><xsl:value-of select="@description"/></xsl:attribute>
					</xsl:if>
					<xsl:apply-templates select="OntologyReference_assn/DatabaseEntry"/>
					<xsl:apply-templates select="OntologyReference_assn/DatabaseEntry/Database_assnref/Database_ref"/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- *********************************************  -->
	<xsl:template match="OntologyEntry[@category=@value]">
		<xsl:variable name="c" select="@category"/>
		<xsl:variable name="d" select="@description"/>
		<xsl:choose>
			<xsl:when test="contains($c, 'has_')">
				<xsl:apply-templates select="Associations_assnlist"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="{$c}">
					<xsl:if test="string-length($d)>0">
						<xsl:attribute name="description"><xsl:value-of select="@description"/></xsl:attribute>
					</xsl:if>
					<xsl:apply-templates select="OntologyReference_assn/DatabaseEntry"/>
					<xsl:apply-templates select="OntologyReference_assn/DatabaseEntry/Database_assnref/Database_ref"/>
					<xsl:apply-templates select="Associations_assnlist"/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- *********************************************  -->
	<xsl:template match="Associations_assnlist">
		<xsl:apply-templates select="OntologyEntry[@category = @value]"/>
		<xsl:apply-templates select="OntologyEntry[@category != @value]"/>
	</xsl:template>
	<!-- *********************************************  -->
	<!-- ***** Database entry associations   *******  -->
	<!-- *********************************************  -->
	<xsl:template match="OntologyReference_assn/DatabaseEntry">
		<xsl:variable name="c" select="../../@category"/>
		<xsl:variable name="v" select="../../@value"/>
		<xsl:variable name="charlist" select="../../../../Characteristics_assnlist"/>
		<xsl:variable name="hascharlistparent" select="count($charlist) > 0"/>
		<!-- if the category and value are the same, then it's a category. Otherwise it's a value. -->
		<!--<xsl:attribute name="test"><xsl:value-of select="$hascharlistparent"/></xsl:attribute>-->
		<xsl:choose>
			<xsl:when test="($c = $v)  or $hascharlistparent  ">
				<xsl:attribute name="CategoryDatabaseAccession"><xsl:value-of select="@accession"/></xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="ValueDatabaseAccession"><xsl:value-of select="@accession"/></xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- *********************************************  -->
	<xsl:template match="OntologyReference_assn/DatabaseEntry/Database_assnref/Database_ref">
		<xsl:variable name="c" select="../../../../@category"/>
		<xsl:variable name="v" select="../../../../@value"/>
		<xsl:variable name="charlist" select="../../../../../../Characteristics_assnlist"/>
		<xsl:variable name="hascharlistparent" select="count($charlist) > 0"/>
		<!-- if the category and value are the same, then it's a category. Otherwise it's a value. -->
		<!--<xsl:attribute name="test"><xsl:value-of select="$hascharlistparent"/></xsl:attribute>-->
		<xsl:choose>
			<xsl:when test="($c = $v)  or $hascharlistparent">
				<xsl:attribute name="CategoryDatabaseIdentifier"><xsl:value-of select="@identifier"/></xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="ValueDatabaseIdentifier"><xsl:value-of select="@identifier"/></xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
