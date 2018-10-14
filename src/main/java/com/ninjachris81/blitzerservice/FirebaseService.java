/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ninjachris81.blitzerservice;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author B1
 */
public class FirebaseService {
 
    private static FirebaseApp app;
    private static FirebaseDatabase firebase;
 
    public static void init(String url, String credentialJsonFile) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        GoogleCredentials token = getAccessToken(credentialJsonFile);
        FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(token)
            .setDatabaseUrl(url)
            .build();
        
        app = FirebaseApp.initializeApp(options);
        firebase = FirebaseDatabase.getInstance(app);
    }
    
    public static void putData(String key, Map<String, String> obj) {
        DatabaseReference ref = firebase.getReference("blitzerservice");
        if (obj.isEmpty()) {
            ref.removeValueAsync();
        } else {
            ref.setValueAsync(obj);
        }
    }
    
    public static void sendNotification() {
        try {
            Message msg = Message.builder().putData("u", "" + System.currentTimeMillis()).setTopic("bs").build();
            FirebaseMessaging.getInstance(app).send(msg);
        } catch (FirebaseMessagingException ex) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*
    private static void checkAccessToken() throws IOException {
        if (firebase.getCredentials().getExpiresInSeconds()<=10) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, "Refreshing credentials {0}", firebase.getCredentials());
            FirebaseApp.getInstance(). .getCredentials().refresh();
        }
    }*/

    private static GoogleCredentials getAccessToken(String credentialJsonFile) throws IOException, FileNotFoundException {
        GoogleCredentials googleCred = GoogleCredentials.fromStream(new FileInputStream(credentialJsonFile));
        GoogleCredentials scoped = googleCred.createScoped(
            Arrays.asList(
              "https://www.googleapis.com/auth/firebase.database",
              "https://www.googleapis.com/auth/userinfo.email"
            )
        );
        
        //scoped.refreshToken();
        scoped.refresh();
        return scoped;
    }

    
}
