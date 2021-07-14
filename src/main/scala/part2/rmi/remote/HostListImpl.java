package part2.rmi.remote;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class HostListImpl implements HostList {

    private List<RemoteHost> hostList;

    public HostListImpl() {
        this.hostList = new ArrayList<>();
    }

    public HostListImpl(HostList hostList) {
        try {
            this.hostList = new ArrayList<>(hostList.getHostList());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<RemoteHost> getHostList() throws RemoteException {
        return this.hostList;
    }

    @Override
    public synchronized void join(RemoteHostImpl host) throws RemoteException {
        System.out.println("A new host has joined: "+host.toString());
        hostList.add(host);
        System.out.println("Now host list is: "+this.hostList.toString());
    }
}
