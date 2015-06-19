package im.amomo.volley;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import java.util.Map;

/**
 * Created by zoki on 17/06/15.
 */
public class OkRequestQueue extends RequestQueue
{
	private Map<String, String> mRequestHeaders = null;

	public OkRequestQueue(Cache cache, Network network)
	{
		super(cache, network);
	}

	public OkRequestQueue(Cache cache, Network network, Map<String, String> mRequestHeaders)
	{
		super(cache, network);
	}

	public OkRequestQueue updateRequestHeaders(Map<String, String> requestHeaders)
	{
		mRequestHeaders = requestHeaders;
		return this;
	}

	@Override
	public <T> Request<T> add(Request<T> request)
	{
		if (request instanceof OkRequest && mRequestHeaders != null)
		{
			((OkRequest)request).headers(mRequestHeaders);
		}
		return super.add(request);
	}
}
