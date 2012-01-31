package org.karpukhin.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.UnmarshallingContext;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Karpukhin
 */
public class ClientStub {

    private static final Log log = LogFactory.getLog(ClientStub.class);

    private final String url;
    private final String namespace;
    private Map<String, Class> map;
    private HttpClient httpClient;
    private IBindingFactory factory;

    public ClientStub(String url, String namespace, Map<String, Class> map, String bindingPackage) {
        this.url = url;
        this.namespace = namespace;
        this.map = map;
        try {
            factory = BindingDirectory.getFactory("binding", bindingPackage);
        } catch (JiBXException e) {
            log.error(e.getMessage(), e);
            throw new ClientException(e.getMessage(), e);
        }
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager();
        httpClient = new DefaultHttpClient(cm);
    }

    /**
     * Invokes remote method with given name
     * @param method remote method name
     * @return a list of objects
     */
    public List invoke(String method) {
        return invokeWithParams(method, null);
    }

    /**
     * Invokes remote method with given name and input parameters
     * @param method remote method name
     * @param params remote method input parameters
     * @return a list of objects with given result type
     */
    public List invokeWithParams(String method, Map<String, Object> params) {
        InputStream stream = getStream(method, params);
        return extractObjects(stream, method);
    }

    private InputStream getStream(String method, Map<String, Object> params) {
        String requestLine = buildRequestLine(method, params);
        log.debug(requestLine);
        HttpGet httpGet = new HttpGet(requestLine);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ClientException(response.getStatusLine().getReasonPhrase());
            }
            return response.getEntity().getContent();
        } catch (ClientProtocolException e) {
            log.error(e.getMessage(), e);
            throw new ClientException(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ClientException(e.getMessage(), e);
        }
    }

    /**
     * Returns request line using given remote method name and its input
     * parameters
     * @param method remote method name
     * @param params remote method input parameters
     * @return built request line
     */
    private String buildRequestLine(String method, Map<String, Object> params) {
        StringBuilder uri = new StringBuilder(url).append("/").append(method);
        if (params != null && !params.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (first) {
                    uri.append("?");
                } else {
                    uri.append("&");
                }
                uri.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return uri.toString();
    }

    /**
     * Extracts objects from remote method invocation result
     * @param stream stream with method invocation result
     * @param method remote method name
     * @return list of objects
     */
    private List extractObjects(InputStream stream, String method) {
        List result = new ArrayList();
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            IXMLReader reader = new StAXReaderWrapper(streamReader, "response", true);

            UnmarshallingContext context = (UnmarshallingContext)factory.createUnmarshallingContext();

            context.setDocument(reader);
            context.toTag();

            context.parsePastStartTag(namespace, method + "Response");
            while(context.isAt(namespace, "return")) {
                if (context.attributeBoolean("http://www.w3.org/2001/XMLSchema-instance", "nil", false)) {
                    context.skipElement();
                } else {
                    Object res = extractObject(context);
                    log.debug(res);
                    result.add(res);
                }
            }
            streamReader.close();
        } catch (JiBXException e) {
            log.error(e.getMessage(), e);
            throw new ClientException(e.getMessage(), e);
        } catch (XMLStreamException e) {
            log.error(e.getMessage(), e);
            throw new ClientException(e.getMessage(), e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new ClientException(e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * Returns object or string extracted from XML
     * @param context JiBX context
     * @return object or string extracted from XML
     * @throws JiBXException
     */
    private Object extractObject(UnmarshallingContext context) throws JiBXException {
        Object result;
        String type = extractObjectType(context);
        if (type != null) {
            log.debug(type);
            try {
                Class clazz = map.get(type);
                assert clazz != null;
                result = context.getUnmarshaller(type).unmarshal(clazz.newInstance(), context);
            } catch (InstantiationException e) {
                log.error(e.getMessage(), e);
                throw new ClientException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
                throw new ClientException(e.getMessage(), e);
            }
            context.parsePastCurrentEndTag(namespace, "return");
        } else {
            result = context.parseElementText(namespace, "return");
        }
        return result;
    }

    /**
     * Returns object type extracted from attribute or null
     * @param context JiBX context
     * @return object type
     */
    private String extractObjectType(UnmarshallingContext context) {
        String type = context.attributeText("http://www.w3.org/2001/XMLSchema-instance", "type", null);
        if (type == null) {
            return null;
        }
        log.debug("Return type: " + type);
        String[] typeTokens = type.split(":");
        return String.format("{%s}:%s", context.getNamespaceUri(typeTokens[0]), typeTokens[1]);
    }
}
