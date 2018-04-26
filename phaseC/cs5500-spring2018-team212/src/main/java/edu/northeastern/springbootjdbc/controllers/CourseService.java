package edu.northeastern.springbootjdbc.controllers;

import java.util.List;
import java.util.Map;
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
import edu.northeastern.springbootjdbc.models.Course;

/**
 * Course web Service
 * 
 * @author abhiruchi
 *
 */
@RestController
public class CourseService {
	private static final Logger LOGGER = Logger.getLogger(CourseService.class.getName());
	/**
	 * Creates a course in the database and 
	 * makes the person creating the course the professor for that course.
	 * @return the number of rows affected
	 *        - indicating whether operation was successful.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/{userId}/courses",method=RequestMethod.POST)
	public @ResponseBody int createCourse(@PathVariable("userId") String id,
			@RequestBody String json) {
		JSONObject obj;
		int r = 0;
		try {
			obj = new JSONObject(json);
			String name = obj.getString("name");
			String semester = obj.getString("semester");
			String code = obj.getString("code");
			CourseDao cdao = CourseDao.getInstance();
			
			Course c = new Course(name, semester, code);
			r = cdao.createCourse(c);
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		CourseRoleDao crdao = CourseRoleDao.getInstance();
		crdao.mapProfessor(r, Integer.parseInt(id));
		return r;
	}

	/**
	 * Updates a course in the database
	 * 
	 * @return the number of rows affected - indicating whether operation was
	 *         successful.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/update/course")
	public @ResponseBody int updateCourse(@RequestParam("name") String name, @RequestParam("code") String semester,
			@RequestParam("semester") String code, @RequestParam("courseID") int courseID) {
		CourseDao cdao = CourseDao.getInstance();
		Course c = new Course(name, semester, code);
		return cdao.updateCourse(courseID, c);
	}


	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/course/{courseId}",method=RequestMethod.POST)
	public @ResponseBody int updateCourseById(@PathVariable("courseId") String id,
			@RequestBody String json) {
		JSONObject obj;
		String name = "";
		String semester = "";
		String code = "";
		try {
			obj = new JSONObject(json);
			name = obj.getString("name");
			semester = obj.getString("semester");
			code = obj.getString("code");
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		CourseDao cdao = CourseDao.getInstance();
		Course c = new Course(name, semester, code);
		return cdao.updateCourse(Integer.parseInt(id), c);
	}
	
	/**
	 * Find course by id
	 * 
	 * @return List of all the courses in the database
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/user/{userId}/course/{courseId}")
	public @ResponseBody Course selectCoursesByCourseId(@PathVariable("userId") String userId,
			@PathVariable("courseId") String courseId) {
		CourseDao cdao = CourseDao.getInstance();
		return cdao.findCoursebyID(Integer.parseInt(courseId));
	}
	

	/**
	 * Deletes a course in the database
	 * 
	 * @return the number of rows affected - indicating whether operation was
	 *         successful.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/course/{courseId}", method=RequestMethod.DELETE)
	public @ResponseBody int deleteCourse(@PathVariable("courseId") int courseID) {
		CourseDao cdao = CourseDao.getInstance();
		return cdao.deleteCourse(courseID);
	}

	/**
	 * Find all courses
	 * 
	 * @return List of all the courses in the database
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/allCourses")
	public List<Course> selectAllCourses() {
		CourseDao cdao = CourseDao.getInstance();
		return cdao.findAllCourses();
	}
	
	/**
	 * Find all courses with same name
	 * 
	 * @return List of all the courses in the database
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/{coursecode}/sameCourse")
	public @ResponseBody List<Course> selectAllCoursesWithSameCode(@PathVariable("coursecode") String coursecode) {
		CourseDao cdao = CourseDao.getInstance();
		return cdao.findCourseWithSameCode(coursecode);
	}
	
	/**
	 * Find all courses in the given semester
	 * 
	 * @return List of all the courses in the database
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = {"/api/allCourses/semester/{semester}"}, method = RequestMethod.GET)
	public List<Course> selectCoursesbySemester(@PathVariable Map<String, String> pathVariablesMap) {
		CourseDao cdao = CourseDao.getInstance();
		if (pathVariablesMap.containsKey("semester"))
			return cdao.findCoursesBySemester(pathVariablesMap.get("semester"));
		else
			return cdao.defaultCourses();
	}

}