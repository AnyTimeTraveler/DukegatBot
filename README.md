
# Dukegat Teestuben Bot

## Building and Deploying

Run `mvn clean compile assembly:single` to get a jar-with-dependencies.

Run that with `java -jar dukegat-bot-1.0-jar-with-dependencies.jar`

It'll generate a config file if none is present.

Fill out the config file and start the bot again.

That's it.

## Commands

`/status closed` Disables timer and manually sets status to closed

`/status open` Disables timer and manually sets status to open

`/status auto` (Re-)Enables timer

`/here` set Channel to send the updates to

`/update` Forces an update manually

`/ping` Replies with Pong!

## License
MIT