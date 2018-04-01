package edu.northeastern.cs5500;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class S3 {
	
	static final Logger LOGGER = Logger.getLogger(S3.class.getName());
	static final String PATH_DELIM = "/";
	static final BasicAWSCredentials awsCreds = new BasicAWSCredentials(Constants.AWS_ACCESS_KEY, Constants.AWS_SECRET_KEY);
	static final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
			.withRegion(Constants.S3_REGION).build();
	
	private S3() {}
	

	/**
	 * Put an object in the database
	 * 
	 * @param bucketName
	 *            Bucket to upload object to
	 * @param keyName
	 *            Key Name(Folder path) to upload the object to
	 * @param uploadFileName
	 *            FileName of the uploaded object
	 * @return
	 */

	public static String putObject(String bucketName, String keyName, String uploadFileName, Boolean getUrl) {
		FileInputStream fis = null;
		String md5 = "";
		if (!getUrl) {
			
			try {
				fis = new FileInputStream(new File(keyName));
			} catch (FileNotFoundException e1) {
				LOGGER.info(e1.toString());
			}

			try {
				md5 = DigestUtils.md5Hex(fis);
			} catch (IOException e1) {
				LOGGER.info(e1.toString());
			}

			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e1) {
				LOGGER.info(e1.toString());
			}
		}

		try {
			PutObjectRequest por = new PutObjectRequest(bucketName, keyName, new File(uploadFileName))
					.withCannedAcl(CannedAccessControlList.PublicRead);
			getS3client().putObject(por);
			if (getUrl) {
				AmazonS3Client s3ClientNew = (AmazonS3Client) AmazonS3ClientBuilder.defaultClient();
				return s3ClientNew.getResourceUrl(bucketName, keyName).replace("us-west-1", "us-east-2");
			}
		} catch (AmazonServiceException e) {
			LOGGER.info(e.getErrorMessage());
		}
		return md5;
	}

	public static AmazonS3 getS3client() {
		return s3Client;
	}
}