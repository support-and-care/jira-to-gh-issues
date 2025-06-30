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

import io.pivotal.github.ImportGithubIssue;
import io.pivotal.jira.JiraIssue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;


/**
 * Configuration for migration of MNG Jira.
 */
@Configuration
@Import(CommonApacheMavenMigrationConfig.class)
@ConditionalOnProperty(name = "jira.projectId", havingValue = "MNG")
public class MngMigrationConfig {

    @Bean
    public IssueProcessor issueProcessor() {
        return new CompositeIssueProcessor(new CommonApacheMavenMigrationConfig.FixDependencyIssueProcessor(), new CommonApacheMavenMigrationConfig.SkipBotCommentIssueProcessor(), new Mng925IssueProcessor(), new Mng5592IssueProcessor());
    }



    /**
     * The description of MNG-925 is large enough to cause import failure.
     */
    private static class Mng925IssueProcessor implements IssueProcessor {

        @Override
        public void beforeConversion(JiraIssue issue) {
            if (issue.getKey().equals("MNG-925")) {
                JiraIssue.Fields fields = issue.getFields();
                fields.setDescription(fields.getDescription().replace(">", ""));
            }
        }
    }

    /**
     * The payload of MNG-5592 is too large. Therefore, we have to cut some comments are cut.
     */
    private static class Mng5592IssueProcessor implements IssueProcessor {

        @Override
        public void beforeImport(JiraIssue jiraIssue, ImportGithubIssue importIssue) {
            if (jiraIssue.getKey().equals("MNG-5592")) {
                importIssue.getComments().forEach(comment -> {
                    if (comment.getBody().contains("org.eclipse.aether.graph.DefaultDependencyNode@")) {
                        String commentBody = comment.getBody();
                        List<String> list = Arrays.stream(StringUtils.delimitedListToStringArray(commentBody, "\n")).filter(line -> !line.contains("org.eclipse.aether.graph.DefaultDependencyNode@")).toList();
                        comment.setBody("[Snipped see original comment in jira]\n\n" + StringUtils.collectionToDelimitedString(list, "\n"));
                    }
                });
            }
        }


    }



}
