package jre;

/*
 * Created by IntelliJ IDEA.
 * jre.User: bmiller
 * Date: Jul 19, 2002
 * Time: 5:38:18 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import org.grouplens.multilens.*;
import org.grouplens.util.GlConfigVars;

/** Build a model */
public class ModelBuilder {
    public static void main(String args[]) throws IOException, SQLException {
        if (args.length != 2) {
    	    System.out.println("usage:   java -Xmx800m -DGL_CONFIG_FILE=/foo/gl.properties jre.ModelBuilder modelfile avgmodelfile");
    	    System.out.println("example: java -Xmx800m -DGL_CONFIG_FILE=/home/vfac01/dfrankow/windows/work/MultiLens/src/org/grouplens/util/test/gl.properties jre.ModelBuilder modelfile avgmodelfile");
    	    System.exit(1);
    	}
        String modelFile = args[0];
        String avgModelFile = args[1];
        String ratingTable = GlConfigVars.getConfigVar("ratingTable", GlConfigVars.JRECSERVER);
    
        ZScore myFilt = new ZScore();
        DBRatingsSource source = new DBRatingsSource(false, ratingTable, myFilt, true);
        {
            CosineModel aModel = new CosineModel(6000,6000);
            aModel.setShowDebug(true);

            long sTime = new Date().getTime();
            // Change the threshold to 1 from 2 for Venkat planB expt - switch back to one
            // save memory for the models
            aModel.buildAndWriteCosineRecModelBinary(source, modelFile, 1);
            System.out.println(new Date() + ": Finished building and writing cosine model: total time = " + ((new Date().getTime()-sTime) / 1000) + " seconds");
        }

        {
            AverageModel newAvg = new AverageModel();
            newAvg.setShowDebug(true);

            long sTime = new Date().getTime();
            newAvg.build(source);
            System.out.println(new Date() + ": Finished building average model: total time = " + ((new Date().getTime()-sTime) / 1000) + " seconds");

            sTime = new Date().getTime();
            newAvg.writeBinaryModel(avgModelFile);
            System.out.println(new Date() + ": Finished writing average model: total time = " + ((new Date().getTime()-sTime) / 1000) + " seconds");
        }
    }
}
