#!/usr/bin/env groovy

@Grapes([
    @Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2'),
    @Grab('org.jsoup:jsoup:1.6.1'),
    @Grab('log4j:log4j:1.2.17'),
    @Grab('com.github.groovy-wslite:groovy-wslite:1.1.2'),
    @GrabConfig(systemClassLoader=true)
])

import groovy.sql.Sql
import org.sqlite.JDBC
import org.jsoup.*
import wslite.rest.*
import groovy.util.slurpersupport.GPathResult
import org.apache.log4j.*
import groovy.util.logging.*

def timer = new Timer()

timer.schedule({
    def spider = new Spidey()
    spider.main()
} as TimerTask, 1000, 10800000)

@Log4j
class Spidey {

    def config = new ConfigSlurper().parse(new File('config.groovy').toURL())

    Spidey() {
        PropertyConfigurator.configure(config.toProperties())
        log.info "Begin spider _init_"
    }

    Sql db = getDbInstance()

    RESTClient slack = new RESTClient(config.jobbot.slackUrl)

    public void main() {

        doXpatJobsSpider()
        doStartup4meSpider()

    }

    public void doXpatJobsSpider() {

        log.info "Starting XpatJobsSpider..."

        GPathResult extractedXml = new XmlSlurper().parse(config.jobbot.xpatJobs.url)

        def existingJobs = getExistingJobTitles(db)

        def ads = extractedXml.channel.item.inject([]) { injected, item ->

            if (!(item.title.text() in existingJobs)) {
                def job = new JobAd()
                job.title = item.title.text()
                job.link = item.link.text()
                job.date = new Date()
                job.guid = item.guid.text()
                job.description = item.description.text()

                injected << job

            }

            return injected
        }

        _processInternal(ads)
    }

    public void doStartup4meSpider() {

        log.info "Starting Startup4meSpider..."

        GPathResult extractedXml = new XmlSlurper().parse(config.jobbot.startup4me.url)

        def existingJobs = getExistingJobTitles(db)

        def ads = extractedXml.channel.item.inject([]) { injected, item ->

            if (!(item.title.text() in existingJobs)) {
                def job = new JobAd()
                job.title = item.title.text()
                job.link = item.link.text()
                job.date = new Date(item.pubDate.text())
                job.guid = item.guid.text()
                job.description = item.description.text()

                injected << job

            }

            return injected
        }

        _processInternal(ads)

    }

    private void _processInternal(def jobs) {
        log.info "Found ${jobs.size()} new job ads..."

        jobs.each { job ->
            try {
                log.info "Sending message to slack for job => ${job.title}"
                sendSlackMessage(slack, job.getSlackMessage())
            }
            catch(Exception e) {
                log.error "Sending message to slack failed due to", e.message
            }
            insert(job, db)
        }
    }

    def sendSlackMessage(RESTClient slack, message) {
        return slack.post() {
            type ContentType.JSON
            text message
        }
    }

    Sql getDbInstance() {
        if (!new File(config.jobbot.db.name).exists()) {
            log.info "DB does not exist, creating..."

            return createDb()
        } else {
            log.info "DB already exists..."
            return Sql.newInstance(config.jobbot.db.url, config.jobbot.db.className)
        }
    }

    Sql createDb() {

        log.info "Creating db..."
        def db = Sql.newInstance(config.jobbot.db.url, config.jobbot.db.className)
        db.execute """
        create table jobs (
            id INTEGER PRIMARY KEY,
            title TEXT(512),
            link TEXT(512),
            guid TEXT(512),
            description TEXT(512),
            date DATETIME
        )"""
        return db
    }

    void insert(JobAd job, Sql db) {

        try {
            db.execute """
                insert into jobs values (null, ${job.title}, ${job.link}, ${job.guid}, ${job.description}, ${job.date})
            """
        } catch(e) {
            log.error "Could not insert job: ${job.title}, reason: ${e.message}"
            log.error "Skipping.."
        }
    }

    def getExistingJobTitles(Sql db) {
        return db.rows("select title from jobs;").collect { it.title }
    }
}
class JobAd {

    Integer id
    String title
    String link
    Date date
    String guid
    String description

    String getSlackMessage() {
        return """
            {
                'username': 'job-bot',
                'icon_emoji': ':monkey_face:',
                'attachments': [
                    {
                        'color': '#36a64f',
                        'title': 'New job: ${this.title}',
                        'text': '${Jsoup.parse(this.description).text()}',
                        'title_link': '${this.link}',
                        'ts': '${Date.getMillisOf(this.date)}'
                    }
                ]
            }
        """
    }

    @Override
    String toString(){
        return "title: ${this.title}".toString()
    }
}
