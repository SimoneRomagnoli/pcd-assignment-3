package part2.rmi.remotes;

import part2.rmi.controller.Propagator;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class HostListImpl implements HostList {

    private List<RemoteHost> hostList;
    private Propagator propagator;

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
    public synchronized void join(RemoteHost host) throws RemoteException {
        hostList.add(host);
        this.propagator.propagate();
    }

    @Override
    public void remoteUpdate(List<RemoteHost> remoteHosts) throws RemoteException {
        this.hostList = new ArrayList<>(remoteHosts);
    }


    @Override
    public void setPropagator(Propagator propagator) throws RemoteException {
        this.propagator = propagator;
    }
}
