package kamilalisp.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private InputStream input;
    private int buffer = -1;

    public Lexer(InputStream input) {
        this.input = input;
    }

    private String readNumber(int c) throws IOException {
        String s = "";

        while((Character.isDigit(c) || c == '.') && input.available() > 0) {
            s += (char) c;
            c = input.read();
        }

        buffer = c;

        return s;
    }

    public List<Token> getTokens() throws IOException {
        List<Token> tokens = new ArrayList<Token>();

        while(input.available() > 0 || buffer != -1) {
            int c;
            if(buffer != -1) { c = buffer; buffer = -1; }
            else { c = input.read(); }
            switch(c) {
                case ' ': case '\t': case '\n': case '\f': break;
                case '(': tokens.add(new Token(TokenType.LP, null)); break;
                case ')': tokens.add(new Token(TokenType.RP, null)); break;
                case '[': tokens.add(new Token(TokenType.LS, null)); break;
                case ']': tokens.add(new Token(TokenType.RS, null)); break;
                case '\'': tokens.add(new Token(TokenType.QUOT, null)); break;
                case '#': c = input.read(); tokens.add(new Token(TokenType.NTH, readNumber(c))); break;
                case '$': c = input.read(); tokens.add(new Token(TokenType.TACK, readNumber(c))); break;
                case ';': while(c != '\n' && input.available() > 0) { c = input.read(); } break;
                case '"': {
                    String s = "";

                    c = input.read();

                    while (c != '"' && input.available() > 0) {
                        s += (char) c;
                        c = input.read();
                    }

                    tokens.add(new Token(TokenType.STR, s));
                    break;
                }
                default:
                    if(Character.isDigit(c)) {
                        tokens.add(new Token(TokenType.NUM, readNumber(c)));
                    } else {
                        String s = "";

                        while((!Character.isSpaceChar(c) && "()[];".indexOf(c) == -1) && input.available() > 0) {
                            s += (char) c;
                            c = input.read();
                        }

                        buffer = c;

                        if(s.equals("nil"))
                            tokens.add(new Token(TokenType.NIL, null));
                        else
                            tokens.add(new Token(TokenType.ID, s));
                    }
            }
        }

        return tokens;
    }
}
