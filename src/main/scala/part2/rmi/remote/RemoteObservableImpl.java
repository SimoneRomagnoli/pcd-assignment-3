package part2.rmi.remote;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RemoteObservableImpl implements RemoteObservable, Serializable {

    private SimpleGUI gui;

    public RemoteObservableImpl() {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            Counter counter = (Counter) registry.lookup("countObj");
            this.gui = new SimpleGUI(0, counter);
            counter.addObservable(this);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public RemoteObservableImpl(int id) {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            Counter remoteCounter = (Counter) registry.lookup("countObj");

            Counter localCounter = new CounterImpl(remoteCounter.getValue());
            Counter counterStub = (Counter) UnicastRemoteObject.exportObject(localCounter, 0);
            LocateRegistry.getRegistry(1099+id).rebind("countObj", counterStub);

            Counter c = (Counter) LocateRegistry.getRegistry(1099+id).lookup("countObj");

            this.gui = new SimpleGUI(id, localCounter);
            c.addObservable(this);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(int value) {
        gui.updateVal(value);
    }
}
