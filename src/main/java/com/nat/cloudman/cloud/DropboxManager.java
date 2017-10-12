package com.nat.cloudman.cloud;

import com.dropbox.core.*;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.users.FullAccount;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class DropboxManager implements CloudManager {

    @Autowired
    private UserManager userManager;

    @Value("${dropbox.app.key}")
    private String APP_KEY;

    @Value("${dropbox.app.secret}")
    private String APP_SECRET;

    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    // Adjust the chunk size based on your network speed and reliability. Larger chunk sizes will
    // result in fewer network requests, which will be faster. But if an error occurs, the entire
    // chunk will be lost and have to be re-uploaded. Use a multiple of 4MiB for your chunk size.
    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 8L << 20; // 8MiB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

    @Override
    public String getServiceName() {
        return "Dropbox";
    }

    public DbxClientV2 getClient(String token) {
        DbxRequestConfig config = new DbxRequestConfig("com/nat/cloudman/cloud/CloudMan/app");
        DbxClientV2 client = new DbxClientV2(config, token);
        return client;
    }

    public void showUserInformation(String token) {
        FullAccount account = null;
        System.out.println("showUserInformation, token: " + token);
        DbxClientV2 client = getClient(token);
        if (client == null) {
            System.out.println("client == null");
        }
        try {

            account = client.users().getCurrentAccount();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        System.out.println("dropbox account.getName: " + account.getName().getDisplayName());
    }

    @Override
    public FilesContainer getFilesList(String accountName, String folderPath) {
        String token = userManager.getCloud(accountName).getAccessToken();
        System.out.println("token: " + token);
        DbxClientV2 client = getClient(token);
        //Get files and folder metadata from Dropbox root directory
        ListFolderResult result = null;
        try {
            result = client.files().listFolder(folderPath);
        } catch (DbxException e) {
            e.printStackTrace();
        }

        ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();
        while (true) {
            try {
                for (Metadata metadata : result.getEntries()) {
                    HashMap<String, String> file = new HashMap<String, String>();
                    file.put("displayPath", metadata.getPathDisplay());
                    if (metadata instanceof FolderMetadata) {
                        System.out.println(" FolderMetadata ");
                        metadata = (FolderMetadata) metadata;
                        file.put("type", "folder");
                        String id = ((FolderMetadata) metadata).getId();
                        if (id.startsWith("id:")) {
                            id = id.substring(3, id.length());
                        }
                        System.out.println(" path: " + ((FolderMetadata) metadata).getPathLower());
                        file.put("id", id);
                        file.put("pathLower", ((FolderMetadata) metadata).getPathLower());
                    } else if (metadata instanceof FileMetadata) {
                        System.out.println(" FileMetadata ");
                        metadata = (FileMetadata) metadata;
                        file.put("type", "file");
                        String id = ((FileMetadata) metadata).getId();
                        if (id.startsWith("id:")) {
                            id = id.substring(3, id.length());
                        }
                        System.out.println(" path: " + ((FileMetadata) metadata).getPathLower());
                        file.put("id", id);
                        file.put("modified", DateFormat.getDateInstance().format(((FileMetadata) metadata).getClientModified()));
                        file.put("size", Long.toString(((FileMetadata) metadata).getSize()));
                        file.put("pathLower", ((FileMetadata) metadata).getPathLower());


                    }
                    files.add(file);
                }

            } catch (JSONException jse) {
                System.out.println("JSONException: " + jse.getMessage());
            }
            if (!result.getHasMore()) {
                break;
            }
        }
        System.out.println("size: " + files.size());
        return new FilesContainer(files);
    }

    public File multipartToFile(MultipartFile multipart, String pathToSave) throws IllegalStateException, IOException {
        File convertedFile = new File(pathToSave + multipart.getOriginalFilename());
        multipart.transferTo(convertedFile);
        System.out.println("converted, exists " + convertedFile.exists());
        System.out.println("converted, getPath " + convertedFile.getPath());
        System.out.println("converted, getName " + convertedFile.getName());
        System.out.println("converted, length " + convertedFile.length());
        return convertedFile;
    }

    @Override
    public void uploadFile(Cloud cloud, File localFile, String dropboxPath) {
        System.err.println("uploadFile");
        System.err.println("dropboxPath: " + dropboxPath);
        System.err.println("localFile getName: " + localFile.getName());
        System.err.println("localFile getPath: " + localFile.getPath());
        DbxClientV2 client = getClient(cloud.getAccessToken());
        if (localFile.length() <= (2 * CHUNKED_UPLOAD_CHUNK_SIZE)) {
            uploadSmallFile(client, localFile, dropboxPath);
        } else {

            chunkedUploadFile(client, localFile, dropboxPath);
        }
    }

    /**
     * Uploads a file in a single request. This approach is preferred for small files since it
     * eliminates unnecessary round-trips to the servers.
     *
     * @param dbxClient   Dropbox user authenticated client
     * @param localFile   local file to upload
     * @param dropboxPath Where to upload the file to within Dropbox
     */
    private void uploadSmallFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) {
        try {
            InputStream in = new FileInputStream(localFile);
            FileMetadata metadata = dbxClient.files().uploadBuilder(dropboxPath)
                    .withMode(WriteMode.ADD)
                    .withClientModified(new Date(localFile.lastModified()))
                    .uploadAndFinish(in);
            System.out.println("File is uploaded, metadata: " + metadata.toStringMultiline());
        } catch (UploadErrorException ex) {
            System.err.println("UploadErrorException Error uploading to Dropbox: " + ex.getMessage());
            System.exit(1);
        } catch (DbxException ex) {
            System.err.println("DbxException Error uploading to Dropbox: " + ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("IOException Error reading from file \"" + localFile + "\": " + ex.getMessage());
            System.exit(1);
        }
    }


    /**
     * Uploads a file in chunks using multiple requests. This approach is preferred for larger files
     * since it allows for more efficient processing of the file contents on the server side and
     * also allows partial uploads to be retried (e.g. network connection problem will not cause you
     * to re-upload all the bytes).
     *
     * @param dbxClient   Dropbox user authenticated client
     *                    //     * @param localFIle local file to upload
     * @param dropboxPath Where to upload the file to within Dropbox
     */
    private void chunkedUploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) {
        long size = localFile.length();

        // assert our file is at least the chunk upload size. We make this assumption in the code
        // below to simplify the logic.
        if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
            System.err.println("File too small, use uploadSmallFile() instead.");
            System.exit(1);
            return;
        }
        long uploaded = 0L;
        DbxException thrown = null;

        // Chunked uploads have 3 phases, each of which can accept uploaded bytes:
        //
        //    (1)  Start: initiate the upload and get an upload session ID
        //    (2) Append: upload chunks of the file to append to our session
        //    (3) Finish: commit the upload and close the session
        //
        // We track how many bytes we uploaded to determine which phase we should be in.
        String sessionId = null;
        for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
            if (i > 0) {
                System.out.printf("Retrying chunked upload (%d / %d attempts)\n", i + 1, CHUNKED_UPLOAD_MAX_ATTEMPTS);
            }

            try (InputStream in = new FileInputStream(localFile)) {
                // if this is a retry, make sure seek to the correct offset
                in.skip(uploaded);

                // (1) Start
                if (sessionId == null) {
                    sessionId = dbxClient.files().uploadSessionStart()
                            .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE)
                            .getSessionId();
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    printProgress(uploaded, size);
                }

                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

                // (2) Append
                while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
                    dbxClient.files().uploadSessionAppendV2(cursor)
                            .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    printProgress(uploaded, size);
                    cursor = new UploadSessionCursor(sessionId, uploaded);
                }

                // (3) Finish
                long remaining = size - uploaded;
                CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath)
                        .withMode(WriteMode.ADD)
                        .withClientModified(new Date(localFile.lastModified()))
                        .build();
                FileMetadata metadata = dbxClient.files().uploadSessionFinish(cursor, commitInfo)
                        .uploadAndFinish(in, remaining);

                System.out.println(metadata.toStringMultiline());
                return;
            } catch (RetryException ex) {
                thrown = ex;
                // RetryExceptions are never automatically retried by the client for uploads. Must
                // catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
                sleepQuietly(ex.getBackoffMillis());
                continue;
            } catch (NetworkIOException ex) {
                thrown = ex;
                // network issue with Dropbox (maybe a timeout?) try again
                continue;
            } catch (UploadSessionLookupErrorException ex) {
                if (ex.errorValue.isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                            .getIncorrectOffsetValue()
                            .getCorrectOffset();
                    continue;
                } else {
                    // Some other error occurred, give up.
                    System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                    System.exit(1);
                    return;
                }
            } catch (UploadSessionFinishErrorException ex) {
                if (ex.errorValue.isLookupFailed() && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                            .getLookupFailedValue()
                            .getIncorrectOffsetValue()
                            .getCorrectOffset();
                    continue;
                } else {
                    // some other error occurred, give up.
                    System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                    System.exit(1);
                    return;
                }
            } catch (DbxException ex) {
                System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                System.exit(1);
                return;
            } catch (IOException ex) {
                System.err.println("Error reading from file \"" + localFile + "\": " + ex.getMessage());
                System.exit(1);
                return;
            }
        }

        // if we made it here, then we must have run out of attempts
        System.err.println("Maxed out upload attempts to Dropbox. Most recent error: " + thrown.getMessage());
        System.exit(1);
    }

    private static void printProgress(long uploaded, long size) {
        System.out.printf("Uploaded %12d / %12d bytes (%5.2f%%)\n", uploaded, size, 100 * (uploaded / (double) size));
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // just exit
            System.err.println("Error uploading to Dropbox: interrupted during backoff.");
            System.exit(1);
        }
    }

    public String getAuthorizeUrl() {

        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
        // Read app info file (contains app key and app secret)

        // Run through Dropbox API authorization process
        DbxRequestConfig requestConfig = new DbxRequestConfig("examples-authorize");
        DbxWebAuth webAuth = new DbxWebAuth(requestConfig, appInfo);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .build();

        String authorizeUrl = webAuth.authorize(webAuthRequest);
        System.out.println("authorizeUrl: " + authorizeUrl);
        return authorizeUrl;
    }

    public void authorise() throws IOException {
        Logger.getLogger("").setLevel(Level.WARNING);
        String argAppInfoFile = "E:\\Dropbox\\Projects\\CloudMan\\src\\main\\resources\\drKS.txt";
        String argAuthFileOutput = "E:\\Dropbox\\Projects\\CloudMan\\src\\main\\resources\\out.txt";

        // Read app info file (contains app key and app secret)
        DbxAppInfo appInfo;
        try {
            appInfo = DbxAppInfo.Reader.readFromFile(argAppInfoFile);
        } catch (JsonReader.FileLoadException ex) {
            System.err.println("Error reading <app-info-file>: " + ex.getMessage());
            System.exit(1);
            return;
        }
        // Run through Dropbox API authorization process
        DbxRequestConfig requestConfig = new DbxRequestConfig("examples-authorize");
        DbxWebAuth webAuth = new DbxWebAuth(requestConfig, appInfo);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .build();

        String authorizeUrl = webAuth.authorize(webAuthRequest);
        System.out.println("1. Go to " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first).");
        System.out.println("3. Copy the authorization code.");
        System.out.print("Enter the authorization code here: ");

        String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (code == null) {
            System.exit(1);
            return;
        }
        code = code.trim();

        DbxAuthFinish authFinish;
        try {
            authFinish = webAuth.finishFromCode(code);
        } catch (DbxException ex) {
            System.err.println("Error in DbxWebAuth.authorize: " + ex.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Authorization complete.");
        System.out.println("- User ID: " + authFinish.getUserId());
        System.out.println("- Access Token: " + authFinish.getAccessToken());

        // Save auth information to output file.
        DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.getAccessToken(), appInfo.getHost());
        File output = new File(argAuthFileOutput);
        try {
            DbxAuthInfo.Writer.writeToFile(authInfo, output);
            System.out.println("Saved authorization information to \"" + output.getCanonicalPath() + "\".");
        } catch (IOException ex) {
            System.err.println("Error saving to <auth-file-out>: " + ex.getMessage());
            System.err.println("Dumping to stderr instead:");
            DbxAuthInfo.Writer.writeToStream(authInfo, System.err);
            System.exit(1);
            return;
        }
    }


    @Override
    public void addFolder(String folderName, Cloud cloud, String path, String parentId) {
        String token = cloud.getAccessToken();
        System.out.println("token: " + token);
        DbxClientV2 client = getClient(token);
        try {
            client.files().createFolder(path + "/" + folderName);
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public File downloadLocal(String fileName, String path, String downloadUrl, Cloud cloud) {
        String token = cloud.getAccessToken();
        DbxClientV2 client = getClient(token);
        File file = new File(DOWNLOAD_PATH + System.currentTimeMillis() + fileName);
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            client.files().download(path + "/" + fileName).download(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (DbxException | IOException e) {
            e.printStackTrace();
        }
        return file;
    }


    @Override
    public DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud) {
        File file = downloadLocal(fileName, path, null, cloud);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("downloaded ");
        try {
            byte[] arr = IOUtils.toByteArray(is);
            is.close();
            if (file.delete()) {
                System.out.println(file.getName() + " is deleted");
            } else {
                System.out.println("Delete operation is failed");
            }
            System.out.println("arr size: " + arr.length);
            return new DownloadedFileContainer(fileName, arr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void deleteFile(String fileName, String fileId, String path, Cloud cloud) {
        String token = cloud.getAccessToken();
        DbxClientV2 client = getClient(token);
        try {
            client.files().delete(path + "/" + fileName);
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void renameFile(String fileName, String fileId, String newName, String path, Cloud cloud) {
        String token = cloud.getAccessToken();
        DbxClientV2 client = getClient(token);
        try {
            client.files().move(path + "/" + fileName, path + "/" + newName);
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud) {
        String token = cloud.getAccessToken();
        DbxClientV2 client = getClient(token);
        try {
            client.files().copy(pathSourse, pathDest);
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }
}
