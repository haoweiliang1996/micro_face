// This sample uses the Apache HTTP client library(org.apache.httpcomponents:httpclient:4.2.4)
// and the org.json library (org.json:json:20170516).

import com.alibaba.fastjson.JSONObject;
import com.github.sarxos.webcam.Webcam;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static final String subscriptionKey = "3462757bd1bb44f2a84c8cd7f1688e1a";
    public static final String uriBase = "https://api.cognitive.azure.cn/face/v1.0/detect";
    public static final String personGroupId = "8b83ebf4-cfbb-4710-9998-14007cf577fd";

    public static void getFaceName(List<String> faceIds) {
        final String uriBase = "https://api.cognitive.azure.cn/face/v1.0/identify";
        try {
            URIBuilder builder = new URIBuilder(uriBase);

            Map<String, Object> map = new HashMap<>();
            map.put("faceIds", faceIds);
            map.put("personGroupId", personGroupId);

            System.out.println(JSONObject.toJSONString(map));
            URI uri = builder.build();
            HttpPost request = new HttpPost(uri);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
            StringEntity stringEntity = new StringEntity(JSONObject.toJSONString(map));
            request.setEntity(stringEntity);
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String jsonString = EntityUtils.toString(entity).trim();
                List<String> res = JSONObject.parseArray(jsonString, JSONObject.class).stream()
                        .map(jsonObject -> jsonObject.getJSONArray("candidates"))
                        .map(jsonArray -> jsonArray.getJSONObject(0))
                        .map(jsonObject -> jsonObject.getString("personId"))
                        .collect(Collectors.toList());
                res.parallelStream().map(Main::getNameById).forEach(System.out::println);
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

    }

    private static byte[] getImgFromWebCam() {
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(webcam.getImage(), "PNG", byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(res.length);
        return null;
    }

    public static String getNameById(String personId) {
        final String url = String.format("https://api.cognitive.azure.cn/face/v1.0/persongroups/%s/persons/%s", personGroupId, personId);
        try {
            URIBuilder builder = new URIBuilder(url);
            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
            HttpClient httpclient = HttpClients.createDefault();
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                return JSONObject.parseObject(EntityUtils.toString(entity).trim()).getString("name");
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        HttpClient httpclient = HttpClients.createDefault();
        for (int i = 0; i < 10; i++)
            try {
                long startTime = System.currentTimeMillis();   //获取开始时间
                URIBuilder builder = new URIBuilder(uriBase);
                builder.setParameter("returnFaceId", "true");
                builder.setParameter("returnFaceLandmarks", "false");
                builder.setParameter("returnFaceAttributes", "");

                // Prepare the URI for the REST API call.
                URI uri = builder.build();
                HttpPost request = new HttpPost(uri);

                // Request headers.
                request.setHeader("Content-Type", "application/octet-stream");
                //request.setHeader("Content-Type", "application/json");
                request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);

                File file;
                if (i % 2 == 0)
                    file = new File("dad.png");
                else
                    file = new File("family.png");
                byte[] picBytes = Files.readAllBytes(file.toPath());
                picBytes = getImgFromWebCam();

                // Request body.
                ByteArrayEntity byteArrayEntity = new ByteArrayEntity(picBytes);
                request.setEntity(byteArrayEntity);

                HttpResponse response = httpclient.execute(request);
                long endTime = System.currentTimeMillis(); //获取结束时间
                System.out.println("程序运行时间： " + (endTime - startTime) + "ms");
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    String jsonString = EntityUtils.toString(entity).trim();
                    System.out.println(jsonString);
                    List<String> res = JSONObject.parseArray(jsonString).stream()
                            .map(object -> (JSONObject) object)
                            .map(jsonObject -> jsonObject.getString("faceId"))
                            .collect(Collectors.toList());
                    getFaceName(res);
                    long endTime2 = System.currentTimeMillis(); //获取结束时间
                    System.out.println("程序运行时间： " + (endTime2 - endTime) + "ms");
                }
            } catch (Exception e) {
                System.out.println("error" + e.getMessage());
            }
    }
}
