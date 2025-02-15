package palaiologos.kamilalisp.runtime.math;

import ch.obermuhlner.math.big.BigComplex;
import ch.obermuhlner.math.big.BigComplexMath;
import ch.obermuhlner.math.big.BigDecimalMath;
import palaiologos.kamilalisp.atom.Atom;
import palaiologos.kamilalisp.atom.Environment;
import palaiologos.kamilalisp.atom.Lambda;
import palaiologos.kamilalisp.atom.PrimitiveFunction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Log2 extends PrimitiveFunction implements Lambda {
    private static Atom ln(Environment e, Atom a) {
        switch (a.getType()) {
            case LIST:
                ArrayList<Atom> list = new ArrayList<>(a.getList().size());
                for (Atom x : a.getList()) {
                    Atom ln = ln(e, x);
                    list.add(ln);
                }
                return new Atom(list);
            case REAL:
            case INTEGER:
                if (a.getReal().compareTo(BigDecimal.ZERO) > 0)
                    return new Atom(BigDecimalMath.log2(a.getReal(), e.getMathContext()));
                else
                    return new Atom(BigComplexMath.log(a.getComplex(), e.getMathContext()).divide(BigComplexMath.log(BigComplex.valueOf(2), e.getMathContext()), e.getMathContext()));
            case COMPLEX:
                return new Atom(BigComplexMath.log(a.getComplex(), e.getMathContext()).divide(BigComplexMath.log(BigComplex.valueOf(2), e.getMathContext()), e.getMathContext()));
            default:
                throw new UnsupportedOperationException("log2 not defined for: " + a.getType());
        }
    }

    @Override
    protected String name() {
        return "log2";
    }

    @Override
    public Atom apply(Environment env, List<Atom> args) {
        if (args.isEmpty()) {
            throw new RuntimeException("log2 called with no arguments.");
        }

        if (args.size() == 1) {
            return ln(env, args.get(0));
        }

        ArrayList<Atom> list = new ArrayList<>(args.size());
        for (Atom x : args) {
            Atom ln = ln(env, x);
            list.add(ln);
        }
        return new Atom(list);
    }
}
