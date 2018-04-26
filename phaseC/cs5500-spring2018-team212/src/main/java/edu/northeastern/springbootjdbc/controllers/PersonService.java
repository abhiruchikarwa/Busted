package edu.northeastern.springbootjdbc.controllers;

import java.util.List;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.northeastern.cs5500.Constants;

import edu.northeastern.springbootjdbc.daos.PersonDao;
import edu.northeastern.springbootjdbc.models.Person;
import edu.northeastern.springbootjdbc.models.RoleType;

/**
 * Person Web Service
 * 
 * @author Paarth
 *
 */
@RestController
@Configuration
public class PersonService {
	private static final Logger LOGGER = Logger.getLogger(PersonService.class.getName());
	/**
	 * Creates a person in the database
	 * /api/user/{userid}
	 * @return
	 * @return the number of rows affected
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/register")
	public @ResponseBody Person insertPerson(@RequestBody String json) {
		PersonDao dao = PersonDao.getInstance();
		JSONObject obj;
		String email = "";
		try {
			obj = new JSONObject(json);
			String firstname = obj.getString("firstname");
			String lastname = obj.getString("lastname");
			email = obj.getString("email");
			String password = obj.getString("password");
			String type = obj.getString("role");
			Person p = new Person(firstname, lastname, email, password, null, type);
			if (type.equals(RoleType.STUDENT.name()))
				p.setIsApproved(1);
			dao.createPerson(p);
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		return dao.findPersonByUsername(email);
	}
	
	/**
	 * Login a person 
	 * 
	 * @return
	 * @return the number of rows affected
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/login")
	public @ResponseBody Person login(@RequestBody String json) {
		PersonDao dao = PersonDao.getInstance();
		JSONObject obj;
		String email = "";
		String pass = "";
		try {
			obj = new JSONObject(json);
			email = obj.getString("username");
			pass = obj.getString("password");
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		Person p = dao.findPersonByUsername(email);
		if (email.equals(p.getEmail()) && pass.equals(p.getPassword()))
			return p;
		else
			return null;
	}


	/**
	 * Find a person by user name
	 * 
	 * @param username
	 *            unique string based on which the search is conducted
	 * @return Person details
	 * @throws JsonProcessingException 
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200",})
	@RequestMapping(value = "/api/user", method = RequestMethod.GET)
	public @ResponseBody String selectPersonByUsername(@RequestParam("username") String email) throws JsonProcessingException {
		PersonDao dao = PersonDao.getInstance();
		ObjectMapper mapperObj = new ObjectMapper();
		Person p = dao.findPersonByUsername(email);
		String returnJson = "";
		if (p == null) {
			returnJson = "{\"email\": \"null\"}";
			return returnJson;
		} else {
			return mapperObj.writeValueAsString(p);
		}
	}

	/**
	 * Find all people
	 * 
	 * @param username
	 *            unique string based on which the search is conducted
	 * @return List of People details
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping("/api/user/all")
	public @ResponseBody List<Person> selectAllPeople() {
		PersonDao dao = PersonDao.getInstance();
		return dao.findAllPeople(false);
	}

	/**
	 * Find all people
	 * 
	 * @param username
	 *            unique string based on which the search is conducted
	 * @return List of People details
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value="/api/user/{userId}", method=RequestMethod.GET)
	public @ResponseBody Person selectPersonById(@PathVariable("userId") String id) {
		PersonDao dao = PersonDao.getInstance();
		return dao.findPersonById(Integer.parseInt(id));
	}
	
	/**
	 * Find a person by user name
	 * 
	 * @param username
	 *            unique string based on which the search is conducted
	 * @return Person details
	 */
	@CrossOrigin(origins = {"http://localhost:4200", "http://ec2-18-188-212-155.us-east-2.compute.amazonaws.com:4200"})
	@RequestMapping(value = "/api/user/{userid}", method = RequestMethod.POST)
	public @ResponseBody Person updatePerson(@PathVariable("userid") String id, @RequestBody String json) {
		PersonDao dao = PersonDao.getInstance();
		JSONObject obj;
		try {
			obj = new JSONObject(json);
			String firstname = obj.getString("firstName");
			String lastname = obj.getString("lastName");
			String password = obj.getString(Constants.AWS_P);
			String email = obj.getString("email");
			String phone = obj.getString("phone");
			String type = obj.getString("type");
			Person p = new Person(firstname, lastname, email, password, phone, type);
			dao.updatePerson(Integer.parseInt(id), p);
		} catch (JSONException e) {
			LOGGER.info(e.toString());
		}
		return dao.findPersonById(Integer.parseInt(id));
	}

}
