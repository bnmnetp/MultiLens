package org.grouplens.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author dfrankow
 *
 * Configuration variables.
 */
public class GlConfigVars {
    /** If this is true, write some info to System.out */
    protected static boolean showDebug = true;

    static private Properties theProperties = null;
    static private boolean loaded = false;
    
    // Config subcategories
    static public final String MULTILENS = "multilens";
    static public final String JRECSERVER = "jrecserver";
    static public final String MOVIELENS3 = "movielens3";

    /** Java system property containing the full path to the config file to load */
    static public final String GLCONFIGFILE = "GL_CONFIG_FILE";
    
    // Some well-known config file properties
    static public final String dbUrl = "dbUrl";
    static public final String dbPassword = "dbPassword";
    static public final String dbUser = "dbUser";
    static public final String ratingTable = "ratingTable";
    static public final String sqlDriver = "sqlDriver";
    static public final String logfile = "logfile";

    /**
     * Unload the config file so it will be loaded again at the next variable get.
     */
    static public void unloadConfig() {
        loaded = false;
    }

    /**
     * Load the configuration file if not yet loaded.
     * 
     * @param fileName  File to load from.
     * @throws GlConfigException
     */
    static public void loadConfig(String fileName) throws GlConfigException {
        if (null == fileName) {
            // Get the filename from a system property
            fileName = System.getProperty(GLCONFIGFILE);
            if (null == fileName) {
                throw new GlConfigException("No system property " + GLCONFIGFILE);
            }
        }
        File f = new File(fileName);
        if (!f.canRead()) {
            throw new GlConfigException("Can't read file " + fileName);
        }
        try {
            loadConfig(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            throw new GlConfigException(e.getMessage());
        } catch (GlConfigException e) {
            throw new GlConfigException(e.getMessage());
        }
    }

    /**
     * Load the configuration file if not yet loaded.
     * 
     * @param is    InputStream to load from
     * @throws GlConfigException
     */
    static synchronized public void loadConfig(InputStream is) throws GlConfigException {
        // Don't load twice
        if (loaded) {
            return;
        }

        try {
            theProperties = new Properties();
            theProperties.load(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        loaded = true;
    }

    /**
     * Get a String config var in subcat subCat.
     * @param var     Var name to get
     * @param subCat  Subcategory in which to get it
     * @return String var value.
     */
    static public String getConfigVar(String var, String subCat) {
        if (!loaded) {
            try {
                loadConfig((String)null);
            } catch (GlConfigException e) {
                e.printStackTrace();
            }
        }

        // Failed to load
        if (!loaded) {
            return null;
        }

        String name = var;
        String val = theProperties.getProperty(name);
        if (subCat != null) {
            String subName = new String(subCat) + "." + var;
            String subVal = theProperties.getProperty(subName);
            if (null != subVal) {
                name = subName;
                val = subVal;
            }
        }
        if (showDebug) {
            System.out.println("GlConfigVars.getConfigVar: " + name + "=" + val);
        }
        return val;
    }

    /**
     * Get a String config var (without a subcat).
     * @param var     Var name to get
     * @return String var value.
     */
    static public String getConfigVar(String var) throws GlConfigException {
        return getConfigVar(var, null);
    }
    
    /**
     * Get a boolean config var in subcat subCat.
     * @param var     Var name to get
     * @param subcat  Subcategory in which to get it
     * 
     * @return boolean var value.
     */
    static public boolean getBooleanConfigVar(String var, String subcat) {
        String val = getConfigVar(var, subcat);
        return Boolean.valueOf(val).booleanValue();
    }

    /**
     * Get a boolean config var (without a subcat)
     * @param var     Var name to get
     * @return boolean var value.
     * @throws GlConfigException
     */
    static public boolean getBooleanConfigVar(String var) throws GlConfigException {
        return getBooleanConfigVar(var, null);
    }

    /**
     * Get an int config var in subcat subCat.
     * @param var     Var name to get
     * @param subCat  Subcategory in which to get it
     * @return int var value.
     */
    static public int getIntConfigVar(String var, String subCat) {
        String val = getConfigVar(var, subCat);
        return Integer.parseInt(val);
    }

    /**
     * Get an int config var (without a subcat).
     * @param var     Var name to get
     * @return int var value.
     */
    static public int getIntConfigVar(String var) throws GlConfigException {
        return getIntConfigVar(var, null);
    }

    /**
     * Turn on or off showing of timing and memory measurements
     * @param show
     */
    static public void setShowDebug(boolean show) {
        showDebug = show;
    }
}
