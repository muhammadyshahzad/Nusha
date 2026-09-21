import AST.Token;
import java.util.LinkedList;
import java.util.Optional;

public class TokenManager {
    private final LinkedList<Token> tokens;

    public TokenManager(LinkedList<Token> tokens) {
        this.tokens= (tokens == null) ? new LinkedList<>() : new LinkedList<>(tokens); //copy tokens or empty list
    }
    public int getCurrentLine() {
        if (tokens.isEmpty()) return 0;
        return tokens.peekFirst().LineNumber; //get line number from first token
    }
    public int getCurrentColumnNumber() {
        if (tokens.isEmpty()) return 0;
        return tokens.peekFirst().ColumnNumber; //get column number from first token
    }

    public int getLine() {
        return getCurrentLine();
    }

    public int getColumn() {
        return getCurrentColumnNumber();
    }

    public boolean Done() { //check if no tokens left
        return tokens.isEmpty(); //true if list empty
    }

    public Optional<Token> MatchAndRemove(Token.TokenTypes t) {
        if (tokens.isEmpty()) return Optional.empty();
        Token next = tokens.peekFirst(); // look at first token
        if (next.Type == t) { //check type match
            return Optional.of(tokens.removeFirst()); //remove and return
        }
        return Optional.empty();
    }

    public Optional<Token> Peek (int i) {
        if (i < 0 || i >= tokens.size()) return Optional.empty(); //check bounds
        return Optional.of(tokens.get(i)); //return token at index i
    }
}
