package org.danmayr.imagej.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.io.InputStream;
import ij.IJ;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.danmayr.imagej.algorithm.structs.Pair;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.*;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import org.danmayr.imagej.updater.Release;
import org.danmayr.imagej.Version;
import java.util.regex.Pattern;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class Updater {

    static Vector<UpdateListener> listener = new Vector<>();

    static CheckForUpdatesThread checkThread;
    static String github_auth_token = "";

    public Updater() {
        checkThread = new CheckForUpdatesThread();
        loadTokenFromFile();
    }

    public void loadTokenFromFile() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("github_auth_token.txt");
            byte[] read = new byte[256];
            int size = is.read(read);
            String token = new String(read,size);
            github_auth_token = token;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static class CheckForUpdatesThread extends Thread {

        static final int CHECK_FOR_UPDATE_TIME = 7200;// Check every 2 hous again and on startup
        public boolean mStopped = false;
        int timer = 0;

        public CheckForUpdatesThread() {
            this.start();
        }

        public void checkNow() {
            timer = 0;
        }

        public void run() {
            while (false == mStopped) {
                try {
                    if (timer <= 0) {
                        checkForUpdates();
                        timer = CHECK_FOR_UPDATE_TIME;
                    } else {
                        timer--;
                    }

                    Thread.sleep(1000);
                } catch (InterruptedException ex) {

                }
            }
        }
    }

    public static void checkForUpdates() {
        Pair<Integer, Release> high = getHighestVersion();
        if (Version.version() < high.getFirst() || Version.status != "release" || high.getFirst() < 0) {
            long ret = downloadNewstUpdate();
            for (int n = 0; n < listener.size(); n++) {
                if (ret > 0) {
                    listener.get(n).newUpdateAvailable(high.getSecond(), UpdateListener.State.NEW_UPDATE_AVAILABLE);
                } else if (ret < 0) {
                    listener.get(n).newUpdateAvailable(null, UpdateListener.State.NO_INTERNET_CONNECTION);
                }
            }
        }
    }

    public void stopAutoUpdate() {
        checkThread.mStopped = true;
    }

    public static void registerUpdateListener(UpdateListener list) {
        listener.add(list);
        checkThread.checkNow();
    }

    public static void installNewsetUpdate() throws IOException {
        Pair<Integer, Release> hiVer = getHighestVersion();
        Files.copy(Paths.get("plugins/exosome_analyzer_updates/ExosomeAnalyzer_" + hiVer.getSecond().version + ".upd"),
                Paths.get("plugins/ExosomeAnalyzer.jar"), StandardCopyOption.REPLACE_EXISTING);
    }

    public static long downloadNewstUpdate() {
        Map<String, Release> releases = getAvailableVersions();
        long downloadSize = 0;
        if (releases != null && releases.size() > 0) {

            Pair<Integer, Release> hiVer = getHighestVersion();
            if (hiVer.getSecond() != null) {
                try {
                    File tempFile = new File(
                            "plugins/exosome_analyzer_updates/ExosomeAnalyzer_" + hiVer.getSecond().version + ".upd");
                    if (tempFile.exists()) {
                        downloadSize = tempFile.length();
                    } else {
                        File theDir = new File("plugins/exosome_analyzer_updates");
                        if (!theDir.exists()) {
                            theDir.mkdirs();
                        }

                        downloadSize = download(releases.get(hiVer.getSecond().version).downloadUrlJar,
                                "plugins/exosome_analyzer_updates/ExosomeAnalyzer_" + hiVer.getSecond().version
                                        + ".upd");
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    downloadSize = -1;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    downloadSize = -1;
                }
            } else {
                downloadSize = -1;
            }
        } else {
            downloadSize = -1;
        }
        return downloadSize;
    }

    public static Pair<Integer, Release> getHighestVersion() {
        Map<String, Release> releases = getAvailableVersions();
        int highestVersion = 0;
        Release vStr = null;
        if (releases.size() > 0) {
            for (Map.Entry<String, Release> entry : releases.entrySet()) {
                String[] parts = entry.getKey().split(Pattern.quote("."));

                if (parts.length > 2) {
                    int major = Integer.parseInt(parts[0].replace("v", "")) * 1000000;
                    int minor = Integer.parseInt(parts[1]) * 1000;
                    int build = Integer.parseInt(parts[2]);
                    int version = major + minor + build;
                    if (version > highestVersion) {
                        vStr = entry.getValue();
                        highestVersion = version;
                    }
                }
            }
        } else {
            highestVersion = -1;
        }
        return new Pair(highestVersion, vStr);
    }

    public static Map<String, Release> getAvailableVersions() {
        Map<String, Release> releases = new HashMap<String, Release>();

        try {
            URL url = new URL("https://api.github.com/repos/joda01/exosome_analyzer/releases");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (github_auth_token.length() > 0) {
                String authHeaderValue = "token " + github_auth_token;
                con.setRequestProperty("Authorization", authHeaderValue);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            //
            // Print debug information
            //
            StringBuilder builder = new StringBuilder();
            Map<String, List<String>> map = con.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (entry.getKey() == null)
                    continue;
                builder.append(entry.getKey()).append(": ");

                List<String> headerValues = entry.getValue();
                Iterator<String> it = headerValues.iterator();
                if (it.hasNext()) {
                    builder.append(it.next());

                    while (it.hasNext()) {
                        builder.append(", ").append(it.next());
                    }
                }

                builder.append("\n");
            }
            System.out.println(builder);
            //
            //
            //

            try {
                JSONArray jsonArray = new JSONArray(content.toString());

                for (int n = 0; n < jsonArray.length(); n++) {
                    if (jsonArray.getJSONObject(n).getBoolean("draft") == false) {
                        Release v = new Release();
                        v.version = jsonArray.getJSONObject(n).getString("name");
                        v.publishedAt = jsonArray.getJSONObject(n).getString("published_at");
                        v.isPrerelease = jsonArray.getJSONObject(n).getBoolean("prerelease");
                        v.releaseText = jsonArray.getJSONObject(n).getString("body");

                        JSONArray assests = jsonArray.getJSONObject(n).getJSONArray("assets");
                        for (int a = 0; a < assests.length(); a++) {
                            String name = assests.getJSONObject(a).getString("name");
                            String downloadUrl = assests.getJSONObject(a).getString("browser_download_url");
                            String contentType = assests.getJSONObject(a).getString("content_type");

                            if (contentType.equals("application/x-java-archive")) {
                                v.downloadUrlJar = downloadUrl;
                            } else if (contentType == "application/zip") {
                                v.downloadUrlDeps = downloadUrl;
                            }
                        }
                        releases.put(v.version, v);
                    }

                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {

        }

        return releases;
    }

    public static long download(String sourceUrl, String targetFileName) throws MalformedURLException, IOException {
        try (InputStream in = URI.create(sourceUrl).toURL().openStream()) {
            return Files.copy(in, Paths.get(targetFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /*
     * 
     * curl -H "Accept: application/vnd.github.v3+json"
     * https://api.github.com/repos/joda01/exosome_analyzer/releases
     */
}