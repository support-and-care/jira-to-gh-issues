package io.pivotal.post;

import org.springframework.http.RequestEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindDuplicatesApp extends GitHubBaseApp {


    public static void main(String[] args) throws IOException {

        logger.info("Start searching for duplicate issues...");


        File failuresFile = new File("github-issue-mappings-failures.txt");
        try (FileWriter failWriter = new FileWriter(failuresFile, true)) {

            String projectId = initJiraConfig().getProjectId();

            UriComponentsBuilder uricBuilder = UriComponentsBuilder.newInstance()
                    .uriComponents(issuesUric)
                    .queryParam("page", "{page}")
                    .queryParam("state", "open");

            Map<String, Integer> foundMappings = new HashMap<>();
            int page = 1;
            while (true){
                RequestEntity<Void> pageRequest = issuesPageRequest(uricBuilder, page);
                List<Map<String, Object>> issues = exchange(pageRequest, LIST_OF_MAPS_TYPE, failWriter, null);
                logger.info("Page " + page + ": " + (issues != null ? issues.size() + " issues" : "no results (exiting)"));
                if (issues == null) {
                    logger.info("No results, exiting..");
                    break;
                }
                if (issues.isEmpty()) {
                    logger.info("Done, exiting..");
                    break;
                }
                for (Map<String, Object> map : issues) {
                    if(!map.containsKey("pull_request")) {
                        String title = (String) map.get("title");
                        Integer number = (Integer) map.get("number");
                        logger.info("number/title: {}/{}", number, title);
                        if(title.contains("[" + projectId)){
                            String jiraKey = title.substring(title.indexOf('[') + 1, title.indexOf(']'));
                            logger.info("mapping: {}/{}", number, jiraKey);
                            if (foundMappings.containsKey(jiraKey)) {
                                logger.info("Duplicate mapping: {}/{}", number, jiraKey);
                                Integer alreadyExistingKey = foundMappings.get(jiraKey);
                                logger.info("Duplicate mapping: {}/{}", alreadyExistingKey, jiraKey);
                            } else {
                                foundMappings.put(jiraKey, number);
                            }

                        }
                    }
                }
                page++;
            }
        }



    }

}
