package org.grouplens.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.*;

/**
 * @author dfrankow
 */
public class Utils {
    /** This method gives a useful string from a Throwable. */
    public static String throwableToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
    /**
        * Filters an XML input such that it can be displayed in a browser as HTML
        * Not optimized for speed. To be used for debugging mainly. Implemented as 
        * a series of calls to >code>replaceSubString</code>
        * @param input input string
        * @return filtered string
        */
       public static String filterToHtml(String input) {
           if (input == null) return null;
    
           input = replaceSubString(input, "&", "&amp;");
           input = replaceSubString(input, "<", "&lt;");
           input = replaceSubString(input, ">", "&gt;");
           input = replaceSubString(input, "\n", "<br>");
           input = replaceSubString(input, "\"", "&quot;");
           input = replaceSubString(input, "\'", "&apos;");
    
           return (input);
       }
    
       /**
        * Filters a string input to safe characters. 
        * Not optimized for speed. To be used sparingly. Implemented as 
        * a series of calls to <code>replaceSubString</code>
        * @param input input string
        * @return filtered string
        */
       public static String filterToSafe(String input) {
           if (input == null) return null;
           input = replaceSubString(input, "&", "&amp;");
           input = replaceSubString(input, "\'", "&apos;");
           input = replaceSubString(input, "\"", "&quot;");
           return (input);
       }
    
       /**
        * Performs a regular expression replacement on the <code>input</code> string
        * by replacing all occurances of pattern <code>patt</code> with string 
        * <code>repl</code>
        * @param input input string
        * @param patt regex pattern
        * @param repl repacement string
        * @return a new, modified string 
        */
       public static String replaceSubString(String input, String patt, String repl) {
           Pattern p = Pattern.compile(patt);
           Matcher m = p.matcher(input);
           StringBuffer sb = new StringBuffer();

           while (m.find()) {
               m.appendReplacement(sb, repl);
           }
           m.appendTail(sb);
           return sb.toString();
       }
    
    
}
