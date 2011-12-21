package org.karpukhin.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jibx.runtime.*;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.UnmarshallingContext;
import org.karpukhin.webservice.StudentService;
import org.karpukhin.webservice.data.Student;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Karpukhin
 */
public class StudentServiceStub implements StudentService {

    private static final String XYZ_XSD_NAMESPACE = "http://www.xyz.org/xsd";
    private static final String STUDENT_TYPE = "{http://data.webservice.karpukhin.org/xsd}:Student";

    private static final Log log = LogFactory.getLog(StudentServiceStub.class);

    private String url;
    private Map<String, Class> map = new HashMap<String, Class>();
    private IBindingFactory factory;

    public StudentServiceStub() {
        this("http://localhost:8080/axis2/services/StudentService");
    }

    public StudentServiceStub(String url) {
        this.url = url;
        map.put(STUDENT_TYPE, Student.class);
        try {
            factory = BindingDirectory.getFactory("binding", "org.karpukhin.webservice.data");
        } catch (JiBXException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createStudent(Student student) {
        Map<String, Object> params = new HashMap<String, Object>();
        //params.put("id", "")
        List<String> result = invokeWithParams("createStudent", params);
        if (!result.isEmpty()) {
            return Boolean.parseBoolean(result.get(0));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Student getStudentById(int id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("args0", id);
        return invokeWithParams("getStudentById", STUDENT_TYPE, params).get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateStudent(Student student) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteStudent(int id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("args0", id);
        List<String> result = invokeWithParams("deleteStudent", params);
        if (!result.isEmpty()) {
            return Boolean.parseBoolean(result.get(0));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Student> getAllStudents() {
        return invoke("getAllStudents", STUDENT_TYPE);
    }

    private InputStream getStream(String method, Map<String, Object> params) {
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
        log.info(uri.toString());
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(uri.toString());
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException(response.getStatusLine().getReasonPhrase());
            }
            return response.getEntity().getContent();
        } catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes remote method with given name and parameters. Returns list of
     * values extracted from method invocation result.
     * @param method remote method name
     * @param params remote method input parameters
     * @return list of values
     */
    private List<String> invokeWithParams(String method, Map<String, Object> params) {
        InputStream stream = getStream(method, params);
        return extractValues(stream, method);
    }

    /**
     * Invokes remote method with given name and result type
     * @param method     remote method name
     * @param resultType result type
     * @return a list of objects with given result type
     */
    private List<Student> invoke(String method, String resultType) {
        return invokeWithParams(method, resultType, null);
    }

    /**
     * Invokes remote method with given name, result type and input parameters
     * @param method     remote method name
     * @param resultType result type
     * @param params     remote method input parameters
     * @return a list of objects with given result type
     */
    private List<Student> invokeWithParams(String method, String resultType, Map<String, Object> params) {
        InputStream stream = getStream(method, params);
        return extractObjects(stream, method, resultType);
    }

    /**
     * Extracts objects from remote method invocation result
     * @param stream     stream with method invocation result
     * @param method     remote method name
     * @param resultType result type
     * @return list of objects
     */
    private List extractObjects(InputStream stream, String method, String resultType) {
        List result = new ArrayList();
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            IXMLReader reader = new StAXReaderWrapper(streamReader, "response", true);

            UnmarshallingContext context = (UnmarshallingContext)factory.createUnmarshallingContext();

            context.setDocument(reader);
            context.toTag();

            context.parsePastStartTag(XYZ_XSD_NAMESPACE, method + "Response");
            while(context.isAt(XYZ_XSD_NAMESPACE, "return")) {
                if (context.attributeBoolean("http://www.w3.org/2001/XMLSchema-instance", "nil", false)) {
                    context.skipElement();
                } else {
                    String type = context.attributeText("http://www.w3.org/2001/XMLSchema-instance", "type");
                    log.debug("Return type: " + type);
                    log.debug(context.getNamespaceUri(type.split(":")[0]));
                    try {
                        Class clazz = map.get(resultType);
                        assert clazz != null;
                        Object res = context.getUnmarshaller(resultType).unmarshal(clazz.newInstance(), context);
                        log.debug(res.toString());
                        result.add(res);
                    } catch (InstantiationException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                    context.parsePastCurrentEndTag(XYZ_XSD_NAMESPACE, "return");
                }
            }
        } catch (JiBXException e) {
            log.error(e);
            throw new RuntimeException(e);
        } catch (XMLStreamException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Extracts single value from remote method invocation result
     * @param stream stream with method invocation result
     * @param method remote method name
     * @return single value
     */
    private List<String> extractValues(InputStream stream, String method) {
        List<String> result = new ArrayList<String>();
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(stream);
            IXMLReader reader = new StAXReaderWrapper(streamReader, "response", true);

            UnmarshallingContext context = (UnmarshallingContext)factory.createUnmarshallingContext();
            context.setDocument(reader);
            context.toTag();

            context.parsePastStartTag(XYZ_XSD_NAMESPACE, method + "Response");
            while(context.isAt(XYZ_XSD_NAMESPACE, "return")) {
                String res = context.parseElementText(XYZ_XSD_NAMESPACE, "return");
                log.debug(res);
                result.add(res);
            }
        } catch (JiBXException e) {
            log.error(e);
            throw new RuntimeException(e);
        } catch (XMLStreamException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
        return null;
    }
}
