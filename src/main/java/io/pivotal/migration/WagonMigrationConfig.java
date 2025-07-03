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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 * Configuration for migration of WAGON.
 */
@Configuration
@ConditionalOnProperty(name = "jira.projectId", havingValue = "WAGON")
@Import({CommonApacheMavenMigrationConfig.class})
public class WagonMigrationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(WagonMigrationConfig.class);

    @Bean
    public IssueProcessor issueProcessor() {
        return new CompositeIssueProcessor(new CommonApacheMavenMigrationConfig.FixDependencyIssueProcessor(), new CommonApacheMavenMigrationConfig.SkipBotCommentIssueProcessor(), new WagonMigrationConfig.WAGON306IssueProcessor());
    }



    /**
     * The description of WAGON-306 contains some characters no supported by markdown conversion.
     */
    private static class WAGON306IssueProcessor implements IssueProcessor {

        @Override
        public void beforeConversion(JiraIssue issue) {
            if (issue.getKey().equals("WAGON-306")) {
                JiraIssue.Fields fields = issue.getFields();
                fields.setDescription(fields.getDescription().replace(">>", " "));
            }
        }
    }

}
