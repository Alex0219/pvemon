package de.alex0219.pvetrafficmonitor.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Alexander on 10.06.2020 19:47
 * © 2020 Alexander Fiedler
 */
public class MySQLConnector {

    String host;
    Integer port;
    String username;
    String database;
    String password;
    public Connection connection;

    public MySQLConnector(final String host, final Integer port, final String username, final String database, final String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.database = database;
        this.password = password;
    }

    public void establishConnection() {
        System.out.println(this.getUsername());
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + this.getHost() + ":" + this.getPort() + "/" + this.getDatabase() + "?autoReconnect=true", this.getUsername(), this.getPassword());
            System.out.println("Connection to mysql server has been successfully established.");
            this.createTables();
        }
        catch (SQLException e) {
            System.err.println("Could not establish connection to mysql server at " + this.getHost());
            e.printStackTrace();
        }
        catch (ClassNotFoundException e2) {
            e2.printStackTrace();
        }
    }

    public void createTables() {
        try {
            final Statement statement = this.getConnection().createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `trafficdata` (\n  `id` int(11) NOT NULL AUTO_INCREMENT,\n  `vmid` int(255) NOT NULL,\n  `time` bigint(255) NOT NULL,\n  `netout` bigint(255) NOT NULL,\n  `netin` bigint(255) DEFAULT NULL,\n  PRIMARY KEY (`id`),\n  KEY `trafficdata_vmid_IDX` (`vmid`) USING BTREE,\n  KEY `trafficdata_netout_IDX` (`netout`) USING BTREE,\n  KEY `trafficdata_netin_IDX` (`netin`) USING BTREE\n,  KEY `trafficdata_time_IDX` (`time`) USING BTREE\n);");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `netoutwarnings` (\n  `id` int(11) NOT NULL AUTO_INCREMENT,\n  `vmid` int(255) NOT NULL,\n  `time` bigint(255) NOT NULL,\n  `netout` bigint(255) NOT NULL,\n  PRIMARY KEY (`id`),\n  KEY `netoutwarnings_vmid_IDX` (`vmid`) USING BTREE,\n  KEY `netoutwarnings_netout_IDX` (`netout`) USING BTREE\n,  KEY `netoutwarnings_time_IDX` (`time`) USING BTREE\n);");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Tables were successfully created.");
    }

    public String getHost() {
        return this.host;
    }

    public Integer getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getPassword() {
        return this.password;
    }

    public Connection getConnection() {
        return this.connection;
    }
}
