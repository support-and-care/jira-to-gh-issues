package io.pivotal.migration;

import io.pivotal.jira.JiraIssue;

import java.util.Arrays;
import java.util.List;

public class CompositeJiraIssueFilter implements JiraIssueFilter {
    private final List<JiraIssueFilter> jiraIssuetFilers;

    public CompositeJiraIssueFilter(JiraIssueFilter... jiraIssuetFilers) {
        this.jiraIssuetFilers = Arrays.asList(jiraIssuetFilers);
    }

    @Override
    public boolean test(JiraIssue jiraIssue) {
        return jiraIssuetFilers.stream().allMatch (f -> f.test(jiraIssue));
    }
}
