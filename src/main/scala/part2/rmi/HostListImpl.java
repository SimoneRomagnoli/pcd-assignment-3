package part2.rmi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class HostListImpl implements HostList {

    private List<Host> hostList;

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
    public List<Host> getHostList() throws RemoteException {
        return this.hostList;
    }

    @Override
    public synchronized void join(Host host) throws RemoteException {
        hostList.add(host);
    }
}
