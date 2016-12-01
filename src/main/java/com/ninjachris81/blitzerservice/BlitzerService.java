/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ninjachris81.blitzerservice;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;

/**
 *
 * @author B1
 */
public class BlitzerService {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, FirebaseException, UnsupportedEncodingException, JacksonUtilityException {
        final Properties props = loadProperties();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        FirebaseService.init(props.getProperty("fbUrl"), props.getProperty("fbBaseUrl"));
        
        HttpFetcher.init(props.getProperty("url"), Integer.parseInt(props.getProperty("timeoutSec")), props.getProperty("keywords"));
        Runnable task = new HttpFetcher();

        final ScheduledFuture<?> taskHandle = scheduler.scheduleAtFixedRate(task, 0, Integer.parseInt(props.getProperty("intervalSec")), java.util.concurrent.TimeUnit.SECONDS);
        
        System.out.println("Press 'q' to stop");
        Scanner scanner = new Scanner(System.in);
        
        do {
            if (scanner.nextLine().startsWith("q")) break;
        } while (true);
        
        System.out.println("Stopped");
        
        taskHandle.cancel(true);
        
        System.exit(0);
    }
    
    private static Properties loadProperties() throws FileNotFoundException, IOException {
        Properties props = new Properties();
        
        File file = new File("config.cfg");
        
        if (file.exists()) {
            System.out.println("Using config file " + file.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                fis.close();
            }
        } else {
            props.setProperty("intervalSec", "60");
            props.setProperty("url", "http://www.antenne1.de/aktuell-und-regional/service/staus-und-blitzer.html");
            props.setProperty("timeoutSec", "10");
            props.setProperty("keywords", "wildpark;solitude;bergheim;engelberg;ulmer;esslinger;krokodilweg");
            props.setProperty("fbUrl", "https://notificationservice-9528d.firebaseio.com/");
            props.setProperty("fbBaseUrl", "");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                System.out.println("Creating new config file " + file.getAbsolutePath());
                props.store(fos, null);
                fos.close();
            }
        }
        
        return props;
    }
}
