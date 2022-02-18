package pl.codeleak.java;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import org.apache.commons.collections4.MultiValuedMap;

@Data
public class JiraIssue {

    @CsvBindByName(column = "Summary")
    private String summary;

    @CsvBindByName(column = "Description")
    private String description;

    @CsvBindByName(column = "Issue Type")
    private String issueType;

    @CsvBindByName(column = "Issue key")
    private String issueKey;

    @CsvBindByName(column = "Issue id")
    private String issueId;

    @CsvBindByName(column = "Status")
    private String status;

    @CsvBindByName(column = "Reporter")
    private String reporter;

    @CsvBindByName(column = "Assignee")
    private String assignee;

    @CsvBindByName(column = "Priority")
    private String priority;

    @CsvBindByName(column = "Parent Id")
    private String parentId;

    @CsvBindByName(column = "Custom field (Story Points)")
    private String storyPoints;

    @CsvBindByName(column = "Created")
    private String createdDate;

    @CsvBindByName(column = "Due Date")
    private String dueDate;

    @CsvBindByName(column = "Original Estimate")
    private String originalEstimate;

    @CsvBindByName(column = "Remaining Estimate")
    private String remainingEstimate;

    @CsvBindByName(column = "Resolved")
    private String resolved;

    @CsvBindByName(column = "Resolution")
    private String resolution;

    @CsvBindByName(column = "Custom field (Epic Name)")
    private String epicName;

    @CsvBindByName(column = "Custom field (Epic Link)")
    private String epicLink;

    @CsvBindByName(column = "Custom field (Epic Link as Epic Name)")
    private String epicLinkAsEpicName;

    @CsvBindAndJoinByName(column = "Outward issue link.*", elementType = String.class)
    private MultiValuedMap<String, String> relatedIssues;

    @CsvBindAndJoinByName(column = "Comment", elementType = String.class)
    private MultiValuedMap<String, String> comments;

    @CsvBindAndJoinByName(column = "Attachment", elementType = String.class)
    private MultiValuedMap<String, String> attachments;

}
