package com.chanacode.core.raft;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RaftNode {

    private final String nodeId;
    private final String address;
    private final Set<String> clusterNodes;
    
    private volatile RaftState state;
    private volatile long currentTerm;
    private volatile String votedFor;
    private volatile String leaderId;
    
    private final ConcurrentSkipListMap<Long, LogEntry> raftLog;
    private volatile long commitIndex;
    private volatile long lastApplied;
    
    private final BlockingQueue<Runnable> applyQueue;
    private final ExecutorService applyExecutor;
    
    private final Map<String, RaftListener> listeners;
    private final RaftStateMachine stateMachine;
    
    private ScheduledFuture<?> electionTimer;
    private ScheduledFuture<?> heartbeatTimer;
    private final ScheduledExecutorService scheduler;
    
    private static final long ELECTION_TIMEOUT_MIN = 150;
    private static final long ELECTION_TIMEOUT_MAX = 300;
    private static final long HEARTBEAT_INTERVAL = 50;

    public RaftNode(String nodeId, String address, Set<String> clusterNodes, RaftStateMachine stateMachine) {
        this.nodeId = nodeId;
        this.address = address;
        this.clusterNodes = new HashSet<>(clusterNodes);
        this.stateMachine = stateMachine;
        
        this.state = RaftState.FOLLOWER;
        this.raftLog = new ConcurrentSkipListMap<>();
        this.applyQueue = new LinkedBlockingQueue<>();
        this.listeners = new ConcurrentHashMap<>();
        
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.applyExecutor = Executors.newSingleThreadExecutor();
        
        startApplyThread();
        startElectionTimer();
    }

    public void start() {
        log.info("RaftNode started: {} at {}", nodeId, address);
    }

    public void stop() {
        electionTimer.cancel(true);
        heartbeatTimer.cancel(true);
        scheduler.shutdown();
        applyExecutor.shutdown();
    }

    private void startElectionTimer() {
        electionTimer = scheduler.schedule(this::startElection, randomTimeout(), TimeUnit.MILLISECONDS);
    }

    private void startElection() {
        if (state == RaftState.LEADER) return;
        
        state = RaftState.CANDIDATE;
        currentTerm++;
        votedFor = nodeId;
        
        log.info("Starting election for term {}", currentTerm);
        
        int voteCount = 1;
        for (String node : clusterNodes) {
            if (requestVote(node)) {
                voteCount++;
            }
        }
        
        if (voteCount > clusterNodes.size() / 2) {
            becomeLeader();
        } else {
            startElectionTimer();
        }
    }

    private boolean requestVote(String targetNode) {
        return true;
    }

    private void becomeLeader() {
        state = RaftState.LEADER;
        leaderId = nodeId;
        log.info("Became leader for term {}", currentTerm);
        
        heartbeatTimer = scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeat() {
        if (state != RaftState.LEADER) return;
        
        for (String node : clusterNodes) {
            appendEntries(node);
        }
    }

    private boolean appendEntries(String targetNode) {
        return true;
    }

    private long randomTimeout() {
        return ELECTION_TIMEOUT_MIN + (long) (Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN));
    }

    public boolean appendLog(String command) {
        if (state != RaftState.LEADER) {
            return false;
        }
        
        LogEntry entry = new LogEntry(currentTerm, command, System.currentTimeMillis());
        raftLog.put(entry.getIndex(), entry);
        
        int replicated = 1;
        for (String node : clusterNodes) {
            if (replicateLog(node, entry)) {
                replicated++;
            }
        }
        
        if (replicated > clusterNodes.size() / 2) {
            commitIndex = entry.getIndex();
            applyLog();
        }
        
        return true;
    }

    private boolean replicateLog(String node, LogEntry entry) {
        return true;
    }

    private void applyLog() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry entry = raftLog.get(lastApplied);
            if (entry != null) {
                applyQueue.offer(() -> stateMachine.apply(entry.getCommand()));
            }
        }
    }

    private void startApplyThread() {
        applyExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Runnable task = applyQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    public void addListener(String name, RaftListener listener) {
        listeners.put(name, listener);
    }

    public RaftState getState() {
        return state;
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public enum RaftState {
        FOLLOWER, CANDIDATE, LEADER
    }

    public static class LogEntry {
        private static final AtomicLong indexGenerator = new AtomicLong(0);
        
        private final long index;
        private final long term;
        private final String command;
        private final long timestamp;

        public LogEntry(long term, String command, long timestamp) {
            this.index = indexGenerator.incrementAndGet();
            this.term = term;
            this.command = command;
            this.timestamp = timestamp;
        }

        public long getIndex() { return index; }
        public long getTerm() { return term; }
        public String getCommand() { return command; }
        public long getTimestamp() { return timestamp; }
    }

    public interface RaftStateMachine {
        void apply(String command);
    }

    public interface RaftListener {
        void onLeaderChange(String newLeader);
        void onTermChange(long newTerm);
    }
}
