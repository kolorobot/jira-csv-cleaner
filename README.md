# (Really) Basic JIRA CSV exported file cleaner and adjuster

This is a simple CLI tool that cleans up and adjusts the exported JIRA server CSV file:

- Appends `Migrated from` with URL to the original issue to the `Description` column
- Appends `Attachments` section with attachments links to the `Description` column
- Replaces `Issue Keys` with `Issue IDs` in `Epic Link` and `Outward Link` columns
- Adds `Epic Link as Epic Name` column
- Fixes email accounts in `Assignee`, `Reporter`, `Comment` and `Attachment` columns 
- Retains only selected columns

# Prerequisites

- Java 11+

# Run

- `./gradlew run --args="-h"`

Example usage:

- `./gradlew run --args="input.csv output.csv --url=https://example.com/browse"`
