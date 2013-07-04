package com.workshare.sample.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class ApiClient {

    public static final String DEFAULT_BASE_URL = "https://my.workshare.com";

    private static final Log log = LogFactory.getLog(ApiClient.class);

    private final String baseUrl;
    private final HttpClient httpclient;
    private final HttpContext context;
    private final String appUid;

    public ApiClient(String appuid) {
        this(DEFAULT_BASE_URL, appuid);
    }

    public ApiClient(String baseurl, String appuid) {
        this.baseUrl = baseurl;
        this.appUid = appuid;

        this.httpclient = new DefaultHttpClient();
        this.context = new BasicHttpContext();
        final CookieStore cookieStore = new BasicCookieStore();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void login(String username, String password) throws IOException {

        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("user_session[email]", username));
        params.add(new BasicNameValuePair("user_session[password]", password));
        params.add(new BasicNameValuePair("device[app_uid]", appUid));

        final HttpPost request = new HttpPost(makeUrl("user_sessions.json"));
        request.setEntity(new UrlEncodedFormEntity(params));

        final HttpResponse response = httpclient.execute(request, context);
        ckeckResponse(consume(response), 201);
    }

    public void logout() throws IOException {

        final HttpGet request = new HttpGet(makeUrl("logout.json"));
        consume(httpclient.execute(request, context));
    }

    public JSONArray getFolders() throws IOException {

        final HttpGet request = new HttpGet(makeUrl("folders.json"));

        final HttpResponse res = httpclient.execute(request, context);
        try {
            final JSONObject result = (JSONObject) JSONValue.parse(res.getEntity().getContent());
            final JSONArray folders = (JSONArray) result.get("folders");
            return folders;
        } finally {
            consume(res);
        }
    }

    public JSONArray getFiles() throws IOException {

        final HttpGet request = new HttpGet(makeUrl("files.json"));
        
        final HttpResponse res = httpclient.execute(request, context);
        try {
            final JSONObject result = (JSONObject) JSONValue.parse(res.getEntity().getContent());
            final JSONArray files = (JSONArray) result.get("files");
            return files;
        } finally {
            consume(res);
        }
    }

    public File download(JSONObject file) throws IOException {

        File result = new File(System.getProperty("java.io.tmpdir"), (String)file.get("name"));

        String url = makeUrl("files/" + file.get("id")+"/download");
        HttpGet request = new HttpGet(url);
        
        HttpResponse res = httpclient.execute(request, context);
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(result));
            try {
                InputStream in = res.getEntity().getContent();
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    in.close();
                }
            } finally {
                out.close();
            }
        } finally {
            consume(res);
        }

        return result;
    }

    public JSONObject upload(File source, Integer folderId) throws IOException {

        MultipartEntity multipart = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipart.addPart("file[name]", new StringBody(source.getName()));
        multipart.addPart("file[updated_with]", new StringBody("cli"));
        multipart.addPart("Filedata", new FileBody(source, "application/octet-stream"));
        if (folderId != null)
            multipart.addPart("file[folder_id]", new StringBody(folderId.toString()));

        HttpPost request = new HttpPost(makeUrl("files.json"));
        request.setEntity(multipart);
        
        HttpResponse res = httpclient.execute(request, context);
        try {
            ckeckResponse(res, 201);
            final JSONObject result = (JSONObject) JSONValue.parse(res.getEntity().getContent());
            return result;
        } finally {
            consume(res);
        }
    }


    private void ckeckResponse(HttpResponse response, final int expectedStatus) throws IOException {
        log.debug(response);
        if (response.getStatusLine().getStatusCode() != expectedStatus)
            throw new IOException("Login failed: " + response.getStatusLine());
    }

    private String makeUrl(final String apipath) {
        return baseUrl + "/api/open-v1.0/" + apipath;
    }

    private HttpResponse consume(HttpResponse response) throws IOException {
        final HttpEntity entity = response.getEntity();
        if (entity != null)
            EntityUtils.consume(entity);

        return response;
    }

}
