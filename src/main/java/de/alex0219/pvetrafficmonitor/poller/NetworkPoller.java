package de.alex0219.pvetrafficmonitor.poller;

import de.alex0219.pvetrafficmonitor.PVEMonitor;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Alexander on 10.06.2020 19:39
 * © 2020 Alexander Fiedler
 */
public class NetworkPoller {

    private ExecutorService executorService;

    public NetworkPoller() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void insertTrafficData(final String vmid, final String time, final float netout, final float netin) {
        this.executorService.execute(() -> {
            try {
                Statement statement = PVEMonitor.getInstance().getMySQlConnector().getConnection().createStatement();
                statement.executeUpdate("INSERT INTO trafficdata (vmid, time, netout, netin) VALUES ('" + vmid + "', '" + time + "', '" + netout + "', '" + netin + "');");
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void insertNetoutAlert(final String vmid, final String time, final float netout) {
        this.executorService.execute(() -> {
            try {
                Statement statement = PVEMonitor.getInstance().getMySQlConnector().getConnection().createStatement();
                statement.executeUpdate("INSERT INTO netoutwarnings (vmid, time, netout) VALUES ('" + vmid + "', '" + time + "', '" + netout + "');");
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
