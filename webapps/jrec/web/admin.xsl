<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/TR/xhtml1/strict">

<xsl:template match="jrecconfig">
<html>
  <head>
    <title>JRE Admin</title>
  </head>

  <body>
    <h1>JRE Admin</h1>

<p>
Recommendation engine url:
<xsl:value-of select="recEngineUrl"/>
</p>

<p>
This is the url to which requests will be sent.  It comes from the
configuration file.
</p>

<p>
Model last loaded: 
<xsl:value-of select="modelLastLoadedDate"/>
</p>

<form action="{recEngineUrl}" method="get">

<p>
Enter The number of columns for each model row
<input type="text" name="trunc" maxlength="40" size="15" value="{trunc}">

</input>
</p>

<p>
Enter the name of the table containing the cooked model
<input type="text" name="modeltable" size="20" value="{modeltable}">
</input>
</p>

<p>
Enter the URL for the database to use
<input type="text" name="dburl" size="25" value="{dburl}">
</input>
</p>

<p>
Database Username
<input type="text" name="dbuser" size="25" value="{dbuser}">
</input>
</p>

<p>
Database Password
<input type="text" name="dbpass" size="25" value="{dbpass}">
</input>
</p>

<p>
File Containing Model
<input type="text" name="modfile" size="25" value="{modfile}">
</input>
</p>

<p>
File Containing Average Model
<input type="text" name="avgmodfile" size="25" value="{avgmodfile}">
</input>
</p>


<input type="SUBMIT" name="request" value="adminsubmit">
</input>

<input type="SUBMIT" name="request" value="loadmodel">
</input>

</form>
</body>
</html>
</xsl:template>
</xsl:stylesheet>

