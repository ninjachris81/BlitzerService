/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blitzerservice;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final String url;
    private final int timeoutSec;
    
    private final Map<String, Boolean> termStatus = new HashMap<>();
    private boolean hasError = false;
    private boolean isFirst = true;
    
    public HttpFetcher(final String url, final int timeoutSec, final String keywords) {
        this.url = url;
        this.timeoutSec = timeoutSec;
        
        for (final String keyword : keywords.split(";")) {
            termStatus.put(keyword, false);
        }
    }

    @Override
    public void run() {
        try {
            final List<String> warningsList = new ArrayList<>();

            boolean sendAlert = false;
            boolean hasChanged = false;

            final Document document = Jsoup.parse(new URL(this.url), this.timeoutSec * 1000);
            //Document document = Jsoup.parse(new File("C:\\Dropbox2\\Dropbox\\temp\\Staus & Blitzer - antenne 1 - Hier für Euch.html"), "UTF-8");
            //Elements elements = document.body().select("div[class='verkehrsmeldungen']");
            Elements elements = document.body().select("p[class='blitzer']");

            if (!document.body().children().isEmpty()) {

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
                                }

                                Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Status of " + s + " changed {0}", matches);
                            }
                        }

                        if (hasChanged) {
                            sendAlert = true;
                        } else {
                            // not changed, do nothing
                            Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Status unchanged: {0}", warningsList);
                        }
                    }

                } else {
                    // no warnings
                    Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "No Warnings");
                    setNoWarnings();
                }
            } else {
                // not found
                Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Error while parsing");

                if (!hasError) {
                    AlertService.sendError("Body is empty !");
                }
                hasError = true;
            }

            if (sendAlert | isFirst) {
                Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "Sending alert {0}", warningsList);
                AlertService.sendWarnings(warningsList);
            }

            isFirst = false;
        } catch (IOException ex) {
            Logger.getLogger(HttpFetcher.class.getName()).log(Level.SEVERE, null, ex);
            AlertService.sendError(ex.getMessage());
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
        if (hasWarnings()) {
            for (final String s : termStatus.keySet()) {
                termStatus.put(s, Boolean.FALSE);
            }

            Logger.getLogger(HttpFetcher.class.getName()).log(Level.INFO, "No Warnings - resetting");
            AlertService.sendWarnings(null);
        }
    }
}
