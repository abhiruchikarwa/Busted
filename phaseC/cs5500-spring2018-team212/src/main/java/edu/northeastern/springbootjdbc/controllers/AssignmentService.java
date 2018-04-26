package edu.northeastern.springbootjdbc.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import edu.northeastern.springbootjdbc.daos.PersonDao;

import org.eclipse.jgit.util.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zeroturnaround.zip.ZipUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.northeastern.cs5500.CallLibrary;
import edu.northeastern.cs5500.Directory;
import edu.northeastern.cs5500.GitUtil;
import edu.northeastern.cs5500.PlagiarismResult;
import edu.northeastern.cs5500.S3;
import edu.northeastern.cs5500.SendMailSSL;
import edu.northeastern.springbootjdbc.daos.AssignmentDao;
import edu.northeastern.springbootjdbc.daos.CourseDao;
import edu.northeastern.springbootjdbc.daos.ReportDao;
import edu.northeastern.springbootjdbc.models.Assignment;
import edu.northeastern.springbootjdbc.models.Course;
import edu.northeastern.springbootjdbc.models.Report;

/**
 * Assignment Web Service
 * 
 * @author sirushti
 *
 */

@RestController
public class AssignmentService {

	static final Logger LOGGER = Logger.getLogger(AssignmentService.class.getName());
	static final String PATH_DELIM = "/";

	/**
	 * Uploads a Git Repository to S3
	 * 
	 * @param hwDir
	 * @param hwName
	 * @param githublink
	 * @param aid
	 */
	public static int uploadGitRepo(String hwDir, String hwName, String githublink, int aid) {
		File assignmentDir = new File(hwDir + PATH_DELIM + aid);
		assignmentDir.mkdirs();
		GitUtil.cloneRepo(githublink, assignmentDir);
		String assignmentZipName = assignmentDir.toString() + ".zip";
		ZipUtil.pack(assignmentDir, new File(assignmentZipName));
		String assignmentBucketName = "plagiarismdetector";
		// Strip plagarismDetector in keyName.
		String md5Hex = S3.putObject(assignmentBucketName, assignmentZipName, assignmentZipName, false);
		AssignmentDao adao = AssignmentDao.getInstance();
		adao.updateSha(md5Hex, aid);
		return adao.getAssignmentCount(hwName);
	}

	/**
	 * Checks for Plagiarism in the assignments that have been uploaded.
	 * 
	 * @param hwDir
	 * @param folderStructure
	 */
	public static void checkPlagiarism(String hwDir, String folderStructure, int strictness,
			int courseID, int studentID, String language) {
		Boolean b = true;
		CallLibrary jplagLib = new CallLibrary();
		List<PlagiarismResult> results = jplagLib.getReports(hwDir, folderStructure, strictness, language); 
		String reportBucketName = "plagiarismresults";
		File tempDir1 = new File(hwDir);
		tempDir1.mkdirs();
		addReports(results, reportBucketName, folderStructure, studentID, courseID);
		String opdir = folderStructure + "op";
		File f = new File(opdir);
		if(f.exists() && f.isDirectory()) {
			try {
				FileUtils.delete(f, FileUtils.RECURSIVE);
			} catch (IOException e) {
				LOGGER.info(e.toString());
			}
		}
	}

	public static void addReports(List<PlagiarismResult> results, String reportBucketName,
			String folderStructure, int studentID, int courseID) {
		ReportDao rdao = ReportDao.getInstance();
		
		if (results != null) {
			for (PlagiarismResult pr : results) {
				String keyName1 = folderStructure + "op" + PATH_DELIM + new File(pr.getPath()).getName();
				String reportZipName = keyName1 + ".zip";
				File keyDir = new File(pr.getPath());
				if(keyDir.isDirectory() && keyDir.list().length == 0) {
					continue;
				}

				ZipUtil.pack(keyDir, new File(reportZipName));
				String reportURL = S3.putObject(reportBucketName, reportZipName, reportZipName, true);
				S3.uploadDir("plagiarismresults", keyName1);
				Report r1 = new Report(pr.getAssignmentID1(), pr.getAssignmentID2(), pr.getSimilarityScore(), reportURL, false);
				int reportID = rdao.createReport(r1);
				if (reportID == 0)
					continue;

				SendMailSSL mail = new SendMailSSL();
				PersonDao personDao = PersonDao.getInstance();
				mail.send("^EjHs0R4&wot", personDao.findPersonById(studentID).getEmail(),
						  "Plagiarism Detected for " + studentID,
						  "Report can be found at http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200/user/website/" + courseID + "/page/" + r1.getAssignment2ID() + "/report/" + reportID,
						  System.getProperty("user.dir")+"/config.properties");
			}
		}
		else
			LOGGER.info("no reports");
	}
	
	/**
	 * Wrapper function for Git Clone and Plagiarism Check.
	 * 
	 * @param folderStructure
	 * @param hwName
	 * @param githublink
	 * @param aid
	 */
	public static void cloneAndCheck(String folderStructure, String hwName, String githublink, int aid,
			int strictness,	int courseID, int studentID, String language) {
		String hwDir = "assignments" + PATH_DELIM + folderStructure;
		String currDir = hwDir + PATH_DELIM + aid;
		uploadGitRepo(hwDir, hwName, githublink, aid);
		checkPlagiarism(currDir, hwDir, strictness, courseID, studentID, language);
	}

	/**
	 * Upload Assignment REST API.
	 * 
	 * @param name
	 *            Name of the assignment.
	 * @param studentid
	 *            ID of the student.
	 * @param courseID
	 *            ID of the course.
	 * @param githublink
	 *            Github repo.
	 * @RequestParam("name") String hwName, @RequestParam("studentid") int studentid,
			@RequestParam("courseid") int courseID, @RequestParam("githublink") String githublink
	 * @return
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/assignment/uploadGit", method = RequestMethod.POST)
	public @ResponseBody int uploadGit(@RequestBody String json) {
		//change this as an input from user 
		int strictness = 1;
		AssignmentDao adao = AssignmentDao.getInstance();
		JSONObject obj;
		String hwName = ""; 
		String githublink = "";
		String language = "";
		int courseID = 0;
		int studentid = 0;
		int parentAid = 0;
		try {
			obj = new JSONObject(json);
			hwName = obj.getString("hwName");
			githublink = obj.getString("githublink");
			courseID = obj.getInt("courseid");
			studentid = obj.getInt("studentid");
			parentAid = obj.getInt("parentAssignment");
			language = obj.getString("language");
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		Assignment a1 = adao.findAssignmentById(parentAid);
		CourseDao cdao = CourseDao.getInstance();
		Course course = cdao.findCoursebyID(courseID);
		java.util.Date utilDate = new java.util.Date();
		Long currentTime = utilDate.getTime();
		String folderStructure = course.getCode() + PATH_DELIM + course.getSemester() + PATH_DELIM + hwName;
		Assignment assignment = new Assignment(hwName, studentid, new Date(currentTime), a1.getDuedate(), false, false, "", 0, githublink, courseID);
		int aid = adao.createAssignment(assignment);

		cloneAndCheck(folderStructure, hwName, githublink, aid, strictness, courseID, studentid, language);

		adao.checkAssignment(aid);
		return aid;
	}

	/**
	 * Get assignments for a particular course.
	 * @param courseid
	 * @return
	 * @throws JsonProcessingException 
	 */ 	
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/{userid}/course/{courseId}/assignment", method = RequestMethod.GET)
	public @ResponseBody List<Assignment> getAssignments(@PathVariable("userid") int profid, @PathVariable("courseId") int courseid) throws JsonProcessingException {
		AssignmentDao adao = AssignmentDao.getInstance();
		return adao.getAvailableAssignments(courseid, profid);
	}

	/**
	 * Get assignments for a particular course.
	 * @param courseid
	 * @return
	 * @throws JsonProcessingException 
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/course/assignment/{assignmentId}", method = RequestMethod.GET)
	public @ResponseBody Assignment getAssignmentById(@PathVariable("assignmentId") int assignmentId) {
		AssignmentDao adao = AssignmentDao.getInstance();
		return adao.findAssignmentById(assignmentId);
	}

	/**
	 * Get submissions for a particular assignment.
	 * 
	 * @param courseid
	 * @param hwName
	 * @return
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/course/{courseid}/assignment/{hwName}", method = RequestMethod.GET)
	public @ResponseBody List<Assignment> getSubmissions(@PathVariable("courseid") int courseid, @PathVariable("hwName") String hwName) {
		AssignmentDao adao = AssignmentDao.getInstance();
		return adao.getSubmissionsForAssignment(hwName, courseid);
	}

	/**
	 * Get submissions for a particular assignment of one student.
	 * 
	 * @param courseid
	 * @param hwName
	 * @return
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/course/{courseid}/assignment/{hwName}/user/{useId}", method = RequestMethod.GET)
	public @ResponseBody List<Assignment> getSubmissions(@PathVariable("courseid") int courseid, @PathVariable("hwName") String hwName,
			@PathVariable("useId") int studentid) {
		AssignmentDao adao = AssignmentDao.getInstance();
		return adao.getSubmissionsForOneStudent(hwName, courseid, studentid);
	}

	/**
	 * Create an assignment for Professor.
	 * 
	 * @param courseid
	 * @param hwName
	 * @return
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/assignment/new", method = RequestMethod.POST)

	public @ResponseBody int createAssignmentForProfessor(@RequestBody String payload) {
		JSONObject obj;
		String hwName = "";
		String jsonDuedate = "";
		int profid = 0, courseid = 0;
		try {
			obj = new JSONObject(payload);
			hwName = obj.getString("name");
			profid = obj.getInt("studentId");
			courseid = obj.getInt("courseId");
			jsonDuedate = obj.getString("duedate");
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date parsed = null;
		java.sql.Date duedate = null;
		try {
			parsed = format.parse(jsonDuedate);
			duedate = new java.sql.Date(parsed.getTime());
		} catch (ParseException e) {
			LOGGER.info(e.toString());
		}

		AssignmentDao adao = AssignmentDao.getInstance();
		java.util.Date utilDate = new java.util.Date();
		Long currentTime = utilDate.getTime();
		Assignment assignment = new Assignment(hwName, profid, new Date(currentTime), duedate, false, false, "prof", 0, "",  courseid);
		return adao.createAssignment(assignment);
	}

	/**
	 * Deletes a assignment in the database
	 * 
	 * @return the number of rows affected - indicating whether operation was
	 *         successful.
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/course/assignment/{assignmentId}", method=RequestMethod.DELETE)
	public @ResponseBody int deleteAssigment(@PathVariable("assignmentId") int assignmentId) {
		AssignmentDao adao = AssignmentDao.getInstance();
		return adao.deleteAssignment(assignmentId);
	}

	/**
	 * Updates a assignment in the database
	 * 
	 * @return the number of rows affected - indicating whether operation was
	 *         successful.
	 * @throws JSONException 
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/assignment/{assignmentId}", method = RequestMethod.PUT)
	public @ResponseBody int updateAssignment(@PathVariable("assignmentId") int assignId,@RequestBody String json) throws JSONException {
		AssignmentDao adao = AssignmentDao.getInstance();

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date parsed = null;
		java.sql.Date aduedate = null;
		JSONObject obj;
		String name = "";
		int profid = 0;
		int courseID = 0; 
		try {
			obj = new JSONObject(json);
			name = obj.getString("name");
			profid = obj.getInt("studentId");
			courseID = obj.getInt("courseId");
			parsed = format.parse(obj.getString("duedate"));
			aduedate = new java.sql.Date(parsed.getTime());
		} catch (ParseException e) {
			LOGGER.info(e.toString());
		}
		java.util.Date utilDate = new java.util.Date();
		Long currentTime = utilDate.getTime();

		Assignment c = new Assignment(name, profid, new Date(currentTime), aduedate, false, false, "prof", 0 , "", courseID);
		c.setId(assignId);
		//		Assignment assignment = new Assignment(hwName, profid, new Date(currentTime), duedate, false, false, "prof", 0, "",  courseid);
		return adao.updateAssignment(courseID, c);
	}


	/** method to compare assignments individually
	 * @param assignmentid1
	 * @param assignmentid2
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/assignment/individual/{assignmentid1}/{assignmentid2}/{language}",
	method = RequestMethod.GET)
	public @ResponseBody void testIndividual(@PathVariable("assignmentid1") int assignmentid1,
			@PathVariable("assignmentid2") int assignmentid2, @PathVariable("language") String language) {
		AssignmentDao adao = AssignmentDao.getInstance();
		Map<Integer, String> aid1 = adao.getInfoforAssignment(assignmentid1);
		Map<Integer, String> aid2 = adao.getInfoforAssignment(assignmentid2);

		ReportDao rdao = ReportDao.getInstance();
		
		String reportBucketName = "plagiarismresults";
		
		Boolean b = false;
		String hwName1 = aid1.get(1);
		String hwName2 = aid2.get(1);
		CourseDao cdao = CourseDao.getInstance();
		Course c1 = cdao.findCoursebyID(Integer.parseInt(aid1.get(2)));
		Course c2 = cdao.findCoursebyID(Integer.parseInt(aid2.get(2)));

		if (hwName1.equalsIgnoreCase(hwName2) && c1.toString().equals(c2.toString())) {
			String folderStructure ="assignments" + PATH_DELIM 
					+ c1.getCode() + PATH_DELIM + c1.getSemester() + PATH_DELIM + hwName1;
			String dir1 = folderStructure + PATH_DELIM + assignmentid1;
			String dir2 = folderStructure + PATH_DELIM + assignmentid2;
			CallLibrary lib = new CallLibrary();
			List<PlagiarismResult> results= lib.getIndividualReport(dir1, dir2, 1, language);
			for (PlagiarismResult pr : results) {
				String keyName1 = "test_op" + PATH_DELIM + new File(pr.getPath()).getName();
				String reportZipName = keyName1 + ".zip";
				File keyDir = new File(pr.getPath());
				if(keyDir.isDirectory() && keyDir.list().length == 0) {
					continue;
				}

				ZipUtil.pack(keyDir, new File(reportZipName));
				String reportURL = S3.putObject(reportBucketName, reportZipName, reportZipName, true);
				S3.uploadDir("plagiarismresults", keyName1);
				Report r1 = new Report(pr.getAssignmentID1(), pr.getAssignmentID2(), pr.getSimilarityScore(), reportURL, false);
				int reportID = rdao.createReport(r1);
				if (reportID == 0)
					continue;

				SendMailSSL mail = new SendMailSSL();
				mail.send("^EjHs0R4&wot", "team212updates@gmail.com",
						"Plagiarism Detected for " + 1,
						"Report can be found at http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200/user/website/" + c1.getId() + "/page/" + r1.getAssignment2ID() + "/report/" + reportID,
						System.getProperty("user.dir")+"/config.properties");
			}
		}
		if(new Directory().deleteDir("test_op"))	
			b = b & new File("test_op").delete();
		
	}

	
}
