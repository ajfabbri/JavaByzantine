
package byzantine;
import java.util.Vector;
import java.util.concurrent.CyclicBarrier;

/**
 * Coordination between generals. Generals report for duty (register), and
 * coordinate their attack here.
 *
 * @author fabbri
 */
class Mission {

    Vector<General> generals;
    int n, m;
    int id = 0;
    int started = 0;
    CyclicBarrier send_barrier;
    CyclicBarrier receive_barrier;
    
    /* Since we process one round at a time, we collect messages here. */
    Vector<Message> messages;

    public int numRounds() {
        return m + 1;
    }

    public Mission(int num_generals, int num_traitors) {
        n = num_generals;
        m = num_traitors;
        if (n <= 3 * m) {
            throw new IllegalArgumentException("Requires n > 3*m.");
        }
        generals = new Vector<General>();
        messages = new Vector<Message>();
        receive_barrier = new CyclicBarrier(n-1, new ReceiveCleanup());
        send_barrier = new CyclicBarrier(n);
    }

    /** Allocates a new list for next round's messages. */
    private class ReceiveCleanup implements Runnable {
        public void run() {
            Byzantine.debugPrint("\t---Clearing messages.---");
            messages = new Vector<Message>();
        }
    }
    
    /**
     * Report for duty. Registers General instance with the Mission and blocks
     * until all generals are accounted for. Since method is synchronized,
     * callers acquire intrinsic lock on this object and we can do stuff like
     * wait() and notify().
     * Could also use CyclicBarrier here.. not as much fun.
     */
    public synchronized void reportForDuty(General g) {
        g.assignId(id++);
        generals.add(g);
        if (generals.size() == n) {
            // We are last general to report, let's begin.
            started = 1;
            notifyAll();
        } else {
            /* Loop to handle spurious wakeups; e.g. from signals. */
            while (started == 0) {
                try {
                    wait();
                } catch (java.lang.InterruptedException e) {
               
                }
            }
        }
    }

    /**
     * Send your messages, if any for round.  
     * @param messages may be null or empty.
     */
    public void sendRound(Vector<Message> m, int id,
            int round) {
      
        int arrive_index = 0;
        /* For now, completion of round is hearing from all generals.  We could
         * add message loss and a timeout as well. */
        Byzantine.debugPrint("[" + id + "] sendRound(" + round + " ) of " 
                + m);
        if (m != null) {
            // implicit on Vectors: synchronized (messages) {
            messages.addAll(m);
            // }
        }
        
        Byzantine.debugPrint("\t[" + id + "] await sends");
        try {
            arrive_index = send_barrier.await();
        } catch (Exception e) {
            System.out.println(e);
        }
        if (arrive_index == 0) {
            if (round == 0) {
                // Round 0 was special case, now commander drops out
                send_barrier = new CyclicBarrier(n-1);
            } else {
                
                // This is not racy because threads will all reach receive barrier
                // before reentering here.
                send_barrier.reset();
            }
        }
        
        Byzantine.debugPrint("[" + id + "] sendRound("+ round + ") finished");
    }
    
    /** 
     * Block until all generals are heard from, then return received messages.
     */
    public Vector<Message> receiveRound(int id, int round) {
        int arrive_index = 0;
        Byzantine.debugPrint("[" + id + "] receiveRound(" + round + ")");
    
        // grab a reference to this round's messages
        Vector<Message> round_msgs = messages;
        Byzantine.debugPrint("\t[" + id + "] await receives");
        try {
            arrive_index = receive_barrier.await();
        } catch (Exception e) {
            System.out.println(e);
        }
        // This is not racy because threads will all reach send barrier
        // before reentering here.
        if (arrive_index == 0) {
            receive_barrier.reset();
        }
        
        Byzantine.debugPrint("[" + id + "]  receiveRound(" + round +
                ") -> " + round_msgs);
       
        return round_msgs;
    }
}
