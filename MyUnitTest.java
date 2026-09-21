import AST.Token;
import AST.Nusha;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

public class MyUnitTest {

    @Test
    public void a1_basicIdentifiersAndKeywords() throws Exception {
        String code = "var a\nunique b\nc";
        LinkedList<Token> tokens = new Lexer(code).Lex();

        Assertions.assertEquals(Token.TokenTypes.VAR, tokens.get(0).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(1).Type);
        Assertions.assertEquals("a", tokens.get(1).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(2).Type);

        Assertions.assertEquals(Token.TokenTypes.UNIQUE, tokens.get(3).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(4).Type);
        Assertions.assertEquals("b", tokens.get(4).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(5).Type);

        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(6).Type);
        Assertions.assertEquals("c", tokens.get(6).Value.orElseThrow());

        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(7).Type);
    }

    @Test
    public void a1_whitespaceOnlyProducesNoTokens() throws Exception {
        String code = " \t   \r   \t  ";
        LinkedList<Token> tokens = new Lexer(code).Lex();
        Assertions.assertEquals(0, tokens.size());
    }

    @Test
    public void a1_handlesCRLFAndLFNewlines() throws Exception {
        String code = "Alpha\r\nBeta\nGamma";
        LinkedList<Token> tokens = new Lexer(code).Lex();

        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(0).Type);
        Assertions.assertEquals("Alpha", tokens.get(0).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(1).Type);

        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(2).Type);
        Assertions.assertEquals("Beta", tokens.get(2).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(3).Type);

        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(4).Type);
        Assertions.assertEquals("Gamma", tokens.get(4).Value.orElseThrow());

        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(5).Type);
    }


    @Test
    public void a2_punctuationAndNumbers() throws Exception {
        String code = "A = {B, C}\nvar D : E[4]\n";
        LinkedList<Token> tokens = new Lexer(code).Lex();

        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(0).Type);
        Assertions.assertEquals("A", tokens.get(0).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.EQUAL, tokens.get(1).Type);
        Assertions.assertEquals(Token.TokenTypes.LEFTCURLY, tokens.get(2).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(3).Type);
        Assertions.assertEquals("B", tokens.get(3).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.COMMA, tokens.get(4).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(5).Type);
        Assertions.assertEquals("C", tokens.get(5).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.RIGHTCURLY, tokens.get(6).Type);
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(7).Type);

        Assertions.assertEquals(Token.TokenTypes.VAR, tokens.get(8).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(9).Type);
        Assertions.assertEquals("D", tokens.get(9).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.COLON, tokens.get(10).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(11).Type);
        Assertions.assertEquals("E", tokens.get(11).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.LEFTBRACE, tokens.get(12).Type);
        Assertions.assertEquals(Token.TokenTypes.NUMBER, tokens.get(13).Type);
        Assertions.assertEquals("4", tokens.get(13).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.RIGHTBRACE, tokens.get(14).Type);
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(15).Type);

        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(16).Type);
    }

    @Test
    public void a2_dotIndexingNotequalAndIndentedBlock() throws Exception {
        String code =
                "X[0].f = Y =>\n" +
                        "    X.g != Z\n";
        LinkedList<Token> tokens = new Lexer(code).Lex();

        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(0).Type);
        Assertions.assertEquals("X", tokens.get(0).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.LEFTBRACE, tokens.get(1).Type);
        Assertions.assertEquals(Token.TokenTypes.NUMBER, tokens.get(2).Type);
        Assertions.assertEquals("0", tokens.get(2).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.RIGHTBRACE, tokens.get(3).Type);
        Assertions.assertEquals(Token.TokenTypes.DOT, tokens.get(4).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(5).Type);
        Assertions.assertEquals("f", tokens.get(5).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.EQUAL, tokens.get(6).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(7).Type);
        Assertions.assertEquals("Y", tokens.get(7).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.YIELDS, tokens.get(8).Type);
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(9).Type);

        Assertions.assertEquals(Token.TokenTypes.INDENT, tokens.get(10).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(11).Type);
        Assertions.assertEquals("X", tokens.get(11).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.DOT, tokens.get(12).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(13).Type);
        Assertions.assertEquals("g", tokens.get(13).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, tokens.get(14).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(15).Type);
        Assertions.assertEquals("Z", tokens.get(15).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(16).Type);

        Assertions.assertEquals(Token.TokenTypes.DEDENT, tokens.get(17).Type);
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(18).Type);
    }

    @Test
    public void a2_multiLevelIndentAndDedentAtEOF() throws Exception {
        String code =
                "A =>\n" +
                        "    B =>\n" +
                        "        C = D";
        LinkedList<Token> tokens = new Lexer(code).Lex();

        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(0).Type);
        Assertions.assertEquals("A", tokens.get(0).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.YIELDS, tokens.get(1).Type);
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(2).Type);

        Assertions.assertEquals(Token.TokenTypes.INDENT, tokens.get(3).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(4).Type);
        Assertions.assertEquals("B", tokens.get(4).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.YIELDS, tokens.get(5).Type);
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(6).Type);

        Assertions.assertEquals(Token.TokenTypes.INDENT, tokens.get(7).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(8).Type);
        Assertions.assertEquals("C", tokens.get(8).Value.orElseThrow());
        Assertions.assertEquals(Token.TokenTypes.EQUAL, tokens.get(9).Type);
        Assertions.assertEquals(Token.TokenTypes.IDENTIFIER, tokens.get(10).Type);
        Assertions.assertEquals("D", tokens.get(10).Value.orElseThrow());

        Assertions.assertEquals(Token.TokenTypes.DEDENT, tokens.get(11).Type);
        Assertions.assertEquals(Token.TokenTypes.DEDENT, tokens.get(12).Type);
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(13).Type);
    }

    @Test
    public void parser_noDeclarations_ok() throws Exception {
        String code = "\n   \n\n";
        LinkedList<Token> tokens = new Lexer(code).Lex();

        Nusha ast = new NushaFall2025Parser().Nusha(tokens).orElseThrow();
        Assertions.assertNotNull(ast);
        Assertions.assertNotNull(ast.variables);
        Assertions.assertEquals(0, ast.variables.variable.size());
    }

    @Test
    public void parser_singleVar_withoutSize() throws Exception {
        String code = "var X : T\n";
        LinkedList<Token> tokens = new Lexer(code).Lex();

        Nusha ast = new NushaFall2025Parser().Nusha(tokens).orElseThrow();
        Assertions.assertEquals(1, ast.variables.variable.size());
        Assertions.assertEquals("X", ast.variables.variable.get(0).variableName);
        Assertions.assertEquals("T", ast.variables.variable.get(0).type);
        Assertions.assertTrue(ast.variables.variable.get(0).size.isEmpty());
    }
}