public class TextManager { //manages input text and current position
    private final String text; //full input string
    private int pos = 0;

    public TextManager(String input) { //constructor take input
        this.text = (input ==null) ? "" : input;
    }

    public boolean isAtEnd() { //true if all characters are read
        return pos >= text.length(); //compares position to length
    }

    public char PeekCharacter() { //look at current character
        if (isAtEnd()) return '\0';
        return text.charAt(pos);//return char at character position
    }

    public char PeekCharacter(int dist) {
        int idx = pos + dist; //compute target index
        if (idx < 0 || idx >= text.length()) return '\0';
        return text.charAt(idx); //return char at that index
    }

    public char GetCharacter() { //get current character and advance
        if (isAtEnd()) return '\0';
        return text.charAt(pos++); //return char then increment position
    }
}
