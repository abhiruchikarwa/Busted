// executed on client

import {Website} from '../models/website.model.client';
import {Injectable} from '@angular/core';
import { User } from '../models/user.model.client';
import { Http } from '@angular/http';
import 'rxjs/Rx';
import {Response} from '@angular/http';
import {environment} from '../../environments/environment';

// make class usable for all components
@Injectable()
export class WebsiteService {

  constructor(private http: Http) {}

  baseUrl = environment.baseUrl;

  // generate a number for website._id
  newId() {
    return (Number( Math.floor((Math.random()) * 10000))).toString();
  }

  // adds the website parameter instance to the local websites array.
  // The new website's developerId is set to the userId parameter

  findAllClasses() {
    const url = this.baseUrl + '/api/allCourses';
    return this.http.get(url).map((response: Response) => {
      return response.json();
    });
  }

  findSameCourses(coursecode) {
    const url = this.baseUrl + '/api/' + coursecode + '/sameCourse';
    return this.http.get(url).map((response: Response) => {
      return response.json();
    });
  }

  createWebsite(userId: String, course) {
    const url = this.baseUrl + '/api/user/' + userId + '/courses';
    return this.http.post(url, course)
      .map((response: Response) => {
        return response.json();
      });
  }

  // retrieves the websites in local websites array whose developerId matches the parameter userId

  findWebsitesByUser(userId: String) {
    // initiate the array
    // const webs: Website[] = [];
    console.log(userId);
    const url = this.baseUrl + '/api/user/' + userId + '/courses';
    return this.http.get(url).map((response: Response) => {
      return response.json();
    });
  }

  //
  findWebsitesByUserAndRole(userId: String, role: String) {
    // initiate the array
    // const webs: Website[] = [];
    const url = this.baseUrl + '/api/user/' + userId + role + '/website';
    return this.http.get(url).map((response: Response) => {
      return response.json();
    });
  }

  // retrieves the website in local websites array whose _id matches the websiteId parameter

  findWebsiteById(userId, courseId) {
    const url = this.baseUrl + '/api/user/' + userId + '/course/' + courseId;
    return this.http.get(url)
      .map((response: Response) => {
      return response.json();
    });
  }

  // updates the website in local websites array whose _id matches the websiteId parameter

  updateWebsite(courseId, course) {
    const url = this.baseUrl + '/api/user/course/' + courseId;
    return this.http.post(url, course)
      .map((response: Response) => {
        return response.json();
      });
  }

  // removes the website from local websites array whose _id matches thewebsiteId parameter

  deleteWebsite(userId, courseId) {
    const url = this.baseUrl + '/api/user/course/' + courseId;
    return this.http.delete(url)
      .map((response: Response) => {
        return response.json();
      });

  }

}
