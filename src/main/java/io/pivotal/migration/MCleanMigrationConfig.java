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
import io.pivotal.jira.JiraFixVersion;
import io.pivotal.jira.JiraIssue;
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

		CompositeLabelHandler handler = new CompositeLabelHandler();
		handler.addLabelHandler(fieldValueHandler);


		return handler;
	}

	@Bean
	public IssueProcessor issueProcessor() {
		return new CompositeIssueProcessor(new AssigneeDroppingIssueProcessor(), new Spr7640IssueProcessor());
	}


	private static class AssigneeDroppingIssueProcessor implements IssueProcessor {

		private static final String label1 = LabelFactories.STATUS_LABEL.apply("waiting-for-triage").get("name");

		private static final String label2 = LabelFactories.STATUS_LABEL.apply("ideal-for-contribution").get("name");


		@Override
		public void beforeImport(JiraIssue issue, ImportGithubIssue importIssue) {
			JiraFixVersion version = issue.getFixVersion();
			GithubIssue ghIssue = importIssue.getIssue();
			if (version != null && version.getName().contains("Backlog") ||
					ghIssue.getLabels().contains(label1) ||
					ghIssue.getLabels().contains(label2)) {

				ghIssue.setAssignee(null);
			}
		}
	}


	/**
	 * The description of SPR-7640 is large enough to cause import failure.
	 */
	private static class Spr7640IssueProcessor implements IssueProcessor {

		@Override
		public void beforeConversion(JiraIssue issue) {
			if (issue.getKey().equals("SPR-7640")) {
				JiraIssue.Fields fields = issue.getFields();
				fields.setDescription(fields.getDescription().substring(0, 1000) + "...");
			}
		}
	}

}
