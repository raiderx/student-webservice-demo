package org.karpukhin.webservice;

import org.karpukhin.webservice.data.Student;

import java.util.List;

/**
 * @author Pavel Karpukhin
 */
public interface StudentService {

    /**
     * Creates new student
     * @param student student data
     * @return true if method invocation was successful
     */
    boolean createStudent(Student student);

    /**
     * Returns student with given id
     * @param id id
     * @return student
     */
    Student getStudentById(int id);

    /**
     * Updates student
     * @param student
     * @return true if method invocation was successful
     */
    boolean updateStudent(Student student);

    /**
     * Deletes student with given id
     * @param id id
     * @return true if method invocation was successful
     */
    boolean deleteStudent(int id);

    /**
     * Returns list of students
     * @return list of students
     */
    List<Student> getAllStudents();

    /**
     * Some method that returns a list of numbers
     * @return list of numbers
     */
    List<Integer> someMethod();
}
