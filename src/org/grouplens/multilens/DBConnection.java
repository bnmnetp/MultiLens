package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Aug 14, 2002
 * Time: 10:37:39 AM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 * Centralize the database connection management.
 */

import java.sql.*;
import java.io.*;

import org.grouplens.util.GlConfigVars;
import org.grouplens.util.Utils;
/**
 * DBConnection is a utility class to hide the mess of setting up JDBC Connections to the
 * database we use for storing ratings and models.
 * <p>
 * So far I have only tested this with the mm.mysql drivers
 */
public class DBConnection {
/**
 * sqlDriver:  The whole multilens package uses the mm.mysql jdbc driver
 */
    private static final String sqlDriver = GlConfigVars.getConfigVar(GlConfigVars.sqlDriver, GlConfigVars.MULTILENS);

    /**
     * Where to write database logfile entries
     */
    private static String logfile = GlConfigVars.getConfigVar(GlConfigVars.logfile, GlConfigVars.MULTILENS);

    /**
     * defaultDbName the default database and table
     */
    private static String defaultDbName = GlConfigVars.getConfigVar(GlConfigVars.dbUrl, GlConfigVars.MULTILENS);
    /**
     *  defaultUser  a user with access to the default table
     */
    private static String defaultUser = GlConfigVars.getConfigVar(GlConfigVars.dbUser, GlConfigVars.MULTILENS);
    /**
     * defaultPW  the password for the user
     */
    private static String defaultPW = GlConfigVars.getConfigVar(GlConfigVars.dbPassword, GlConfigVars.MULTILENS);

    private String dbName;
    private String userName;
    private String pword;
    private Connection conn;

    /**
     * Default constructor, sets up a database connection using the default values
     *
     * Note that any changes to the default values for dbname, user, or pw MUST be done
     * prior to creating the connection, since createing a connection object also has the side effect
     * of creating a real connection to the database using the default values.
     */
    public DBConnection() {
        dbName = defaultDbName;
        userName = defaultUser;
        pword = defaultPW;

        initDB();
    }

    /**
     * Create a new database connection using the database, user and password provided
     *
     * @param dbName
     * @param uName
     * @param pw
     */
    public DBConnection(String dbName, String uName, String pw) {
        this.dbName = dbName;
        this.userName = uName;
        this.pword = pw;
        initDB();
    }

    public void finalize() {
        dbClose();
    }

    /**
     * Tell the DBConnection object to close its database connection.  If this method is not called the connection will
     * be closed when the finalize is called.
     */
    public void dbClose() {
        try {
            if (conn != null)
            {
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState:     " + e.getSQLState());
            System.err.println("VendorError:  " + e.getErrorCode());
        }
    }
    
    public static void logSqlException(SQLException e) {
        System.err.println(Utils.throwableToString(e));
        System.err.println("SQLState:     " + e.getSQLState());
        System.err.println("VendorError:  " + e.getErrorCode());
    }

//-------------------------------------------------------------
// Getters and Setters
//-------------------------------------------------------------

    /**
     *
     * @return The pathname to the logfile for jdbc messages
     */
    public static String getLogfile() {
        return logfile;
    }

    /**
     * Sets the path for the logfile.  This needs to be set before calling the constructor.
     *
     * @param logfile
     */
    public static void setLogfile(String logfile) {
        DBConnection.logfile = logfile;
    }

    /**
     * Get the default database URL
     * @return default URL
     */
    public static String getDefaultURL() {
        return defaultDbName;
    }
    /**
     * Set the default database URL.
     * 
     * <p>
     * The structure of a JDBC URL for MySQL is as follows
     * <code>jdbc:mysql://hostname/tablename</code>
     *
     * @param defaultDbName
     */
    public static void setDefaultURL(String defaultDbName) {
        DBConnection.defaultDbName = defaultDbName;
    }
    /**
     *  Get the default user
     *
     * @return default database user name
     */
    public static String getDefaultUser() {
        return defaultUser;
    }
    /**
     * Set the default user
     * The user should be a valid username for the database speicified by the defaultdb URL
     * @param defaultUser
     */
    public static void setDefaultUser(String defaultUser) {
        DBConnection.defaultUser = defaultUser;
    }
    /**
     * Get the default password (for the default user)
     * @return default password
     */
    public static String getDefaultPW() {
        return defaultPW;
    }
    /**
     * Set the password for the default user
     * @param defaultPW
     */
    public static void setDefaultPW(String defaultPW) {
        DBConnection.defaultPW = defaultPW;
    }

    /**
     * Get the raw jdbc connection object
     * @return the JDBC Connection object
     */
    public Connection getConn() {
        return conn;
    }

//-------------------------------------------------------------
// Private Functions
//-------------------------------------------------------------

    private void initDB() {
        try {
            // This registers the particular mysql JDBC driver I've
            // chosen to be the driver that gets used.
            try {
                Class.forName(sqlDriver);
            }
            catch (Exception E) {
                System.err.println("Unable to load driver " + sqlDriver);
                E.printStackTrace();
            }
            // Send JDBC error messages to stderr.
            try {
                DriverManager.setLogWriter(new PrintWriter( new PrintStream(new FileOutputStream(logfile))));
            } catch (FileNotFoundException e) {
            }
            // Open a connection to a database.  The form of the URL is:
            // jdbc:mysql://[hostname][:port]/dbname?label1=value1&label2=value2
            // interesting values are "?user=foo&passwd=bar"
            // Instead, I used the 2nd and 3rd parms to getConnection to
            // set the user and password.
            conn = DriverManager.getConnection(dbName,userName,pword);
            // If a SQLWarning object is available, print its
            // warning(s).
            SQLWarning warn = conn.getWarnings();
            while (warn != null) {
                System.out.println("SQLState: " + warn.getSQLState()
                        + "Message: " + warn.getMessage()
                        + "Vendor Code: " + warn.getErrorCode()
                        + "");
                warn = warn.getNextWarning();
            }

        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState:     " + e.getSQLState());
            System.err.println("VendorError:  " + e.getErrorCode());
        }

    }






}
