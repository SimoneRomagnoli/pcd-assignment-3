package part2.rmi.remote;

import java.io.Serializable;
import java.rmi.RemoteException;

public class NotifierImpl implements Notifier, Serializable {

    private int changes;

    public NotifierImpl() {
        this.changes = 0;
    }

    @Override
    public void notifyChanges() throws RemoteException {
        this.changes++;
    }

    @Override
    public String getChanges() throws RemoteException {
        return String.valueOf(changes);
    }


}
