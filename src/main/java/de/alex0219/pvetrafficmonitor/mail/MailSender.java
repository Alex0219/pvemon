package de.alex0219.pvetrafficmonitor.mail;

import de.alex0219.pvetrafficmonitor.PVEMonitor;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Created by Alexander on 10.06.2020 19:49
 * © 2020 Alexander Fiedler
 */
public class MailSender {


    String to;
    String from;
    String subject;
    String text;
    Integer vm;
    Integer netoutValue;
    private final long MEGABYTE = 1048576L;

    public MailSender(final String to, final String from, final String subject, final String text, final Integer vm, final Integer netoutValue) {
        this.to = to;
        this.from = from;
        this.subject = subject;
        this.text = text;
        this.vm = vm;
        this.netoutValue = netoutValue;
    }

    public void sendMail() {
        final String username = PVEMonitor.getInstance().getConfigurationManager().properties.get("SMTP-Username").toString();
        final String password = PVEMonitor.getInstance().getConfigurationManager().properties.get("SMTP-Password").toString();
        final Properties props = new Properties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", PVEMonitor.getInstance().getConfigurationManager().properties.get("SMTP-Server"));
        props.put("mail.smtp.port", PVEMonitor.getInstance().getConfigurationManager().properties.get("SMTP-Port"));
        final Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            final Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Netout alert about VM " + this.vm);
            message.setText("Netout alert: \nVM " + this.vm + " has a high netout. \nNetout-Value: " + this.bytesToMegabyte((long)this.netoutValue) + "MB");
            System.out.println("Sending email");
            Transport.send(message);
            System.out.println("Done sending email");
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public long bytesToMegabyte(final long bytes) {
        return bytes / 1048576L;
    }
}
