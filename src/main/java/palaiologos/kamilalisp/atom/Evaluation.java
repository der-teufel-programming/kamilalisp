package palaiologos.kamilalisp.atom;

import palaiologos.kamilalisp.error.TypeError;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Evaluation {
    @Nonnull
    public static Atom evaluate(Environment env, Atom atom) {
        switch (atom.getType()) {
            case STRING:
            case REAL:
            case COMPLEX:
            case INTEGER:
            case CALLABLE:
                return atom;
            case LIST:
                if (atom.getList().get(0) instanceof CodeAtom) {
                    StackFrame.push((CodeAtom) atom.getList().get(0));
                }
                Atom head = evaluate(env, atom.getList().get(0));
                if (head.getType() != Type.CALLABLE) {
                    // Replace LF in matrix stringification...
                    throw new TypeError("Cannot call non-callable atom in evaluation of list with head " + head.toDisplayString().replace('\n', ' '));
                }
                Callable c = head.getCallable();
                Atom result;
                if (c instanceof Lambda) {
                    List<Atom> args = new ArrayList<>(atom.getList().size());
                    for(int i = 1; i < atom.getList().size(); i++)
                        args.add(evaluate(env, atom.getList().get(i)));
                    result = evaluate(env, c, args);
                } else if (c instanceof SpecialForm) {
                    result = evaluate(env, c, atom.getList().subList(1, atom.getList().size()));
                } else {
                    throw new RuntimeException("Unknown callable type: " + c.getClass().getName());
                }
                if (atom.getList().get(0) instanceof CodeAtom) {
                    StackFrame.pop();
                }
                return result;
            case IDENTIFIER:
                if (env.has(atom.getIdentifier())) {
                    Atom a = env.get(atom.getIdentifier());
                    if (a != null && a.getType() == Type.LIST && !a.getList().isEmpty() && a.getList().get(0).getType() == Type.CALLABLE && a.getList().get(0).getCallable() instanceof ReactiveFunction) {
                        Callable rf = a.getList().get(0).getCallable();
                        return rf.apply(env, List.of());
                    }
                    return a;
                } else
                    return atom;
        }

        throw new RuntimeException("Unknown atom type: " + atom.getType());
    }

    @Nonnull
    public static Atom safeEvaluate(Environment env, Atom atom, BiFunction<String, Throwable, Atom> exceptionHandler) {
        switch (atom.getType()) {
            case STRING:
            case REAL:
            case COMPLEX:
            case INTEGER:
            case CALLABLE:
                return atom;
            case LIST:
                int depth = StackFrame.depth();
                try {
                    if (atom.getList().get(0) instanceof CodeAtom) {
                        StackFrame.push((CodeAtom) atom.getList().get(0));
                    }
                    Atom head = evaluate(env, atom.getList().get(0));
                    if (head.getType() != Type.CALLABLE) {
                        throw new TypeError("Cannot call non-callable atom in evaluation of list with head " + head.toDisplayString().replace('\n', ' '));
                    }
                    Callable c = head.getCallable();
                    Atom result;
                    if (c instanceof Lambda) {
                        List<Atom> args = new ArrayList<>(atom.getList().size());
                        for(int i = 1; i < atom.getList().size(); i++)
                            args.add(evaluate(env, atom.getList().get(i)));
                        result = evaluate(env, c, args);
                    } else if (c instanceof SpecialForm) {
                        result = evaluate(env, c, atom.getList().subList(1, atom.getList().size()));
                    } else {
                        throw new RuntimeException("Unknown callable type: " + c.getClass().getName());
                    }
                    if (atom.getList().get(0) instanceof CodeAtom) {
                        StackFrame.pop();
                    }
                    return result;
                } catch (Throwable t) {
                    String trace = StackFrame.stackTrace(t);
                    while (StackFrame.depth() > depth)
                        StackFrame.pop();
                    return exceptionHandler.apply(trace, t);
                }
            case IDENTIFIER:
                if (env.has(atom.getIdentifier())) {
                    Atom a = env.get(atom.getIdentifier());
                    if (a != null && a.getType() == Type.LIST && !a.getList().isEmpty() && a.getList().get(0).getType() == Type.CALLABLE && a.getList().get(0).getCallable() instanceof ReactiveFunction) {
                        Callable rf = a.getList().get(0).getCallable();
                        return evaluate(env, rf, List.of());
                    }
                    return a;
                } else
                    return atom;
        }

        throw new RuntimeException("Unknown atom type: " + atom.getType());
    }

    public static Atom evaluate(Environment env, Callable c, List<Atom> args) {
        StackFrame.push(c);
        Atom r = c.apply(env, args);
        StackFrame.pop();
        return r;
    }

    public static Atom safeEvaluate(Environment env, Callable c, List<Atom> args, Function<String, Atom> exceptionHandler) {
        int depth = StackFrame.depth();
        try {
            StackFrame.push(c);
            Atom r = c.apply(env, args);
            StackFrame.pop();
            return r;
        } catch (Throwable t) {
            String trace = StackFrame.stackTrace(t);
            while (StackFrame.depth() > depth)
                StackFrame.pop();
            return exceptionHandler.apply(trace);
        }
    }
}
