package com.nat.cloudman.dropbox;

import java.io.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import com.dropbox.core.v2.files.FolderMetadata;
import com.nat.cloudman.model.User;
import com.nat.cloudman.service.UserService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;


import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.files.WriteMode;


import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishErrorException;
import com.dropbox.core.v2.files.UploadSessionLookupErrorException;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

//access from any domain
//@CrossOrigin(origins = "*")
@RestController
public class DropboxController {

    private DbxClientV2 client;

    @Autowired
    private UserService userService;

    public DbxClientV2 getClient() {
        if (client == null) {
            DbxAuthInfo authInfo;
            try {
                authInfo = DbxAuthInfo.Reader.readFromFile("E:\\Dropbox\\Projects\\CloudMan\\src\\main\\resources\\out.txt");
            } catch (JsonReader.FileLoadException ex) {
                System.err.println("Error loading <auth-file>: " + ex.getMessage());
                System.exit(1);
                return null;
            }

            // Create Dropbox client
            DbxRequestConfig config = new DbxRequestConfig("com/nat/cloudman/dropbox/CloudMan/app");
            client = new DbxClientV2(config, authInfo.getAccessToken());
            return client;
        } else {
            return client;
        }
    }


    // Adjust the chunk size based on your network speed and reliability. Larger chunk sizes will
    // result in fewer network requests, which will be faster. But if an error occurs, the entire
    // chunk will be lost and have to be re-uploaded. Use a multiple of 4MiB for your chunk size.
    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 8L << 20; // 8MiB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

    //@CrossOrigin(origins = "*")
    @RequestMapping(value = "/dropbox", method = RequestMethod.POST)
    public DropboxManager listFiles(@RequestParam(value = "path", defaultValue = "") String path, HttpServletRequest request, HttpServletResponse response) {
        System.out.println("got path: " + path);

        System.out.println("headers: ");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            System.out.println(key + ": " + value);
        }

        showAuth("dropbox");
        addCorsHeader(response);
        return new DropboxManager(getFilesList(path));
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public DropboxManager logout(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("logout ");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        showAuth("logout");
        addCorsHeader(response);
        return null;
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    @ResponseBody
    public String signUp(
            @RequestParam("email") String email,
            @RequestParam("firstname") String firstName,
            @RequestParam("lastname") String lastName,
            @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response

    ) {
        System.out.println("params. email: " + email + ", firstName: " + firstName + ", lastName: " + lastName + ", password: " + password);
        String result = "";
        User userExists = userService.findUserByEmail(email);
        if (userExists != null) {
            result = "User already exists";
        } else {
            User user = new User();
            user.setEmail(email);
            user.setName(firstName);
            user.setLastName(lastName);
            user.setPassword(password);
            userService.saveUser(user);
            result = "User was saved";
        }
        System.out.println("return: " + result);
        System.out.println("from request: " + request.getParameter("email") + " " +
                request.getParameter("password"));

        addCorsHeader(response);
        showAuth("signup");
        return result;
    }

    @RequestMapping(value = "/loginform", method = RequestMethod.POST)
    @ResponseBody
    public String login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response

    ) {
        System.out.println("params. email: " + email + ", password: " + password);
        User userExists = userService.findUserByEmail(email);
        String result = "";
        if (userExists == null) {
            result = "User doesn't exist";
        } else {
            result = (userExists.getPassword().equals(password)) ? "login success" : "wrong password";
        }
        System.out.println("return for login: " + result);
        showAuth("login");
        addCorsHeader(response);
        return result;
    }


    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public String handleFileUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("dropboxPath") String dropboxPath,
            HttpServletRequest request, HttpServletResponse response
    ) {
        System.out.println("dropboxPath: " + dropboxPath);
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    System.out.println("file getOriginalFilename: " + file.getOriginalFilename());
                    System.out.println("file getContentType: " + file.getContentType());
                    System.out.println("file getName: " + file.getName());
                    System.out.println("file getSize: " + file.getSize());
                    File convertedFile = multipartToFile(file, "E:\\pics\\uploaded\\");
                    System.out.println("convertedFile: " + convertedFile.exists() + " " + convertedFile.isFile() + " " + convertedFile.getName() + " " + convertedFile.getPath() + " " + convertedFile.getCanonicalPath());
                    uploadFile(convertedFile, dropboxPath + "/" + convertedFile.getName());
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                }
            } else {
                System.out.println("file is empty ");
            }
        }
        addCorsHeader(response);
        return null;
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


    public void uploadFile(File localFile, String dropboxPath) throws Exception {
        System.err.println("uploadFile");
        System.err.println("dropboxPath: " + dropboxPath);
        System.err.println("localFile getName: " + localFile.getName());
        System.err.println("localFile getPath: " + localFile.getPath());
        if (localFile.length() <= (2 * CHUNKED_UPLOAD_CHUNK_SIZE)) {
            uploadSmallFile(getClient(), localFile, dropboxPath);
        } else {
            chunkedUploadFile(getClient(), localFile, dropboxPath);
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
    private void uploadSmallFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) throws Exception {
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
    private void chunkedUploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) throws Exception {
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


    public ArrayList<HashMap<String, String>> getFilesList(String folderPath) {

        // Get current account info
        FullAccount account = null;
        try {
            account = getClient().users().getCurrentAccount();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        System.out.println("account.getName: " + account.getName().getDisplayName());

        // Get files and folder metadata from Dropbox root directory
        ListFolderResult result = null;
        try {
            result = getClient().files().listFolder(folderPath);
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
        System.out.println("file: " + files.get(0).get("displayPath"));
        return files;
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

    private void showAuth(String path) {
        System.out.println("auth in path: " + path);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            System.out.println("name from auth: " + auth.getName() + " " + auth.isAuthenticated());
            User user = userService.findUserByEmail(auth.getName());
            if (user != null) {
                System.out.println("User name: " + user.getName());
                System.out.println("User email: " + user.getEmail());
                System.out.println("User id: " + user.getId());
            } else {
                System.out.println("User is null ");
            }
        } else {
            System.out.println("Auth is null");
        }

    }

    private void addCorsHeader(HttpServletResponse response) {

        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type,X-XSRF-TOKEN");
        response.addHeader("Access-Control-Max-Age", "1");
    }

}
