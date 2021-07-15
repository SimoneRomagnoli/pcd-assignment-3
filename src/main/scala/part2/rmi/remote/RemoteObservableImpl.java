package part2.rmi.remote;

import java.io.Serializable;

public class RemoteObservableImpl implements RemoteObservable, Serializable {

    private final Controller controller;

    public RemoteObservableImpl(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void update() {
        this.controller.update();
    }
}
