/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.post;

import io.pivotal.jira.JiraConfig;
import io.pivotal.util.ProgressTracker;
import org.springframework.http.RequestEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;

/**
 * This app will scan issues of a Github repo to search the jira.projectId in issue title to find the jira id
 * such "[MCLEAN-10] blabla do something" to extract MCLEAN-10 and create the file github-issue-mappings.properties with
 * MCLEAN-10:292
 * @author Olivier Lamy
 */
public class JiraGithubMappingApp extends GitHubBaseApp {


	public static void main(String[] args) throws IOException {

		File failuresFile = new File("github-issue-mappings-failures.txt");

		File mappingsFile;
		if(args !=null && args.length > 0) {
			mappingsFile = new File(args[0]);
		} else {
			mappingsFile = new File("github-issue-mappings.properties");
		}

		JiraConfig jiraConfig = new JiraConfig();

		List<String> mappings = new ArrayList<>();

		try (FileWriter failWriter = new FileWriter(failuresFile, true)) {

			String projectId = initJiraConfig().getProjectId();

			UriComponentsBuilder uricBuilder = UriComponentsBuilder.newInstance()
					.uriComponents(issuesUric)
					.queryParam("page", "{page}")
					.queryParam("state", "all");

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
							String jiraKey = title.substring(title.lastIndexOf('[') + 1, title.indexOf(']'));
							logger.info("ghId: {}/{}", number, jiraKey);
							mappings.add(jiraKey + ":" + number);
						}
					}
				}
				page++;
			}
			Files.write(mappingsFile.toPath(), mappings);
		}
	}


}
