package pl.codeleak.java;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import picocli.CommandLine.*;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@Command(name = "cleaner", description = "Cleans and adjusts JIRA exported CSV file", mixinStandardHelpOptions = true, version = "0.0.1")
class JiraCsvCleaner implements Callable<Integer> {

    @Parameters(index = "0", description = "Input file path")
    private File input;

    @Parameters(index = "1", description = "Output file path")
    private File output;

    @Option(names = {"-u", "--url"}, description = "Migrated from base URL (e.g. https://jira.yourcompany.com/browse/)", required = true)
    private URL migratedFromBaseUrl;

    @Option(names = {"-s", "--separator"}, description = "Separator", defaultValue = ",")
    private char separator;

    @Option(names = {"-a", "--attachments"}, description = "Include attachments links in description", defaultValue = "true")
    private boolean includeAttachmentsLinks;

    @Override
    public Integer call() throws Exception {

        var issuesIn = new CsvToBeanBuilder<JiraIssue>(new FileReader(input))
                .withSeparator(separator)
                .withType(JiraIssue.class)
                .build()
                .parse();


        var issuesOut = issuesIn.stream()
                .peek(issue -> updateDescription(issue))
                .peek(issue -> updateEpicLink(issuesIn, issue))
                .peek(issue -> updateIssueLinks(issuesIn, issue))
                .collect(Collectors.toList());

        try (var writer = new FileWriter(output)) {
            var beanToCsvBuilder = new StatefulBeanToCsvBuilder<JiraIssue>(writer).build();
            beanToCsvBuilder.write(issuesOut);
        }
        return 0;
    }

    private void updateDescription(JiraIssue issue) {
        if (includeAttachmentsLinks) {
            appendAttachmentsLinksToDescription(issue);
        }
        appendToDescription(issue, "\n\n----\n\nMigrated from " + migratedFromBaseUrl.toString() + "/" + issue.getIssueKey());
    }


    private void appendAttachmentsLinksToDescription(JiraIssue issue) {
        var attachments = issue.getAttachments();
        var hasAttachments = attachments != null && !attachments.isEmpty();
        if (!hasAttachments) {
            return;
        }
        var attachmentsLinks = attachments.values().stream()
                .filter(attachmentsLine -> attachmentsLine != null && !attachmentsLine.isBlank())
                .map(attachmentLine -> attachmentLine.split(";"))
                .map(attachmentRecord -> "- Name: " + attachmentRecord[2] + ", Link: " + attachmentRecord[3])
                .collect(Collectors.joining("\n"));

        if (!attachmentsLinks.isBlank()) {
            appendToDescription(issue, "\n\n----\n\nAttachments\n\n" + attachmentsLinks);
        }
    }

    private void appendToDescription(JiraIssue issue, String tail) {
        issue.setDescription(issue.getDescription() + tail);
    }

    private static void updateEpicLink(List<JiraIssue> issuesIn, JiraIssue issue) {
        var epicLink = issue.getEpicLink();
        if (epicLink == null || epicLink.isBlank()) {
            return;
        }
        issue.setEpicLink(findIssueIdByIssueKey(epicLink, issuesIn));
    }

    private static void updateIssueLinks(List<JiraIssue> issues, JiraIssue issue) {
        MultiValuedMap<String, String> relatedIssuesKeys = issue.getRelatedIssues();
        if (relatedIssuesKeys == null || relatedIssuesKeys.isEmpty()) {
            return;
        }
        MultiValuedMap<String, String> relatedIssuesIds = new ArrayListValuedHashMap<>();
        relatedIssuesKeys.asMap().forEach((relation, issuesKeys) -> relatedIssuesIds.putAll(relation, mapKeysToIds(issues, issuesKeys)));
        issue.setRelatedIssues(relatedIssuesIds);
    }

    private static List<String> mapKeysToIds(List<JiraIssue> issues, Collection<String> value) {
        return value.stream().map(issueKey -> findIssueIdByIssueKey(issueKey, issues)).collect(Collectors.toList());
    }

    private static String findIssueIdByIssueKey(String issueKey, List<JiraIssue> issues) {
        var notFoundValue = "";
        if (issueKey == null) {
            return notFoundValue;
        }
        return issues.stream()
                .filter(issue -> issue.getIssueKey().equals(issueKey))
                .map(JiraIssue::getIssueId)
                .findFirst()
                .orElse(notFoundValue);
    }
}
