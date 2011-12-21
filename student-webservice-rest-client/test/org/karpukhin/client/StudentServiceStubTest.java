package org.karpukhin.client;

import org.junit.Before;
import org.junit.Test;
import org.karpukhin.webservice.StudentService;
import org.karpukhin.webservice.data.Student;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Karpukhin
 */
public class StudentServiceStubTest {

    private StudentService service;
    private static final int STUDENT_ID = 123;

    @Before
    public void setUp() {
        service = new StudentServiceStub();
    }

    //@Test
    public void testCreateStudent() {
        Student student = new Student();
        boolean result = service.createStudent(student);
        assertTrue(result);
    }

    @Test
    public void testGetStudentById() {
        Student result = service.getStudentById(STUDENT_ID);
        assertNotNull(result);
    }

    //@Test
    public void testUpdateStudent() {
        Student student = new Student();
        boolean result = service.updateStudent(student);
        assertTrue(result);
    }

    @Test
    public void testDeleteStudent() {
        boolean result = service.deleteStudent(STUDENT_ID);
        assertTrue(result);
    }

    @Test
    public void testGetAllStudents() {
        List<Student> result = service.getAllStudents();
        assertNotNull(result);
    }
}
