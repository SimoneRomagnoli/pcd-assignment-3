package part2.rmi.remotes;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class HostListImpl implements HostList {

    private List<RemoteHost> hostList;
    private List<RemoteObserver> observers;

    public HostListImpl() {
        this.hostList = new ArrayList<>();
        this.observers = new ArrayList<>();
    }

    @Override
    public synchronized List<RemoteHost> getHostList() throws RemoteException {
        return this.hostList;
    }

    @Override
    public synchronized void join(RemoteHost host) throws RemoteException {
        hostList.add(host);
        for(RemoteObserver ro: observers) {
            try {
                ro.update();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
