package de.alex0219.pvetrafficmonitor.connector;

import java.util.HashMap;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONException;
import us.monoid.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import us.monoid.web.JSONResource;
import javax.security.auth.login.LoginException;
import us.monoid.web.Resty;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.util.Date;

public class ProxmoxConnector {

    protected String hostname;
    protected Integer port;
    protected String username;
    protected String realm;
    protected String password;
    protected Boolean ignoreSSL;
    protected String baseURL;
    private String authTicket;
    private Date authTicketIssuedTimestamp;
    private String csrfPreventionToken;
    private static SSLSocketFactory cachedSSLSocketFactory;
    private static HostnameVerifier cachedHostnameVerifier;

    private static void ignoreAllCerts() {
        if (ProxmoxConnector.cachedSSLSocketFactory == null) {
            ProxmoxConnector.cachedSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        }
        if (ProxmoxConnector.cachedHostnameVerifier == null) {
            ProxmoxConnector.cachedHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        }
        final TrustManager trm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        final HostnameVerifier hnv = new HostnameVerifier() {
            @Override
            public boolean verify(final String s, final SSLSession sslSession) {
                return true;
            }
        };
        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { trm }, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hnv);
        }
        catch (KeyManagementException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e2) {
            e2.printStackTrace();
        }
    }

    public static void resetCachedSSLHelperObjects() {
        if (ProxmoxConnector.cachedSSLSocketFactory != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(ProxmoxConnector.cachedSSLSocketFactory);
        }
        if (ProxmoxConnector.cachedHostnameVerifier != null) {
            HttpsURLConnection.setDefaultHostnameVerifier(ProxmoxConnector.cachedHostnameVerifier);
        }
    }

    public ProxmoxConnector(final String hostname, final String username, final String realm, final String password) {
        this(hostname, username, realm, password, false);
    }

    public ProxmoxConnector(final String hostname, final String username, final String realm, final String password, final Boolean ignoreSSL) {
        this.hostname = hostname;
        this.port = 8006;
        this.username = username;
        this.realm = realm;
        this.password = password;
        this.ignoreSSL = ignoreSSL;
        if ((boolean)ignoreSSL) {
            ignoreAllCerts();
        }
        else {
            resetCachedSSLHelperObjects();
        }
        this.authTicketIssuedTimestamp = null;
        this.baseURL = "https://" + hostname + ":" + this.port.toString() + "/api2/json/";
    }

    public void login() throws IOException, LoginException {
        final Resty r = new Resty(new Resty.Option[0]);
        final JSONResource authTickets = r.json(this.baseURL + "access/ticket", Resty.form("username=" + this.username + "@" + this.realm + "&password=" + this.password));
        try {
            this.authTicket = authTickets.get("data.ticket").toString();
            this.csrfPreventionToken = authTickets.get("data.CSRFPreventionToken").toString();
            this.authTicketIssuedTimestamp = new Date();
        }
        catch (Exception e) {
            throw new LoginException("Failed reading JSON response");
        }
    }

    public void checkIfAuthTicketIsValid() throws IOException, LoginException {
        if (this.authTicketIssuedTimestamp == null || this.authTicketIssuedTimestamp.getTime() <= new Date().getTime() - 7200000L) {
            this.login();
        }
    }

    private Resty authedClient() throws IOException, LoginException {
        this.checkIfAuthTicketIsValid();
        final Resty r = new Resty(new Resty.Option[0]);
        r.withHeader("Cookie", "PVEAuthCookie=" + this.authTicket);
        r.withHeader("CSRFPreventionToken", this.csrfPreventionToken);
        return r;
    }

    private JSONResource getJSONResource(final String resource) throws IOException, LoginException {
        return this.authedClient().json(this.baseURL + resource);
    }

    public List<String> getNodes() throws IOException, LoginException, JSONException {
        final List<String> res = new ArrayList<String>();
        final JSONArray nodes = this.getJSONResource("nodes").toObject().getJSONArray("data");
        for (int i = 0; i < nodes.length(); ++i) {
            res.add(nodes.getJSONObject(i).getString("node"));
        }
        return res;
    }

    public JSONObject getTaskStatus(final String node, final String taskId) throws IOException, LoginException, JSONException {
        final JSONResource response = this.getJSONResource("nodes/" + node + "/tasks/" + taskId + "/status");
        return response.toObject().getJSONObject("data");
    }

    public JSONObject waitForTaskToFinish(final String node, final String taskId) throws IOException, LoginException, JSONException {
        JSONObject lastTaskStatus = null;
        for (Boolean isRunning = true; (boolean)isRunning; isRunning = lastTaskStatus.getString("status").equals("running")) {
            lastTaskStatus = this.getTaskStatus(node, taskId);
        }
        return lastTaskStatus;
    }

    public ArrayList<Integer> getQemuMachines(final String node) throws IOException, LoginException, JSONException {
        final ArrayList<Integer> res = new ArrayList<Integer>();
        final JSONArray qemuVMs = this.getJSONResource("nodes/" + node + "/qemu").toObject().getJSONArray("data");
        for (int i = 0; i < qemuVMs.length(); ++i) {
            final JSONObject vm = qemuVMs.getJSONObject(i);
            res.add(vm.getInt("vmid"));
        }
        return res;
    }

    public List<String> getQemuMachineSnapshots(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final List<String> res = new ArrayList<String>();
        final JSONArray snapshots = this.getJSONResource("nodes/" + node + "/qemu/" + vmid.toString() + "/snapshot").toObject().getJSONArray("data");
        for (int i = 0; i < snapshots.length(); ++i) {
            res.add(snapshots.getJSONObject(i).getString("name"));
        }
        return res;
    }

    public String rollbackQemuMachineSnapshot(final String node, final Integer vmid, final String snapshotName) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/snapshot/" + snapshotName + "/rollback";
        final JSONResource response = r.json(this.baseURL + resource, Resty.form(""));
        return response.toObject().getString("data");
    }

    public String startQemuMachine(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/status/start";
        final JSONResource response = r.json(this.baseURL + resource, Resty.form(""));
        return response.toObject().getString("status");
    }

    public String stopQemuMachine(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/status/stop";
        final JSONResource response = r.json(this.baseURL + resource, Resty.form(""));
        return response.toObject().getString("data");
    }

    public HashMap<String, Integer> getNetout(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/rrddata?timeframe=hour&cf=AVERAGE";
        final HashMap<String, Integer> res = new HashMap<String, Integer>();
        final JSONArray netoutEntries = this.getJSONResource(resource).toObject().getJSONArray("data");
        for (int i = 0; i < netoutEntries.length(); ++i) {
            final JSONObject entry = netoutEntries.getJSONObject(i);
            if (entry.has("netout")) {
                res.put(entry.getString("time"), entry.getInt("netout"));
            }
        }
        return res;
    }

    public String getNetoutSpecific(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/rrddata?timeframe=hour&cf=AVERAGE";
        String res = "";
        final JSONArray netoutEntries = this.getJSONResource(resource).toObject().getJSONArray("data");
        final JSONObject entry = netoutEntries.getJSONObject(69);
        if (entry.has("netout")) {
            res = entry.getString("time") + ":" + entry.getString("netout");
        }
        return res;
    }

    public String getNetinSpecific(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/rrddata?timeframe=hour&cf=AVERAGE";
        String res = "";
        final JSONArray netoutEntries = this.getJSONResource(resource).toObject().getJSONArray("data");
        final JSONObject entry = netoutEntries.getJSONObject(69);
        if (entry.has("netin")) {
            res = entry.getString("time") + ":" + entry.getString("netin");
        }
        return res;
    }

    public String getNetstat(final String node) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/netstat";
        final JSONResource response = r.json(this.baseURL + resource);
        return response.object().toString();
    }

    public String getQuemuMachineStatus(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/status/current";
        final JSONResource response = r.json(this.baseURL + resource);
        return response.toObject().toString();
    }

    public String getRRDImage(final String node, final Integer vmid) throws IOException, LoginException, JSONException {
        final Resty r = this.authedClient();
        final String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/rrd?cf=AVERAGE&ds=netout&timeframe=hour";
        final JSONResource response = r.json(this.baseURL + resource);
        return response.toObject().getString("data");
    }

    static {
        ProxmoxConnector.cachedSSLSocketFactory = null;
        ProxmoxConnector.cachedHostnameVerifier = null;
    }
}
