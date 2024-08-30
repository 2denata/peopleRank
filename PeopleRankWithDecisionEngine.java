/*
 * People Rank 
 * by J. P. Denata
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import routing.community.Duration;

public class PeopleRankWithDecisionEngine implements RoutingDecisionEngine, NodeRanking {

    /** identifier for damping factor */
    protected static final String DAMPING = "dampingFactor";
    
    /** identifier for threshold */
    protected static final String THRESHOLD = "threshold";

    protected static final double DAMPING_DEFAULT = 0.5;
    protected static final int THRESHOLD_DEFAULT = 3;
    
    protected double dampingFactor = 0.4;
    protected double peopleRank;
    protected int threshold;
    protected Map<DTNHost, Double> startTime; 
    protected Map<DTNHost, List<Duration>> connectionHistory;
    protected Map<DTNHost, Information<Double, Integer>> friends;

    public Map<DTNHost, List<Duration>> getConnectionHistory() {
        return connectionHistory;
    }
    
    public PeopleRankWithDecisionEngine(Settings s) {
        if (s.contains(DAMPING)) {
            dampingFactor = s.getDouble(DAMPING);
        } else {
            dampingFactor = DAMPING_DEFAULT;
        }
        
        if (s.contains(THRESHOLD)) {
            threshold = s.getInt(THRESHOLD);
        } else {
            threshold = THRESHOLD_DEFAULT;
        }
    }

    public PeopleRankWithDecisionEngine(PeopleRankWithDecisionEngine proto) {
        this.peopleRank = 0.0;
        this.startTime = new HashMap<>();
        this.connectionHistory = new HashMap<>();
        this.friends = new HashMap<>();
    }

    
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankWithDecisionEngine prde = getDecisionRouterFrom(peer);
        
        // record the start time of the meeting in the map
        this.startTime.put(peer, SimClock.getTime());
        
        if (this.friends.containsKey(peer)) {

            // send host information to peer
            int newSize = getFriendshipSize();
            Information<Double, Integer> data = new Information<>(getPeopleRank(), newSize);
            send(myHost, peer, data);
            
            // receive info from peer, update the map
            data = receive(peer);
            this.friends.put(peer, data);
            
            // update people rank for host and peer
            prde.updatePeopleRank(peer);
            updatePeopleRank(myHost);
        }
    }

    /**
     * Method to calculate the total duration of meetings between the host and peer.
     * @param peer DTNHost counterpart
     * @return total meeting time in seconds
     */
    private double calculateTotalMeetingTimeWith(DTNHost peer) {
        // if never met, return 0
        if (!connectionHistory.containsKey(peer)) {
            return 0;
        }

        double total = 0;
        List<Duration> meetingList = connectionHistory.get(peer);
        for (Duration duration : meetingList) {
            total += (duration.end - duration.start);
        }
        
        return total;
    }
    
    /**
     * Method used to send people rank information and friendship count
     * from the host node to the peer node.
     *
     * @param peer Target DTNHost peer node
     * @param information People rank and friendship data from host
     */
    private void send(DTNHost myHost, DTNHost peer, Information<Double, Integer> information) {
        PeopleRankWithDecisionEngine prde = getDecisionRouterFrom(peer);
        
        // update info on peer
        prde.friends.put(myHost, information);
        prde.updatePeopleRank(peer);
    }

    /**
     * Method used to receive people rank information and friendship count
     * from the peer node.
     *
     * @param peer DTNHost peer node
     */
    private Information receive(DTNHost peer) {
        PeopleRankWithDecisionEngine prde = getDecisionRouterFrom(peer);
        double peerRank = prde.getPeopleRank();
        int peerFriends = prde.friends.size();

        return new Information(peerRank, ++peerFriends);
    }

    /**
     * Method used to update people rank value after receiving
     * latest information from peer
     *
     * @param myHost
     */
    public void updatePeopleRank(DTNHost myHost) {
        double sigma = 0;

        //calculate sigma value
        for (Map.Entry<DTNHost, Information<Double, Integer>> entry : friends.entrySet()) {
            double tempPeopleRank = entry.getValue().getPeopleRank();
            int size = entry.getValue().getSize();
            
            if (size == 0) {
                sigma += 0;
            } else {
                sigma += (tempPeopleRank / size);          
            }
        }
        // change/update people rank value
        this.peopleRank = (1 - dampingFactor) + dampingFactor * sigma;
        
        // update in each friend's map that stores this people rank
        for (Map.Entry<DTNHost, Information<Double, Integer>> e : friends.entrySet()) {
            PeopleRankWithDecisionEngine de = getDecisionRouterFrom(e.getKey());
            
            de.friends.put(myHost, new Information<>(this.peopleRank, this.friends.size()));
        }
    }
    
    
    /**
     * Returns the value of PeR(i) from node i
     * @return people rank value of the node
     */
    @Override
    public double getPeopleRank() {
        return this.peopleRank;
    }

    /**
     * Returns the value of F(i) from node i
     * @return friendship count of the node
     */
    public int getFriendshipSize() {
        return this.friends.size();
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        PeopleRankWithDecisionEngine prde = getDecisionRouterFrom(otherHost);
        
        // forward message if destination node equals destination OR PeR(peer) >= PeR(host)
        return (prde.getPeopleRank() >= this.peopleRank || m.getTo().equals(otherHost));
    }

    // convert DTNHost to Router Decision Engine
    public PeopleRankWithDecisionEngine getDecisionRouterFrom(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "Not a Decision Engine Router";
        return (PeopleRankWithDecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }


    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // check the initial connection time
        double start = check(peer);
        double end = SimClock.getTime();
        
        /* check or create list for connectionHistory */ 
        List<Duration> history;
        
        // if first time, create a duration list in connectionHistory map
        if (!connectionHistory.containsKey(peer)) {
            history = new ArrayList<>();
            connectionHistory.put(peer, history);
        } 
        // if previously met, get the duration list from the map 
        else {
            history = connectionHistory.get(peer);
        }
        // add start and end time information to the list
        Duration meeting = new Duration(start, end);
        history.add(meeting);
        
        // remove temporary meeting time data after completion
        startTime.remove(peer);
        
        // check between host-peer. If not friends yet...
        if (!this.friends.containsKey(peer)) {

            // check total meeting time to compare with the threshold
            double totalMeeting = calculateTotalMeetingTimeWith(peer);
            
            //if it meets the threshold
            if (totalMeeting >= threshold) {
                PeopleRankWithDecisionEngine prde = getDecisionRouterFrom(peer);
                
                int peerSize = prde.getFriendshipSize();
                Information<Double, Integer> peerInfo = new Information<>(prde.peopleRank, ++peerSize);
                
                // add peer to the friend's map of the host
                this.friends.put(peer, peerInfo);
                
                // add host to the friend's map of the peer
                Information<Double, Integer> ourInfo = new Information<>(peopleRank, friends.size());
                prde.friends.put(thisHost, ourInfo);
            }            
        } 
    }

    /**
     * Method to check if the host and peer have met or not
     * @param thisHost Host
     * @param peer Peer
     * @return start time when host-peer connection is up
     */
    private double check(DTNHost peer) {
        if (startTime.containsKey(peer)) {
            return startTime.get(peer);
        }
        return 0;
    }
    
    public Map<DTNHost, Information<Double, Integer>> getFriends() {
        return friends;
    }
    
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }
    
    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return m.getTo() == otherHost;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankWithDecisionEngine(this);
    }
}
