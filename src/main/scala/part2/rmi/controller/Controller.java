package part2.rmi.controller;

import part2.rmi.puzzle.PuzzleBoard;
import part2.rmi.remotes.*;

import java.util.List;

import java.rmi.RemoteException;


public class Controller {

    private final Propagator propagator;
    private final LocalObserver observer;

    private final PuzzleBoard puzzleBoard;

    private final BoardStatus boardStatus;
    private final HostList hostList;

    private int id;

    public Controller(int id, BoardStatus boardStatus, HostList hl) {
        this.id = id;
        this.boardStatus = boardStatus;
        this.hostList = hl;

        this.puzzleBoard = new PuzzleBoard(this, id == 1);
        this.puzzleBoard.setVisible(true);
        this.observer = new LocalObserver(this);
        this.propagator = new Propagator(id, this.hostList, this.boardStatus);

        try {
            this.boardStatus.setPropagator(this.propagator);
            this.boardStatus.setLocalObserver(this.observer);

            this.hostList.setPropagator(this.propagator);

            RemoteHost localhost = new RemoteHostImpl(id);
            this.hostList.join(localhost);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getSelectedList() throws RemoteException {
        return this.boardStatus.getSelectedList();
    }

    public List<Integer> getCurrentPositions() throws RemoteException {
        return this.boardStatus.getCurrentPositions();
    }

    public void select(int index) throws RemoteException {
        this.boardStatus.select(index, this.id);
    }

    public void update() {
        this.puzzleBoard.updateBoard();
    }

    public void loadCurrentPositions(List<Integer> currentPositions) {
        try {
            this.boardStatus.loadCurrentPositions(currentPositions);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
