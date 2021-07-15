package part2.rmi.remote;

import akka.io.Tcp;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.stream.Collectors;


public class Controller {

    private SimpleGUI gui;
    private Counter localCounter;
    private HostList hostList;
    private RemoteObservable observer;
    private int id;

    public Controller(int id, Counter counter, HostList hl) {
        this.id = id;
        this.localCounter = counter;
        this.hostList = hl;
        this.gui = new SimpleGUI(this.id, this);
        this.observer = new RemoteObservableImpl(this);

        try {
            this.localCounter.addObservable(this.observer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getValue() throws RemoteException {
        return this.localCounter.getValue();
    }

    public void inc() throws RemoteException {
        propagate();
    }

    public void update() {
        this.gui.updateVal();
    }

    public void propagate() {
        List<RemoteHost> reachableHosts = reachableHosts();

        for (RemoteHost host : reachableHosts) {
            try {
                Registry registry = LocateRegistry.getRegistry(1099 + host.getId());
                Counter remoteCounter = (Counter) registry.lookup("countObj");
                remoteCounter.inc();
            } catch(RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    private List<RemoteHost> reachableHosts() {
        List<RemoteHost> hosts = new ArrayList<>();
        try {
            hosts = hostList.getHostList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if(!allReachable(hosts))
            this.removeUnreachableHosts(hosts);

        return hosts.stream().filter(RemoteHost::isReachable).collect(Collectors.toList());
    }

    private boolean allReachable(List<RemoteHost> hosts) {
        boolean allPinged = true;
        for (RemoteHost host : hosts.stream().filter(RemoteHost::isReachable).collect(Collectors.toList())) {
            try {
                LocateRegistry.getRegistry(1099 + host.getId()).lookup("hostlist");
            } catch (RemoteException | NotBoundException e) {
                System.out.println("Could not reach node " + host.getId());
                System.out.println("Removing the node from distributed host list");
                host.setUnreachable();
                allPinged = false;
            }
        }
        return allPinged;
    }

    private void removeUnreachableHosts(final List<RemoteHost> hosts) {
        hosts.stream().filter(RemoteHost::isReachable).forEach(host -> {
            try {
                Registry registry = LocateRegistry.getRegistry(1099 + host.getId());
                HostList remoteHostList = (HostList)registry.lookup("hostlist");
                remoteHostList.drop(
                        hosts.stream()
                                    .filter(rh -> !rh.isReachable())
                                    .map(RemoteHost::getId)
                                    .collect(Collectors.toList())
                );
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        });
    }
}
