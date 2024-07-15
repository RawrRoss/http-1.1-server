# Example: Fun Website

Adapted and improved from an old school project. A simple website containing some entertaining interactive pages.

## Features

- Programmatically-generated HTML pages
- Server-side input processing
- Static file serving

### Fortune

Use a provided name and age to select a fortune from a predefined list. Each unique input generates the same fortune each time, which is accomplished by calculating its MD5 hash as an index to list of fortunes.

### Pokemon Fusion

Select two Pokemon types, and create a fusion between two random Pokemon of the chosen types. Uses images from Alex Onsager's [Pokemon Fusion](https://pokemon.alexonsager.net/) website.

## Running

Execute `gradlew :examples:fun-website:run` in the repository root.

Website starts on port `8080`
