package com.appacoustic.cointester.utils;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.appacoustic.cointester.App;
import com.gabrielmorenoibarra.g.GLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Request management.
 * Created by Gabriel Moreno on 2017-07-01.
 */
public class RequestManager {

    public static final String TAG = RequestManager.class.getSimpleName();

    private static RequestManager ourInstance = new RequestManager();

    /**
     * @return a <code>RequestManager</code> instance.
     */
    public static RequestManager getInstance() {
        return ourInstance;
    }

    private RequestManager() {
    }

    /** Callback interface for delivering parsed responses. */
    public interface RequestManagerResponse {
        /** Called when a response is received. */
        void onResponse(boolean success, JSONObject content, Exception e);
    }

    /**
     * Does a request to a specific service.
     * @param user User for making requests to the protected directory.
     * @param password Password for making requests to the protected directory.
     * @param app Application class.
     * @param requestTag Name of request.
     * @param url URL address of this service.
     * @param params Map with the request parameters.
     * @param requestManagerResponse Object that manages service response.
     */
    public void doRequest(String user, String password, App app, final String requestTag, String url, Map<String, Object> params, final RequestManagerResponse requestManagerResponse) {
        GLog.i(TAG, requestTag + " REQUEST: " + params.toString());
        app.addToRequestQueue(new CustomRequest(user, password, Request.Method.POST, url, params, false,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) { // When the response is successful received
                        GLog.i(TAG, requestTag + " RESPONSE: " + response.toString());
                        try {
                            String status = response.getString("status"); // We get the status response
                            JSONObject content = response.getJSONObject("content");
                            if (status.equals("ok")) {
                                if (requestManagerResponse != null)
                                    requestManagerResponse.onResponse(true, content, null);
                            } else if (status.equals("error")) {
                                if (requestManagerResponse != null)
                                    requestManagerResponse.onResponse(false, content, null);
                            }
                        } catch (JSONException e) {
                            if (requestManagerResponse != null)
                                requestManagerResponse.onResponse(false, null, e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError errorResponse) { // When there is an error
                        GLog.i(TAG, requestTag + ": " + errorResponse.toString());
                        if (requestManagerResponse != null)
                            requestManagerResponse.onResponse(false, null, errorResponse);
                    }
                }), requestTag); // Adding request to request queue
    }
}