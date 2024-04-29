import org.json.simple.*;
import org.json.simple.parser.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.jsoup.*;
import java.time.*;

public class Monitor implements Runnable
{
    private final ArrayList<String> proxies;
    private final ArrayList<String> accounts;
    private HashMap<String, String[]> hookEmbeds;
    private HashMap<String, ArrayList<String>> accountHooks;
    private int delay;
    private String session;
    private int attempts;
    
    public Monitor() throws IOException, ParseException {
        ThreadLocalAuthenticator.setAsDefault();
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("https.protocols", "TLSv1.1");
        final Scanner scanProx = new Scanner(new File("proxies.txt"));
        this.proxies = new ArrayList<String>();
        while (scanProx.hasNextLine()) {
            this.proxies.add(scanProx.nextLine());
        }
        scanProx.close();
        final JSONParser parser = new JSONParser();
        final FileReader configFile = new FileReader("config.txt");
        final JSONObject config = (JSONObject)parser.parse(configFile);
        this.delay = Integer.parseInt(config.get("delay"));
        this.session = config.get("session");
        final FileReader embedFile = new FileReader("embed.txt");
        final JSONObject embed = (JSONObject)parser.parse(embedFile);
        final int webhooksAmt = embed.size();
        final String[][] embeds = new String[webhooksAmt][3];
        for (int x = 0; x < embed.size(); ++x) {
            final JSONArray embedArray = embed.get(new StringBuilder().append(x + 1).toString());
            embeds[x][0] = embedArray.get(0);
            embeds[x][1] = embedArray.get(1);
            embeds[x][2] = embedArray.get(2);
        }
        this.accounts = new ArrayList<String>();
        this.hookEmbeds = new HashMap<String, String[]>();
        this.accountHooks = new HashMap<String, ArrayList<String>>();
        for (int x = 0; x < webhooksAmt; ++x) {
            final Scanner scanAccounts = new Scanner(new File(String.valueOf(x + 1) + ".txt"));
            final String hook = scanAccounts.nextLine();
            this.hookEmbeds.put(hook, embeds[x]);
            while (scanAccounts.hasNextLine()) {
                final String account = scanAccounts.nextLine();
                if (!this.accounts.contains(account)) {
                    this.accounts.add(account);
                    this.accountHooks.put(account, new ArrayList<String>());
                }
                this.accountHooks.get(account).add(hook);
            }
            scanAccounts.close();
        }
        System.out.println("\n" + this.proxies.size() + " proxies loaded");
        this.attempts = 0;
    }
    
    public void run() {
        final Random r = new Random();
        for (final String account : this.accounts) {
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    final String url = "https://instagram.com/" + account + "?__a=1";
                    JSONObject post = null;
                    String currentBio = "";
                    String currentBioLink = "";
                    String currentId = "";
                    boolean privateProfile = false;
                    String profilePic = "";
                    Proxy currentProxy = null;
                    int currentProxyIdx = r.nextInt(Monitor.this.proxies.size());
                    boolean gotInitial = false;
                    while (!gotInitial) {
                        try {
                            currentProxy = Monitor.this.rotateProxy(currentProxy, currentProxyIdx);
                            ++currentProxyIdx;
                            final JSONObject user = Monitor.this.getUser(Monitor.this.getPage(Monitor.this.session, url, currentProxy).body());
                            try {
                                post = Monitor.this.getPost(user);
                                currentId = post.get("id").toString();
                            }
                            catch (IndexOutOfBoundsException e2) {
                                System.out.println("Caught no posts on account (Initial call)");
                            }
                            privateProfile = Boolean.parseBoolean(user.get("is_private").toString());
                            try {
                                currentBio = user.get("biography").toString();
                            }
                            catch (NullPointerException e3) {
                                currentBio = "";
                            }
                            try {
                                currentBioLink = user.get("external_url").toString();
                            }
                            catch (NullPointerException e3) {
                                currentBioLink = "";
                            }
                            profilePic = user.get("profile_pic_url_hd").toString();
                            gotInitial = true;
                            System.out.println("Got intial for " + account);
                        }
                        catch (ParseException ex) {}
                        catch (IOException e4) {
                            System.out.println("IOException, likely bad proxies (" + account + "), repeating initial check");
                            gotInitial = false;
                            try {
                                Thread.sleep(Monitor.this.delay);
                            }
                            catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                Label_0315_Outer:
                    while (true) {
                        while (true) {
                            try {
                                while (true) {
                                    currentProxy = Monitor.this.rotateProxy(currentProxy, currentProxyIdx);
                                    ++currentProxyIdx;
                                    final JSONObject user = Monitor.this.getUser(Monitor.this.getPage(Monitor.this.session, url, currentProxy).body());
                                    try {
                                        post = Monitor.this.getPost(user);
                                        currentId = Monitor.this.checkPosts(post, account, profilePic, currentId, currentProxy);
                                    }
                                    catch (IndexOutOfBoundsException e2) {
                                        System.out.println("Caught no posts on account");
                                    }
                                    privateProfile = Monitor.this.checkPrivacy(user, account, profilePic, privateProfile);
                                    currentBio = Monitor.this.checkBio(user, account, profilePic, currentBio);
                                    currentBioLink = Monitor.this.checkBioLink(user, account, profilePic, currentBioLink);
                                    final Monitor this$0 = Monitor.this;
                                    Monitor.access$4(this$0, this$0.attempts + 1);
                                    System.out.println("Attempt " + Monitor.this.attempts + " with proxy " + currentProxy.toString() + " to " + account + " .... waiting " + Monitor.this.delay + "ms");
                                    Thread.sleep(Monitor.this.delay);
                                }
                            }
                            catch (IOException e4) {
                                System.out.println("IOException, likely invalid sessionid, or bad proxies if repeated (" + account + ")");
                                continue Label_0315_Outer;
                            }
                            catch (ParseException ex2) {
                                continue Label_0315_Outer;
                            }
                            catch (InterruptedException ex3) {
                                continue Label_0315_Outer;
                            }
                            continue;
                        }
                    }
                }
            };
            thread.start();
            System.out.println("Started thread for " + account + " thread id: " + thread.getId());
        }
    }
    
    public Proxy rotateProxy(final Proxy currentProxy, final int currentProxyIdx) throws FileNotFoundException {
        final String selectedProxy = this.proxies.get(currentProxyIdx % this.proxies.size());
        final String[] proxyArr = selectedProxy.split(":");
        if (proxyArr.length == 2) {
            final String host = proxyArr[0];
            final int port = Integer.parseInt(proxyArr[1]);
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        }
        final String host = proxyArr[0];
        final int port = Integer.parseInt(proxyArr[1]);
        final String username = proxyArr[2];
        final String password = proxyArr[3];
        ThreadLocalAuthenticator.setProxyAuth(username, password);
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }
    
    public Connection.Response getPage(final String session, final String url, final Proxy currentProxy) throws IOException {
        return Jsoup.connect(url).proxy(currentProxy).followRedirects(true).ignoreContentType(true).method(Connection.Method.GET).cookie("sessionid", session).timeout(20000).execute();
    }
    
    public JSONObject getUser(final String body) throws ParseException {
        final JSONParser parser = new JSONParser();
        return ((JSONObject)parser.parse(body)).get("graphql").get("user");
    }
    
    public JSONObject getPost(final JSONObject user) {
        return user.get("edge_owner_to_timeline_media").get("edges").get(0).get("node");
    }
    
    public String[] getMultiplePhotos(final String postUrl, final Proxy currentProxy, final String session) throws IOException, ParseException {
        String[] photos = null;
        final JSONParser parser = new JSONParser();
        final JSONObject[] barObjects = null;
        final String body = this.getPage(session, postUrl, currentProxy).body();
        final int start = body.indexOf("{\"config\"");
        final int end = body.indexOf(";</script>");
        final JSONArray json = ((JSONObject)parser.parse(body.substring(start, end))).get("entry_data").get("PostPage").get(0).get("graphql").get("shortcode_media").get("edge_sidecar_to_children").get("edges");
        final int photosAmt = json.size();
        photos = new String[photosAmt - 1];
        for (int x = 0; x < photos.length; ++x) {
            final JSONObject node = json.get(x + 1).get("node");
            photos[x] = node.get("display_url");
        }
        return photos;
    }
    
    public boolean checkPrivacy(final JSONObject user, final String account, final String profilePic, final boolean privateProfile) {
        final boolean currentPrivacy = Boolean.parseBoolean(user.get("is_private").toString());
        if (privateProfile != currentPrivacy) {
            String message;
            if (currentPrivacy) {
                message = String.valueOf(account) + " is now private. Be Ready!";
            }
            else {
                message = String.valueOf(account) + " is no longer private!";
            }
            this.shortPostDiscord(message, profilePic, account, "Profile Status");
            System.out.println("Found a new privacy for " + account + " ! Private profile: " + currentPrivacy + " (Previous count: " + this.attempts + ")");
            this.attempts = 0;
        }
        return currentPrivacy;
    }
    
    public String checkBio(final JSONObject user, final String account, final String profilePic, String currentBio) {
        String bio;
        try {
            bio = user.get("biography").toString();
        }
        catch (NullPointerException e) {
            bio = "";
        }
        if (!bio.equals(currentBio)) {
            this.shortPostDiscord(bio, profilePic, account, String.valueOf(account) + " Bio Change");
            currentBio = bio;
            System.out.println("Found a new bio for " + account + " ! Attempts reset (Previous count: " + this.attempts + ")");
            this.attempts = 0;
        }
        return bio;
    }
    
    public String checkBioLink(final JSONObject user, final String account, final String profilePic, String currentBioLink) {
        String bioLink;
        try {
            bioLink = user.get("external_url").toString();
        }
        catch (NullPointerException e) {
            bioLink = "";
        }
        if (!bioLink.equals(currentBioLink)) {
            this.shortPostDiscord(bioLink, profilePic, account, String.valueOf(account) + " Bio Link Change");
            currentBioLink = bioLink;
            System.out.println("Found a new bio link for " + account + " ! Attempts reset (Previous count: " + this.attempts + ")");
            this.attempts = 0;
        }
        return bioLink;
    }
    
    public String checkPosts(final JSONObject post, final String account, final String profilePic, final String currentId, final Proxy currentProxy) throws IOException {
        final String id = post.get("id").toString();
        if (!id.equals(currentId)) {
            this.newPost(post, account, profilePic, currentProxy);
            System.out.println("Found a new post for " + account + " ! Attempts reset (Previous count: " + this.attempts + ")");
            this.attempts = 0;
        }
        return id;
    }
    
    public void shortPostDiscord(final String newChange, final String profilePic, final String account, final String message) {
        final ArrayList<String> hooks = this.accountHooks.get(account);
        for (final String hook : hooks) {
            final String[] embeds = this.hookEmbeds.get(hook);
            final Discord d = new Discord(embeds[0], embeds[1], embeds[2]);
            d.shortook(hook, newChange, profilePic, message, account);
        }
    }
    
    public void postDiscord(final String account, final String link, final String pic, final String caption, final String profilePic, final boolean isVideo, final String video) {
        final ArrayList<String> hooks = this.accountHooks.get(account);
        for (final String hook : hooks) {
            final String[] embeds = this.hookEmbeds.get(hook);
            final Discord d = new Discord(embeds[0], embeds[1], embeds[2]);
            d.webHookMessage(account, hook, link, pic, caption, profilePic, isVideo, video);
        }
    }
    
    public void postMulti(final String account, final String[] photos, final String link) {
        final ArrayList<String> hooks = this.accountHooks.get(account);
        for (final String hook : hooks) {
            final String[] embeds = this.hookEmbeds.get(hook);
            final Discord d = new Discord(embeds[0], embeds[1], embeds[2]);
            for (int x = 0; x < photos.length; ++x) {
                d.multiPhoto(hook, photos[x], "Picture #" + (x + 1), link);
                try {
                    Thread.sleep(400L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void newPost(final JSONObject post, final String account, final String profilePic, final Proxy currentProxy) throws IOException {
        final long timeStamp = post.get("taken_at_timestamp");
        long timeNow = Instant.now().toEpochMilli();
        timeNow /= 1000L;
        if (timeNow - timeStamp > 60L) {
            System.out.println("Old post!!!");
            return;
        }
        String newCaption = "";
        boolean isVideo = false;
        isVideo = Boolean.parseBoolean(post.get("is_video").toString());
        try {
            final JSONObject edge_media_to_caption = post.get("edge_media_to_caption");
            final JSONArray postEges = edge_media_to_caption.get("edges");
            final JSONObject postEdgesObject = postEges.get(0);
            final JSONObject caption = postEdgesObject.get("node");
            newCaption = caption.get("text").toString();
        }
        catch (IndexOutOfBoundsException e3) {
            System.out.println("No caption found");
        }
        final String newLink = "https://www.instagram.com/p/" + post.get("shortcode");
        final String newPic = post.get("display_url").toString();
        if (!isVideo) {
            this.postDiscord(account, newLink, newPic, newCaption, profilePic, isVideo, "");
        }
        else {
            final String postURL = "https://instagram.com/p/" + post.get("shortcode");
            final String src = this.getPage(this.session, postURL, currentProxy).body();
            final String video = src.substring(src.indexOf("og:video") + 19, src.indexOf("<meta property=\"og:video:") - 9);
            this.postDiscord(account, newLink, newPic, newCaption, profilePic, isVideo, video);
        }
        try {
            final String[] photos = this.getMultiplePhotos(newLink, currentProxy, this.session);
            this.postMulti(account, photos, newLink);
        }
        catch (IOException e1) {
            System.out.println(e1 + " at multi");
        }
        catch (ParseException e2) {
            System.out.println(e2 + " at multi");
        }
        catch (NullPointerException e4) {
            System.out.println("No multiple photos");
        }
    }
    
    static /* synthetic */ void access$4(final Monitor monitor, final int attempts) {
        monitor.attempts = attempts;
    }
}
