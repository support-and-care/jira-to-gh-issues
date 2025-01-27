package io.pivotal.migration;

import io.pivotal.github.GithubComment;
import io.pivotal.github.ImportGithubIssue;
import io.pivotal.jira.JiraIssue;
import org.springframework.context.annotation.Bean;

import javax.annotation.Priority;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonApacheMavenMigrationConfig {

    private static final List<String> skipVersions =
            Arrays.asList("Contributions Welcome", "Pending Closure", "Waiting for Triage", "waiting-for-feedback", "backlog", "more-investigation");


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

        fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.PRIORITY, "Blocker", "blocker");
        fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.PRIORITY, "Critical", "critical");
        fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.PRIORITY, "Major", "major");
        fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.PRIORITY, "Minor", "minor");
        fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.PRIORITY, "Trivial", "trivial");

        fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.VERSION, "waiting-for-feedback", "waiting-for-feedback");
        fieldValueHandler.addMapping(FieldValueLabelHandler.FieldType.VERSION, "more-investigation", "help wanted");


        CompositeLabelHandler handler = new CompositeLabelHandler();
        handler.addLabelHandler(fieldValueHandler);


        return handler;
    }

    @Bean
    public IssueProcessor issueProcessor() {
        return new CompositeIssueProcessor(new FixDependencyIssueProcessor(), new SkipBotCommentIssueProcessor());
    }

    @Bean
    public JiraIssueFilter jiraIssueFilter() {
        return new CompositeJiraIssueFilter();
    }

    public static class FixDependencyIssueProcessor implements IssueProcessor {

        @Override
        public void beforeImport(JiraIssue jiraIssue, ImportGithubIssue githubIssue) {
            if (jiraIssue.getFields().getIssuetype().getName().equals("Task") ||
                    jiraIssue.getFields().getIssuetype().getName().equals("Improvement")) {
                if (jiraIssue.getFields().getSummary().contains("Bump") || jiraIssue.getFields().getSummary().contains("Upgrade")) {
                    List<String> labels = githubIssue.getIssue().getLabels().stream().filter(label -> label.contains("priority:")).toList();
                    List<String> newLabels = new ArrayList<>(labels);
                    newLabels.add("dependencies");
                    githubIssue.getIssue().setLabels(newLabels);
                }

            }
        }
    }

    public static class SkipBotCommentIssueProcessor implements IssueProcessor {
        @Override
        public void beforeImport(JiraIssue jiraIssue, ImportGithubIssue importIssue) {
            List<GithubComment> filteredCommentList = importIssue.getComments().stream()
                    .filter(githubComment -> !(githubComment.getBody().contains("https://issues.apache.org/jira/secure/ViewProfile.jspa?name=hudson") || githubComment.getBody().contains("https://issues.apache.org/jira/secure/ViewProfile.jspa?name=githubbot")))
                    .toList();
            importIssue.setComments(filteredCommentList);
        }
    }
}
