/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ninjachris81.blitzerservice;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author B1
 */
public class HttpFetcher implements Runnable {

    private static String url;
    private static int timeoutSec;
    
    private static final Map<String, Boolean> termStatus = new HashMap<>();
    private static boolean hasError = false;
    private static boolean isFirst = true;
    
    private static int activeTimeFrom;
    private static int activeTimeUntil;
    
    private static String FB_BLITZER_LC_KEY = "blitzerservice_status";
    private static String FB_BLITZER_DATA_KEY = "blitzerservice_data";
    
    public static void init(final String thisUrl, final int thisTimeoutSec, final String keywords) {
        url = thisUrl;
        timeoutSec = thisTimeoutSec;
        
        for (final String keyword : keywords.split(";")) {
            termStatus.put(keyword, false);
        }
    }

    public static void setActiveTime(int activeTimeFrom, int activeTimeUntil) {
        HttpFetcher.activeTimeFrom = activeTimeFrom;
        HttpFetcher.activeTimeUntil = activeTimeUntil;
    }
    
    private boolean isWithinActiveTime() {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        return hour>activeTimeFrom && hour<activeTimeUntil;
    }

    @Override
    public void run() {
        try {
            setNoWarnings(true);

            if (!isWithinActiveTime()) {
                Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "No active time");
                return;
            }
            
            final List<String> warningsList = new ArrayList<>();

            boolean hasChanged = false;

            final Document document = Jsoup.parse(new URL(url), timeoutSec * 1000);
            //final Document document = Jsoup.parse(new File("C:\\Dropbox2\\Dropbox\\temp\\Staus & Blitzer - antenne 1 - Hier für Euch.html"), "UTF-8");
            //Elements elements = document.body().select("div[class='verkehrsmeldungen']");
            Elements elements = document.body().select("p[class='blitzer']");

            if (!document.body().children().isEmpty()) {
                setLastCheckTS();

                if (!elements.isEmpty()) {

                    for (Element e : elements) {
                        String str = e.text();

                        for (final String s : termStatus.keySet()) {
                            int startText = str.toLowerCase().indexOf(s);
                            boolean matches = startText >= 0;

                            if (termStatus.get(s).equals(matches)) {
                                // nothing changed
                                //Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Status of " + s + " remained {0}", matches);
                            } else {
                                hasChanged = true;
                                termStatus.put(s, matches);

                                if (matches) {
                                    warningsList.add(str);
                                } else {
                                    warningsList.remove(str);
                                }

                                Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Status of " + s + " changed {0}", matches);
                            }
                        }
                    }
                    if (hasChanged) {
                        FirebaseService.putData(FB_BLITZER_DATA_KEY, getWarningsMap(warningsList));
                        //FirebaseService.sendNotification();
                    } else {
                        // not changed, do nothing
                        Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Status unchanged: {0}", warningsList);
                    }
                } else {
                    // no warnings
                    Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "No Warnings");
                    setNoWarnings();
                }
            } else {
                // not found
                Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Error while parsing");
                hasError = true;
            }

            if (isFirst && warningsList.isEmpty()) {
                FirebaseService.putData(FB_BLITZER_DATA_KEY, getWarningsMap(warningsList));
            }

            isFirst = false;
        } catch (IOException ex) {
            Logger.getLogger(HttpFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean hasWarnings() {
        for (final String s : termStatus.keySet()) {
            if (termStatus.get(s)) {
                return true;
            }
        }

        return false;
    }

    private void setNoWarnings() {
        setNoWarnings(false);
    }

    private void setNoWarnings(boolean override) {
        if (override || hasWarnings()) {
            for (final String s : termStatus.keySet()) {
                termStatus.put(s, Boolean.FALSE);
            }

            Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "No Warnings - resetting");
            FirebaseService.putData(FB_BLITZER_DATA_KEY, null);
        }
    }
    
    private void setLastCheckTS() {
        Map<String, String> map = new HashMap<>();
        map.put("TS", Long.toString(System.currentTimeMillis()));
        
        FirebaseService.putData(FB_BLITZER_LC_KEY, map);
    }

    private Map<String, String> getWarningsMap(List<String> warningsList) {
        Map<String, String> map = new HashMap<>();
        
        for (String warning : warningsList) {
            map.put("ID_" + Math.abs(new Random().nextInt()) + "@" + System.currentTimeMillis(), warning);
        }
        
        return map;
    }
}
