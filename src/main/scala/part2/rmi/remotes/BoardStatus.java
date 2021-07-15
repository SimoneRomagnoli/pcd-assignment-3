package part2.rmi.remotes;

import java.util.List;
import java.rmi.RemoteException;

public interface BoardStatus extends RemoteObject {

    void select(int index, int id) throws RemoteException;

    List<Integer> getSelectedList() throws RemoteException;

    List<Integer> getCurrentPositions() throws RemoteException;

    void remoteUpdate(List<Integer> remoteSelectedList, List<Integer> remoteCurrentPositions) throws RemoteException;

    void setLocalObserver(LocalObserver localObserver) throws RemoteException;

    void loadCurrentPositions(List<Integer> currentPositions) throws RemoteException;
}
