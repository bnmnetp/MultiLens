package jre.servlet;

/*
 * Created by IntelliJ IDEA.
 * jre.User: bmiller
 * Date: Jul 3, 2002
 * Time: 2:36:12 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import org.grouplens.multilens.*;
import org.grouplens.util.GlConfigVars;
import org.grouplens.util.Utils;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.*;
import java.io.*;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;


public class Jrec extends HttpServlet {
    //TODO BUGFIX:  Right now if you don't initialize the CosineRecModel with some parms read will die due
    //     to a null rowMap.
    CosineRecModel cosModel = null;
    AverageModel avgModel = new AverageModel();
    boolean loadingModel = false;
    Date modelLastLoaded;
    /** This is used to synchronize model readers */
    private static final Object modelReadSync = new Object();

    String defaultModFile = GlConfigVars.getConfigVar("modelFile", GlConfigVars.JRECSERVER);
    String defaultAvgModFile = GlConfigVars.getConfigVar("avgModelFile", GlConfigVars.JRECSERVER);

    private static final String RATING_TABLE = GlConfigVars.getConfigVar("ratingTable", GlConfigVars.JRECSERVER);

    RecFilter newDVD;
    RecFilter newVideo;
    RecFilter newFilm;
    static int modelTrunc = 4000;
    Thread initT;
    Thread sentT;
    static boolean initialized = false;
    
    private String  sqlDriver = GlConfigVars.getConfigVar(GlConfigVars.sqlDriver, GlConfigVars.JRECSERVER);
    private String  sqlUrl = GlConfigVars.getConfigVar(GlConfigVars.dbUrl, GlConfigVars.JRECSERVER);
    private String sqlUser = GlConfigVars.getConfigVar(GlConfigVars.dbUser, GlConfigVars.JRECSERVER);
    private String sqlPasswd = GlConfigVars.getConfigVar(GlConfigVars.dbPassword, GlConfigVars.JRECSERVER);
    private String masterPasswd = GlConfigVars.getConfigVar("masterPassword", GlConfigVars.JRECSERVER);
    
            
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        //spin off a thread in init to read the model.
        initT = new Thread(new ModelReader(defaultModFile, defaultAvgModFile));
        initT.start();
    }

    /** Destroys the servlet.
    */
    public void destroy() {
    }


    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
    * Some of the requests made are:
    * getrecs: userid, numrecs(optional)
    * getpreds: userid, item, numrecs(optional)
    * getbasketrecs: userid, item_rating(given as itemid,rating), numrecs(optional)
    * @param request servlet request
    * @param response servlet response
    */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {
        java.io.PrintWriter out = response.getWriter();
        // Added by Venkat - planB
        Date fileDate = new Date();
        String fileSuf = fileDate.toString();       
        FileOutputStream fos = new FileOutputStream("planb_"+fileSuf.substring(8,10)
            +"_"+fileSuf.substring(4,7)+Thread.currentThread().getName(),true);
        PrintWriter exptOut = new PrintWriter(fos);
        String ratingThresh = request.getParameter("ratingThresh");
        String recModel = request.getParameter("recModel");
        // remove when done

        ServletContext myContext = getServletContext();
        int numRecs = 10;
        String reqType = request.getParameter("request");
        String userID = request.getParameter("userid");
        String passwd = request.getParameter("passwd");
        String nRecs = request.getParameter("numrecs");
        String extraInfo = request.getParameter("extraInfo");
        String[] predList = request.getParameterValues("item");
        String[] basket = request.getParameterValues("item_rating");

        // This is untested code to filter by media, genre, etc.
        // If we wish to do this at the jrecserver level, we should uncomment and test 
        //        String mediaType = request.getParameter("media");
        //        String[] gFilter = request.getParameterValues("genre");
        //        String afterDate = request.getParameter("after");
        //        String beforeDate = request.getParameter("before");
    

        /*
         For now, comment out "admin" and "adminsubmit" because they'd
         need to be password protected. For "admin" just check 
         passwd.equals(masterPasswd), same for "adminsubmit", and add
         the masterPasswd to the submit in the admin form (along with
         making it POST so it doesn't show up in web logs!).
        */
        /*
        if (reqType.equals("adminsubmit")) {
            configSubmit(request, response);
        }else if (reqType.equals("admin")) {
            response.setContentType("text/html");
                configRequest(request, response, out);
            }
        } else
        */

        if(reqType.equals("loadmodel")) {
            Thread readerT = new Thread(new ModelReader(request.getParameter("modfile"),
                                                            request.getParameter("avgmodfile")));
                readerT.start();
            } else if(reqType.equals("getiteminfo")) {
            response.setContentType("text/xml");
            out.println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
            out.println("<recapi>");
            getItemInfo(out);
            out.println("</recapi>");
        } else {
            response.setContentType("text/xml");
           
            if (nRecs == null) {
                // As long as we don't have more than MAX INTEGER items this should work
                numRecs = Integer.MAX_VALUE;
            } else {
                numRecs = Integer.parseInt(nRecs);
            }

            TreeSet myTopN = null;            
            AdaptiveRec myRE = null;
            
            // figureout what filter set to use.
            // HashSet myFilter = createFilterSet(mediaType, gFilter, afterDate, beforeDate);
            
            try {
                out.println("<?xml version=\"1.0\"?>");
                out.println("<recapi>");
                
                if (null == cosModel) {
                    out.println("<error>Model not initialized</error></recapi>");
                }
                else {
                    myRE = new AdaptiveRec(cosModel,avgModel);
                    myRE.setShowDebug(true);
                    // Added by Venkat - planB
                    if(ratingThresh != null){
                        myRE.setRatingThresh(Integer.parseInt(ratingThresh));
                        // default recModel is cosine
                        myRE.setSimThresh(-100000);
                        if(recModel!=null){
                            if(recModel.equals("average")){
                                myRE.setSimThresh(100000);                                    
                            }                            
                        }
                    }            
                    // remove when done
                    NumberFormat outForm = new DecimalFormat("0.##");
                    if (reqType.equals("getrecs")) {
                        if(userID == null) {
                            out.println("<error> Must specify userID </error>");
                        } else if(checkPassword(userID, passwd, out)) {
                            // Added by Venkat - planB
                            HashSet exptSet = getExptSet(out);
                            // remove when done
                            User myUser = new User(userID);
                            Date d1 = new java.util.Date();
                            long sTime = d1.getTime();
                            myTopN = myRE.getRecs(myUser,numRecs,false,null);
                            Date d2 = new java.util.Date();
                            long fTime = d2.getTime();
                            System.out.println("User = " + userID + " Time = " + (fTime - sTime));
                            Iterator it = myTopN.iterator();
                            int i = 0;

                            out.println("<userid>" + userID + "</userid>");
                            while (it.hasNext() && i < numRecs) {
                                Recommendation r = (Recommendation)it.next();
                                //The following is correct.  To get the engine to sort with pred as primary key
                                // and similarity as secondary the quickest way was to switch them around.
                                //TODO:  if adaptive is a win long term fix this in the engine so the semantics aren't backward.
                                out.print("<item movie=\"" + r.getItemID() +
                                            "\" pred=\"" + outForm.format(r.getSimilarityScore()) + "\" "
                                            + "sim=\"" + outForm.format(r.getPrediction()) + "\" ");
                                if (extraInfo != null) {
                                    out.print(" numItemRats=\"" + outForm.format(avgModel.getNumRatings(r.getItemID())) + "\" "
                                        + "type=\"" + r.getTypeString() + "\"");                                        
                                }
                                out.println("/>");
                                // Added by Venkat - planB
                                if(ratingThresh == null && exptSet.contains(new Integer(r.getItemID()))){
                                    // Write to file
                                    exptOut.println(userID+","+r.getItemID()+","+r.getType()+","+ r.getTypeString()+","+outForm.format(r.getPrediction())+
                                        ","+outForm.format(r.getSimilarityScore())+ ","+avgModel.getNumRatings(r.getItemID())+
                                         ","+new Date());
                                }
                                if(!it.hasNext() && ratingThresh == null){
                                    exptOut.println("*** End for user"+userID);
                                    
                                }
                                // remove when done
                                i++;
                            }
                            exptOut.flush(); // remove when done - Venkat planB
                            exptOut.close(); // remove when done - Venkat planB
                        } else {
                            out.println("<error>incorrect password</error>");
                        }
                    } else if (reqType.equals("getpreds")) {
                        if(userID == null) {
                            out.println("<error> Must specify userID </error>");
                        } else if(checkPassword(userID, passwd, out)) {

                            User myUser = new User(userID);

                            out.print("<userid>" + userID + "</userid>");
                            for (int i = 0; i<predList.length; i++) {
                                float pred = myRE.ipredict(myUser,Integer.parseInt(predList[i])).getHalfStarPred();
                                out.println("<item movie=\"" + predList[i] + "\" pred=\"" + outForm.format(pred) + "\" />");
                            }
                        } else {
                            out.println("<error>incorrect password</error>");
                        }
                    } else if(reqType.equals("getbasketrecs")) {
                        Set marketBasket = new HashSet();
                        User dummyUser = new User();
                        int i;
                
                        for(i = 0; i < basket.length; i++) {
                            int id;
                            float rating;
                            int index = basket[i].indexOf(',');
                            
                            if(index  == -1){
                                out.println("<error> Incorrect arguments. for getbasketrecs: item=id,rating</error></recapi>");
                                return;
                            }
                            id = Integer.parseInt(basket[i].substring(0, index));
                            rating = Float.parseFloat(basket[i].substring(index + 1));
                            
                            if((rating <= 0 && rating != -1) || rating > 5 ){
                                out.println("<error> Rating must be between 0.5 and 5 inclusive or -1</error></recapi>");
                                return;
                            }
                            
                            dummyUser.addRating(id, rating);
                        }
                       
                        myTopN = myRE.getRecs(dummyUser, numRecs, false, null);
                        
                        Iterator it = myTopN.iterator();
                        i = 0;
                       
                        while (it.hasNext() && i < numRecs) {
                            
                            Recommendation r = (Recommendation)it.next();
                            //The following is correct.  To get the engine to sort with pred as primary key
                            // and similarity as secondary the quickest way was to switch them around.
                            //TODO:  if adaptive is a win long term fix this in the engine so the semantics aren't backward.
                            
                            out.println("<item movie=\"" + r.getItemID() +
                                        "\" pred=\"" + outForm.format(r.getSimilarityScore()) +
                                        "\" sim=\""+ outForm.format(r.getPrediction()) + "\" />" );

                            
                            i++;
                        }
                        
                    }
                    
                    out.print("</recapi>");
                }
            }
            catch (Exception e){
                System.err.println("Caught an error while processing rec/pred request");
                System.err.println(e);
                e.printStackTrace();
                out.println("<error>" + Utils.throwableToString(e) + "</error>");
                out.print("</recapi>");
            }
        }
    }

    /** 
     * prints to the PrintWriter all the items from the database with their id, title and genre
     * @param out
     */
    public void getItemInfo(java.io.PrintWriter out) {
        DBConnection myConn = new DBConnection(sqlUrl,sqlUser,sqlPasswd);
        Connection conn = myConn.getConn();
        Statement stmt = null;
        PreparedStatement prepStat = null;
        ResultSet rs = null;
        ResultSet resSet = null;
        try {
              stmt = conn.createStatement();
              rs = stmt.executeQuery("select movieid, title from movie_data");
              int movieId;
              String title;
              String genreId;
              while (rs.next()) {
                  movieId = rs.getInt("movieid");
                  title = Utils.filterToHtml(rs.getString("title"));
                  out.print("<item id = \"" + movieId + "\" title = \"" + title +"\" >");
                  prepStat = conn.prepareStatement( "select info from info_genre, movie_genre_pairs where movie_genre_pairs.genreid = info_genre.genreid and movie_genre_pairs.movieid = ?");
                  prepStat.setInt(1, movieId);
                  resSet = prepStat.executeQuery();
                  String genre;
                  while (resSet.next()) {
                      genre = resSet.getString("info");
                      out.print("<genre>" + genre + "</genre>");
                  }
                  out.println("</item>");
                  out.println(" ");
              }
        }
        catch (SQLException E) {
             DBConnection.logSqlException(E);
        }
        finally {
            myConn.dbClose();
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (prepStat != null) {
                    prepStat.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (resSet != null) {
                    resSet.close();
                }
            }
            catch (SQLException E) {
                DBConnection.logSqlException(E);
            }
        }
    }

    private HashSet createFilterSet(String mediaType, String[] gFilter, String afterDate, String beforeDate) {
        // Check to see if this is one of the three standard special cases.
        // if it is a special case, return one of the 3 predefined filter sets.
        if ( mediaType == null && gFilter == null && afterDate == null && beforeDate == null) {
            return null;
        }
        if ( mediaType != null && gFilter == null && afterDate == null && beforeDate == null) {
            if( mediaType.equals("dvd")) {
                return newDVD.getFilterSet();
            } else if (mediaType.equals("video")) {
                return newVideo.getFilterSet();
            } else {
                return newFilm.getFilterSet();
            }
        }
        Vector g = new Vector();
        for (int i = 0; i < gFilter.length; i++) {
            g.add(gFilter[i]);
        }
        RecFilter r = new RecFilter(g,afterDate,beforeDate,mediaType);
        return r.getFilterSet();
    }

    /** Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    public String getConfigs() {
        boolean canConnect = false;
        try {
            DBConnection conn = new DBConnection();
            conn.finalize();
            canConnect = true;
        }
        catch (Throwable t) {
        }

        String modelLastLoadedStr = "never";
        if (null != modelLastLoaded) {
            modelLastLoadedStr = modelLastLoaded.toString();
        }

        String retString = "<jrecconfig>";
        retString += "<modeltable>" + GenericModel.getDefaultModelTable() + "</modeltable>";
        retString += "<recEngineUrl>" + GlConfigVars.getConfigVar("recEngineUrl", GlConfigVars.JRECSERVER) + "</recEngineUrl>";
        retString += "<trunc>" + modelTrunc + "</trunc>";
        retString += "<dburl>" + DBConnection.getDefaultURL() + "</dburl>";
        retString += "<dbuser>" + DBConnection.getDefaultUser() + "</dbuser>";
        retString += "<dbpass>" + DBConnection.getDefaultPW() + "</dbpass>";
        retString += "<canConnect>" + canConnect + "</canConnect>";
        retString += "<modelLastLoadedDate>" + modelLastLoadedStr + "</modelLastLoadedDate>";
        retString += "<modfile>" + defaultModFile + "</modfile>";
        retString += "<avgmodfile>" + defaultAvgModFile + "</avgmodfile>";
        retString += "</jrecconfig>";

        return retString;
    }

    public void configRequest(HttpServletRequest request, HttpServletResponse response, java.io.PrintWriter out) {
        StringReader xmlData = new StringReader(getConfigs());
        ServletContext myContext = getServletContext();
        File xsltFile = new File(myContext.getRealPath("/web/admin.xsl"));
        Source xmlSource  = new StreamSource(xmlData);
        Source xsltSource = new StreamSource(xsltFile);
        try {
            TransformerFactory transFact =
                    TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            trans.transform(xmlSource, new StreamResult(out));

        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public void configSubmit(HttpServletRequest request, HttpServletResponse response) {
        // Grab the parameters from the submitted form.
        // set the appropriate values
        String mt = request.getParameter("modeltable");
        if (mt != null) GenericModel.setDefaultModelTable(mt);
        String t = request.getParameter("trunc");
        if (t != null) modelTrunc = Integer.parseInt(t);
        String dburl = request.getParameter("dburl");
        if (dburl != null) DBConnection.setDefaultURL(dburl);
        String dbuser = request.getParameter("dbuser");
        if (dbuser != null) DBConnection.setDefaultUser(dbuser);
        String dbpass = request.getParameter("dbpass");
        if (dbpass != null) DBConnection.setDefaultPW(dbpass);
    }


    //password protection for jrecserver

    /**
     * Check that the password passed in is proper for this user, or is the
     * master password. 
     * 
     * @param userId  User to check for
     * @param passwd  Password to check
     * @param out     Stream on which to print errors
     */
    private boolean checkPassword(String userID, String passwd, java.io.PrintWriter out) {
        ResultSet resSet = null;
        Connection con = null;
        PreparedStatement prepStat = null;
        boolean retVal = false;

        if (null == masterPasswd) {
            System.err.println("WARNING: masterPasswd is null.");
        }

        try{
            if ((null != masterPasswd) && masterPasswd.equals(passwd)) {
                retVal = true;
            } else {
                Driver driver = (Driver) Class.forName(sqlDriver).newInstance();
                con = DriverManager.getConnection(sqlUrl, sqlUser, sqlPasswd);
                try {
                    prepStat = con.prepareStatement( "Select userPasswd from user_data where userId = ? ");
                    prepStat.setInt(1, Integer.parseInt(userID));
                    resSet = prepStat.executeQuery();
                    if(resSet.next()) {
                        String passwdDB = resSet.getString("userPasswd");
                        if(passwdDB.equals(passwd)){
                        retVal = true;
                        }
                    }  
                }catch(Exception e) {
                    System.err.println("Caught an exception in checking passwd"); 
                    System.err.println(e);          
                    out.println("<error>" + Utils.throwableToString(e) + "</error>");
                }finally {
                    if(con != null ){
                        con.close();
                    }   
                }            
            }        
        } catch(Exception e) {
            System.err.println("Caught an exception in checking passwd"); 
            System.err.println(e);          
            out.println("<error>" + Utils.throwableToString(e) + "</error>");
        }

        return retVal;
    }
    // Added by Venkat for planB
    private HashSet getExptSet(java.io.PrintWriter out) {
        ResultSet resSet = null;
        Connection con = null;
        PreparedStatement prepStat = null;
        boolean retVal = false;
        HashSet retSet = new HashSet();
    
         try{
            Driver driver = (Driver) Class.forName(sqlDriver).newInstance();
            con = DriverManager.getConnection(sqlUrl, sqlUser, sqlPasswd);
            try {
                prepStat = con.prepareStatement( "Select movieId from venkat_planb");
                resSet = prepStat.executeQuery();
                resSet.beforeFirst();
                while(resSet.next()) {
                    int mId = resSet.getInt("movieId");
                    retSet.add(new Integer(mId));
                }
                  
            }catch(Exception e) {
                    System.err.println("Caught an exception writing expt stuff"); 
                    System.err.println(e);          
                    out.println("<error>" + Utils.throwableToString(e) + "</error>");
            }finally {
                    if(con != null ){
                        con.close();
                    }   
            }            
                    
        } catch(Exception e) {
            System.err.println("Caught an exception in checking passwd"); 
            System.err.println(e);          
            out.println("<error>" + Utils.throwableToString(e) + "</error>");
        }
    
        return retSet;
    }
    
    
    // remove when done
    /**
     * Load new models, then swap out the old one
     */
    class ModelReader implements Runnable {
        String modelFile;
        String avgModelFile;

        public ModelReader(String modelfile, String avgModelFile) {
            this.modelFile = modelfile;
            this.avgModelFile = avgModelFile;
        }

        public void run() {
            synchronized (modelReadSync) {
                if (loadingModel) {
                    System.out.println("Exiting this ModelReader thread, since another thread is already loading.");
                    return;
                }

                loadingModel = true;
            }

            ZScore myFilt = new ZScore();

            try {
                if (null != modelFile) {
                    // Read in the model.
                    
                    // If cosModel == null, this is the first time, and 
                    // we don't want cosModel != null until the reading is done,
                    // so requests don't get satisfied on a partial model.
                    //
                    // If cosModel != null, and we are re-reading, then re-read
                    // into place to conserve memory.
                    //
                    // By the way, I assume the model being read is in-row-order,
                    // otherwise, the readBinaryModel() will clear() the model
                    // first.  I'd rather NOT assume this, and have readBinaryModel()
                    // be smart on the inside, but this turns out to be trickier
                    // than one might think.
                    
                    CosineRecModel newModel = cosModel;
                    if (null == newModel) {
                        newModel = new CosineRecModel(6300,6300);
                    } 
                    newModel.setShowDebug(true);
                    System.out.println(new Date() + ": Jrec.ModelReader.run(): Loading cosine rec model " + modelFile);
                    newModel.readBinaryModel(modelFile);
                    System.out.println(new Date() + ": Jrec.ModelReader.run(): Done loading cosine rec model");
                    cosModel = newModel;
                }
                else {
                    System.out.println("modelFile is null .. skipping loading that");
                }
                
                if (null != avgModelFile) {
                    System.out.println(new Date() + ": Jrec.ModelReader.run(): Loading average rec model from " + avgModelFile);
                    AverageModel newAvg = new AverageModel();
                    newAvg.setShowDebug(true);
                    newAvg.readBinaryModel(avgModelFile);
                    System.out.println(new Date() + ": Jrec.ModelReader.run(): Done loading average rec model");
                    avgModel = newAvg;
                }
                else {
                    System.out.println("avgModelFile is null .. skipping loading that");
                }
                
                initialized = true;
                modelLastLoaded = new Date();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                loadingModel = false;
            }
        }
    }
}
