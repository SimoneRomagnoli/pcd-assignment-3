package part2.rmi.remotes;

import java.rmi.RemoteException;

public interface Counter extends RemoteObject {

	void inc() throws RemoteException;

	int getValue() throws RemoteException;

	void remoteUpdate(int value) throws RemoteException;

	void setLocalObserver(LocalObserver localObserver) throws RemoteException;
}
