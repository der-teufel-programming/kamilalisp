package palaiologos.kamilalisp.runtime.string;

import org.apache.commons.text.StringEscapeUtils;
import palaiologos.kamilalisp.atom.Atom;
import palaiologos.kamilalisp.atom.Environment;
import palaiologos.kamilalisp.atom.Lambda;
import palaiologos.kamilalisp.atom.PrimitiveFunction;

import java.util.List;

public class Upper extends PrimitiveFunction implements Lambda {

    @Override
    public Atom apply(Environment env, List<Atom> args) {
        assertArity(args, 1);
        String s = args.get(0).getString();
        return new Atom(s.toUpperCase());
    }

    @Override
    protected String name() {
        return "str:upper";
    }
}
