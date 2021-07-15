package part2.rmi.remote;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class SimpleGUI extends JFrame implements Serializable {

    private JPanel panel;
    private int id;
    private Counter counter;

    public SimpleGUI(int id, Counter counter) {
        setTitle("i am id "+id);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.id = id;
        this.counter = counter;

        panel = new JPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.gray));
        panel.setLayout(new FlowLayout());
        getContentPane().add(panel, BorderLayout.CENTER);

        drawButton(0);

        setVisible(true);
        pack();

    }

    private void drawButton(int value) {
        JButton btn = new JButton();
        try {
            System.out.println("In drawing button i got counter "+value+" while my value is "+counter.getValue());
            btn.setText(String.valueOf(counter.getValue()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        btn.addActionListener(al -> {
            try {
                /*
                HostList hl = (HostList) LocateRegistry.getRegistry(1099+id).lookup("hostlist");
                for(RemoteHost host: hl.getHostList()) {
                    Registry registry = LocateRegistry.getRegistry(1099+host.getId());
                    Counter counter = (Counter) registry.lookup("countObj");
                    counter.inc();
                }

                 */
                //btn.setText(String.valueOf(counter.getValue()));
                this.counter.inc();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
        panel.add(btn);
    }

    public void updateVal(int value) {
        SwingUtilities.invokeLater(() -> {
            this.panel.removeAll();
            this.drawButton(value);
        });
    }
}
