# Contributing to ChartFx
:+1::tada: First off, thanks for taking the time to contribute! :tada::+1:

The following is a set of guidelines for contributing to ChartFx and its packages. These are mostly guidelines, not rules. Use your best judgement, and feel free to propose changes to this document in a pull request.

## Open an issue
For bugs, issues, or other discussion, please log a new issue in the GitHub repository.

GitHub supports [markdown](https://help.github.com/categories/writing-on-github/), so when filing bugs make sure you check the formatting before clicking submit.

## Other discussions
For general "how-to" and guidance questions about using ChartFx to build and run applications, please have a look at the various [samples](https://github.com/fair-acc/chart-fx/tree/master/chartfx-samples/src/main/java/io/fair_acc) here or if you cannot find anything that fits your use-case use [Stack Overflow](http://stackoverflow.com/questions/tagged/chartfx-api) tagged with `ChartFx-api`.

## Contributing code and content
We welcome all forms of contributions from the community. Please read the following guidelines to maximise the chances of your PR being merged.

### Communication
 - Before starting work on a feature, check if there isn't already an examples in the 'samples' sub-module.
   If not, then please open an issue on GitHub describing the proposed feature. We want to make sure any feature work goes smoothly.
   We're happy to work with you to determine if it fits the current project direction and make sure no one else is already working on it.

 - For any work related to setting up build, test, and CI for ChartFx on GitHub, or for small patches or bug fixes, please open an issue
   for tracking purposes, but we generally don't need a discussion prior to opening a PR.

### Development process
Please be sure to follow the usual process for submitting PRs:

 - Fork the repository
 - Make sure it compiles w/o errors against the current release 'main' branch::
    - 'main' (active) code-compatibility level: JDK11, OpenJFX13, limited to Java 11 language features
    - 'JDK8' (out of support) code-compatibility level: JDK8 This branch contains the last version working on JDK8 and will not receive any further updates.
 - Write and add a descriptive/meaningful JUnit test-case or [minimal working example](https://github.com/fair-acc/chart-fx/tree/master/chartfx-samples/src/main/java/io/fair_acc)
 - [TODO] apply default code formatter (to minimise future refactoring)
 - Please check against the current [PMD](https://pmd.github.io/) [default rules](https://github.com/fair-acc/chart-fx/blob/master/pmd_rules.xml), [FindBugs](http://findbugs.sourceforge.net/) or similar QA code checker (N.B. other/further code improvements are welcome)
 - Create a pull request
   - Make sure your PR title is descriptive
   - Include a link back to an open issue in the PR description

We reserve the right to close PRs that are not making progress. Closed PRs can be reopened again later and work can resume.

### Contributor License Agreement
By contributing your code to ChartFx you grant us a non-exclusive,
irrevocable, worldwide, royalty-free, sublicenseable, transferable
license under all of Your relevant intellectual property rights
(including copyright, patent, and any other rights), to use, copy,
prepare derivative works of, distribute and publicly perform and
display the Contributions on any licensing terms, including without limitation:
(a) open source licenses like the Apache license; and (b) binary,
proprietary, or commercial licenses. Except for the licenses granted herein,
You reserve all right, title, and interest in and to the Contribution.

You confirm that you are able to grant us these rights. You represent
that You are legally entitled to grant the above license. If Your employer
has rights to intellectual property that You create, You represent that
You have received permission to make the Contributions on behalf of that
employer, or that Your employer has waived such rights for the Contributions.

You represent that the Contributions are Your original works of
authorship, and to Your knowledge, no other person claims, or
has the right to claim, any right in any invention or patent
related to the Contributions. You also represent that You are
not legally obligated, whether by entering into an agreement
or otherwise, in any way that conflicts with the terms of this license.

We acknowledge that, except as explicitly described in this
Agreement, any Contribution which you provide is on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION,
ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT,
MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.

## Code of Conduct
To ensure an inclusive community, contributors and users in the ChartFx
community should follow the [code of conduct](./CODE_OF_CONDUCT.md).
