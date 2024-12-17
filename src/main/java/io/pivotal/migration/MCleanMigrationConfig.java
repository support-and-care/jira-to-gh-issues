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
package io.pivotal.migration;

import io.pivotal.github.GithubIssue;
import io.pivotal.github.ImportGithubIssue;
import io.pivotal.jira.JiraIssue;
import io.pivotal.jira.JiraIssueType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;


/**
 * Configuration for migration of SPR Jira.
 */
@Configuration
@ConditionalOnProperty(name = "jira.projectId", havingValue = "MCLEAN")
public class MCleanMigrationConfig {

	private static final List<String> skipVersions =
			Arrays.asList("Contributions Welcome", "Pending Closure", "Waiting for Triage");


	@Bean
	public MilestoneFilter milestoneFilter() {
		return fixVersion -> !skipVersions.contains(fixVersion.getName());
	}

	@Bean
	public LabelHandler labelHandler() {
		FieldValueLabelHandler fieldValueHandler = new FieldValueLabelHandler();
		fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.ISSUE_TYPE, "Bug", "bug");
		fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.ISSUE_TYPE, "Improvement", "enhancement");
		fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.ISSUE_TYPE, "New Feature", "enhancement");
		fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.ISSUE_TYPE, "Task", "maintenance");
		fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.ISSUE_TYPE, "Dependency Upgrade", "dependencies");

		CompositeLabelHandler handler = new CompositeLabelHandler();
		handler.addLabelHandler(fieldValueHandler);


		return handler;
	}

	@Bean
	public IssueProcessor issueProcessor() {
		return new CompositeIssueProcessor(new FixDependencyIssueProcessor());
	}


	private static class FixDependencyIssueProcessor implements IssueProcessor {

		@Override
		public void beforeImport(JiraIssue jiraIssue, ImportGithubIssue githubIssue) {
			if (jiraIssue.getFields().getIssuetype().getName().equals("Task") ||
			jiraIssue.getFields().getIssuetype().getName().equals("Improvement")) {
				if(jiraIssue.getFields().getSummary().contains("Bump") || jiraIssue.getFields().getSummary().contains("Upgrade")) {
					githubIssue.getIssue().setLabels(List.of("dependencies"));
				}

			}
		}
	}

}
