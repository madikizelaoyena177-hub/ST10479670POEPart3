package com.mycompany.main1;

import org.junit.Test;
import static org.junit.Assert.*;


public class Main1Test {
    
    public Main1Test() {
    }

    /**
     * Test of main method, of class Main1.
     */
    @org.junit.Test
    public void testMain() {
        System.out.println("main");
        String[] args = null;
        Main1.main(args);
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkUserName method, of class Main1.
     */
    @org.junit.Test
    public void testCheckUserName() {
        System.out.println("checkUserName");
        String username = "";
        boolean expResult = false;
        boolean result = Main1.checkUserName(username);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkPasswordComplexity method, of class Main1.
     */
    @org.junit.Test
    public void testCheckPasswordComplexity() {
        System.out.println("checkPasswordComplexity");
        String password = "";
        boolean expResult = false;
        boolean result = Main1.checkPasswordComplexity(password);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkSouthAfricanCell method, of class Main1.
     */
    @org.junit.Test
    public void testCheckSouthAfricanCell() {
        System.out.println("checkSouthAfricanCell");
        String cell = "";
        boolean expResult = false;
        boolean result = Main1.checkSouthAfricanCell(cell);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }
    
}