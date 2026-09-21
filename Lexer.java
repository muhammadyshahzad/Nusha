import AST.*;

import java.util.*;

public class Lexer {
    private final TextManager text; //handles raw text
    private int line = 0; //current line number
    private int column = 0; //current column number
    private boolean atLineStart = true; //true at start of line
    private final Deque<Integer> indentStack = new ArrayDeque<>(); //stack of indentation levels
    private final Map<String, Token.TokenTypes> keywords;//keyword table
    private final Map<String, Token.TokenTypes> punctuation; //punctuation/operation table
    private boolean producedAnyToken = false; //tracks if at least one token is produced

    public Lexer(String input) {
        this.text = new TextManager(input == null ? "" : input);
        this.keywords = new HashMap<String, Token.TokenTypes>(); //creates keyword map
        this.punctuation = new HashMap<String, Token.TokenTypes>(); //creates punctuation map
        indentStack.push(0);
        keywords.put("var", Token.TokenTypes.VAR); //var is keyword
        keywords.put("unique", Token.TokenTypes.UNIQUE);//unique is keyword
        //punctuation/operations
        punctuation.put("=", Token.TokenTypes.EQUAL);
        punctuation.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuation.put("=>", Token.TokenTypes.YIELDS);
        punctuation.put("{", Token.TokenTypes.LEFTCURLY);
        punctuation.put("}", Token.TokenTypes.RIGHTCURLY);
        punctuation.put("[", Token.TokenTypes.LEFTBRACE);
        punctuation.put("]", Token.TokenTypes.RIGHTBRACE);
        punctuation.put(",", Token.TokenTypes.COMMA);
        punctuation.put(":", Token.TokenTypes.COLON);
        punctuation.put(".", Token.TokenTypes.DOT);

    }

    public LinkedList<Token> Lex() throws SyntaxErrorException {
        LinkedList<Token> tokens = new LinkedList<>(); //output the list of tokens
        while (!text.isAtEnd()) { //loop until end
            if (atLineStart) { //handle indentation
                handleIndentation(tokens);
                atLineStart = false;
            }
            char c = text.PeekCharacter(); //looks at current character

            if (c == '\n' || c == '\r' && text.PeekCharacter(1) == '\n') {
                consumeNewLine(tokens); //emit NEWLINE and move to next line
                atLineStart = true;
                continue;
            }
            if (c =='\r'){
                consumeChar();
                continue;
            }
            if (c == ' ' || c == '\t') { //skip whitespace
                consumeWhiteSpaceInLine();
                continue;
            }
            if (Character.isLetter(c)) { //start of a word, identifier or keyword
                tokens.add(readWord());
                producedAnyToken = true;
                continue;
            }
            if (Character.isDigit(c) || c == '.' && Character.isDigit(text.PeekCharacter(1))) {
                tokens.add(readNumber());
                producedAnyToken = true;
                continue;
            }
            Token p = readPunctuation();
            if (p != null) {
                tokens.add(p);
                producedAnyToken = true;
                continue;
            }
            throw new SyntaxErrorException("Unknown character: '" + c + "'", line, column);
        }
        while (indentStack.size() > 1) { //unwind any remaining indentation back to 0
            indentStack.pop(); //pop one level
            tokens.add(new Token(Token.TokenTypes.DEDENT, line, column, Optional.empty()));
            producedAnyToken = true; //marks that a token is produced
        }
        if (producedAnyToken) {
            tokens.add(new Token(Token.TokenTypes.NEWLINE, line, column, Optional.empty()));
        }
        return tokens; //return all collected tokens
    }

    private Token makeIdentifier(String word) { //build IDENTIFIER token with value
        return new Token(Token.TokenTypes.IDENTIFIER, line, column, Optional.of(word)); //return token
    }

    private Token makeKeyword(Token.TokenTypes type) { //build keyword token
        return new Token(type, line, column, Optional.empty());
    }

    private Token makeNumber(String numText) {
        return new Token(Token.TokenTypes.NUMBER, line, column, Optional.of(numText));
    }

    private Token readWord() {
        StringBuilder sb = new StringBuilder(); //build the word text
        sb.append(consumeChar()); //consume first letter
        while (!text.isAtEnd()) { //while not at end keep reading
            char c = text.PeekCharacter(); //look at next character
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(consumeChar());
            } else {
                break; //stop when non-word char is found
            }
        }
        String word = sb.toString();

        Token.TokenTypes kw = keywords.get(word); //checks if the word is a keyword
        if (kw != null) return makeKeyword(kw);
        return makeIdentifier(word);
    }

    private Token readNumber() throws SyntaxErrorException {
        StringBuilder sb = new StringBuilder();
        boolean seenDot = false; //track if dot is already used
        if (text.PeekCharacter() == '.') {
            seenDot = true; //marks a dot has been seen
            sb.append(consumeChar());
        }
        while (Character.isDigit(text.PeekCharacter())) sb.append(consumeChar());
        if (text.PeekCharacter() == '.') {
            if (seenDot) throw new SyntaxErrorException("Multiple '.' in number", line, column);
            seenDot = true;
            sb.append(consumeChar());
            if (!Character.isDigit(text.PeekCharacter())) //dot must be followed by a digit
                throw new SyntaxErrorException("Number cannot end with '.'", line, column);
            while (Character.isDigit(text.PeekCharacter())) sb.append(consumeChar());
        }
        if (text.PeekCharacter() == '.')
            throw new SyntaxErrorException("Multiple '.' in number", line, column);
        return makeNumber(sb.toString()); //return NUMBER token with text
    }

    private Token readPunctuation() {
        char c1 = text.PeekCharacter(); //first char
        char c2 = text.PeekCharacter(1); //second char
        String two = "" + c1 + c2;
        if (punctuation.containsKey(two)) {
            Token.TokenTypes type = punctuation.get(two);
            int startColumn = column;
            consumeChar();
            consumeChar();
            return new Token(type, line, startColumn, Optional.of(two)); //return token at start position
        }
        String one = "" + c1;
        if (punctuation.containsKey(one)) {
            Token.TokenTypes type = punctuation.get(one);
            int startColumn = column;
            consumeChar();
            return new Token(type, line, startColumn, Optional.of(one));
        }
        return null; //not punctuation
    }

    private void consumeWhiteSpaceInLine() {
        while (!text.isAtEnd()) { //loop until non-space
            char c = text.PeekCharacter();
            if (c == ' ') { //space is 1 column
                consumeChar();
            } else if (c == '\t') { //tab counts as 4 columns
                consumeChar();
                column += 3;
            } else {
                break; //stop at first non-space/tab
            }
        }
    }

    private void consumeNewLine(LinkedList<Token> tokens) {
        if (text.PeekCharacter() == '\r') {
            consumeChar();
            if (text.PeekCharacter() == '\n') {
                consumeChar();
            }
        } else {
            consumeChar();
        }
        tokens.add(new Token(Token.TokenTypes.NEWLINE, line, column, Optional.empty()));
        producedAnyToken = true;
        line += 1; //advance to next line
        column = 0; //reset column at line start
    }
    private void handleIndentation(LinkedList<Token> tokens) throws SyntaxErrorException {
        int spaces = 0;
        while (!text.isAtEnd()) {
            char c = text.PeekCharacter();
            if (c == ' ') {
                consumeChar();
                spaces += 1;
            } else if (c == '\t') {
                consumeChar();
                spaces += 4;
                column += 3;
            } else {
                break;
            }
        }
        char next = text.PeekCharacter();
        if (next == '\n' || next == '\r' || text.isAtEnd()) return;
        if (spaces % 4 != 0) {
            throw new SyntaxErrorException("Indentation must be a multiple of 4", line, column);
        }

        int level = spaces / 4; //target indent level
        int current = indentStack.peek();

        if (level > current) { //increased indentation
            for (int i = current; i < level; i++) {
                indentStack.push(i + 1); //push new level
                tokens.add(new Token(Token.TokenTypes.INDENT, line, column, Optional.empty()));
                producedAnyToken = true;
            }
        } else if (level < current) { //decreased indentation
            while (indentStack.peek() > level) {
                indentStack.pop();
                tokens.add(new Token(Token.TokenTypes.DEDENT, line, column, Optional.empty()));
                producedAnyToken = true;
            }
        }
    }

    private char consumeChar() { //get current character and advance
        char got =  text.GetCharacter(); //consume one character from TextManager
        column += 1;
        return got; //return consumed character
    }
}
