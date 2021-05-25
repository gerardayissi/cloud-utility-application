
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

import org.apache.http.*;
import org.apache.http.client.HttpClient;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class SendRequest {
    private final String USER_AGENT = "Mozilla/5.0";
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public static void main(String[] args) {
        // write your code here
        String access_token;
        System.out.println("Sending HTTP Request to Manhattan Active Omni");
        SendRequest http = new SendRequest();

        // get access token first, then fulfillment next
        try {
            access_token = http.sendManhattanTokenPost();
            http.sendManhattanFulfillmentRequest(access_token);
            // build soap body using packageid (s) and call soap client
            http.callSoapService();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // HTTP POST token request
    private String sendManhattanTokenPost() throws Exception {
        //test
        /*
        String part1 = "https://tssls-auth.omni.manh.com/oauth/token?grant_type=password&username=admin@";
        String part2 = ".com&password=Password1!";
        String brand = "tmw";
        String user= "omnicomponent.1.0.0";
        String pwd = "b4s8rgTyg55XYNun";
        */
        //production
        /*String part1 = "https://tsslp-auth.omni.manh.com/oauth/token?grant_type=password&username=eisuser@";
        String part2 = ".com&password=EISmao20!9";
        String brand = "tmw";
        String user= "omnicomponent.1.0.0";
        String pwd = "b4s8rgTyg55XYNun";*/

        String part1 = "https://tssls4-auth.omni.manh.com/oauth/token?grant_type=password&username=eisuser";
        String part2 = "&password=Password1!";
        String brand = "tmw";
        String user= "omnicomponent.1.0.0";
        String pwd = "b4s8rgTyg55XYNun";

        String url = part1 + part2;

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);

        String encoding = Base64.getEncoder().encodeToString((user+":"+pwd).getBytes());
        post.setHeader("Authorization", "Basic " + encoding);
        //post.setHeader("Authorization", "Basic " + encoding);
        post.setHeader("content-type", "text/xml;charset=UTF-8");

        HttpResponse response = client.execute(post);
        System.out.println("sending 'POST' request to URL : " + url);
        //System.out.println("Post parameters : " + post.getEntity());
        System.out.println("token response code : " +
                response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        System.out.println("result: "+ result.toString());
        Object obj = new JSONParser().parse(result.toString());
        JSONObject jo = (JSONObject) obj;
        String access_token = (String) jo.get("access_token");
        System.out.println("access token: "+ access_token);

        return access_token;
    }

    // HTTP GET fulfillment request
    private void sendManhattanFulfillmentRequest(String access_token) throws Exception {
        // test
        /*
        String part1 = "https://tssls.omni.manh.com/fulfillment/api/fulfillment/fulfillment/fulfillmentId/";
        String part2 = "?templateName=TBIFulfillmentSearchForTalend";
        String asn_id= "PKG001425916";
        String extension = "&throwExceptionOnFailure=false";
        */
        //production
        String part1 = "https://tssls4.omni.manh.com/fulfillment/api/fulfillment/fulfillment/fulfillmentId/";
        String part2 = "?templateName=TBIFulfillmentSearchForTalend";
        String asn_id= "25434516";
        //String asn_id= "16407294";
        String extension = "&throwExceptionOnFailure=false";

        String url = part1 + asn_id + part2 + extension;

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);

        request.setHeader("Authorization", "Bearer " + access_token);

        HttpResponse response = client.execute(request);

        System.out.println("sending 'GET' request to URL : " + url);
        System.out.println("fulfillment response code : " +
                response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        System.out.println("fulfillment response: "+result.toString());
    }

    public void callSoapService()  throws IOException {
        // Create a StringEntity for the SOAP XML.
        String url = "http://dom12tstapp01.tmw.com:18000/ws/services/CustomerOrderWebService.CustomerOrderWebServiceHttpSoap12Endpoint?throwExceptionOnFailure=false";
        //String url = "http://dom12app.tmw.com:18000/ws/services/CustomerOrderWebService.CustomerOrderWebServiceHttpSoap12Endpoint?throwExceptionOnFailure=false";
        String soapAction = "urn:autoLPNRecieving";
        //String soapEnvBody ="<?xml version='1.0' encoding='utf-8'?> <soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cus=\"http://customerordermanager.sellingservice.services.scope.manh.com\"> <soap:Header/> <soapenv:Body> <cus:autoLPNRecieving> <cus:param0> <![CDATA[<tXML><Header><Source>ECOM</Source><Action_Type>Update</Action_Type><Message_Type>LPN_RECEIVING</Message_Type><Company_ID>1</Company_ID><Msg_Locale>English (United States)</Msg_Locale></Header><Message><FacilityAliasId>2668</FacilityAliasId><LPNList>P605321650734</LPNList></Message></tXML>]]> </cus:param0> </cus:autoLPNRecieving> </soapenv:Body> </soapenv:Envelope>";
        //String soapEnvBody ="<?xml version='1.0' encoding='utf-8'?> <soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cus=\"http://customerordermanager.sellingservice.services.scope.manh.com\"> <soap:Header/> <soapenv:Body> <cus:autoLPNRecieving> <cus:param0> <![CDATA[<tXML><Header><Source>ECOM</Source><Action_Type>Update</Action_Type><Message_Type>LPN_RECEIVING</Message_Type><Company_ID>1</Company_ID><Msg_Locale>English (United States)</Msg_Locale></Header><Message><FacilityAliasId>2668</FacilityAliasId><LPNList>P6053216507346</LPNList></Message></tXML>]]> </cus:param0> </cus:autoLPNRecieving> </soapenv:Body> </soapenv:Envelope>";
        String soapEnvBody = "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cus=\"http://customerordermanager.sellingservice.services.scope.manh.com\"><soapenv:Header/><soapenv:Body><cus:autoLPNRecieving><cus:param0><![CDATA[<TXMLType><Header><Source>ECOM</Source><Action_Type>Update</Action_Type><Message_Type>LPN_RECEIVING</Message_Type><Company_ID>1</Company_ID><Msg_Locale>English (United States)</Msg_Locale></Header><Message><FacilityAliasId>2668</FacilityAliasId><LPNList>P6053216507346</LPNList></Message></TXMLType>]]> </cus:param0></cus:autoLPNRecieving></soapenv:Body></soapenv:Envelope>";
        StringEntity stringEntity = new StringEntity(soapEnvBody, "UTF-8");
        stringEntity.setChunked(true);

        // Request parameters and other properties.
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(stringEntity);

        httpPost.addHeader("SOAPAction", soapAction);

        // Execute and get the response.
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();

        System.out.println("sending 'POST' request to URL : " + url);
        System.out.println("soap response code : " +
                response.getStatusLine().getStatusCode());

        String strResponse = null;
        if (entity != null) {
            strResponse = EntityUtils.toString(entity);
        }
        System.out.println("SOAP Response: "+strResponse);
    }

}
