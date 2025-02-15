package palaiologos.kamilalisp.runtime.meta;

import palaiologos.kamilalisp.atom.*;

import java.util.List;
import java.util.stream.Collectors;

public class Fork implements SpecialForm, ReactiveFunction {
    // Need to evaluate these.
    private final Atom reductor;
    private final List<Atom> reductees;

    private final int l;
    private final int c;

    public Fork(Atom reductor, List<Atom> reductees, int l, int c) {
        this.reductor = reductor;
        this.reductees = reductees;
        this.l = l;
        this.c = c;
    }

    @Override
    public int line() {
        return l;
    }

    @Override
    public int column() {
        return c;
    }

    @Override
    public String stringify() {
        return "[" + reductor.toString() + " " + reductees.stream().map(Atom::toString).collect(Collectors.joining(" ")) + "]";
    }

    @Override
    public Atom apply(Environment env, List<Atom> args) {
        Atom eReductor = Evaluation.evaluate(env, reductor);
        List<Atom> eReductees = reductees.stream().map(x -> Evaluation.evaluate(env, x)).toList();
        return new Atom(new Lambda() {
            @Override
            public String stringify() {
                return Fork.this.stringify();
            }

            @Override
            public String frameString() {
                return Fork.this.frameString();
            }

            @Override
            public Atom apply(Environment env, List<Atom> args) {
                return eReductor.getCallable()
                        .apply(env, !eReductees.isEmpty() ? eReductees.stream().map(a -> {
                            if (a.getType() == Type.CALLABLE)
                                return a.getCallable().apply(env, args);
                            else
                                return a;
                        }).collect(Collectors.toList()) : args);
            }

            @Override
            public int line() {
                return Fork.this.line();
            }

            @Override
            public int column() {
                return Fork.this.column();
            }
        });
    }

    @Override
    public String frameString() {
        return "[]/syn";
    }
}
