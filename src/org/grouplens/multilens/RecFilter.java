package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Jul 31, 2002
 * Time: 11:53:26 AM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import org.grouplens.multilens.DBConnection;

import java.util.*;
import java.sql.*;

/**
 * A RecFilter is a container for holding a set of items that you want to include, or exclude
 * from a set of recommendations.
 */
public class RecFilter {
    HashSet filterSet;

    /**
     * Create a null filterSet.
     */
    public RecFilter(){
        filterSet = null;
    }
    /**
     * Create a new filterSet based on the genre and release date of a film.
     * @param genre
     * @param releasedAfter
     * @param releasedBefore
     * @param releaseMedia  One of 'film', 'dvd', or 'video', if 'null' assume 'film'
     */
    public RecFilter(Vector genre,
                     String releasedAfter,
                     String releasedBefore,
                     String releaseMedia) {
        filterSet = new HashSet(200);
        // This registers the particular mysql JDBC driver I've
        // chosen to be the driver that gets used.
        DBConnection myConn = new DBConnection();
        try {
            Statement stmt = myConn.getConn().createStatement();

            // Construct the Select statment to select the desired set of items.
            String SQLStmt = constructQuery(releasedAfter, releasedBefore, releaseMedia, genre);

            ResultSet rs = stmt.executeQuery(SQLStmt);
            Integer mlid;
            while (rs.next()) {
                mlid = new Integer(rs.getInt(1));
                filterSet.add(mlid);
            }
            stmt.close();
        }
        catch (SQLException E) {
            System.err.println("SQLException: " + E.getMessage());
            System.err.println("SQLState:     " + E.getSQLState());
            System.err.println("VendorError:  " + E.getErrorCode());
        }

    }

    private String constructQuery(String releasedAfter, String releasedBefore, String releaseMedia, Vector genre) {
        String releaseDateColumn = "";
        // Lets assume that we will always use movie_genre_pair as the source of movie_ids;
        String tableList = " movie_genre_pair";
        // Now if we are looking at a release date we need to add the movie_info table
        if (releasedAfter != null || releasedBefore != null) {
            tableList += " , movie_info";
            // Now figure out the column for the release date to check
            if (releaseMedia == null) {
                releaseDateColumn = "film_release_date";
            } else if (releaseMedia.equals("dvd")) {
                releaseDateColumn = "dvd_release_date";
            } else if (releaseMedia.equals("video")) {
                releaseDateColumn = "video_release_date";
            } else {
                releaseDateColumn = "film_release_date";
            }
        }
        // Start to build the query
        String SQLStmt = "SELECT distinct movie_genre_pair.movie_id FROM " + tableList;
        boolean needAnd = false;
        // If the genre vector is not null then add a clause to get the movies in one or more genres
        if (genre != null) {
            SQLStmt += " WHERE movie_genre_pair.genre_id " + arrayToInClause(genre) ;
            needAnd = true;
        }
        // Now add a clause for the released After date
        if (releasedAfter != null) {
            SQLStmt += andOr(needAnd);
            SQLStmt += " movie_info.film_release_date > '" + releasedAfter + "'";
            needAnd = true;
        }
        // Now add a clause for the released before date
        if (releasedBefore != null) {
            SQLStmt += andOr(needAnd);
            SQLStmt += " movie_info.film_release_date < '" + releasedBefore + "'";
        }
        // If this is a join, make it explicit.
        if (genre != null && (releasedBefore != null || releasedAfter != null)) {
            SQLStmt += " AND movie_genre_pair.movie_id = movie_info.movie_id";
        }
        // return the final query.
        return SQLStmt;
    }

    // construct a clause of the form 'in (x,y,z)' to add to the select statement.
    private String arrayToInClause(Vector genre) {
        String inPart = "in (";
        Iterator it = genre.iterator();
        while(it.hasNext()) {
            String g = (String)it.next();
            if (it.hasNext()) {
                inPart += g + ",";
            } else {
                inPart += g;
            }
        }
        inPart += ")";
        return inPart;
    }

    // return WHERE or AND as needed
    private String andOr(boolean needAnd) {
        String ret = "";
        if (needAnd) {
            ret += " AND ";
        } else {
            ret += " WHERE ";
            needAnd = true;
        }
        return ret;
    }

    public HashSet getFilterSet() {
        return filterSet;
    }


}
