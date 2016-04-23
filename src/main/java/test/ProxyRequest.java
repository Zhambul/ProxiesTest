package test;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sun.istack.internal.Nullable;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import test.entity.Proxy;
import test.event.ProxyRequestEvent;
import test.event.ProxyResponseEvent;

import java.io.IOException;

/**
 * Created by 10 on 19.04.2016.
 */
class ProxyRequest extends UntypedActor {
    private int timeOut;
    private CloseableHttpClient httpClient;

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(),this);

    public ProxyRequest(int timeOut) {
        this.timeOut = timeOut;
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if(o instanceof ProxyRequestEvent) {
            ProxyRequestEvent proxyRequestEvent = (ProxyRequestEvent) o;

            ProxyResponseEvent proxyResponseEvent = executeRequest(proxyRequestEvent.getUrl(),
                    proxyRequestEvent.getProxy());
            getSender().tell(proxyResponseEvent, getSelf());
        }else {
            unhandled(o);
        }
    }

    private ProxyResponseEvent executeRequest(String targetUrl, Proxy proxy) throws IOException {
        HttpHost proxyHost = new HttpHost(proxy.getIp(), proxy.getPort(), proxy.getScheme());

        int targetPort = proxy.getScheme().toLowerCase().equals("https") ? 443 : 80;
        HttpHost target = new HttpHost(targetUrl,targetPort,proxy.getScheme());

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeOut)
                .setProxy(proxyHost)
                .build();

        HttpGet request = new HttpGet("/");
        request.setConfig(config);

        logger.debug("Executing request " + request.getRequestLine() + " to " + target + " via " + proxyHost);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(target, request);
        }
        catch (Exception e) {
            logger.debug("error during request");
        }
        return new ProxyResponseEvent(proxy,response);
    }
}
