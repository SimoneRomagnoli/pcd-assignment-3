package part2.rmi.controller;

import part2.rmi.remotes.BoardStatus;
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

    private static final int BASE_PORT = 1098;
    private static final String HOST_LIST = "hostlist";
    private static final String BOARD = "boardStatus";

    private final List<RemoteHost> remoteHosts;

    private final int id;

    public Propagator(int id, List<RemoteHost> remoteHosts) {
        this.remoteHosts = remoteHosts;
        this.id = id;
    }

    public void propagate() throws RemoteException, NotBoundException {
        List<RemoteHost> reachableHosts = reachableHosts();
        Registry localRegistry = LocateRegistry.getRegistry(BASE_PORT+this.id);
        HostList hostlist = (HostList) localRegistry.lookup(HOST_LIST);
        BoardStatus board = (BoardStatus) localRegistry.lookup(BOARD);

        for (RemoteHost host : reachableHosts) {
            if(host.getId() != this.id) {
                try {
                    final int port = BASE_PORT + host.getId();
                    Registry registry = LocateRegistry.getRegistry(port);
                    registry.rebind(HOST_LIST, hostlist);
                    registry.rebind(BOARD, board);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<RemoteHost> reachableHosts() {
        flagUnreachableHosts(this.remoteHosts);
        return this.remoteHosts.stream().filter(RemoteHost::isReachable).collect(Collectors.toList());
    }

    private void flagUnreachableHosts(List<RemoteHost> hosts) {
        for (RemoteHost host : hosts.stream().filter(RemoteHost::isReachable).collect(Collectors.toList())) {
            if(host.getId() != this.id) {
                try {
                    final int port = BASE_PORT + host.getId();
                    LocateRegistry.getRegistry(port).lookup(HOST_LIST);
                } catch (RemoteException | NotBoundException e) {
                    System.out.println("Could not reach node " + host.getId() + " that has port "+ (BASE_PORT+host.getId()));
                    System.out.println("Removing the node from distributed host list");
                    host.setUnreachable();
                }
            }
        }
    }
}
