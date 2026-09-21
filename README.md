# Nusha Interpreter

A Java-based interpreter for the Nusha programming language, developed for ISCI 311.

## Features

- Lexical analysis and token generation
- Parsing into an abstract syntax tree
- Support for variables, structures, arrays, and rules
- Equality and inequality constraints
- Indentation-based rule blocks
- Backtracking constraint solver
- Syntax errors with line and column information
- JUnit tests for the lexer and parser

## Project Structure

- `Lexer.java` — Converts source code into tokens
- `NushaFall2025Parser.java` — Builds the abstract syntax tree
- `Interpreter.java` — Evaluates rules and produces solutions
- `TextManager.java` — Manages source-text traversal
- `TokenManager.java` — Manages the token stream
- `SyntaxErrorException.java` — Reports syntax errors
- `AST/` — Contains the abstract syntax tree classes
- Test files — Test the lexer, parser, and interpreter

## Requirements

- Java 22 or a compatible recent JDK
- IntelliJ IDEA
- JUnit 5.8.1

## Running the Project

1. Clone or download the repository.
2. Open the project in IntelliJ IDEA.
3. Select a compatible Java SDK.
4. Allow IntelliJ to load the JUnit dependency.
5. Run the included test classes.

## Testing

The project includes tests for:

- Keywords and identifiers
- Numbers and punctuation
- Indentation and dedentation
- Variable declarations
- Structures and arrays
- Rules and expressions
- Interpreter behavior****
