import AST.*;
import java.util.Optional;
import java.util.LinkedList;

public class NushaFall2025Parser {
    private TokenManager tm;
    public NushaFall2025Parser() { }

    public Optional<Nusha> Nusha(LinkedList<Token> tokens) throws SyntaxErrorException {
        this.tm = new TokenManager(tokens); //create token manager for token stream
        Nusha root = new Nusha(); //root AST node
        skipSeperators();
        root.definitions = parseDefenitionsSection();
        skipSeperators();
        root.variables = parseVariablesSection();
        skipSeperators();
        root.rules = parseRulesSection();
        return Optional.of(root);
    }
    private Definitions parseDefenitionsSection() throws SyntaxErrorException {
        Definitions defs = new Definitions();
        defs.definition = new LinkedList<>();
        while(true) {
            Optional<Token> nameTok = tm.MatchAndRemove(Token.TokenTypes.IDENTIFIER);
            if (!nameTok.isPresent()) break; //stop when no more definitions
            if (!nameTok.get().Value.isPresent()) {
                throw new SyntaxErrorException("Expected definition name", tm.getCurrentLine(), tm.getCurrentColumnNumber());
            }
            String defName = nameTok.get().Value.get();
            requireToken(Token.TokenTypes.EQUAL, "Expected '=' after definition name");
            Definition d = new Definition();
            d.definitionName = defName;
            if(tm.MatchAndRemove(Token.TokenTypes.LEFTCURLY).isPresent()) {
                Choices ch = new Choices();
                ch.choice = new LinkedList<>();
                ch.choice.add(readRequiredIdentifier("Expected identifier in choices"));
                while (tm.MatchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                    ch.choice.add(readRequiredIdentifier("Expected identifier after ',' in choices"));
                }
                requireToken(Token.TokenTypes.RIGHTCURLY, "Expected '}' after choices");
                d.choices = Optional.of(ch);
                d.nstruct = Optional.empty();
            }
            else if (tm.MatchAndRemove(Token.TokenTypes.LEFTBRACE).isPresent()) {
                NStruct ns = new NStruct();
                ns.entry = new LinkedList<>();
                if (!tm.MatchAndRemove(Token.TokenTypes.RIGHTBRACE).isPresent()) { //allow empty struct
                    ns.entry.add(parseStructEntry());
                    while (tm.MatchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                        ns.entry.add(parseStructEntry());
                    }
                    requireToken(Token.TokenTypes.RIGHTBRACE, "Expected ']' after struct");
                }
                d.nstruct = Optional.of(ns);
                d.choices = Optional.empty();
            } else {
                throw new SyntaxErrorException("Expected '{' or '[' after '='", tm.getCurrentLine(), tm.getCurrentColumnNumber());
            }
            defs.definition.add(d);
            skipSeperators();
        }
        return defs;
    }
    private Entry parseStructEntry() throws SyntaxErrorException {
        Entry entry = new Entry();
        entry.unique = tm.MatchAndRemove(Token.TokenTypes.UNIQUE).isPresent();
        entry.type = readRequiredIdentifier("Expected type identifier in struct entry");
        entry.name = readRequiredIdentifier("Expected name identifier in struct entry");
        return entry;
    }
    private Variables parseVariablesSection() throws SyntaxErrorException {
        Variables vars = new Variables(); //list to hold variables
        vars.variable = new LinkedList<>();
        RequireNewLine(); //swallow layout
        while (true) {
            Optional<Variable> maybeVar = VarDecl(); //try parse one variable
            if (!maybeVar.isPresent()) break; //stop in no variable found
            vars.variable.add(maybeVar.get()); //add variable to list
            RequireNewLine();
        }
        return vars; //return collected variables
    }

    private Optional<Variable> VarDecl() throws SyntaxErrorException {
        if (!tm.MatchAndRemove(Token.TokenTypes.VAR).isPresent()) {
            return Optional.empty();
        }
        String name = readRequiredIdentifier("Expected variable name after 'var'."); //variable name
        requireToken(Token.TokenTypes.COLON, "Expected ':' after variable name."); //colon required
        String typeName = readRequiredIdentifier("Expected type name after ':'."); //type name
        Optional<String> size = Optional.empty();
        if (tm.MatchAndRemove(Token.TokenTypes.LEFTBRACE).isPresent()) { //check for [
            String num = readRequiredNumber("Expected number inside '[' ... ']'.");
            size = Optional.of(num);
            requireToken(Token.TokenTypes.RIGHTBRACE, "Expected ']' after size."); //require ]
        }
        Variable v = new Variable(); //AST variable
        v.variableName = name;
        v.type = typeName;
        v.size = size;
        return Optional.of(v);
    }
    private Rules parseRulesSection() throws SyntaxErrorException {
        Rules rules = new Rules();
        rules.rule = new LinkedList<>();
        skipSeperators();
        while (true) {
            Optional<Token> head = tm.MatchAndRemove(Token.TokenTypes.IDENTIFIER);
            if (!head.isPresent()) break;
            if (!head.get().Value.isPresent()) {
                throw new SyntaxErrorException("Expected identifier", tm.getCurrentLine(), tm.getCurrentColumnNumber());
            }
            String first = head.get().Value.get();
            Rule rule  = new Rule();
            rule.expression = parseExpressionStartingWith(first);
            rule.thens = new LinkedList<>();
            if (tm.MatchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) { //simple assertion rule
            } else if (tm.MatchAndRemove(Token.TokenTypes.YIELDS).isPresent()) {
                requireToken(Token.TokenTypes.NEWLINE, "Expected newline after '=>'");
                requireToken(Token.TokenTypes.INDENT, "Expected indent after '=>'");
                while (true) {
                    while (tm.MatchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {}
                    if (tm.MatchAndRemove(Token.TokenTypes.DEDENT).isPresent()) break;
                    Optional<Token> t = tm.MatchAndRemove(Token.TokenTypes.IDENTIFIER);
                    if (!t.isPresent() || !t.get().Value.isPresent()) {
                        throw new SyntaxErrorException("Expected identifier in consequence", tm.getCurrentLine(), tm.getCurrentColumnNumber());
                    }
                    String start = t.get().Value.get();
                    Expression thenExpr = parseExpressionStartingWith(start);
                    rule.thens.add(thenExpr);
                    requireToken(Token.TokenTypes.NEWLINE, "Expected newline after consequence");
                }
                    while (tm.MatchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {}
                } else {
                    throw new SyntaxErrorException("Expected NEWLINE or '=>'", tm.getCurrentLine(), tm.getCurrentColumnNumber());
                }
                rules.rule.add(rule);
                skipSeperators();
            }
            return rules;
        }
    private Expression parseExpressionStartingWith(String first) throws SyntaxErrorException {
        Expression expression = new Expression();
        expression.left = parseVRWithHead(first);
        expression.op = parseOp();
        String rhs = readRequiredIdentifier("Expected identifier on right-hand side");
        VariableReference r = new VariableReference();
        r.variableName = rhs;
        r.vrmodifier = Optional.empty();
        expression.right = r;
        return expression;
    }
    private VariableReference parseVRWithHead(String name) throws SyntaxErrorException {
        VariableReference vr = new VariableReference();
        vr.variableName = name;
        vr.vrmodifier = Optional.empty();
        VRModifier last = null;
        if (tm.MatchAndRemove(Token.TokenTypes.LEFTBRACE).isPresent()) {
            VRModifier idx = new VRModifier();
            idx.size = readRequiredNumber("Expected number in index");
            idx.dot = false;
            idx.part = Optional.empty();
            idx.vrmodifier = Optional.empty();
            requireToken(Token.TokenTypes.RIGHTBRACE, "Expected ']' after index");
            vr.vrmodifier = Optional.of(idx);
            last = idx;
        }
        if (tm.MatchAndRemove(Token.TokenTypes.DOT).isPresent()) {
            VRModifier field = new VRModifier();
            field.size = null;
            field.dot = true;
            field.part = Optional.of(readRequiredIdentifier("Expected field name after '.'"));
            field.vrmodifier = Optional.empty();
            if (last == null) vr.vrmodifier = Optional.of(field);
            else last.vrmodifier = Optional.of(field);
        }
        return vr;
    }
    private Op parseOp() throws SyntaxErrorException {
        Op op = new Op();
        if (tm.MatchAndRemove(Token.TokenTypes.EQUAL).isPresent()) {
            op.type = Op.OpTypes.Equal;
        } else if (tm.MatchAndRemove(Token.TokenTypes.NOTEQUAL).isPresent()) {
            op.type = Op.OpTypes.NotEqual;
        } else {
            throw new SyntaxErrorException("Expected '=' or '!='", tm.getCurrentLine(), tm.getCurrentColumnNumber());
        }
        return op;
    }
    private void skipSeperators() {
        while (true) {
            boolean ate = false;
            if (tm.MatchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) ate = true;
            if (tm.MatchAndRemove(Token.TokenTypes.INDENT).isPresent())  ate = true;
            if (tm.MatchAndRemove(Token.TokenTypes.DEDENT).isPresent())  ate = true;
            if (!ate) break;
        }
    }
    private void RequireNewLine() {
        boolean consumedAny = false;
        while (true) { //track if consumed
            if (tm.MatchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
                consumedAny = true;
                continue;
            }
            if (tm.MatchAndRemove(Token.TokenTypes.INDENT).isPresent()) { consumedAny = true; continue; }
            if (tm.MatchAndRemove(Token.TokenTypes.DEDENT).isPresent()) { consumedAny = true; continue; }
            break; //exit when no more mtches
        }
    }
    private void requireToken(Token.TokenTypes t, String message) throws SyntaxErrorException {
        if (!tm.MatchAndRemove(t).isPresent()) {
            throw new SyntaxErrorException(message, tm.getCurrentLine(), tm.getCurrentColumnNumber());
        }
    }
    //read an identifier token or throw error
    private String readRequiredIdentifier(String message) throws SyntaxErrorException {
        Optional<Token> id = tm.MatchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (!id.isPresent() || !id.get().Value.isPresent()) {
            throw new SyntaxErrorException(message, tm.getCurrentLine(), tm.getCurrentColumnNumber());
        }
        return id.get().Value.get();
    }
    //read number or thorw error
    private String readRequiredNumber(String message) throws SyntaxErrorException {
        Optional<Token> n = tm.MatchAndRemove(Token.TokenTypes.NUMBER);
        if (!n.isPresent() || !n.get().Value.isPresent()) {
            throw new SyntaxErrorException(message, tm.getCurrentLine(), tm.getCurrentColumnNumber());
        }
        return n.get().Value.get();
    }
}
