/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package byzantine;

import java.util.ArrayList;

/**
 * Main application container.  A command line app that runs the classic
 * Byzantine Generals problem as a group of threads.
 * @author fabbri
 */
public class Byzantine {

    private static final int NUM_GENERALS = 5;
    private static final int NUM_TRAITORS = 1;
    
    /** Choose m integers, randomly, out of [0,n). */
    private static ArrayList nChooseM(int n, int m)
    {
        ArrayList choices = new ArrayList();
        double p = m / (double)n;
        int count = 0;
        for (int i = 0; i < n && count < m; i++) {
            if (Math.random() < p) {
                choices.add(i);
                count++;
            }
        }
        return choices;
    }
    
    /**
     * @todo Take command line args
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Mission mission = new Mission(NUM_GENERALS, NUM_TRAITORS);
        ArrayList traitors = nChooseM(NUM_GENERALS, NUM_TRAITORS);
        
        ArrayList<General> generals = new ArrayList<General>();
        
        System.out.println("Hello.");
        for (int i = 0; i < NUM_GENERALS; i++) {
            General g = new General(mission);
            generals.add(g);
            g.start();
        }
        System.out.println("Started " + NUM_GENERALS + " generals.");
        
        for (General g : generals) {
            try {
                g.join();
            } catch (InterruptedException e) {
               
            }
        }
        System.out.println("Generals finished, exiting");
    }
}

