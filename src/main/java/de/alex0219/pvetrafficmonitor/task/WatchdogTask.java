package de.alex0219.pvetrafficmonitor.task;

import de.alex0219.pvetrafficmonitor.PVEMonitor;
import de.alex0219.pvetrafficmonitor.mail.MailSender;
import us.monoid.json.JSONException;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.io.IOException;
import java.util.Map;
import java.util.TimerTask;

/**
 * Created by Alexander on 10.06.2020 19:41
 * © 2020 Alexander Fiedler
 */
public class WatchdogTask extends TimerTask {

    @Override
    public void run() {
        try {
            System.out.println("Checking all vms..");
            for (final String nodes : PVEMonitor.getInstance().getProxmoxConnector().getNodes()) {
                for (final Integer vms : PVEMonitor.getInstance().getProxmoxConnector().getQemuMachines(nodes)) {
                    System.out.println("Checking vm " + vms);
                    if (!PVEMonitor.getInstance().getProxmoxConnector().getQuemuMachineStatus(nodes, vms).contains("stopped")) {
                        final String time = PVEMonitor.getInstance().getProxmoxConnector().getNetoutSpecific(nodes, vms).split(":")[0];
                        final String vmNetout = PVEMonitor.getInstance().getProxmoxConnector().getNetoutSpecific(nodes, vms).split(":")[1];
                        final String vmNetin = PVEMonitor.getInstance().getProxmoxConnector().getNetinSpecific(nodes, vms).split(":")[1];

                        PVEMonitor.getInstance().getNetworkPoller().insertTrafficData(vms.toString(), time, Float.valueOf(vmNetout), Float.valueOf(vmNetin));
                    }
                    for (final Map.Entry<String, Integer> traffic : PVEMonitor.getInstance().getProxmoxConnector().getNetout(nodes, vms).entrySet()) {
                        final Integer netout = (Integer) traffic.getValue();
                        // Stop vm if network bandwidth is more than 50MB
                        if (netout > 120000000) {
                            if (PVEMonitor.getInstance().getProxmoxConnector().getQuemuMachineStatus(nodes, vms).contains("stopped")) {
                                System.out.println("[PVEMonitor] Vm " + vms + " had high netout values but was already stopped by us or stopped by the customer.");
                                return;
                            }
                            if (PVEMonitor.getInstance().alreadyReported.contains(vms)) {
                                continue;
                            }
                            System.out.println("[PVEMonitor] Vm " + vms + " has been stopped due to high netout values.");
                            final String time2 = PVEMonitor.getInstance().getProxmoxConnector().getNetoutSpecific(nodes, vms).split(":")[0];
                            PVEMonitor.getInstance().getNetworkPoller().insertNetoutAlert(String.valueOf(vms), time2, netout);
                            final MailSender mailSender = new MailSender("sendto@this.email", "sendfrom@this.email", "Netout alert", "Netout warning: " + vms, vms, netout);
                            mailSender.sendMail();
                            PVEMonitor.getInstance().alreadyReported.add(vms);
                            final Timer timer = new Timer(1800000, arg0 -> PVEMonitor.getInstance().alreadyReported.remove(vms));
                            timer.setRepeats(false);
                            timer.start();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LoginException e2) {
            e2.printStackTrace();
        } catch (JSONException e3) {
            e3.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
    }

}
