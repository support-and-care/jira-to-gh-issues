package io.pivotal.migration;

import io.pivotal.jira.JiraIssue;

import java.util.function.Predicate;


public interface JiraIssueFilter extends Predicate<JiraIssue> {

    default boolean test(JiraIssue jiraIssue) {
        return true;
    }
}
