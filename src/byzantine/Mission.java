
package byzantine;
import java.util.Vector;


/**
 * Coordination between generals. Generals report for duty (register), and
 * coordinate their attack here.
 *
 * @author fabbri
 */
public class Mission {

    Vector<General> generals;
    int n, m;
    int round = 0, id = 0;
    int started = 0;
    
    /* Since we process one round at a time, we collect messages here,
     * indexed by sender id. */
    Vector<Vector<Message>> mailboxes;  // This can be eliminated
    Vector<Message> ready_messages;

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
        mailboxes = new Vector<Vector<Message>>();
        ready_messages = new Vector<Message>();
    }

    /**
     * Report for duty. Registers General instance with the Mission and blocks
     * until all generals are accounted for. Since method is synchronized,
     * callers acquire intrinsic lock on this object and we can do stuff like
     * wait() and notify().
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

    public synchronized void broadcast(Vector<Message> messages) {
      
        /* For now, completion of round is hearing from all generals.  We could
         * add message loss and a timeout as well. */
       
        ready_messages.isEmpty();// could avoid this after the first call
        mailboxes.add(messages);
        if (mailboxes.size() == n) {
            for (Vector<Message> mailbox : mailboxes) {
                ready_messages.addAll(mailbox);
            }
            mailboxes.clear();
            notifyAll();
        }
    }
    
    /** 
     * Block until all generals are heard from, then return received messages.
     */
    public synchronized Vector<Message> receiveRound() {
        while (ready_messages.isEmpty()) {
            try {
                wait();
            } catch (java.lang.InterruptedException e) {
            }
        }
        return ready_messages;
    }
}
