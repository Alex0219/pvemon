package de.alex0219.pvetrafficmonitor.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by Alexander on 10.06.2020 19:42
 * © 2020 Alexander Fiedler
 */
public class ConfigurationManager {

    public final Properties properties;
    public final File propertiesFile;

    public ConfigurationManager() {
        this.properties = new Properties();
        this.propertiesFile = new File("pvemonitor.properties");
    }

    public void createConfiguration() {
        try {
            if (!this.propertiesFile.exists()) {
                this.propertiesFile.createNewFile();
                this.properties.load(new FileInputStream(this.propertiesFile));
                this.properties.setProperty("Proxmox-Host", "127.0.0.1");
                this.properties.setProperty("Proxmox-Password", "abcde");
                this.properties.setProperty("MySQL-Host", "127.0.0.1");
                this.properties.setProperty("MySQL-Port", "3306");
                this.properties.setProperty("MySQL-Username", "root");
                this.properties.setProperty("MySQL-Database", "pvetracker");
                this.properties.setProperty("MySQL-Password", "abc");
                this.properties.setProperty("SMTP-Server", "127.0.0.1");
                this.properties.setProperty("SMTP-Port", "587");
                this.properties.setProperty("SMTP-Username", "changeme@localhost.email");
                this.properties.setProperty("SMTP-Password", "changeme");



                this.properties.store(new FileOutputStream("pvemonitor.properties"), null);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPropertiesFile() {
        try {
            this.properties.load(new FileInputStream(this.propertiesFile));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Properties getProperties() {
        return this.properties;
    }
}
