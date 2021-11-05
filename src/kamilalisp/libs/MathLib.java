package kamilalisp.libs;

import ch.obermuhlner.math.big.BigDecimalMath;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import kamilalisp.api.Evaluation;
import kamilalisp.data.*;
import kamilalisp.libs.math.Constant;
import kamilalisp.libs.math.Logarithm;
import kamilalisp.libs.math.PollardRho;
import kamilalisp.libs.math.Trigonometry;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MathLib {
    public static void install(Environment env) {
        Logarithm.install(env);
        Constant.install(env);
        Trigonometry.install(env);
        PollardRho.install(env);

        env.push("+", new Atom(new Closure() {
            private Atom IDENTITY = new Atom(BigDecimal.ZERO);

            private Atom add2(Atom a1, Atom a2) {
                if(a1.getType() == Type.NUMBER && a2.getType() == Type.NUMBER) {
                    return new Atom(a1.getNumber().get().add(a2.getNumber().get()));
                } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.STRING_CONSTANT) {
                    return new Atom(new StringConstant(a1.getStringConstant().get().get() + a2.getStringConstant().get().get()));
                } else if(a1.getType() == Type.NUMBER && a2.getType() == Type.STRING_CONSTANT) {
                    return new Atom(new StringConstant(a1.getNumber().get().toPlainString() + a2.getStringConstant().get().get()));
                } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.NUMBER) {
                    return new Atom(new StringConstant(a1.getStringConstant().get().get() + a2.getNumber().get().toPlainString()));
                } else {
                    throw new Error("+ unsupported on operands of type " + a1.getType().name() + " and " + a2.getType().name());
                }
            }

            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0 || arguments.size() > 2)
                    throw new Error("Invalid + invocation.");
                else if(arguments.size() == 1)
                    return arguments.get(0);
                return new Atom(new LbcSupplier<>(() -> add2(arguments.get(0), arguments.get(1)).get().get()));
            }
        }));

        env.push("-", new Atom(new Closure() {
            private Atom IDENTITY = new Atom(BigDecimal.ZERO);

            private Atom sub2(Atom a1, Atom a2) {
                if(a1.getType() == Type.NUMBER && a2.getType() == Type.NUMBER) {
                    return new Atom(a1.getNumber().get().subtract(a2.getNumber().get()));
                } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.STRING_CONSTANT) {
                    final String lookup = a2.getStringConstant().get().get();
                    return new Atom(new StringConstant(a1.getStringConstant().get().get()
                            .chars().filter(x -> lookup.indexOf(x) != -1).collect(StringWriter::new, StringWriter::write,
                                    (swl, swr) -> swl.write(swr.toString())).toString()));
                } else if(a1.getType() == Type.NUMBER && a2.getType() == Type.STRING_CONSTANT) {
                    return new Atom(new StringConstant(a2.getStringConstant().get().get().substring(a1.getNumber().get().intValue())));
                } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.NUMBER) {
                    String s = a1.getStringConstant().get().get();
                    return new Atom(new StringConstant(s.substring(0, s.length() - a2.getNumber().get().intValue())));
                } else {
                    throw new Error("- unsupported on operands of type " + a1.getType().name() + " and " + a2.getType().name());
                }
            }

            private Atom sub1(Atom a) {
                a.guardType("Argument to monadic -", Type.NUMBER);
                throw new Error("- unsupported on operand of type " + a.getType().name());
            }

            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0 || arguments.size() > 2)
                    throw new Error("Invalid - invocation.");
                else if(arguments.size() == 1)
                    return sub1(arguments.get(0));
                return new Atom(new LbcSupplier<>(() -> sub2(arguments.get(0), arguments.get(1)).get().get()));
            }
        }));

        env.push("*", new Atom(new Closure() {
            private Atom IDENTITY = new Atom(BigDecimal.ZERO);

            private Atom mul2(Atom a1, Atom a2) {
                if(a1.getType() == Type.NUMBER && a2.getType() == Type.NUMBER) {
                    return new Atom(a1.getNumber().get().multiply(a2.getNumber().get()));
                } else if(a1.getType() == Type.NUMBER && a2.getType() == Type.STRING_CONSTANT) {
                    return new Atom(new StringConstant(a2.getStringConstant().get().get().repeat(a1.getNumber().get().intValue())));
                } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.NUMBER) {
                    return new Atom(new StringConstant(a1.getStringConstant().get().get().repeat(a2.getNumber().get().intValue())));
                } else {
                    throw new Error("* unsupported on operands of type " + a1.getType().name() + " and " + a2.getType().name());
                }
            }

            public Atom mul1(Atom a) {
                a.guardType("Argument to monadic *", Type.NUMBER);
                return new Atom(new LbcSupplier<>(() -> new BigDecimal(a.getNumber().get().compareTo(BigDecimal.ZERO))));
            }

            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0 || arguments.size() > 2)
                    throw new Error("Invalid * invocation.");
                if(arguments.size() == 1)
                    return mul1(arguments.get(0));
                return new Atom(new LbcSupplier<>(() -> mul2(arguments.get(0), arguments.get(1)).get().get()));
            }
        }));

        env.push("/", new Atom(new Closure() {
            private Atom IDENTITY = new Atom(BigDecimal.ZERO);

            private Atom div2(Atom a1, Atom a2) {
                if(a1.getType() == Type.NUMBER && a2.getType() == Type.NUMBER) {
                    return new Atom(a1.getNumber().get().divide(a2.getNumber().get(), MathContext.DECIMAL128).setScale(0, RoundingMode.FLOOR));
                } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.NUMBER) {
                    String s = a1.getStringConstant().get().get();
                    return new Atom(Lists.newLinkedList(
                            Splitter
                                    .fixedLength(s.length() / a2.getNumber().get().intValue())
                                    .split(s)
                        ).stream()
                            .map(x -> new Atom(new StringConstant(x)))
                            .collect(Collectors.toList()));
                } else if(a1.getType() == Type.LIST && a2.getType() == Type.NUMBER) {
                    List<Atom> s = a1.getList().get();
                    return new Atom(Lists.partition(s, s.size() / a2.getNumber().get().intValue()).stream().map(x -> new Atom(x)).collect(Collectors.toList()));
                } else {
                    throw new Error("/ unsupported on operands of type " + a1.getType().name() + " and " + a2.getType().name());
                }
            }

            public Atom div1(Atom a) {
                a.guardType("Argument to monadic /", Type.NUMBER);
                return new Atom(new LbcSupplier<>(() -> BigDecimal.ONE.divide(a.getNumber().get())));
            }

            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0 || arguments.size() > 2)
                    throw new Error("Invalid / invocation.");
                if(arguments.size() == 1)
                    return div1(arguments.get(0));
                return new Atom(new LbcSupplier<>(() -> div2(arguments.get(0), arguments.get(1)).get().get()));
            }
        }));

        env.push("%", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0 || arguments.size() > 2)
                    throw new Error("Invalid % invocation.");
                if(arguments.size() == 1) {
                    return new Atom(new LbcSupplier<>(() -> {
                        arguments.get(0).guardType("Argument to monadic %", Type.NUMBER);
                        return arguments.get(0).getNumber().get().abs();
                    }));
                } else
                    return new Atom(new LbcSupplier<>(() -> {
                        arguments.get(0).guardType("Argument to dyadic %", Type.NUMBER);
                        arguments.get(1).guardType("Argument to dyadic %", Type.NUMBER);
                        return arguments.get(0).getNumber().get().remainder(arguments.get(1).getNumber().get());
                    }));
            }
        }));

        env.push("gcd", new Atom(new Closure() {
            private Atom IDENTITY = new Atom(BigDecimal.ZERO);

            private Atom gcd2(Atom a1, Atom a2) {
                a1.guardType("First argument to 'gcd'", Type.NUMBER);
                a2.guardType("Second argument to 'gcd'", Type.NUMBER);
                return new Atom(new BigDecimal(a1.getNumber().get().toBigInteger().gcd(a2.getNumber().get().toBigInteger())));
            }

            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0 || arguments.size() > 2)
                    throw new Error("Invalid 'gcd' invocation.");
                if(arguments.size() == 1)
                    return arguments.get(0);
                return new Atom(new LbcSupplier<>(() -> gcd2(arguments.get(0), arguments.get(1)).get().get()));
            }
        }));

        env.push("lcm", new Atom(new Closure() {
            private Atom IDENTITY = new Atom(BigDecimal.ZERO);

            private Atom gcd2(Atom a1, Atom a2) {
                a1.guardType("First argument to 'lcm'", Type.NUMBER);
                a2.guardType("Second argument to 'lcm'", Type.NUMBER);
                return new Atom(a1.getNumber().get().multiply(a2.getNumber().get()).divide(new BigDecimal(a1.getNumber().get().toBigInteger().gcd(a2.getNumber().get().toBigInteger()))));
            }

            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0 || arguments.size() > 2)
                    throw new Error("Invalid 'lcm' invocation.");
                if(arguments.size() == 1)
                    return arguments.get(0);
                return new Atom(new LbcSupplier<>(() -> gcd2(arguments.get(0), arguments.get(1)).get().get()));
            }
        }));

        env.push("~", new Atom(new Closure() {
            public String reverseCase(String text) {
                return text.chars()
                        .map(c -> Character.isUpperCase(c) ?
                                Character.toLowerCase(c) :
                                Character.toUpperCase(c))
                        .collect(
                                StringBuilder::new,
                                StringBuilder::appendCodePoint,
                                StringBuilder::append)
                        .toString();
            }

            public Atom neg1(Atom a) {
                a.guardType("First argument to monadic '~'", Type.NUMBER, Type.STRING_CONSTANT);

                if(a.getType() == Type.NUMBER) {
                    return new Atom(BigDecimal.valueOf(a.getNumber().get().compareTo(BigDecimal.ZERO) == 0 ? 1 : 0));
                } else if(a.getType() == Type.STRING_CONSTANT) {
                    return new Atom(new StringConstant(reverseCase(a.getStringConstant().get().get())));
                }

                // Should never get here.
                return null;
            }

            public Atom neg2(Atom a, Atom b) {
                a.guardType("First argument to dyadic '~'", Type.LIST);
                if(b.getType() == Type.LIST) {
                    List<Atom> clone = new LinkedList<>(a.getList().get());
                    clone.removeIf(x -> a.getList().get().contains(x.get().get()));
                    return new Atom(clone);
                } else {
                    List<Atom> clone = new LinkedList<>(a.getList().get());
                    clone.removeIf(x -> x.get().get().equals(b.get().get()));
                    return new Atom(clone);
                }
            }

            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() == 0)
                    throw new Error("Invalid invocation to '~'.");
                else if(arguments.size() == 1)
                    return new Atom(new LbcSupplier(() -> neg1(arguments.get(0)).get().get()));
                else if(arguments.size() == 2)
                    return new Atom(new LbcSupplier(() -> neg2(arguments.get(0), arguments.get(1)).get().get()));
                throw new Error("Invalid invocation to '~'.");
            }
        }));

        env.push("<", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to '<'.");
                return new Atom(new LbcSupplier<>(() -> {
                    if(arguments.get(0).getType() == Type.NUMBER && arguments.get(1).getType() == Type.NUMBER) {
                        return arguments.get(0).getNumber().get().compareTo(arguments.get(1).getNumber().get()) < 0 ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else if(arguments.get(0).getType() == Type.STRING_CONSTANT && arguments.get(1).getType() == Type.STRING_CONSTANT) {
                        return arguments.get(0).getStringConstant().get().get().compareTo(arguments.get(1).getStringConstant().get().get()) < 0 ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else if(arguments.get(0).getType() == Type.STRING_CONSTANT && arguments.get(1).getType() == Type.NUMBER) {
                        return arguments.get(0).getStringConstant().get().get().length() < arguments.get(1).getNumber().get().intValue() ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else if(arguments.get(0).getType() == Type.NUMBER && arguments.get(1).getType() == Type.STRING_CONSTANT) {
                        return arguments.get(1).getStringConstant().get().get().length() < arguments.get(0).getNumber().get().intValue() ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else {
                        throw new Error("Invalid invocation to '<'. Expected two numbers or two strings.");
                    }
                }));
            }
        }));

        env.push(">", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to '>'.");
                return new Atom(new LbcSupplier<>(() -> {
                    if(arguments.get(0).getType() == Type.NUMBER && arguments.get(1).getType() == Type.NUMBER) {
                        return arguments.get(0).getNumber().get().compareTo(arguments.get(1).getNumber().get()) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else if(arguments.get(0).getType() == Type.STRING_CONSTANT && arguments.get(1).getType() == Type.STRING_CONSTANT) {
                        return arguments.get(0).getStringConstant().get().get().compareTo(arguments.get(1).getStringConstant().get().get()) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else if(arguments.get(0).getType() == Type.STRING_CONSTANT && arguments.get(1).getType() == Type.NUMBER) {
                        return arguments.get(0).getStringConstant().get().get().length() > arguments.get(1).getNumber().get().intValue() ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else if(arguments.get(0).getType() == Type.NUMBER && arguments.get(1).getType() == Type.STRING_CONSTANT) {
                        return arguments.get(1).getStringConstant().get().get().length() > arguments.get(0).getNumber().get().intValue() ? BigDecimal.ONE : BigDecimal.ZERO;
                    } else {
                        throw new Error("Invalid invocation to '>'. Expected two numbers or two strings.");
                    }
                }));
            }
        }));

        env.push("|", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to '|'.");
                return new Atom(new LbcSupplier<>(() -> {
                    if(arguments.get(0).getType() == Type.NUMBER && arguments.get(1).getType() == Type.NUMBER) {
                        return new BigDecimal(
                                arguments
                                        .get(0)
                                        .getNumber()
                                        .get()
                                        .toBigInteger()
                                        .or(arguments.get(1).getNumber().get().toBigInteger()));
                    } else if(arguments.get(0).getType() == Type.STRING_CONSTANT && arguments.get(1).getType() == Type.STRING_CONSTANT) {
                        return new StringConstant(arguments.get(0).getStringConstant().get().get().concat(arguments.get(1).getStringConstant().get().get())
                                        .codePoints()
                                        .distinct()
                                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                        .toString());
                    } else if(arguments.get(0).getType() == Type.LIST && arguments.get(1).getType() == Type.LIST) {
                        Set<Object> result = new LinkedHashSet<>();
                        result.addAll(arguments.get(0).getList().get().stream().map(x -> x.get().get()).collect(Collectors.toList()));
                        result.addAll(arguments.get(1).getList().get().stream().map(x -> x.get().get()).collect(Collectors.toList()));
                        return result.stream().map(x -> new Atom(new LbcSupplier<>(() -> x))).collect(Collectors.toList());
                    } else
                        throw new Error("Invalid invocation to '|'. Expected two numbers, two strings or two lists.");
                }));
            }
        }));

        env.push("&", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to '&'.");
                return new Atom(new LbcSupplier<>(() -> {
                    if(arguments.get(0).getType() == Type.NUMBER && arguments.get(1).getType() == Type.NUMBER) {
                        return new BigDecimal(
                                arguments
                                        .get(0)
                                        .getNumber()
                                        .get()
                                        .toBigInteger()
                                        .and(arguments.get(1).getNumber().get().toBigInteger()));
                    } else if(arguments.get(0).getType() == Type.STRING_CONSTANT && arguments.get(1).getType() == Type.STRING_CONSTANT) {
                        Set<Integer> result = new LinkedHashSet<>();
                        result.addAll(arguments.get(0).getStringConstant().get().get().codePoints().boxed().collect(Collectors.toList()));
                        result.retainAll(arguments.get(1).getStringConstant().get().get().codePoints().boxed().collect(Collectors.toList()));
                        return new StringConstant(result.stream().collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
                    } else if(arguments.get(0).getType() == Type.LIST && arguments.get(1).getType() == Type.LIST) {
                        Set<Atom> result = new LinkedHashSet<>();
                        result.addAll(arguments.get(0).getList().get());
                        result.retainAll(arguments.get(1).getList().get());
                        return result.stream().collect(Collectors.toList());
                    } else
                        throw new Error("Invalid invocation to '&'. Expected two numbers, two strings or two lists.");
                }));
            }
        }));

        env.push(">=", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to '>='.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a1 = arguments.get(0);
                    Atom a2 = arguments.get(1);
                    return env.evaluate(new Atom(List.of(
                            new Atom("|"),
                            new Atom(List.of(new Atom("="), a1, a2)),
                            new Atom(List.of(new Atom(">"), a1, a2))
                    )));
                }));
            }
        }));

        env.push("<=", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to '<='.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a1 = arguments.get(0);
                    Atom a2 = arguments.get(1);
                    return env.evaluate(new Atom(List.of(
                            new Atom("|"),
                            new Atom(List.of(new Atom("="), a1, a2)),
                            new Atom(List.of(new Atom("<"), a1, a2))
                    )));
                }));
            }
        }));

        env.push("=", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("invalid invocation to '='.");
                return new Atom(new LbcSupplier(() -> arguments.get(0).equals(arguments.get(1)) ? BigDecimal.ONE : BigDecimal.ZERO));
            }
        }));

        env.push("/=", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("invalid invocation to '='.");
                return new Atom(new LbcSupplier(() -> arguments.get(0).equals(arguments.get(1)) ? BigDecimal.ZERO : BigDecimal.ONE));
            }
        }));

        env.push("min", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to 'min'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a1 = arguments.get(0);
                    Atom a2 = arguments.get(1);
                    if(a1.getType() == Type.NUMBER && a2.getType() == Type.NUMBER) {
                        return a1.getNumber().get().min(a2.getNumber().get());
                    } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.STRING_CONSTANT) {
                        return a1.getStringConstant().get().get().compareTo(a2.getStringConstant().get().get()) < 0 ? a1.get().get() : a2.get().get();
                    } else if(a1.getType() == Type.LIST && a2.getType() == Type.LIST) {
                        return a1.getList().get().size() < a2.getList().get().size() ? a1.get().get() : a2.get().get();
                    }
                    throw new Error("Invalid invocation to 'min': expected two lists, two strings or two numbers.");
                }));
            }
        }));

        env.push("max", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to 'max'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a1 = arguments.get(0);
                    Atom a2 = arguments.get(1);
                    if(a1.getType() == Type.NUMBER && a2.getType() == Type.NUMBER) {
                        return a1.getNumber().get().max(a2.getNumber().get());
                    } else if(a1.getType() == Type.STRING_CONSTANT && a2.getType() == Type.STRING_CONSTANT) {
                        return a1.getStringConstant().get().get().compareTo(a2.getStringConstant().get().get()) > 0 ? a1.get().get() : a2.get().get();
                    } else if(a1.getType() == Type.LIST && a2.getType() == Type.LIST) {
                        return a1.getList().get().size() > a2.getList().get().size() ? a1.get().get() : a2.get().get();
                    }
                    throw new Error("Invalid invocation to 'max': expected two lists, two strings or two numbers.");
                }));
            }
        }));

        env.push("approx-eq", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 3)
                    throw new Error("Invalid invocation to 'approx-eq'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    Atom b = arguments.get(1);
                    Atom epsilon = arguments.get(2);
                    a.guardType("First argument to 'approx-eq'", Type.NUMBER);
                    b.guardType("Second argument to 'approx-eq'", Type.NUMBER);
                    epsilon.guardType("Third argument to 'approx-eq'", Type.NUMBER);
                    return a.getNumber().get().subtract(b.getNumber().get()).abs().compareTo(epsilon.getNumber().get()) < 0 ? BigDecimal.ONE : BigDecimal.ZERO;
                }));
            }
        }));

        env.push("sqrt", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 1)
                    throw new Error("Invalid invocation to 'sqrt'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    a.guardType("First argument to 'sqrt'", Type.NUMBER);
                    return BigDecimalMath.sqrt(a.getNumber().get(), MathContext.DECIMAL128);
                }));
            }
        }));

        env.push("exp", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 1)
                    throw new Error("Invalid invocation to 'exp'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    a.guardType("First argument to 'exp'", Type.NUMBER);
                    return BigDecimalMath.exp(a.getNumber().get(), MathContext.DECIMAL128);
                }));
            }
        }));

        env.push("**", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to '**'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    Atom b = arguments.get(1);
                    a.guardType("First argument to '**'", Type.NUMBER);
                    b.guardType("Second argument to '**'", Type.NUMBER);
                    return a.getNumber().get().pow(b.getNumber().get().intValue(), MathContext.DECIMAL128);
                }));
            }
        }));

        env.push("!", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 1)
                    throw new Error("Invalid invocation to '!'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    a.guardType("First argument to '!'", Type.NUMBER);
                    return BigDecimalMath.factorial(a.getNumber().get(), MathContext.DECIMAL128);
                }));
            }
        }));

        env.push("binomial", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to 'binomial'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    Atom b = arguments.get(1);
                    a.guardType("First argument to 'binomial'", Type.NUMBER);
                    b.guardType("Second argument to 'binomial'", Type.NUMBER);
                    BigDecimal aBang = BigDecimalMath.factorial(a.getNumber().get(), MathContext.DECIMAL128);
                    BigDecimal bBang = BigDecimalMath.factorial(b.getNumber().get(), MathContext.DECIMAL128);
                    BigDecimal abBang = BigDecimalMath.factorial(a.getNumber().get().subtract(b.getNumber().get()), MathContext.DECIMAL128);
                    return aBang.divide(bBang.multiply(abBang), MathContext.DECIMAL128);
                }));
            }
        }));

        env.push("root", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to 'root'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    Atom b = arguments.get(1);
                    a.guardType("First argument to 'root'", Type.NUMBER);
                    b.guardType("Second argument to 'root'", Type.NUMBER);
                    return BigDecimalMath.root(b.getNumber().get(), a.getNumber().get(), MathContext.DECIMAL128);
                }));
            }
        }));

        env.push("floor", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 1 && arguments.size() != 2)
                    throw new Error("Invalid invocation to 'floor'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    a.guardType("First argument to 'floor'", Type.NUMBER);
                    if(arguments.size() == 2) {
                        arguments.get(1).guardType("Second argument to 'floor'", Type.NUMBER);
                        return a.getNumber().get().setScale(arguments.get(1).getNumber().get().intValue(), RoundingMode.FLOOR);
                    } else
                        return a.getNumber().get().setScale(0, RoundingMode.FLOOR);
                }));
            }
        }));

        env.push("ceil", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 1 && arguments.size() != 2)
                    throw new Error("Invalid invocation to 'ceil'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    a.guardType("First argument to 'ceil'", Type.NUMBER);
                    if(arguments.size() == 2) {
                        arguments.get(1).guardType("Second argument to 'ceil'", Type.NUMBER);
                        return a.getNumber().get().setScale(arguments.get(1).getNumber().get().intValue(), RoundingMode.CEILING);
                    } else
                        return a.getNumber().get().setScale(0, RoundingMode.CEILING);
                }));
            }
        }));

        env.push("is-prime?", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 1)
                    throw new Error("Invalid invocation to 'is-prime'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    a.guardType("First argument to 'is-prime'", Type.NUMBER);
                    return a.getNumber().get().toBigInteger().isProbablePrime(50) ? BigDecimal.ONE : BigDecimal.ZERO;
                }));
            }
        }));

        env.push("decode", new Atom(new Closure() {
            @Override
            public Atom apply(Executor env, List<Atom> arguments) {
                if(arguments.size() != 2)
                    throw new Error("Invalid invocation to 'decode'.");
                return new Atom(new LbcSupplier<>(() -> {
                    Atom a = arguments.get(0);
                    Atom b = arguments.get(1);
                    b.guardType("Second argument to 'decode'", Type.LIST);
                    if(a.getType() == Type.NUMBER) {
                        BigDecimal n = new BigDecimal(1);
                        BigDecimal base = a.getNumber().get();
                        BigDecimal s = new BigDecimal(0);
                        List<Atom> l = Lists.reverse(b.getList().get());
                        for(int i = 0; i < l.size(); i++) {
                            l.get(i).guardType("Element in list argument to 'decode'", Type.NUMBER);
                            s = s.add(l.get(i).getNumber().get().multiply(n));
                            n = n.multiply(base);
                        }
                        return s;
                    } else if(a.getType() == Type.LIST) {
                        List<BigDecimal> ns = a.getList().get().stream().map(x -> {
                            x.guardType("Element in list argument to 'decode'", Type.NUMBER);
                            return x.getNumber().get();
                        }).collect(Collectors.toList());
                        BigDecimal s = new BigDecimal(0);
                        BigDecimal n = new BigDecimal(1);
                        List<Atom> l = b.getList().get();
                        for(int i = l.size() - 1; i >= 0; i--) {
                            l.get(i).guardType("Element in list argument to 'decode'", Type.NUMBER);
                            s = s.add(l.get(i).getNumber().get().multiply(n));
                            n = n.multiply(ns.get(i));
                        }
                        return s;
                    }
                    throw new Error("First argument to 'decode' must be a number or a list.");
                }));
            }
        }));

        // Math utilities implemented in Lisp for no real reason.
        // Except that they're easier to maintain.
        Evaluation.evalString(env, "(def sum (bind foldl' + 0))");
        Evaluation.evalString(env, "(def prod (bind foldl' * 1))");
        Evaluation.evalString(env, "(defun totient (x) (let ((y (p-factors x))) (prod (zip-with - y (unique-mask y)))))");
    }
}
