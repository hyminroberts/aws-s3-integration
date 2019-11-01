/**
 * Base class of storing, retrieving and removing miscellaneous resources files on Amazon Simple Storage Service(Amazaon S3) cloud storage platform.
 * End user could upload miscellaneous resources(XML files, images, text files, etc.) on behalf of different requirements within XXX system.
 * Because there is no folder/directory structure on Amazon S3, we would like to emulate folder structure using key prefixes.
 */

package com.xxx.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;

public abstract class BaseFileAwsStorage extends CommonObjectBase implements ResourceStorageResolver {

    private static String AWS_ACCESS_KEY_ID;
    private static String AWS_SECRET_ACCESS_KEY;
    private static String BUCKET;

    private AmazonS3 s3 = null;
    private String context = "";
    /**
     * Forward slash serving as folder delimiters on Amazon S3.
     * AWS added a concept of delimiter, which tells S3 to behave as though it has subdirectories.We could tell AWS to use a delimiter with --delimiter=X or -d.
     * If we specify -d, then the delimiter will be set to slash (/). This way, we can get behavior similar to what we get under Linux (etc.)
     * For more information, please refer to https://github.com/timkay/aws/wiki/S3-Prefixes-and-Delimiters-Explained
     */
    protected static final String FORWARD_SLASH = FilePathUtil.PATH_FORWARD_SLASH;

    /**
     * Exists only to defeat instantiation.
     */
    public BaseFileAwsStorage() {
        // Get system properties
        AWS_ACCESS_KEY_ID = Environment.getString("secure_s3_access_key_id", "");
        AWS_SECRET_ACCESS_KEY = Environment.getString("secure_s3_secret_access_key", "");
        BUCKET = Environment.getString("order_import_s3_bucket", "xxx-order-import-test");
        context = ContextName.getValue();
    }

    /**
     * Get the bucket.
     * @return BUCKET
     */
    protected String getBucket() {
        return BUCKET;
    }

    /**
     * Set context.
     * @param context to be set
     */
    protected void setContextForStorage(String context) {
        this.context = context;
    }

    /**
     * Set context.
     * @return current context
     */
    protected String getContextForStorage() {
        return context;
    }

    /**
     * Get the s3 client.
     * @return an instance of AmazonS3 object
     */
    protected AmazonS3 getS3Client() {
        if (s3 == null) {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);
            s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        }
        return s3;
    }

    /**
     * Cloud file storage doesn't really care about the concept of folders.
     * Key names contain prefixes with forward slashes to emulate the concept of folders.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @param fileName name of the file
     * @return file name prefixed with folder name
     */
    private String prefix(long resourcesId, String fileName) {
        return prefix(resourcesId) + fileName;
    }

    /**
     * The Amazon S3 data model is a flat structure: we create a bucket to store objects whose keys will be file names prefixed conceptional folder.
     * There is no hierarchy of subbuckets or subfolders; however, you can infer logical hierarchy using key name prefixes and delimiters as the Amazon S3 console does.
     * The Amazon S3 console supports a concept of folders, for more information, please refer to:
     * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @return file name prefixed with upper lever folder/directory
     */
    protected abstract String prefix(long resourcesId);

    /**
     * Remove the emulated prefix.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @param str file name
     * @return file name without prefixed folders
     */
    protected String removePrefix(long resourcesId, String str) {
        Pattern p = Pattern.compile("^" + prefix(resourcesId));
        return p.matcher(str).replaceAll("");
    }

    /**
     * Get file names without emulated folder prefix.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @return file names assembled in an array.
     */
    public String[] getFileNames(long resourcesId) {
        List<String> list = new ArrayList<>();
        List<S3ObjectSummary> summaries = getFileObjectSummaries(resourcesId);
        summaries.forEach((summary) -> {
            list.add(removePrefix(resourcesId, summary.getKey()));
        });
        return list.toArray(new String[0]);
    }

    /**
     * Get list of object summaries.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @return a list of S3ObjectSummary objects.
     */
    public List<S3ObjectSummary> getFileObjectSummaries(long resourcesId) {
        return getFileObjectSummaries(prefix(resourcesId));
    }

    /**
     * Get list of object summaries.
     * @param fullFileName full file name with prefixed folder/directory
     * @return a list of S3ObjectSummary objects.
     */
    public List<S3ObjectSummary> getFileObjectSummaries(String fullFileName) {
        List<S3ObjectSummary> list = new ArrayList<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(BUCKET).withPrefix(fullFileName);
        ObjectListing objectListing;
        do {
            objectListing = getS3Client().listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                list.add(objectSummary);
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());
        return list;
    }

    /**
     * Get file data with full file name.
     * @param fullFileName full file name with prefixed folder/directory
     * @return input stream of the file content.
     */
    public InputStream getFile(String fullFileName) {
        try {
            S3Object object = getS3Client().getObject(new GetObjectRequest(BUCKET, fullFileName));
            return (object != null ? object.getObjectContent() : null);
        } catch (AmazonS3Exception e) {
            // This exception is thrown when the key does not exist.
            return null;
        } catch (AmazonClientException e) {
            String message = "Error occurred while getting image from S3.";
            logger.severe(message);
            throw e;
        }
    }

    /**
     * Get file data with emulated folder prefix.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @param fileName file name
     * @return input stream of the file content.
     */
    public InputStream getFile(long resourcesId, String fileName) {
        return getFile(prefix(resourcesId, fileName));
    }

    /**
     * Get the public URL of the uploaded file on Amazon S3.
     * @param fullFileName full file name serving as the file key
     * @return public URL of the uploaded file
     */
    public String getFilePublicUrl(String fullFileName) {
        GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(BUCKET, fullFileName);
        Calendar expirationDate = Calendar.getInstance();
        //Requests that are pre-signed by AWS's SigV4 algorithm are valid for at most 7 days.
        expirationDate.add(Calendar.DAY_OF_MONTH, 6);  //In order to avoid exceeding this limit globally, set 6 days to allow 24 hours of time difference.
        urlRequest.setExpiration(expirationDate.getTime());
        URL url = this.getS3Client().generatePresignedUrl(urlRequest);
        if (url == null) {
            return null;
        }
        return url.toString();
    }
    
    /**
     * Check if file with full file name exists.
     * @param fullFileName file name with prefixed folder/directory
     * @return true if the file already exists on Amazon S3.
     */
    public Boolean exists(String fullFileName) {
        try {
            return getS3Client().doesObjectExist(BUCKET, fullFileName);
        } catch (AmazonS3Exception e) {
            String message = "Error occurred while executing doesObjectExist S3.";
            logger.severe(message);
            throw e;
        }
    }

    /**
     * Check if file exists with emulated folder prefix.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @param fileName file name
     * @return true if the file already exists on Amazon S3.
     */
    public Boolean exists(long resourcesId, String fileName) {
        return exists(prefix(resourcesId, fileName));
    }

    /**
     * Put file with full file name.
     * @param fullFileName full file name with prefixed folder/directory
     * @param mimeType mime type of the file
     * @param stream input stream of the file content.
     * @throws FileAlreadyExistsException when file already exists on Amazon S3.
     * @see to avoid No content length warning, https://stackoverflow.com/questions/36201759/how-to-set-inputstream-content-length
     */
    public void putFile(String fullFileName, String mimeType, InputStream stream) throws FileAlreadyExistsException {
        if (exists(fullFileName)) {
            throw new FileAlreadyExistsException(fullFileName);
        }

        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(mimeType);
            byte[] bytes = IOUtils.toByteArray(stream);
            meta.setContentLength(bytes.length);  //to avoid WARNING: No content length specified for stream data
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            getS3Client().putObject(new PutObjectRequest(BUCKET, fullFileName, byteArrayInputStream, meta));
        } catch (AmazonServiceException e) {
            String message = "Error occurred while uploading to S3.";
            logger.severe(message);
            throw e;
        } catch (IOException ex) {
            logger.warning("Failed to get the content length of file '" + fullFileName + "'.");
        }
    }

    /**
     * Put file with emulated folder prefix.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @param fileName file name
     * @param mimeType mime type of the file
     * @param stream input stream of the file content.
     * @throws FileAlreadyExistsException when file already exists on Amazon S3.
     */
    public void putFile(long resourcesId, String fileName, String mimeType, InputStream stream) throws FileAlreadyExistsException {
        putFile(prefix(resourcesId, fileName), mimeType, stream);
    }

    /**
     * Accept a base64 encoded string.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @param fileName file name
     * @param mimeType mime type of the file
     * @param base64Data file content.
     * @throws FileAlreadyExistsException when file already exists on Amazon S3.
     */
    public void putFile(long resourcesId, String fileName, String mimeType, String base64Data) throws FileAlreadyExistsException {
        // From https://stackoverflow.com/questions/30939904/uploading-base64-encoded-image-to-amazon-s3-using-java
        byte[] bts = org.apache.commons.codec.binary.Base64.decodeBase64((base64Data.substring(base64Data.indexOf(",") + 1)).getBytes());
        InputStream fis = new ByteArrayInputStream(bts);
        putFile(resourcesId, fileName, mimeType, fis);
    }


    /**
     * Remove file with full file name.
     * @param fullFileName full file name with prefixed folder/directory
     */
    public void deleteFile(String fullFileName) {
        try {
            getS3Client().deleteObject(BUCKET, fullFileName);
        } catch (AmazonServiceException e) {
            String message = "Error occurred while deleting the file from S3.";
            logger.severe(message);
            throw e;
        }
    }

    /**
     * Remove file with emulated folder prefix.
     * @param resourcesId used to construct prefixed folder, it could be venue ID, cobrand ID, organization ID, performer catalog ID, log ID, etc.
     * @param fileName file name
     */
    public void deleteFile(long resourcesId, String fileName) {
        deleteFile(prefix(resourcesId, fileName));
    }

}
