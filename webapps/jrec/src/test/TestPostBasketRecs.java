import java.util.*;
import java.util.Date;
import java.sql.*;
import java.net.*;
import java.io.*;

/** Takes as an argument that takes a filename with ratings of the form
 *  movieid   rating
 *   item1     rating1
 *   item2     rating2
 *   ..
 *   posts a request to the jrecserver and prints out the result
 */   
public class TestPostBasketRecs {   
    

    
    public static void main(String[] args){
	
	if(args.length < 1){
	    System.out.println("Usage: java TokenizeLine <file to tokenize>");
	    System.exit(-1);
	}
	
	BufferedReader in = null;
	String fileLine = null;

	try {
	    
	    
	    //get URL connection
	    URL url = new URL("http://gibson.cs.umn.edu:1613/jrec/servlet/Jrec");
	    URLConnection conn = url.openConnection();
	    conn.setDoOutput(true);
	    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	    

	    // create data 
	    int num = 0;
	    in = new BufferedReader(new FileReader(args[0]));
	    String data = URLEncoder.encode("request", "UTF-8") + "=" 
		+ URLEncoder.encode("getbasketrecs", "UTF-8");
            data += "&" + URLEncoder.encode("numrecs", "UTF-8") + "=" + URLEncoder.encode("10", "UTF-8");
	    
	    while ((fileLine = in.readLine()) != null){
		num++;
		if(num == 1) {
		    continue;
		}
		
		StringTokenizer st = new StringTokenizer(fileLine);
		
		String item_rating = st.nextToken();
		while (st.hasMoreTokens()){
		    item_rating += "," + st.nextToken();
		}
	 
		data += "&" + URLEncoder.encode("item_rating", "UTF-8") + "=" 
		    + URLEncoder.encode(item_rating, "UTF-8");
		
	    }
	    
	    wr.write(data);
	    wr.flush();
	    
	    
	    //get response
	    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    String line;
	    while ((line = rd.readLine()) != null) {
		System.out.println(line);
	    }
	    wr.close();
	    rd.close();
	    
	    
	}
	
	catch (IOException e){
	    e.printStackTrace();
	}

	catch (Exception e) {
	    System.err.println("msg: " + e.getMessage());
	}
	finally {
            try { in.close(); } catch(IOException e) { e.printStackTrace(); }
        }

    }
}
