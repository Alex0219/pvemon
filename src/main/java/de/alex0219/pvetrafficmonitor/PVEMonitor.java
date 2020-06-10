package de.alex0219.pvetrafficmonitor;

import de.alex0219.pvetrafficmonitor.configuration.ConfigurationManager;
import de.alex0219.pvetrafficmonitor.connector.ProxmoxConnector;
import de.alex0219.pvetrafficmonitor.db.MySQLConnector;
import de.alex0219.pvetrafficmonitor.poller.NetworkPoller;
import de.alex0219.pvetrafficmonitor.task.WatchdogTask;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

/**
 * Created by Alexander on 10.06.2020 19:30
 * © 2020 Alexander Fiedler
 */
public class PVEMonitor {

    public static PVEMonitor instance;
    ProxmoxConnector proxmoxConnector;
    MySQLConnector mySQlConnector;
    ConfigurationManager configurationManager;
    NetworkPoller networkPoller;
    public ArrayList<Integer> alreadyReported;

    private PVEMonitor() {
        this.alreadyReported = new ArrayList<Integer>();
        PVEMonitor.instance = this;
    }

    public static void main(final String[] args) {
        final PVEMonitor PVEMonitor = new PVEMonitor();
        (PVEMonitor.configurationManager = new ConfigurationManager()).createConfiguration();
        PVEMonitor.configurationManager.loadPropertiesFile();
        PVEMonitor.connectToDatabase();
        PVEMonitor.connectToInstance();
        PVEMonitor.startNetworkPoller();
    }

    private void connectToInstance() {
        (this.configurationManager = new ConfigurationManager()).createConfiguration();
        this.configurationManager.loadPropertiesFile();
        this.proxmoxConnector = new ProxmoxConnector(this.configurationManager.getProperties().getProperty("Proxmox-Host"), "watchdog", "pve", this.configurationManager.getProperties().getProperty("Proxmox-Password"), true);
        try {
            this.proxmoxConnector.login();
            final Timer timer = new Timer();
            timer.schedule(new WatchdogTask(), 0L, 60000L);
        }
        catch (IOException | LoginException e2) {
            e2.printStackTrace();
        }
    }

    public void startNetworkPoller() {
        this.networkPoller = new NetworkPoller();
    }

    public void connectToDatabase() {
        mySQlConnector = new MySQLConnector(this.configurationManager.getProperties().getProperty("MySQL-Host"), Integer.valueOf(this.configurationManager.getProperties().getProperty("MySQL-Port")), this.configurationManager.getProperties().getProperty("MySQL-Username"), this.configurationManager.getProperties().getProperty("MySQL-Database"), this.configurationManager.getProperties().getProperty("MySQL-Password"));
        mySQlConnector.establishConnection();
    }

    public static PVEMonitor getInstance() {
        return PVEMonitor.instance;
    }

    public MySQLConnector getMySQlConnector() {
        return this.mySQlConnector;
    }

    public ProxmoxConnector getProxmoxConnector() {
        return this.proxmoxConnector;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public NetworkPoller getNetworkPoller() {
        return this.networkPoller;
    }
}
