package edu.northeastern.springbootjdbc.controllers;

import java.util.List;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import edu.northeastern.springbootjdbc.daos.CourseDao;
import edu.northeastern.springbootjdbc.daos.CourseRoleDao;
import edu.northeastern.springbootjdbc.daos.PersonDao;
import edu.northeastern.springbootjdbc.models.Course;
import edu.northeastern.springbootjdbc.models.Person;
import edu.northeastern.springbootjdbc.models.RoleType;

/**
 * CourseRole web Service
 * @author abhiruchi
 *
 */
@RestController
public class CourseRoleService {
	private static final Logger LOGGER = Logger.getLogger(CourseRoleService.class.getName());
	/**
	 * method for student to join a class
	 * @return the number of rows affected
	 *        - indicating whether operation was successful.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/course/join", method=RequestMethod.POST)
	public @ResponseBody int joinCourse(
			@RequestBody String json) {
		CourseRoleDao crdao = CourseRoleDao.getInstance();
		int courseId = 0;
		int userId = 0;
		JSONObject obj;
		try {
			obj = new JSONObject(json);
			courseId = obj.getInt("courseId");
			userId = obj.getInt("userId");
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		
		return crdao.joinCourseAsStudent(courseId,userId);
	}
	
	/**
	 * method to add TA for a course
	 * @return the number of rows affected
	 *        - indicating whether operation was successful.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/add/courseTA")
	public @ResponseBody int addCourseTA(
			@RequestParam("code") String code,
			@RequestParam("semester") String semester,
			@RequestParam("username") String username) {
		int personID =  PersonDao.getInstance().findPersonByUsername(username).getId();
		int cid = CourseDao.getInstance().getCourseID(code, semester);
		return CourseRoleDao.getInstance().addTA(cid, personID);
	}
	
	/**
	 * method to drop person for a course
	 * @return the number of rows affected
	 *        - indicating whether operation was successful.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/course/drop", method=RequestMethod.POST)
	public @ResponseBody int deleteStudentFromCourse(
			@RequestBody String json) {
		int courseId = 0;
		int userId = 0;
		JSONObject obj;
		try {
			obj = new JSONObject(json);
			courseId = obj.getInt("courseId");
			userId = obj.getInt("userId");
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		
		return CourseRoleDao.getInstance().unmapProfessor(courseId, userId);
	}
	
	/**
	 * method to get Persons for a course
	 * @return the list of Persons with given role.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/get/persons")
	public @ResponseBody List<Person> getPersonsforCourse(
			@RequestParam("code") String code,
			@RequestParam("semester") String semester,
			@RequestParam("role") String role) {
		int cid = CourseDao.getInstance().getCourseID(code, semester);
		return CourseRoleDao.getInstance().getPersonsForCourse(cid, role);
	}
	
	/**
	 * method to get Courses for a person
	 * @return the list of Courses for the Person
	 */
	@RequestMapping(value="/api/courses", method=RequestMethod.GET)
	public @ResponseBody List<Course> getCoursesforPerson(
			@RequestParam("username") String username,
			@RequestParam("role") String role) {
		int personid = PersonDao.getInstance().findPersonByUsername(username).getId();
		return CourseRoleDao.getInstance().getCourseForPerson(personid);
	}
	
	/**
	 * method to get Courses for a person
	 * @return the list of Courses for the Person
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/{id}/courses", method=RequestMethod.GET)
	public @ResponseBody List<Course> getCoursesforPerson(
			@PathVariable("id") String id) {
		return CourseRoleDao.getInstance().getCourseForPerson(Integer.parseInt(id));
	}
	
	/**
	 * method to get Courses for a person
	 * @return the list of Courses for the Person
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/{userId}/course/{courseId}/uniqueid", method=RequestMethod.POST)
	public @ResponseBody String checkIfPersonEnrolled(
			@PathVariable("userId") String id, @PathVariable("courseId") String cId,
			@RequestBody String json) {
		JSONObject obj;
		String type = "";
		try {
			obj = new JSONObject(json);
			type = obj.getString("type");
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		List<Person> plist = CourseRoleDao.getInstance().getPersonsForCourse(Integer.parseInt(cId), type);
		String returnJson = "";
		for(Person p : plist) {
			if(p.getId() == Integer.parseInt(id))
				returnJson = "{\"id\": "+p.getId()+"}";
			else
				returnJson = "{\"id\": -1}";
		}
		
		return returnJson;
	}
	
	/**
	 * Check if a person is in a course already
	 * @return the 1 if yes else 0
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/{userId}/course/{courseId}/isInCourse", method=RequestMethod.GET)
	public @ResponseBody int checkIfEnrolled(
			@PathVariable("userId") int id, @PathVariable("courseId") int cId) {

		return CourseRoleDao.getInstance().isEnrolled(id, cId);
	}
	
	
	
	/**
	 * method to get course Professor's email
	 * @return the String of Professor's email.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/get/profemail")
	public @ResponseBody String getProfEmail(
			@RequestParam("code") String code,
			@RequestParam("semester") String semester) {
		int cid = CourseDao.getInstance().getCourseID(code, semester);
		return CourseRoleDao.getInstance().getPersonsForCourse(cid, RoleType.PROFESSOR.name()).get(0).getEmail();
	}
}
