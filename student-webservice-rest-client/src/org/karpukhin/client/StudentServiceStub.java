package org.karpukhin.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.karpukhin.webservice.StudentService;
import org.karpukhin.webservice.data.Student;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Karpukhin
 */
public class StudentServiceStub extends ClientStub implements StudentService {

    private static final String XYZ_XSD_NAMESPACE = "http://www.xyz.org/xsd";
    private static final String STUDENT_TYPE = "{http://data.webservice.karpukhin.org/xsd}:Student";

    private static final Log log = LogFactory.getLog(StudentServiceStub.class);

    private static final Map<String, Class> map = new HashMap<String, Class>();

    static {
        map.put(STUDENT_TYPE, Student.class);
    }

    public StudentServiceStub() {
        this("http://localhost:8080/axis2/services/StudentService");
    }

    public StudentServiceStub(String url) {
        super(url, XYZ_XSD_NAMESPACE, map, "org.karpukhin.webservice.data");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createStudent(Student student) {
        Map<String, Object> params = new HashMap<String, Object>();
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
        return (Student)invokeWithParams("getStudentById", params).get(0);
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
        List result = invokeWithParams("deleteStudent", params);
        if (!result.isEmpty()) {
            return Boolean.parseBoolean(result.get(0).toString());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Student> getAllStudents() {
        return invoke("getAllStudents");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> someMethod() {
        return invoke("someMethod");
    }
}
