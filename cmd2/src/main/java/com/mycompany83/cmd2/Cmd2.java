package com.mycompany83.cmd2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;

/**
 * Sample code to 
 * - read ElasticSearch, 
 * - retrieve web page content, 
 * - do google search and parse out its results
 * @author Adam Palmer, amp8293@gmail.com, 703-834-1265
 */
public class Cmd2 {
    // https://aboullaite.me/jsoup-html-parser-tutorial-examples/
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//        System.out.println("Hello 2!");
//    }
    //We need a real browser user agent or Google will block our request with a 403 - Forbidden
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36";

    public static void main(String[] args) throws IOException {
        //main_fetch_web_page(args);
        //main_google_search(args);
        //main_elastic_search_low_level(args);
        main_elastic_search_high_level(args);
    }
    
    /**
     * Get list of "searchtask" items from ElasticSearch (using the ElasticSearch high level Java REST APIs)
     * 
     * @param args
     * @throws IOException 
     */
    private static void main_elastic_search_high_level(String[] args) throws IOException {
        // also see: https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-getting-started.html
        org.elasticsearch.client.RestHighLevelClient esRestClient = new org.elasticsearch.client.RestHighLevelClient(
        org.elasticsearch.client.RestClient.builder(
                new HttpHost("localhost", 9200, "http")));

        // see: https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-search.html
        org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search.SearchRequest("searchtask");
        org.elasticsearch.search.builder.SearchSourceBuilder searchSourceBuilder = new org.elasticsearch.search.builder.SearchSourceBuilder();
        searchSourceBuilder.query(org.elasticsearch.index.query.QueryBuilders.matchAllQuery());
        // other options such as: searchSourceBuilder.from(0); searchSourceBuilder.size(5); 
        searchRequest.source(searchSourceBuilder);

        //searchSourceBuilder.sort(new org.elasticsearch.search.sort.ScoreSortBuilder().order(org.elasticsearch.search.sort.SortOrder.DESC)); // descending by score
        searchSourceBuilder.sort(new org.elasticsearch.search.sort.FieldSortBuilder("_id").order(org.elasticsearch.search.sort.SortOrder.ASC));  

        org.elasticsearch.action.search.SearchResponse searchResponse = esRestClient.search(searchRequest, org.elasticsearch.client.RequestOptions.DEFAULT);
      
        //SearchHits provides global information about all hits, like total number of hits or the maximum score:
        org.elasticsearch.search.SearchHits hits = searchResponse.getHits();

        //long totalHits = hits.getTotalHits();
        //float maxScore = hits.getMaxScore();

        org.elasticsearch.search.SearchHit[] searchHits = hits.getHits();
        for (org.elasticsearch.search.SearchHit hit : searchHits) {
            System.out.println("Doc ID=" + hit.getId() + " " + hit.toString());
            Map<String, Object> mapSrc = hit.getSourceAsMap();
            String strTaskName = (String) mapSrc.get("taskName");
            List<String> keywords = (List<String>)mapSrc.get("keywords");
            System.out.println("   TaskName=" + strTaskName + " Keywords=" + keywords.toString());
            
            // executeSearchTask(strTaskName, keywords); // can do this at this point!   
        }

//        The SearchHit provides access to basic information like index, type, docId and score of each search hit:
//
//        String index = hit.getIndex();
//        String type = hit.getType();
//        String id = hit.getId();
//        float score = hit.getScore();
//
//        Furthermore, it lets you get back the document source, either as a simple JSON-String or as a map of key/value pairs. In this map, regular fields are keyed by the field name and contain the field value. Multi-valued fields are returned as lists of objects, nested objects as another key/value map. These cases need to be cast accordingly:
//
//        String sourceAsString = hit.getSourceAsString();
//        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
//        String documentTitle = (String) sourceAsMap.get("title");
//        List<Object> users = (List<Object>) sourceAsMap.get("user");
//        Map<String, Object> innerObject =
//                (Map<String, Object>) sourceAsMap.get("innerObject");


        esRestClient.close();
    }
    
    /**
     * Search for "searchtask" items using ElasticSearch's low level JAVA API
     * 
     * (Won't be using this because it requires too much work to get to the records)
     * @param args
     * @throws IOException 
     */
    private static void main_elastic_search_low_level(String[] args) throws IOException {
        // also see: https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-low-usage-initialization.html
        org.elasticsearch.client.RestClient esRestClient = org.elasticsearch.client.RestClient.builder(
                new HttpHost("localhost", 9200, "http")).build();
        org.elasticsearch.client.Request request = new org.elasticsearch.client.Request(
                "GET",
                "/searchtask/_search?q=*&sort=_id:asc");
        org.elasticsearch.client.Response response = esRestClient.performRequest(request);
        System.out.println("ElasticSearch Result: " + response.toString());
        String responseBody = EntityUtils.toString(response.getEntity()); 
        System.out.println(responseBody);
        esRestClient.close();
    }

    /**
     * Do google search and parse out the results.
     * 
     * Current version searches for "water" and prints out the results.  
     * This can easily be changed to:  List<SearchResult> searchGoogle(String searchString)
     * where instead of printing out the results, we gather them into a list
     * 
     * @param args
     * @throws IOException 
     */
    private static void main_google_search(String[] args) throws IOException {

        //Fetch the page
        final Document doc = Jsoup.connect("https://google.com/search?q={water}&num=10&as_qdr=all").userAgent(USER_AGENT).get();

        //Traverse the results
        // Depending on the browser (user agent) and what google feels like, different web page formats are returned
        // some old search strings:   "h3.r > a"      or  "p > a" if using simple Mozilla
        for (Element result : doc.select("div.r > a")){

            //final String title = result.text();
            String title="";
            Element th = result.select("h3").first();
            if (th != null) {
                title = th.text();
            } else {
                title = result.text();
            }
            final String url = result.attr("href");

            // we have "title" and "url" for this search result
            System.out.println(title + " -> " + url);
        }
    }
    
    /**
     * Sample test to fetch a web page (using Jsoup)
     * 
     * @param args
     * @throws IOException 
     */
    private static void main_fetch_web_page(String[] args) throws IOException {
        Document doc = Jsoup.connect("http://en.wikipedia.org/").get();
        log(doc.title());

        Elements newsHeadlines = doc.select("#mp-itn b a");
        for (Element headline : newsHeadlines) {
            log("%s\n\t%s", headline.attr("title"), headline.absUrl("href"));
        }
    }

    private static void log(String msg, String... vals) {
        System.out.println(String.format(msg, vals));
}
}
