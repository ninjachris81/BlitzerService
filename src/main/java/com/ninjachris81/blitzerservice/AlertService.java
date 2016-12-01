/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ninjachris81.blitzerservice;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 *
 * @author B1
 */
public class AlertService {
    
    private static String ANDROID_NOTIFICATION_URL = "https://fcm.googleapis.com/fcm/send";
    private static String ANDROID_NOTIFICATION_KEY = "AAAAN9gJ96I:APA91bEN-gzxOb-4T7XkT_w4A2deFzVt18LDDhHcsOOgSNb6YvMsDaIU04E07rEGN9JQHnZD20yRVRp7M3cIOf28dtPHrRjnHUoZgEL_bZ89N2AaFgxWx_zSfKj48-RjLQhF1bKPDy2S-Gl36-Ibvdj5ix3y00dKnA";
    private static String CONTENT_TYPE = "application/json";
    
    public static void sendWarnings(final List<String> list) {
        String data = "";
        
        if (list!=null) {
            for (String s : list) {
                data+=s + "|";
            }
        }
        
        if (data.endsWith("|")) data = data.substring(0, data.length()-1);

        Logger.getLogger(AlertService.class.getName()).log(Level.SEVERE, "Sending list {0}", data);

        try {
            sendAndroidNotification("blitzer", "blitzer", "blitzer", list.size() + " Warnings", "Blitzer Service", data, list!=null);
        } catch (IOException ex) {
            Logger.getLogger(AlertService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void sendError(String msg) {
        try {
            sendAndroidNotification("blitzer", "blitzer", "blitzer", "Error", msg, "", true);
        } catch (IOException ex) {
            Logger.getLogger(AlertService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void sendAndroidNotification(String tag, String collapseKey, String topic, String title, String message, String data, boolean withSound) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(ANDROID_NOTIFICATION_URL);
        
        JSONObject obj = new JSONObject();

        JSONObject dataObj = new JSONObject();
        
        dataObj.put("click_action", "OPEN_NOTIFICATION_APP");
        dataObj.put("data", data);
        dataObj.put("title", title);
        dataObj.put("body", message);
        
        obj.put("to", "/topics/" + topic);
        obj.put("collapse_key", collapseKey);
        obj.put("time_to_live", 60*60*3);
        
        obj.put("data", dataObj);
        obj.put("priority", "HIGH");
        
        JSONObject notiObj = new JSONObject();
        
        notiObj.put("title", title);
        if (withSound) notiObj.put("sound", "default");
        notiObj.put("body", message);
        notiObj.put("tag", tag);
        
        obj.put("notification", notiObj);

        Logger.getLogger(AlertService.class.getName()).log(Level.INFO, obj.toString());
        
        StringEntity entity = new StringEntity(obj.toString());
        httpPost.setEntity(entity);
        
        httpPost.setHeader("Content-Type", CONTENT_TYPE);
        httpPost.setHeader("authorization", "key="+ANDROID_NOTIFICATION_KEY);
        
        final HttpResponse response = httpClient.execute(httpPost);
        
        //msgObject.put("icon", ANDROID_NOTIFICATION_ICON);
        //msgObject.put("color", ANDROID_NOTIFICATION_COLOR);

        Logger.getLogger(AlertService.class.getName()).log(Level.INFO, "CODE: {0}", response.getStatusLine().getStatusCode());
        Logger.getLogger(AlertService.class.getName()).log(Level.INFO, "Notification response: {0}", EntityUtils.toString(response.getEntity()));
    }
    
}
