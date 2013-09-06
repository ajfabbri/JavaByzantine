/*
 * Fun thread-based solution to Byzantine Generals problem.  For algorithm, see 
 * "The Byzantine Generals Problem", Lamport, Shostak, Pease 1982.
 */
package byzantine;

import byzantine.Mission;
import java.util.*;

class Message {

    public boolean value;
    public Vector<Integer> path;

    public int senderId() {
        return path.lastElement();
    }
    
    public Message(boolean value) {
        path = new Vector<Integer>();
    }
    
    public Message(Message m2) {
        value = m2.value;
        path = (Vector<Integer>)m2.path.clone();
    }
    
    /**
     * Return true iff elements of path match beginning of p
     */
    public boolean prefixMatch(Vector<Integer> p) {
        for (int i : path) {
            if (i != p.elementAt(i)) {
                return false;
            }
        }
        return true;
    }
    @Override public String toString() {
        return "Message: " + value + ", " + path;
    }
}

/**
 * A tree of messages. Could be nested inside General. I'm sure this could be
 * simplified, but it is late.
 */
class MessageTree {

    MessageNode root;

    /**
     * Interview question: Static nested classes can't access stuff inside their
     * parent, because they can be used without a instance of the parent.
     */
    public static class MessageNode {

        MessageNode parent;
        Message message;
        boolean decision;
        Vector<MessageNode> children;
        // Default constructor.
    }

    // TODO final decision code.. Simple Depth First Search with
    // decision accumultion on the walk back up to root.
    
    /**
     * Given a list of received messages, where should it live in the tree?
     */
    private MessageNode findParent(Vector<Message> messages, int round) {
        assert messages.firstElement().path.size() == round + 1;
        int i = 1;
        MessageNode node = root;
        Vector path = messages.firstElement().path;

        /* Descend tree via prefix matches until we hit our rank. */
        while (i < round) {
            for (MessageNode n : node.children) {
                if (n.message.prefixMatch(path)) {
                    node = n;
                    break;
                }
            }
        }
        return node;
    }

    /**
     * Add received list of messages to ADT for later decision making.
     */
    public void insert(Vector<Message> messages, int round) {
        if (round == 0) {
            assert messages.size() == 1;    // Usually avoid assert in public
            root = new MessageNode();
            root.parent = null;
            root.message = messages.firstElement();
        } else {
            MessageNode parent = findParent(messages, round);
            assert parent.children == null;
            for (Message m  : messages) {
                // Could optimize this out
                MessageNode node = new MessageNode();
                node.message = m;
                parent.children.add(node);
            }
        }

    }
}

/**
 * A general in the Byzantine General problem, represented as a thread.
 *
 * @author fabbri
 */
class General extends Thread {

    Mission mission;
    int id;
    /**
     * @TODO make a command line arg.
     */
    final boolean commander_should_attack = true;

    boolean amCommander() {
        return (id == 0);
    }

    boolean majority(Vector<Message> messages) {

        int truth_sum = 0;
        for (Message m : messages) {
            truth_sum += (m.value ? 1 : -1);
        }
        return (truth_sum > 0);  // More trues than falses?
    }

    public General(Mission m) {
        mission = m;
    }

    public void assignId(int id) {
        this.id = id;
    }

    /**
     * Communication phase: Iterative approach.
     */
    void communicationPhase() {
        boolean decision_this_round = false;
        Vector<Message> messages = null;

        System.out.println("Reporting for duty...");
        mission.reportForDuty(this);
        System.out.println("  ... assigned id " + id);

        for (int round = 0; round < mission.numRounds(); round++) {

            // TODO finish round even when not sending message.
            
            // Sending phase
            if (amCommander() && round == 0) {
                Message m = new Message(commander_should_attack);
                m.path.add(id);
                Vector<Message> v = new Vector<Message>();
                v.add(m);
                mission.sendRound(v, id, round);
                break;
            }

          
            // Send out copies of received messages, adding self to path,
            // and including our decision.
            Vector<Message> newMessages = new Vector();
            if (round != 0) {
                for (Message m : messages) {
                    if (m.senderId() != id) {
                        Message m_new = new Message(m); // a copy
                        m_new.path.add(id);
                        m_new.value = decision_this_round;
                        newMessages.add(m_new);
                    }
                }
            }
            mission.sendRound(newMessages, id, round);

            // Receiving phase
            messages = mission.receiveRound(id, round);

            // Deciding phase
            decision_this_round = majority(messages);
        }
    }

    public void run() {
        communicationPhase();

    }
}
