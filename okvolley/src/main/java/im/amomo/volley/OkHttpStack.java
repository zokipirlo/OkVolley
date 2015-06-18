package im.amomo.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.VolleyLog;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by GoogolMo on 10/22/13.
 */
public class OkHttpStack implements OkStack {

    private final OkHttpClient mClient;

    private final UrlRewriter mUrlRewriter;

    /**
     * An interface for transforming URLs before use.
     */
    public interface UrlRewriter {
        /**
         * Returns a URL to use instead of the provided one, or null to indicate
         * this URL should not be used at all.
         */
        public String rewriteUrl(String originalUrl);
    }


    public OkHttpStack() {
        this(null);
    }

    public OkHttpStack(UrlRewriter urlRewriter) {
        this(urlRewriter, null);
    }

    public OkHttpStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        this.mClient = new OkHttpClient();
        this.mUrlRewriter = urlRewriter;
        this.mClient.setSslSocketFactory(sslSocketFactory);
    }

    /**
     * perform the request
     *
     * @param request           request
     * @param additionalHeaders headers
     * @return http response
     * @throws java.io.IOException
     * @throws com.android.volley.AuthFailureError
     */
    @Override
    public Response performRequest(Request<?> request,
                                   Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        String url = request.getUrl();
        if (mUrlRewriter != null) {
            String rewritten = mUrlRewriter.rewriteUrl(url);
            if (rewritten == null) {
                throw new IOException("URL blocked by rewriter: " + url);
            }
            url = rewritten;
        }

        OkHttpClient client = mClient.clone();

        int connectionTimeoutMs = request.getTimeoutMs();
        int readTimeoutMs = request.getTimeoutMs();
        int writeTimeoutMs = request.getTimeoutMs();

        if (request instanceof OkRequest)
        {
            connectionTimeoutMs = Math.max(((OkRequest)request).getConnectionTimeoutMs(), connectionTimeoutMs);
            readTimeoutMs = Math.max(((OkRequest)request).getReadTimeoutMs(), readTimeoutMs);
            writeTimeoutMs = Math.max(((OkRequest)request).getWriteTimeoutMs(), writeTimeoutMs);
        }

        client.setConnectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS);
        client.setReadTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS);

        com.squareup.okhttp.Request.Builder builder = new com.squareup.okhttp.Request.Builder();
        builder.url(url);

        Map<String, String> headers = request.getHeaders();
        for (final String name : headers.keySet()) {
            builder.header(name, headers.get(name));
        }
        for (final String name : additionalHeaders.keySet()) {
            builder.header(name, additionalHeaders.get(name));
        }

        setConnectionParametersForRequest(builder, request);
        // Initialize HttpResponse with data from the okhttp.
        Response okhttpResponse = mClient.newCall(builder.build()).execute();

        int responseCode = okhttpResponse.code();
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
        }
        return okhttpResponse;
    }

    /* package */
    static void setConnectionParametersForRequest(com.squareup.okhttp.Request.Builder builder,
                                                  Request<?> request) throws IOException, AuthFailureError {

        if (VolleyLog.DEBUG) {
            VolleyLog.d("request.method = %1$s", request.getMethod());
        }
        switch (request.getMethod()) {
            case Method.DEPRECATED_GET_OR_POST:
                // Ensure backwards compatibility.
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    builder.post(RequestBody.create(MediaType.parse(request.getPostBodyContentType()), postBody));
                    if (VolleyLog.DEBUG) {
                        VolleyLog.d("RequestHeader: %1$s:%2$s", OkRequest.HEADER_CONTENT_TYPE, request.getPostBodyContentType());
                    }
                } else {
                    builder.get();
                }
                break;
            case Method.GET:
                builder.get();
                break;
            case Method.DELETE:
                builder.delete();
                break;
            case Method.POST:
                builder.post(createRequestBody(request));
                break;
            case Method.PUT:
                builder.put(createRequestBody(request));
                break;
            case Method.HEAD:
                builder.head();
                break;
            case Request.Method.OPTIONS:
                builder.method("OPTIONS", null);
                break;
            case Request.Method.TRACE:
                builder.method("TRACE", null);
                break;
            case Method.PATCH:
                builder.patch(createRequestBody(request));
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }
    private static RequestBody createRequestBody(Request r) throws AuthFailureError {
        if (VolleyLog.DEBUG) {
            VolleyLog.d("RequestHeader: %1$s:%2$s", OkRequest.HEADER_CONTENT_TYPE, r.getBodyContentType());
        }
        final byte[] body = r.getBody();
        if (body == null) return null;

        return RequestBody.create(MediaType.parse(r.getBodyContentType()), body);
    }

    /**
     * pins certificate via okHttp provided functions - see CertificatePinner class
     *
     * @return this http stack
     */
    public OkHttpStack pinnCert(final CertificatePinner certificatePinner) {
        this.mClient.setCertificatePinner(certificatePinner);
        return this;
    }

    /**
     * set request trust all certs include untrusts
     *
     * @return this http stack
     */
    public OkHttpStack trustAllCerts() {
        this.mClient.setSslSocketFactory(getTrustedFactory());
        return this;
    }

    /**
     * set request trust all hosts include hosts with untrusts
     *
     * @return
     */
    public OkHttpStack trustAllHosts() {
        this.mClient.setHostnameVerifier(getTrustedVerifier());
        return this;
    }

    /**
     * set custom host name verifier
     *
     * @param verifier verifier
     * @return this http stack
     */
    public OkHttpStack setHostnameVerifier(HostnameVerifier verifier) {
        this.mClient.setHostnameVerifier(verifier);
        return this;
    }

    private static SSLSocketFactory TRUSTED_FACTORY;
    private static HostnameVerifier TRUSTED_VERIFIER;

    private static SSLSocketFactory getTrustedFactory() {
        if (TRUSTED_FACTORY == null) {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Intentionally left blank
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Intentionally left blank
                }
            }};
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, new SecureRandom());
                TRUSTED_FACTORY = context.getSocketFactory();
            } catch (GeneralSecurityException e) {
                IOException ioException = new IOException(
                        "Security exception configuring SSL context");
                ioException.initCause(e);
            }
        }
        return TRUSTED_FACTORY;
    }

    private static HostnameVerifier getTrustedVerifier() {
        if (TRUSTED_VERIFIER == null)
            TRUSTED_VERIFIER = new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

        return TRUSTED_VERIFIER;
    }


}
