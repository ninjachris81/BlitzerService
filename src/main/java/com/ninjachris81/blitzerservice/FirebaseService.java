/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ninjachris81.blitzerservice;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author B1
 */
public class FirebaseService {
 
    private static Firebase firebase;
    
    public static void init(String url, String credentialJsonFile) throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException, FileNotFoundException, IOException {
        GoogleCredential token = getAccessToken(credentialJsonFile);
        firebase = new Firebase(url, token);
    }
    
    public static boolean putData(String key, JSONObject obj) {
        try {
            checkAccessToken();
            JSONObject rootObj = new JSONObject();
            rootObj.put(key, obj);
            
            FirebaseResponse response = firebase.put( rootObj.toString() );
            return response.getSuccess();
        } catch (FirebaseException | UnsupportedEncodingException ex) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    public static boolean putData(String key, JSONArray arr) {
        try {
            checkAccessToken();
            Map<String, Object> dataMap = new LinkedHashMap<>();
            dataMap.put( key, arr );
            FirebaseResponse response = firebase.put( dataMap );
            return response.getSuccess();
        } catch (JacksonUtilityException | FirebaseException | UnsupportedEncodingException ex) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    private static void checkAccessToken() throws IOException {
        if (firebase.getCredentials().getExpiresInSeconds()<=10) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, "Refreshing credentials {0}", firebase.getCredentials());
            firebase.getCredentials().refreshToken();
        }
    }

    private static GoogleCredential getAccessToken(String credentialJsonFile) throws IOException, FileNotFoundException {
        GoogleCredential googleCred = GoogleCredential.fromStream(new FileInputStream(credentialJsonFile));
        GoogleCredential scoped = googleCred.createScoped(
            Arrays.asList(
              "https://www.googleapis.com/auth/firebase.database",
              "https://www.googleapis.com/auth/userinfo.email"
            )
        );
        
        scoped.refreshToken();
        return scoped;
    }

    
}
