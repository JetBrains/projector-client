Thank you for reading this: we welcome any contributions.

All Projector-related projects have the same contributing guidelines. This is the place where they are described: here we talk about creating tickets in our issue tracker, proposing changes by GitHub Pull Requests, and writing about Projector.

## Creating an issue

You can create issues in our [issue tracker](https://youtrack.jetbrains.com/issues/PRJ).

If you **report a bug**, please either *describe reproduction steps* or *mention that you don't know how to reproduce it stably*. You can also **suggest a feature**.

Please don't forget to specify fields of the ticket (in the top right corner):

* Type (Bug/Feature/Usability Problem and so on),
* Subsystem (if you are *not sure*, we have a corresponding item),
* Affected versions (we use the following formats: `server-vX.Y.Z` for plain Projector server and client, `installer-vX.Y.Z` for projector-installer, `agent-vX.Y.Z` for projector-plugin, `launcher-vX.Y.Z` for Projector Electron App; if multiple items are affected, they can be enumerated separated by a comma with a space).

If you've found the same ticket, please **avoid creating a duplicate**. Better vote (and comment with more info) for the existing one! Some tickets have a workaround, so maybe you will get a temporary solution right away.

There are some explanations on how we work with YouTrack to make you a bit more comfortable.

### Ticket fields

Fields is a table in the top right corner of a ticket screen. Projector project has the following ones:

* Project – obviously `Projector` (can be referenced by `PRJ` code).
* Priority – how we see the severity of the issue. We usually take the severity into account when deciding what to resolve next.
* Type.
* Subsystem – which part of Projector is affected (Server/Web client/Plugin and so on).
* State – more on this below in a special section.
* Affected versions – which Projector versions have the issue.
* Fix versions – in which Projector versions the issue is resolved.
* Assignee – who is working/has worked on the issue.
* Testing – how we should test the issue.
* Tester – visible only if we should test the issue.

Sometimes the fields are not applicable, so we leave them with a default value.

### Ticket states

Please don't change the state field of a ticket. When we triage the ticket, we will change its state from `Submitted` to `Open`.

When we are looking for an answer from a reporter, we will change the state to `Wait for reply`.

Sometimes we place tickets to `Backlog` meaning that the task will be solved without the priority queue, hopefully soon.

When a ticket is marked as gray and strikethrough, it means it's closed.

### Ticket activity settings

By default, YouTrack shows only comments under the description of a ticket. You can also enable *Issue history* to see changes in the description and fields and *VCS changes* to see the commits and pull requests related to the issue. You can find the corresponding switcher buttons [just under the description of a ticket](https://www.jetbrains.com/help/youtrack/standalone/VCS-Changes.html).

## Submitting changes

If you want to resolve an **issue submitted in the issue tracker** or propose a **minor edit**, open a Pull Request. Better don't include multiple changes in a single PR, create separate.

**Pre-commit actions**. Before committing, please ensure that code style is correct. If you use IntelliJ IDEA, you can just select the following checkboxes of "Before Commit" actions:

* Reformat code
* Rearrange code
* Optimize imports
* Cleanup

**Commits naming**. If there are issues that you address, please mention them in the prefix of commit messages. Usually, a commit name starts with a verb. Example: `PRJ-68 Make clipboard owners lose ownership when clipboard is changed by a client`.

## Spread Projector

We are glad when people talk about our technology. Feel free to tell your friends and colleagues about Projector. If you have written something publicly, a guide or just a short review, or maybe even used Projector in another project, you can always share it on Twitter mentioning [@ProjectorJB](https://twitter.com/ProjectorJB), we often answer and retweet.
