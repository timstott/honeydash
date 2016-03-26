# honeydash

A Honeybadger dashboard for teams

## Features

- Multiple projects faults in one page
- Supports filtering by tag
- Project specific configuration

## Setup

## Development

Dependencies:

- [leiningen](http://leiningen.org/)
- phantomjs 2.0 (optional)

To get an interactive development environment run:

```
bin/boot
```

This will install dependencies, auto compile and send all changes to the browser
without the need to reload.
Open your browser at [localhost:3449](http://localhost:3449/).

Tests automatically run in the browser console when the source updates.

Alternatively you can run the tests in the terminal with (phantomjs required):

```
bin/test
```
