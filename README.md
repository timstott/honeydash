# honeydash

A Honeybadger dashboard for teams

![honeydash](https://cloud.githubusercontent.com/assets/1334474/14407554/7e04f46e-fec5-11e5-9542-7b323bbe5eb0.jpg)

## Features

- Aggregates multiple projects faults on one page
- Supports filtering by tag
- Project specific configuration

## How to Use

WIP
`https://localhost?auth_token=_honeybadger_token_&gist_id=_gist_id_&order_by=_method_`

Query parameters:

- `auth_token`: Honeybadger user token available at https://app.honeybadger.io/users/edit
- `gist_id`: Projects configuration Gist id
- `order_by`: Sort faults by highest number of occurrence with `count` or by most recent occurrence with `recent`


Projects configuration:

Honeydash fetches Honeybadger project faults based on the configuration specified in a Gist.

The Gist must include one JSON file with the following syntax:
```json
[
  {
    "id": 9009,
    "name": "Data Warehouse",
    "tags": []
  },
  {
    "id": 9010,
    "name": "Data Processor",
    "tags": ["SPLIT A", "SPLIT B"]
  }
]
```

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
