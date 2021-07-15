package part2.rmi.remotes;

import part2.rmi.controller.Propagator;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class BoardStatusImpl implements BoardStatus {

    private List<Integer> selectedList;
    private List<Integer> currentPositions;

    private Propagator propagator;
    private LocalObserver observer;

    public BoardStatusImpl(List<Integer> selectedList, List<Integer> currentPositions) {
        this.selectedList = selectedList;
        this.currentPositions = currentPositions;
    }

    @Override
    public void select(int index, int id) throws RemoteException {
        if(!selectedList.contains(id)) {
            this.selectedList.set(index, id);
        } else {
            final int firstIndex = this.selectedList.indexOf(id);
            this.selectedList.set(firstIndex, 0);

            final int firstPosition = this.currentPositions.get(firstIndex);
            this.currentPositions.set(firstIndex, this.currentPositions.get(index));
            this.currentPositions.set(index, firstPosition);
        }

        this.propagator.propagate();
        this.observer.update();
    }

    @Override
    public List<Integer> getSelectedList() throws RemoteException {
        return this.selectedList;
    }

    @Override
    public List<Integer> getCurrentPositions() throws RemoteException {
        return this.currentPositions;
    }

    @Override
    public void remoteUpdate(List<Integer> remoteSelectedList, List<Integer> remoteCurrentPositions) throws RemoteException {
        this.currentPositions = new ArrayList<>(remoteCurrentPositions);
        this.selectedList = new ArrayList<>(remoteSelectedList);
        this.observer.update();
    }

    @Override
    public void setLocalObserver(LocalObserver localObserver) throws RemoteException {
        this.observer = localObserver;
    }

    @Override
    public void loadCurrentPositions(List<Integer> currentPositions) {
        this.currentPositions = new ArrayList<>(currentPositions);
    }

    @Override
    public void setPropagator(Propagator propagator) throws RemoteException {
        this.propagator = propagator;
    }
}
