# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Overview

This is a Babashka-based tool for removing passwords from PDFs by retrieving credentials from 1Password. The typical use case is removing SSN/EIN-based passwords from tax documents or other financial PDFs.

## Architecture

The codebase is a single-namespace Babashka script with the following key components:

- **Configuration system** (`load-config!`): Reads `pdfunpassword.edn` (gitignored) which contains an array of 1Password entries to try. See `pdfunpassword.example.edn` for the expected structure.

- **1Password integration** (`get-onepassword-output!`, `get-password!`): Uses the `op` CLI to fetch credentials. Supports:
  - Account, vault, and item name specification
  - Custom field labels (defaults to "password")
  - Filter chains (e.g., `:digits` filter removes non-numeric characters to handle SSN/EIN formatting differences)

- **PDF processing** (`remove-password!`): Uses `qpdf` to decrypt PDFs in-place. First checks if the PDF requires a password using `qpdf --requires-password`. If the PDF is already passwordless, skips decryption. For password-protected PDFs, iterates through config entries until one succeeds, throwing an error if all fail.

- **Main entry point** (`-main`): Processes all `*.pdf` files in specified directories (supports shell expansion via `~`). Supports recursive directory traversal via `--recursive` flag.

## Running the Tool

```bash
# Process PDFs in specified directories (non-recursive by default)
bb go ~/path/to/pdfs ~/another/dir

# Process PDFs recursively through all subdirectories
bb go --recursive ~/path/to/pdfs

# Disable recursion explicitly (same as default behavior)
bb go --recursive=false ~/path/to/pdfs
```

The `go` task in `bb.edn` invokes the main function with command-line arguments. CLI parsing is handled by `babashka.cli/parse-args` with the following options:

- `--recursive` (boolean, default: false): When enabled, searches for PDFs recursively in all subdirectories.

## Configuration

Create `pdfunpassword.edn` in the project root (copy from `pdfunpassword.example.edn`):

```clojure
[{:account "company.1password.com"
  :vault "Operations"
  :name "Company EIN"
  :field-label "number"
  :filters [:digits]}]
```

Each entry specifies a 1Password item to try. The tool tries entries sequentially until one successfully decrypts the PDF.

## Dependencies

- **Babashka**: The runtime environment
- **qpdf**: PDF manipulation tool (installable via Homebrew)
- **1Password CLI (`op`)**: Must be installed and authenticated

The tool will attempt to install qpdf via Homebrew if not present (implied by README).

## Project Structure

- `src/io/lvh/pdfunpassword.clj` - Main implementation (single namespace)
- `bb.edn` - Babashka task definitions
- `pdfunpassword.edn` - Local config (gitignored, contains sensitive 1Password references)
- `pdfunpassword.example.edn` - Example configuration structure

## Testing

Run tests with:

```bash
bb test
```

Tests are located in `test/io/lvh/pdfunpassword_test.clj`.
