/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Upload a video to the authenticated user's channel. Use OAuth 2.0 to
 * authorize the request. Note that you must add your video files to the
 * project folder to upload them with this application.
 *
 * @author Jeremy Walker
 */
public class YoutubeApi {

    /**
     * Define a global instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /**
     * Define a global instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /**
     * This is the directory that will be used under the user's home directory where OAuth tokens will be stored.
     */
    private static final String CREDENTIALS_DIRECTORY = "oauth-credentials";

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;

    /**
     * Define a global variable that specifies the MIME type of the video
     * being uploaded.
     */
    private static final String VIDEO_FILE_FORMAT = "video/*";

    private static final String SAMPLE_VIDEO_FILENAME = "sample-video.mp4";

    /**
     * Upload the user-selected video to the user's YouTube channel. The code
     * looks for the video in the application's project folder and uses OAuth
     * 2.0 to authorize the API request.
     *
     * @param args command line args (not used).
     */
    public static void main(String[] args) throws IOException {

        // This OAuth 2.0 access scope allows an application to upload files
        // to the authenticated user's YouTube channel, but doesn't allow
        // other types of access.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube","https://www.googleapis.com/auth/youtube.force-ssl");


try{
        String credentialDatastore = "uploadvideo";

        // Load client secrets.
        File file = new File("/home/priam/projects/youtubeapi/src/main/resources/client_secrets.json");
        Reader clientSecretReader = new InputStreamReader(new FileInputStream(file));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);

//         Checks that the defaults have been replaced (Default = "Enter X here").
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://console.developers.google.com/project/_/apiui/credential "
                            + "into src/main/resources/client_secrets.json");
            System.exit(1);
        }

        // This creates the credentials datastore at ~/.oauth-credentials/${credentialDatastore}
        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
        DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore(credentialDatastore);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes).setCredentialDataStore(datastore)
                .build();

        // Build the local server and bind it to port 8080
        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8081).build();

        // Authorize.
        Credential credential = new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");





            // This object is used to make YouTube Data API requests.
            youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(
                    "youtube-cmdline-uploadvideo-sample").build();

            System.out.println("Uploading: " + SAMPLE_VIDEO_FILENAME);

            YouTube.Playlists items = youtube.playlists();


            System.out.println(items);

    YouTube.Channels.List channelRequest = youtube.channels().list("contentDetails");
    channelRequest.setMine(true);
    channelRequest.setFields("items/contentDetails,nextPageToken,pageInfo");
    ChannelListResponse channelResult = channelRequest.execute();



    List<Channel> channelsList = channelResult.getItems();

        // The user's default channel is the first item in the list.
        // Extract the playlist ID for the channel's videos from the
        // API response.
        String uploadPlaylistId =
                channelsList.get(0).getContentDetails().getRelatedPlaylists().getLikes();

        // Define a list to store items in the list of uploaded videos.
        List<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();

        // Retrieve the playlist of the channel's uploaded videos.
        YouTube.PlaylistItems.List playlistItemRequest =
                youtube.playlistItems().list("snippet");/*id,contentDetails,*/
        playlistItemRequest.setPlaylistId(uploadPlaylistId);

        // Only retrieve data used in this application, thereby making
        // the application more efficient. See:
        // https://developers.google.com/youtube/v3/getting-started#partial
        ////playlistItemRequest.setFields(
        ////        "items(contentDetails/videoId,snippet/title,snippet/publishedAt),nextPageToken,pageInfo");

        String nextToken = "";

        // Call the API one or more times to retrieve all items in the
        // list. As long as the API response returns a nextPageToken,
        // there are still more items to retrieve.
        do {
            playlistItemRequest.setPageToken(nextToken);
            PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();

            playlistItemList.addAll(playlistItemResult.getItems());
            nextToken = playlistItemResult.getNextPageToken();
        } while (nextToken != null);


    System.out.println(playlistItemList);

    StringBuilder videoIds = new StringBuilder();
    for(PlaylistItem i :playlistItemList) {
       videoIds.append(i.getSnippet().getResourceId().getVideoId()).append(",");
    }
    if (videoIds.length() > 0)
    {
        videoIds.deleteCharAt(videoIds.length()-1);
    }


    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("part", "snippet,contentDetails,statistics,topicDetails");
    parameters.put("id", videoIds.toString());

    YouTube.Videos.List videosListByIdRequest = youtube.videos().list(parameters.get("part").toString());

    videosListByIdRequest.setId(parameters.get("id").toString());


    VideoListResponse videos = videosListByIdRequest.execute();

    for (Video video : videos.getItems())
    {
        List<String> tags = video.getSnippet().getTags();
        if (tags != null) {
            System.out.println(">>> >>> >>> >>> >>> >>> >>>");
            tags.forEach(e -> {
                if (StringUtils.isNotBlank(e)) System.out.println(e);
            });
        }
    }

        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
