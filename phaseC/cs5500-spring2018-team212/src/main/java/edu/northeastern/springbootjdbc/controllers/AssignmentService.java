package edu.northeastern.springbootjdbc.controllers;

import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.zeroturnaround.zip.ZipUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.northeastern.cs5500.CallLibrary;
import edu.northeastern.cs5500.GitUtil;
import edu.northeastern.cs5500.PlagiarismResult;
import edu.northeastern.cs5500.S3;
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
	public static void checkPlagiarism(String hwDir, String folderStructure) {
		CallLibrary jplagLib = new CallLibrary();
		List<PlagiarismResult> results = jplagLib.getReports(hwDir, folderStructure, 1, 50); 
		String reportBucketName = "plagiarismresults";
		ReportDao rdao = ReportDao.getInstance();
		File tempDir1 = new File(hwDir);
		tempDir1.mkdirs();
		for (PlagiarismResult pr : results) {
			String keyName1 = folderStructure + "op" + PATH_DELIM + pr.getPath();
			// Strip plagiarismResults in reportURL
			String reportZipName = keyName1 + ".zip";
			File keyDir = new File(keyName1);
			if(keyDir.isDirectory() && keyDir.list().length == 0) {
			    return;
			}

			ZipUtil.pack(keyDir, new File(reportZipName));
			String reportURL = S3.putObject(reportBucketName, reportZipName, reportZipName, true);
			Report r1 = new Report(pr.getAssignmentID1(), pr.getAssignmentID2(), pr.getSimilarityScore(), reportURL, false);
			rdao.createReport(r1);
		}
	}

	/**
	 * Wrapper function for Git Clone and Plagiarism Check.
	 * 
	 * @param folderStructure
	 * @param hwName
	 * @param githublink
	 * @param aid
	 */
	public static void cloneAndCheck(String folderStructure, String hwName, String githublink, int aid) {
		String hwDir = "assignments" + PATH_DELIM + folderStructure;
		String currDir = hwDir + PATH_DELIM + aid;
		uploadGitRepo(hwDir, hwName, githublink, aid);
		checkPlagiarism(currDir, hwDir);
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
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-222-88-122.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/assignment/uploadGit", method = RequestMethod.POST)
	public @ResponseBody int uploadGit(@RequestBody String json) {
		AssignmentDao adao = AssignmentDao.getInstance();
		JSONObject obj;
		String hwName = ""; 
		String githublink = "";
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
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		Assignment a1 = adao.findAssignmentById(parentAid);
		CourseDao cdao = CourseDao.getInstance();
		Course course = cdao.findCoursebyID(courseID);
		java.util.Date utilDate = new java.util.Date();
		Long currentTime = utilDate.getTime();
		String folderStructure = course.getCode() + PATH_DELIM + course.getSemester() + PATH_DELIM + hwName;
		Assignment assignment = new Assignment(hwName, studentid, new Date(currentTime), a1.getDuedate(), false,
				false, "", 0, githublink, courseID);
		int aid = adao.createAssignment(assignment);
		if (aid == 0) {
			return 0;
		}

		cloneAndCheck(folderStructure, hwName, githublink, aid);
		adao.checkAssignment(aid);
		return aid;
	}

	/**
	 * Get assignments for a particular course.
	 * @param courseid
	 * @return
	 * @throws JsonProcessingException 
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-222-88-122.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/{userid}/course/{courseId}/assignment")
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
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-222-88-122.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/course/assignment/{assignmentId}", method = RequestMethod.GET)
	public @ResponseBody Assignment getAssignmentById(@PathVariable("assignmentId") int assignmentId) {
		AssignmentDao adao = AssignmentDao.getInstance();
		System.out.println(assignmentId);
		Assignment a = adao.findAssignmentById(assignmentId);
		System.out.println(a);
		return a;
	}
	
	/**
	 * Get submissions for a particular assignment.
	 * 
	 * @param courseid
	 * @param hwName
	 * @return
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-222-88-122.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/course/{courseId}/assignment/{hwName}/user/{userId}")
	public @ResponseBody List<Assignment> getSubmissions(@RequestParam("studentid") int studentid, @RequestParam("courseid") int courseid, @RequestParam("hwName") String hwName) {
		AssignmentDao adao = AssignmentDao.getInstance();
		return adao.getSubmissions(hwName, courseid, studentid);
	}
	
	/**
	 * Create an assignment for Professor.
	 * 
	 * @param courseid
	 * @param hwName
	 * @return
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-222-88-122.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/assignment/new")
	public @ResponseBody int createAssignmentForProfessor(@RequestBody Map<String, String> payload) {
		String hwName = payload.get("name");
		int profid = Integer.parseInt(payload.get("studentId"));
		int courseid = Integer.parseInt(payload.get("courseId"));
		SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
		java.util.Date parsed = null;
		Date duedate = null;
		try {
			parsed = format.parse(payload.get("duedate"));
			duedate = new Date(parsed.getTime());
		} catch (ParseException e) {
			LOGGER.info(e.toString());
		}
		
		AssignmentDao adao = AssignmentDao.getInstance();
		java.util.Date utilDate = new java.util.Date();
		Long currentTime = utilDate.getTime();
		Assignment assignment = new Assignment(hwName, profid, new Date(currentTime), duedate, false, false, "prof", 0, "",  courseid);
		return adao.createAssignment(assignment);
	}
}
