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

import io.pivotal.jira.JiraIssue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;


/**
 * Configuration for migration of Apache Maven Jira Project MPom.
 */
@Configuration
@ConditionalOnProperty(name = "jira.projectId", havingValue = "MPOM")
@Import({CommonApacheMavenMigrationConfig.class})
public class MPomMigrationConfig {

    @Primary
    @Bean
    public MilestoneFilter milestoneFilter(@Value("${jira.component}") String componentName) {
        return fixVersion -> {
            String versionName = fixVersion.getName();
            String[] splittedName = versionName.split("-");
            return splittedName.length == 2 && versionName.startsWith(componentName.toUpperCase());
        };
    }

    @Primary
    @Bean
    public JiraIssueFilter jiraIssueFilter(@Value("${jira.component}") String componentName) {
        return new CompositeJiraIssueFilter(new MPomMigrationConfig.JiraIssueComponentFiler(componentName));
    }

    private static class JiraIssueComponentFiler implements JiraIssueFilter {

        private final String componentName;

        public JiraIssueComponentFiler(String componentName) {
            this.componentName = componentName;
        }

        @Override
        public boolean test(JiraIssue jiraIssue) {
            return jiraIssue.getFields().getComponents().stream().anyMatch(jiraComponent -> jiraComponent.getName().contains(componentName));
        }
    }



}
