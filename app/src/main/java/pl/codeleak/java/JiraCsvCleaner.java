package pl.codeleak.java;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import picocli.CommandLine.*;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
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

    @Option(names = {"-e", "--email"}, description = "Append email domain name to the user accounts (e.g. example.com")
    private Optional<String> domain;

    @Override
    public Integer call() throws Exception {

        var issuesIn = new CsvToBeanBuilder<JiraIssue>(new FileReader(input))
                .withSeparator(separator)
                .withType(JiraIssue.class)
                .build()
                .parse();


        var issuesOut = issuesIn.stream()
                .peek(issue -> updateDescription(issue))
                .peek(issue -> setEpicLinkAsEpicName(issuesIn, issue))
                .peek(issue -> updateEpicLink(issuesIn, issue))
                .peek(issue -> updateIssueLinks(issuesIn, issue))
                .peek(issue -> updateAccounts(issue))
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
        if (isNullOrEmpty(attachments)) {
            return;
        }
        var attachmentsLinks = attachments.values().stream()
                .filter(attachmentLine -> !isNullOrEmpty(attachmentLine))
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

    private void setEpicLinkAsEpicName(List<JiraIssue> issuesIn, JiraIssue issue) {
        var epicLink = issue.getEpicLink();
        if (isNullOrEmpty(epicLink)) {
            return;
        }
        issue.setEpicLinkAsEpicName(findEpicNameByIssueKey(epicLink, issuesIn));
    }


    private static void updateEpicLink(List<JiraIssue> issuesIn, JiraIssue issue) {
        var epicLink = issue.getEpicLink();
        if (isNullOrEmpty(epicLink)) {
            return;
        }
        issue.setEpicLink(findIssueIdByIssueKey(epicLink, issuesIn));
    }


    private static void updateIssueLinks(List<JiraIssue> issues, JiraIssue issue) {
        MultiValuedMap<String, String> relatedIssuesKeys = issue.getRelatedIssues();
        if (isNullOrEmpty(relatedIssuesKeys)) {
            return;
        }
        MultiValuedMap<String, String> relatedIssuesIds = new ArrayListValuedHashMap<>();
        relatedIssuesKeys.asMap().forEach((relation, issuesKeys) -> relatedIssuesIds.putAll(relation, mapKeysToIds(issues, issuesKeys)));
        issue.setRelatedIssues(relatedIssuesIds);
    }

    private void updateAccounts(JiraIssue issue) {
        if (domain.isEmpty()) {
            return;
        }
        var domainValue = domain.get();
        appendDomain(issue.getAssignee(), domainValue).ifPresent(issue::setAssignee);
        appendDomain(issue.getReporter(), domainValue).ifPresent(issue::setReporter);
        updateAccountsInMultiValueMap(domainValue, "Comment", issue.getComments(), issue::setComments);
        updateAccountsInMultiValueMap(domainValue, "Attachment", issue.getAttachments(), issue::setAttachments);
    }

    private void updateAccountsInMultiValueMap(String domain, String key, MultiValuedMap<String, String> input, Consumer<MultiValuedMap<String, String>> updater) {
        if (isNullOrEmpty(input)) {
            return;
        }

        List<String> updatedValues = updateAccounts(domain, input);
        if (isNullOrEmpty(updatedValues)) {
            return;
        }

        MultiValuedMap<String, String> result = new ArrayListValuedHashMap<>();
        result.putAll(key, updatedValues);
        updater.accept(result);
    }

    private List<String> updateAccounts(String domain, MultiValuedMap<String, String> input) {
        // number of output lines must match number of input lines
        return input.values().stream()
                .map(line -> updateAccountsInSingleLine(domain, line))
                .collect(Collectors.toList());

    }

    private String updateAccountsInSingleLine(String domain, String line) {

        if (isNullOrEmpty(line)) {
            return line;
        }
        var delimiter = ";";
        var record = line.split(delimiter);
        appendDomain(record[1], domain).ifPresent(value -> record[1] = value);
        return String.join(delimiter, record);
    }

    private Optional<String> appendDomain(String account, String domain) {
        if (isNullOrEmpty(account)) {
            return Optional.empty();
        }
        if (!account.contains("@")) {
            account = account + "@" + domain;
            return Optional.of(account);
        }
        return Optional.empty();
    }

    private static List<String> mapKeysToIds(List<JiraIssue> issues, Collection<String> value) {
        return value.stream().map(issueKey -> findIssueIdByIssueKey(issueKey, issues)).collect(Collectors.toList());
    }


    private static String findEpicNameByIssueKey(String issueKey, List<JiraIssue> issues) {
        return findByIssueKey(issueKey, issues, jiraIssue -> Optional.ofNullable(jiraIssue.getEpicName()));
    }

    private static String findIssueIdByIssueKey(String issueKey, List<JiraIssue> issues) {
        return findByIssueKey(issueKey, issues, jiraIssue -> Optional.ofNullable(jiraIssue.getIssueId()));
    }

    private static String findByIssueKey(String issueKey, List<JiraIssue> issues, Function<JiraIssue, Optional<String>> mapper) {
        var defaultValue = "";
        if (isNullOrEmpty(issueKey)) {
            return defaultValue;
        }
        return issues.stream()
                .filter(issue -> issue.getIssueKey().equals(issueKey))
                .map(value -> mapper.apply(value).orElse(defaultValue))
                .findFirst()
                .orElse(defaultValue);
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isNullOrEmpty(Collection value) {
        return value == null || value.isEmpty();
    }

    private static boolean isNullOrEmpty(MultiValuedMap value) {
        return value == null || value.isEmpty();
    }
}
