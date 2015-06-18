package im.amomo.volley.toolbox;

import com.android.volley.Cache;
import com.android.volley.toolbox.DiskBasedCache;
import com.squareup.okhttp.CertificatePinner;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;

import im.amomo.volley.OkHttpStack;
import im.amomo.volley.OkNetwork;
import im.amomo.volley.OkRequest;
import im.amomo.volley.OkRequestQueue;
import im.amomo.volley.OkStack;

/**
 * Created by GoogolMo on 10/22/13.
 * Modified by zoki on 06/17/15
 */
public class OkVolley {

    private static final String INIT_ERROR = "OkVolley must be initialized! Call init method in Application class";
    private static final String VERSION = "OkVolley/1.0";
    /**
     * Default on-disk cache directory.
     */
    private static final String DEFAULT_CACHE_DIR = "volley";

    private static OkVolley _instance;
    private String mUserAgent;

    private Map<String, String> mRequestHeaders;

    private OkRequestQueue mRequestQueue;
    private Cache mCache;
    private OkNetwork mNetwork;
    private OkHttpStack mHttpStack;

    public static OkVolley getInstance() {
        if (_instance == null) {
            throw new IllegalStateException(INIT_ERROR);
        }
        return _instance;
    }

    private OkVolley(Context context) {
        mUserAgent = generateDefaultUserAgent(context);
        mRequestHeaders = new HashMap<>();
        mRequestHeaders.put(OkRequest.HEADER_USER_AGENT, mUserAgent);
        mRequestHeaders.put(OkRequest.HEADER_ACCEPT_CHARSET, OkRequest.CHARSET_UTF8);

        mRequestQueue = newDefaultRequestQueue(context);
    }

    /**
     * init method
     *
     * @param context Context
     * @return this Volley Object
     */
    public static OkVolley init(Context context) {
        if (_instance == null) {
            _instance = new OkVolley(context);
        }
        return _instance;
    }

    /**
     * set default all user-agent
     *
     * @param userAgent user-agent
     * @return this Volley Object
     */
    public OkVolley setUserAgent(String userAgent)
    {
        this.mUserAgent = userAgent;
        mRequestHeaders.put(OkRequest.HEADER_USER_AGENT, mUserAgent);
        mRequestQueue.updateRequestHeaders(mRequestHeaders);
        return this;
    }

    /**
     * build the default User-Agent
     *
     * @param context
     * @return
     */
    public static String generateDefaultUserAgent(Context context) {
        StringBuilder ua = new StringBuilder("api-client/");
        ua.append(VERSION);

        String packageName = context.getApplicationContext().getPackageName();

        ua.append(" ");
        ua.append(packageName);

        PackageInfo pi = null;
        try {
            pi = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (pi != null) {
            ua.append("/");
            ua.append(pi.versionName);
            ua.append("(");
            ua.append(pi.versionCode);
            ua.append(")");
        }
        ua.append(" Android/");
        ua.append(Build.VERSION.SDK_INT);

        try {
            ua.append(" ");
            ua.append(Build.PRODUCT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ua.append(" ");
            ua.append(Build.MANUFACTURER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ua.append(" ");
            ua.append(Build.MODEL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ua.toString();
    }

    /**
     * set trusted verifier
     *
     * @param verifier HostnameVerifier
     * @return this Volley Object
     */
    public OkVolley setHostnameTrustedVerifier(HostnameVerifier verifier) {
        mHttpStack.setHostnameVerifier(verifier);
        return this;
    }

    /**
     * trust all certs
     *
     * @return this Volley Object
     */
    public OkVolley trustAllCerts() {
        mHttpStack.trustAllCerts();
        return this;
    }

    /**
     * pins certificate
     *
     * @return this Volley Object
     */
    public OkVolley pinnCert(final CertificatePinner certificatePinner){
          mHttpStack.pinnCert(certificatePinner);
        return this;
    }

    /**
     * get the default request queue
     *
     * @return default {@link com.android.volley.RequestQueue}
     */
    public OkRequestQueue getRequestQueue() {
         return mRequestQueue;
    }

    public OkRequestQueue newRequestQueue(Context context) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);

        OkRequestQueue queue = new OkRequestQueue(new DiskBasedCache(cacheDir), new OkNetwork(getDefaultHttpStack()));
        queue.start();

        return queue;
    }
    protected OkRequestQueue newDefaultRequestQueue(Context context)
    {
        mNetwork = new OkNetwork(getDefaultHttpStack());

        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        mCache = new DiskBasedCache(cacheDir);

        OkRequestQueue queue = new OkRequestQueue(mCache, mNetwork, mRequestHeaders);
        queue.start();

        return queue;
    }

    protected OkStack getDefaultHttpStack() {
        if (mHttpStack == null) {
            mHttpStack = new OkHttpStack();
        }
        return mHttpStack;
    }
}
