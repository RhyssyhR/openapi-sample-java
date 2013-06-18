import java.io.File;
import java.io.IOException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import com.workshare.sample.client.ApiClient;

public class Main {

    public static void main(String[] args) throws IOException {
        
        if (args.length != 3) {
            System.out.println("Please specify three parameters:");
            System.out.println("- applicationid");
            System.out.println("- username");
            System.out.println("- password");
            System.exit(-1);
        }

        String appid = args[0];
        String user = args[1];
        String pass = args[2];

        run(appid, user, pass);

        System.exit(0);
    }

    private static void run(String appid, String user, String pass) throws IOException {
        ApiClient client = new ApiClient(appid);
        client.login(user, pass);
        System.out.println("Login successful!\n");
        try {
            JSONArray folders = client.getFolders();
            System.out.println("Folders:\n" + folders+"\n");

            JSONArray files = client.getFiles();
            System.out.println("Files:\n" + files);

            if (files.size() > 0) {
                JSONObject file = (JSONObject) files.get(0);
                File result = client.download(file);
                System.out.println("First file found downloaded to: \n"+result+"\n");
            }
        } finally {
            client.logout();
            System.out.println("Logout successful!\n");
        }
    }

}
