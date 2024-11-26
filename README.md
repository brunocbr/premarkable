# Premarkable

![](./typewriter-logo.png)

## A Simple Markdown Previewer

Premarkable is a lightweight Markdown previewer inspired by the excellent Marked App for the Mac. It allows you to view your Markdown files rendered as HTML in real-time, making it easy to write and preview your content.

### Features

- Real-time preview of Markdown files.
- Customizable CSS for styling the preview.
- Support for Pandoc to convert Markdown to HTML.
- Simple to use command-line interface.

### Installation

1. **Install [Babashka](https://github.com/babashka/babashka)**: Premarkable requires Babashka to run. Follow the instructions on the Babashka GitHub page to install it.

2. **Install Pandoc**: Ensure that you have Pandoc installed. You can download it from [the Pandoc website](https://pandoc.org/installing.html).

### Usage

To run Premarkable, use the command line. Here’s a basic invocation:

```bash
bb premarkable.clj <path/to/your/markdown/file.md>
```

#### Command-Line Options

- `-S, --css CSS`  
  Path to a CSS file for styling the preview.

- `-P, --processor CMD`  
  Command and arguments for the processor (default: `"env pandoc -f markdown -t html -s"]`).

- `-p, --http-port PORT`  
  Port number for the HTTP server (default: `8082`).

- `-w, --max-width N`  
  Limit the text width in the preview (in pixels).

- `-h, --help`  
  Show help information.

### Example

To start a preview with a custom CSS file:

```bash
bb premarkable.clj -S /path/to/your/styles.css -p 8082 /path/to/your/markdown/file.md
```

Open your web browser and navigate to `http://localhost:8082/` to see your Markdown file rendered as HTML.

### Monitoring File Changes

Premarkable will automatically detect changes to your Markdown file every 5 seconds, refreshing the preview content as you edit.

### Contributing

If you would like to contribute to Premarkable, feel free to fork the repository and submit a pull request. Suggestions and improvements are welcome!

#### To do

- Implement automatic refreshing via Websockets.

### License

Premarkable is released under the MIT License. See the [LICENSE](LICENSE) file for more information.

### Acknowledgments

Inspired by [Marked App](https://marked2app.com/), Premarkable brings an open-source alternative for Markdown previewing that runs on many platforms.
