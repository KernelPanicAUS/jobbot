# Jobbot

## Why

Jobbot was written to help me search for jobs in Germany.

In a nutshell, the bot kicks off every three hours, hits RSS feeds of websites that post interesting jobs and sends me slack messages for all new jobs!

Jobbot keeps a small sqlite database to keep track of the jobs its seen before, so I don't get inundated with slack messages for the same jobs over and over again.


## So how do I get started ?

First thing you need to do is to create a webhook on your slack team that will allow you to post messges.
Once that's been done, copy the sample config to the root of the project, like so:

```
[14:41] ~/code/jobbot [master=] > cp sample/config.groovy.sample config.groovy
```

You will then need to paste your Slack webhook url into the file like so:

```

jobbot {

    slackUrl = "https://hooks.slack.com/services/ZZZZZ/XXXXXXX/YYYYYYYY"

    db {
      ....
    }
    ...
}
```

## Ok, so I've done all that. Now what ?

Kick off the bot as a background process, like so:

```
[14:41] ~/code/jobbot [master=] > ./jobbot.groovy&
```

and keep an eye out for those message :)

The process will log to a file (log snippet below), so you can keep tabs on what its doing if you need to.

```
2016-09-22 14:56:33,073 INFO [Spidey] - Begin spider _init_
2016-09-22 14:56:33,074 INFO [Spidey] - Starting XpatJobsSpider...
2016-09-22 14:56:34,376 INFO [Spidey] - Found 1 new job ads...
2016-09-22 14:56:34,377 INFO [Spidey] - Sending message to slack for job => Senior Java Analyst/Developer
```

## Requirements
Groovy v2.3.8 at least

The prefered method to install groovy is via http://sdkman.io

You can also install it via homebrew or from source, whatever makes you happy :)
