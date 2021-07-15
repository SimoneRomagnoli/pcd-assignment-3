package part2.rmi.remote;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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

    public void inc() {
        try {
            for (RemoteHost host : hostList.getHostList()) {
                Registry registry = LocateRegistry.getRegistry(1099 + host.getId());
                Counter remoteCounter = (Counter) registry.lookup("countObj");
                remoteCounter.inc();
            }
        } catch(RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        this.gui.updateVal();
    }

}
