/*
 * Copyright 2002-2018 the original author or authors.
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
package io.pivotal.pre;

import io.pivotal.jira.JiraClient;
import io.pivotal.jira.JiraConfig;
import io.pivotal.jira.JiraIssue;
import io.pivotal.jira.JiraUser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Extract the full list of assignees from Jira tickets. This can be used to prepare
 * src/main/resources/jira-to-github-users.properties.
 *
 * @author Rossen Stoyanchev
 */
public class AssigneesReport extends BaseApp {


	public static void main(String[] args) {

		record JiraUserName(String value) {}

		record JiraUserData(JiraUserName jiraUserName, String displayName, AtomicInteger asigneeCount) {
			static JiraUserData create(JiraUserName userName, String displayName) {
				return new JiraUserData(userName, displayName, new AtomicInteger(0));
			}

			@Override
			public String toString() {
				return jiraUserName().value + " (" + displayName() + ") [" + asigneeCount() + "]";
			}
		}

		JiraConfig config = initJiraConfig();
		JiraClient client = new JiraClient(config);

		Map<JiraUserName, JiraUserData> result = new HashMap<>();
		for (JiraIssue issue : client.findIssues(config.getMigrateJql())) {
			JiraUser user = issue.getFields().getAssignee();
			if (user != null) {
				JiraUserName asignee = new JiraUserName(user.getKey());

				result.computeIfPresent(asignee, (key, data) -> {
					data.asigneeCount().incrementAndGet();
					return data;
				});

				result.computeIfAbsent(asignee, (userName) -> JiraUserData.create(userName, user.getDisplayName()));
			}
		}

		List<String> knownUsers = new ArrayList<>();

		try {
			var lines = Files.readAllLines(Path.of("./jira-to-github-users.properties"));
			for (String line : lines) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}

				String[] parts = line.split(":");

				if (parts.length < 2) {
					System.out.println("Invalid line: " + line);
					continue;
				}

				String jiraUser = parts[0];
				String githubUser = parts[1];
				knownUsers.add(jiraUser);
			}
		} catch (Exception e) {
			System.out.println("File not found. Wont apply filtering.");
			e.printStackTrace();
		}

		List<JiraUserData> assignees = result.entrySet().stream()
			.filter(entry -> !knownUsers.contains(entry.getKey().value))
			.map(Entry::getValue)
			.sorted((a, b) -> b.asigneeCount().get() - a.asigneeCount().get())
			.toList();

		for (JiraUserData assignee : assignees) {
			System.out.println(assignee);
		}
	}

}
