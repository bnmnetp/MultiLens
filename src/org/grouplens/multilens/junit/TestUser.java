package org.grouplens.multilens.junit;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.*;
import org.grouplens.multilens.*;

public class TestUser extends junit.framework.TestCase{
    public TestUser(String s) {
        super(s);
    }

    public void testDeleteRating() {
	User user = new User();
	Set s = new TreeSet();
	user.addRating(912,4);
	user.addRating(930, -1);
	assertEquals("NUM-RATINGS-AFTER-ADD", 1, user.getNumRatings());
	user.deleteRating(912);
	assertEquals("NUM-RATINGS-AFTER-DELETE",0, user.getNumRatings());
	assertTrue("MINUS-1-RATINGS", (user.getMinus1Ratings()).contains(new Integer(930)));
    }

    
    
    public static Test suite() {
        return new TestSuite(TestUser.class);
    }

}
