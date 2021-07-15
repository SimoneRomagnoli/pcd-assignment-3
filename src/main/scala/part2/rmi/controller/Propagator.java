package part2.rmi.controller;

import part2.rmi.remotes.BoardStatus;
import part2.rmi.remotes.Counter;
import part2.rmi.remotes.HostList;
import part2.rmi.remotes.RemoteHost;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Propagator implements Serializable {

    public static final int BASE_PORT = 1098;
    public static final String HOST_LIST = "hostlist";
    public static final String BOARD = "boardStatus";

    private final HostList hostlist;
    private final BoardStatus board;

    private final int id;

    public Propagator(int id, HostList hostlist, BoardStatus board) {
        this.hostlist = hostlist;
        this.board = board;
        this.id = id;
    }

    public void propagate() {
        List<RemoteHost> reachableHosts = reachableHosts();

        for (RemoteHost host : reachableHosts) {
            if(host.getId() != this.id) {
                try {
                    Registry registry = LocateRegistry.getRegistry(BASE_PORT + host.getId());

                    //PROPAGATE BOARD
                    BoardStatus remoteBoardStatus = (BoardStatus) registry.lookup(BOARD);
                    remoteBoardStatus.remoteUpdate(board.getSelectedList(), board.getCurrentPositions());

                    //PROPAGATE HOST LIST
                    HostList remoteHostlist = (HostList) registry.lookup(HOST_LIST);
                    remoteHostlist.remoteUpdate(hostlist.getHostList());
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<RemoteHost> reachableHosts() {
        List<RemoteHost> hosts = new ArrayList<>();
        try {
            hosts = hostlist.getHostList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        flagUnreachableHosts(hosts);

        return hosts.stream().filter(RemoteHost::isReachable).collect(Collectors.toList());
    }

    private void flagUnreachableHosts(List<RemoteHost> hosts) {
        for (RemoteHost host : hosts.stream().filter(RemoteHost::isReachable).collect(Collectors.toList())) {
            try {
                LocateRegistry.getRegistry(BASE_PORT + host.getId()).lookup(HOST_LIST);
            } catch (RemoteException | NotBoundException e) {
                System.out.println("Could not reach node " + host.getId());
                System.out.println("Removing the node from distributed host list");
                host.setUnreachable();
            }
        }
    }
}
