package org.piax.trans.ts.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;


public class HttpClient {
    public static DefaultHttpClient open () {
        return new DefaultHttpClient();
    }

    public static void setBasicAuth(DefaultHttpClient client, String domain, int port, String name, String pass) throws HttpException,
                                                                                                                        IOException {
        client.getCredentialsProvider().setCredentials(new AuthScope(domain, port),
                                                       new UsernamePasswordCredentials(name, pass));
    }

    public static String get(DefaultHttpClient client, String url) throws ClientProtocolException, IOException {
        if (client == null)
            return null;
        HttpGet method = new HttpGet(url);
        HttpResponse resp = client.execute(method);
        if (resp.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("error " + resp.getStatusLine().getStatusCode());
        } else {
            InputStream in = resp.getEntity().getContent();
            InputStreamReader r = new InputStreamReader(in, "UTF-8");
            StringWriter sw = new StringWriter();
            int c = r.read();
            while (c != -1) {
                sw.write(c);
                c = r.read();
            }
            return sw.toString();
        }
    }

    public static String post(DefaultHttpClient client, String url, Map<String, String> hash) throws ClientProtocolException, IOException {
        HttpPost method = new HttpPost(url);
        Set<Entry<String, String>> entry = hash.entrySet();
        Entry<String, String> obj;
        Iterator<Entry<String, String>> iterator = entry.iterator();
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        while (iterator.hasNext()) {
            obj = iterator.next();
            String key = obj.getKey();
            String val = obj.getValue();
            params.add(new BasicNameValuePair(key, val));
        }
        method.setEntity(new UrlEncodedFormEntity(params, org.apache.http.protocol.HTTP.UTF_8));

        HttpParams httpParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams, org.apache.http.protocol.HTTP.UTF_8);
        // required by twitter.com...
        HttpProtocolParams.setUseExpectContinue(httpParams, false);
        method.setParams(httpParams);
        method.removeHeaders("Expect");

        HttpResponse resp = client.execute(method);
        //        if (resp.getStatusLine().getStatusCode() != 200) {
        //            return null;
        //        } else {
        if (resp.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("error " + resp.getStatusLine().getStatusCode());
        }
        InputStream in = resp.getEntity().getContent();
        StringWriter sw = new StringWriter();
        int c = in.read();
        while (c != -1) {
            sw.write(c);
            c = in.read();
        }
        return sw.toString();
        //        }
    }
}
