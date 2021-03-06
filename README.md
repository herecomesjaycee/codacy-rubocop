[![Codacy Badge](https://api.codacy.com/project/badge/grade/7cb874a40208406dbe62fe96d6b3a727)](https://www.codacy.com/app/Codacy/codacy-rubocop)
[![Build Status](https://circleci.com/gh/codacy/codacy-rubocop.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-rubocop)

# Codacy Rubocop

This is the docker engine we use at Codacy to have [Rubocop](https://www.rubocop.org) support.
You can also create a docker to integrate the tool and language of your choice!
See the [codacy-engine-scala-seed](https://github.com/codacy/codacy-engine-scala-seed) repository for more information.

## Usage

You can create the docker by doing:

```
sbt docker:publishLocal
```

The docker is ran with the following command:

```
docker run -it -v $srcDir:/src  <DOCKER_NAME>:<DOCKER_VERSION>
```

## Test

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

## Update Version with Docs

Change the version in `.rubocop-version` and then run on this project root:

```sh
bundle install --path vendor/bundle
bundle exec src/main/ruby/rubocop_doc/generator.rb
bundle exec src/main/ruby/codacy/rubocop/generator.rb
rm -f rubocop-doc.yml
```

## What is Codacy?

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features:

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
