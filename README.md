# OpenQA

## Introduction

OpenQA, Open Question Answer, is an opensource REST server that provides core data structure and REST service for question and answer, modeled after StackOverflow, Quora and Zhihu (知乎).

The purpose of OpenQA is to provide a solide backend for developers to develop Question and Answer. It ** DOES NOT ** aims to be an QA with full featured UI.

OpenQA Core is the center of OpenQA, a REST server developed with Clojure. It uses Postgresql as datastore and uses ElasticSearch as search engine.

OpenQA Core focuses purely on the esentials of an Question and Answer site:

- Create / Update / Delete / Query Questions, Answers, and Comments
- Upvote / Downvote Questions, Answers
- Drafting / Publish / Delete Drafts
- Search powered by ElasticSearch

A Web frontend will be developed but itself is not considered a core component, as least initially.

## Design Principle

It was designed with following principle:

** Flexible data strucutre that can power complex QA sites **

The core data structure is built with following features:

- domain : allows developers to split QA data into different domains, and deploy domain data to different database instances.
- topic: subject topics for organizing content.
- tags: used for functional tags used by end users.
- meta_tags: used for meta tags used by developers.
- folder: user as an extra layer for organizing content.
- upvote_count / downvote_cout: used for upvoting / downvoting functions.
- seq_id: used to indicate time squence of Questions / Answers / Comments with the same domain.

** Minimal core **

The core only provides prebuilt question and answer content management services, and aims to provide these services fast, correct and extensible.

It dosn't do the follows:

- User management. All the core needs is uid.
- Security. Developers should bring in security features.
- Data backup and restore. It purely relies on database to provide data backup and restore functions.

## Installation

TBD.

## Usage

TBD

### Bugs

...

## License

Copyright © 2019 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
