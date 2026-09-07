package com.reallifedeveloper.tools.test.database.inmemory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.PredicateSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * An evaluator of {@code org.springframework.data.jpa.domain.PredicateSpecification} instances.
 *
 * @param <T> the type of entity for which the specification
 *
 * @see <a href= "https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html#predicate-specification">Spring Data JPA
 *      documentation</a>
 *
 * @author ChatGPT, RealLifeDeveloper
 */
public final class PredicateSpecificationEvaluator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(PredicateSpecificationEvaluator.class);

    /**
     * Checks if the given entity matches the given specification.
     *
     * @param specification the {@code PredicateSpecification} to use
     * @param entity        the entity to check
     *
     * @return {@code true} if {@code specification} matches {@code entity}, {@code false} otherwise
     */
    public boolean matches(PredicateSpecification<T> specification, T entity) {

        Objects.requireNonNull(specification);
        Objects.requireNonNull(entity);

        RecordingState state = new RecordingState();

        From<?, T> root = Proxies.root(state);
        CriteriaBuilder cb = Proxies.criteriaBuilder(state);

        Predicate predicate = specification.toPredicate(root, cb);

        // PredicateSpecification.unrestricted() and similar specifications are represented by a null predicate.
        if (predicate == null) {
            return true;
        }

        Expr expr = Proxies.expressionOf(predicate);

        LOG.debug("matches: expr={}, entity={}", expr, entity);
        LOG.trace("matches: joins={}", state.joins());

        List<EvaluationContext> rows = expandJoins(state, entity);

        LOG.trace("matches: rows={}", rows);

        return rows.stream().anyMatch(context -> evaluateBoolean(expr, context) == Truth.TRUE);
    }

    /**
     * Filters a collection of entities based on if they match the given specification or not.
     *
     * @param specification the {@code PredicateSpecification} to use
     * @param entities      the collecction of entities to filter
     *
     * @return a list of the entities from the {@code entities} collection that match {@code specification}
     */
    public List<T> filter(PredicateSpecification<T> specification, Collection<T> entities) {

        return entities.stream().filter(entity -> matches(specification, entity)).toList();
    }

    // ============================================================
    // Expression Model
    // ============================================================

    private sealed interface Expr {
    }

    private record Constant(@Nullable Object value) implements Expr {
    }

    private record PathExpr(Source source, List<String> attributes) implements Expr {
    }

    private record Equal(Expr left, Expr right) implements Expr {
    }

    private record NotEqual(Expr left, Expr right) implements Expr {
    }

    private record GreaterThan(Expr left, Expr right) implements Expr {
    }

    private record GreaterThanOrEqual(Expr left, Expr right) implements Expr {
    }

    private record LessThan(Expr left, Expr right) implements Expr {
    }

    private record LessThanOrEqual(Expr left, Expr right) implements Expr {
    }

    private record IsNull(Expr expression) implements Expr {
    }

    private record IsNotNull(Expr expression) implements Expr {
    }

    private record And(List<Expr> expressions) implements Expr {
    }

    private record Or(List<Expr> expressions) implements Expr {
    }

    private record Not(Expr expression) implements Expr {
    }

    private record Product(Expr left, Expr right) implements Expr {
    }

    private record TrueExpr() implements Expr {
    }

    private record FalseExpr() implements Expr {
    }

    // ============================================================
    // Sources and Joins
    // ============================================================

    private sealed interface Source {
    }

    private record RootSource() implements Source {
    }

    private record JoinSource(int id) implements Source {
    }

    private record JoinDefinition(int id, Source parent, String attribute, JoinType joinType) {
    }

    @Getter
    @Accessors(fluent = true)
    private static final class RecordingState {

        private int nextJoinId;

        private final List<JoinDefinition> joins = new ArrayList<>();

        JoinSource addJoin(Source parent, String attribute, JoinType joinType) {

            int id = nextJoinId++;

            joins.add(new JoinDefinition(id, parent, attribute, joinType));

            return new JoinSource(id);
        }
    }

    // ============================================================
    // Evaluation Context
    // ============================================================

    private record EvaluationContext(Object root, Map<Integer, Object> joins) {

        EvaluationContext bind(int joinId, @Nullable Object value) {

            Map<Integer, Object> copy = new HashMap<>(joins);

            copy.put(joinId, value);

            return new EvaluationContext(root, copy);
        }

        @Nullable
        Object source(Source source) {
            return switch (source) {
            case RootSource ignored -> root;

            case JoinSource join -> joins.get(join.id());
            };
        }
    }

    // ============================================================
    // Join Expansion
    // ============================================================

    private List<EvaluationContext> expandJoins(RecordingState state, Object entity) {

        List<EvaluationContext> rows = List.of(new EvaluationContext(entity, Map.of()));

        for (JoinDefinition join : state.joins) {
            List<EvaluationContext> next = new ArrayList<>();

            for (EvaluationContext row : rows) {
                Object parent = row.source(join.parent());

                Object value = parent == null ? null : PropertyAccess.read(parent, join.attribute());

                List<?> joinedValues = normalizeJoinValue(value);

                if (joinedValues.isEmpty()) {
                    if (join.joinType() == JoinType.LEFT) {
                        next.add(row.bind(join.id(), null));
                    }

                    // INNER JOIN:
                    // no resulting row.
                    continue;
                }

                for (Object joinedValue : joinedValues) {
                    next.add(row.bind(join.id(), joinedValue));
                }
            }

            rows = next;
        }

        return rows;
    }

    private static List<?> normalizeJoinValue(@Nullable Object value) {

        if (value == null) {
            return List.of();
        }

        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }

        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();

            iterable.forEach(result::add);

            return result;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);

            List<Object> result = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                result.add(Array.get(value, i));
            }

            return result;
        }

        return List.of(value);
    }

    // ============================================================
    // Expression Evaluation
    // ============================================================

    private enum Truth {
        TRUE, FALSE, UNKNOWN
    }

    private Truth evaluateBoolean(Expr expr, EvaluationContext context) {

        LOG.trace("evaluateBoolean: expr={}", expr);

        return switch (expr) {

        case TrueExpr ignored -> Truth.TRUE;

        case FalseExpr ignored -> Truth.FALSE;

        case Equal e -> equal(evaluateValue(e.left(), context), evaluateValue(e.right(), context));

        case NotEqual e -> not(equal(evaluateValue(e.left(), context), evaluateValue(e.right(), context)));

        case GreaterThan e -> compare(e.left(), e.right(), context, result -> result > 0);

        case GreaterThanOrEqual e -> compare(e.left(), e.right(), context, result -> result >= 0);

        case LessThan e -> compare(e.left(), e.right(), context, result -> result < 0);

        case LessThanOrEqual e -> compare(e.left(), e.right(), context, result -> result <= 0);

        case IsNull e -> evaluateValue(e.expression(), context) == null ? Truth.TRUE : Truth.FALSE;

        case IsNotNull e -> evaluateValue(e.expression(), context) != null ? Truth.TRUE : Truth.FALSE;

        case And e -> evaluateAnd(e.expressions(), context);

        case Or e -> evaluateOr(e.expressions(), context);

        case Not e -> not(evaluateBoolean(e.expression(), context));

        default -> throw new IllegalArgumentException("Not a boolean expression: " + expr);
        };
    }

    private @Nullable Object evaluateValue(Expr expr, EvaluationContext context) {

        LOG.trace("evaluateValue: expr={}", expr);

        return switch (expr) {

        case Constant constant -> constant.value();

        case PathExpr path -> evaluatePath(path, context);

        case Product product -> multiply(evaluateValue(product.left(), context), evaluateValue(product.right(), context));

        default -> throw new IllegalArgumentException("Not a value expression: " + expr);
        };
    }

    private @Nullable Object evaluatePath(PathExpr path, EvaluationContext context) {

        Object current = context.source(path.source());

        for (String attribute : path.attributes()) {
            if (current == null) {
                return null;
            }

            current = PropertyAccess.read(current, attribute);
        }

        return current;
    }

    private static Truth equal(@Nullable Object left, @Nullable Object right) {

        // SQL semantics:
        // NULL = anything => UNKNOWN.
        if (left == null || right == null) {
            return Truth.UNKNOWN;
        }

        return Objects.equals(left, right) ? Truth.TRUE : Truth.FALSE;
    }

    private Truth evaluateAnd(List<Expr> expressions, EvaluationContext context) {

        LOG.trace("evaluateAnd: expressions={}", expressions);

        Truth result = Truth.TRUE;

        for (Expr expr : expressions) {
            LOG.trace("evaluateAnd: expr={}", expr);
            Truth value = evaluateBoolean(expr, context);
            LOG.trace("evaluateAnd: value={}", value);

            if (value == Truth.FALSE) {
                return Truth.FALSE;
            }

            if (value == Truth.UNKNOWN) {
                result = Truth.UNKNOWN;
            }
        }

        return result;
    }

    private Truth evaluateOr(List<Expr> expressions, EvaluationContext context) {

        LOG.trace("evaluateOr: expressions={}", expressions);

        Truth result = Truth.FALSE;

        for (Expr expr : expressions) {
            LOG.trace("evaluateOr: expr={}", expr);
            Truth value = evaluateBoolean(expr, context);
            LOG.trace("evaluateOr: value={}", value);

            if (value == Truth.TRUE) {
                return Truth.TRUE;
            }

            if (value == Truth.UNKNOWN) {
                result = Truth.UNKNOWN;
            }
        }

        return result;
    }

    private static Truth not(Truth truth) {
        return switch (truth) {
        case TRUE -> Truth.FALSE;
        case FALSE -> Truth.TRUE;
        case UNKNOWN -> Truth.UNKNOWN;
        };
    }

    private Truth compare(Expr leftExpr, Expr rightExpr, EvaluationContext context, java.util.function.IntPredicate condition) {

        Object left = evaluateValue(leftExpr, context);

        Object right = evaluateValue(rightExpr, context);

        LOG.trace("compare: left={}, right={}", left, right);

        if (left == null || right == null) {
            return Truth.UNKNOWN;
        }

        int result = compareValues(left, right);

        return condition.test(result) ? Truth.TRUE : Truth.FALSE;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static int compareValues(Object left, Object right) {

        if (left instanceof Number l && right instanceof Number r) {

            return toBigDecimal(l).compareTo(toBigDecimal(r));
        }

        if (left instanceof Comparable comparable) {
            return comparable.compareTo(right);
        }

        throw new IllegalArgumentException("Cannot compare " + left.getClass().getName() + " and " + right.getClass().getName());
    }

    private static @Nullable Object multiply(@Nullable Object left, @Nullable Object right) {

        LOG.trace("multiply: left={}, right={}", left, right);

        if (left == null || right == null) {
            return null;
        }

        if (!(left instanceof Number l) || !(right instanceof Number r)) {
            throw new IllegalArgumentException("prod() requires numeric operands");
        }

        return toBigDecimal(l).multiply(toBigDecimal(r));
    }

    private static BigDecimal toBigDecimal(Number value) {

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        return new BigDecimal(value.toString());
    }

    // ============================================================
    // Criteria API Proxy Implementation
    // ============================================================

    private static final class Proxies {

        private Proxies() {
        }

        @SuppressWarnings("unchecked")
        static <T> From<?, T> root(RecordingState state) {

            return (From<?, T>) Proxy.newProxyInstance(From.class.getClassLoader(), new Class<?>[] { From.class },
                    new FromHandler(state, new RootSource()));
        }

        static CriteriaBuilder criteriaBuilder(RecordingState state) {

            return (CriteriaBuilder) Proxy.newProxyInstance(CriteriaBuilder.class.getClassLoader(),
                    new Class<?>[] { CriteriaBuilder.class }, new CriteriaBuilderHandler(state));
        }

        static Expr expressionOf(Object value) {

            if (value == null) {
                return new Constant(null);
            }

            if (Proxy.isProxyClass(value.getClass())) {

                InvocationHandler handler = Proxy.getInvocationHandler(value);

                if (handler instanceof ExpressionHandler h) {
                    return h.expression;
                }

                if (handler instanceof FromHandler h) {
                    return new PathExpr(h.source, List.of());
                }
            }

            return new Constant(value);
        }

        @SuppressWarnings("unchecked")
        static <X> Path<X> path(Expr expr) {

            return (Path<X>) Proxy.newProxyInstance(Path.class.getClassLoader(), new Class<?>[] { Path.class },
                    new ExpressionHandler(expr));
        }

        static Predicate predicate(Expr expr) {

            return (Predicate) Proxy.newProxyInstance(Predicate.class.getClassLoader(), new Class<?>[] { Predicate.class },
                    new ExpressionHandler(expr));
        }

        @SuppressWarnings("unchecked")
        static <X> Expression<X> expression(Expr expr) {

            return (Expression<X>) Proxy.newProxyInstance(Expression.class.getClassLoader(), new Class<?>[] { Expression.class },
                    new ExpressionHandler(expr));
        }

        @SuppressWarnings("unchecked")
        static <X, Y> Join<X, Y> join(RecordingState state, JoinSource source) {

            return (Join<X, Y>) Proxy.newProxyInstance(Join.class.getClassLoader(), new Class<?>[] { Join.class },
                    new FromHandler(state, source));
        }
    }

    // ============================================================
    // From / Join Proxy
    // ============================================================

    private static final class FromHandler implements InvocationHandler {

        private final RecordingState state;
        private final Source source;

        private FromHandler(RecordingState state, Source source) {

            this.state = state;
            this.source = source;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            String name = method.getName();

            if (name.equals("get") && args != null && args.length == 1 && args[0] instanceof String attribute) {

                return Proxies.path(new PathExpr(source, List.of(attribute)));
            }

            if (name.equals("join") && args != null && args.length >= 1 && args[0] instanceof String attribute) {

                JoinType joinType = args.length >= 2 && args[1] instanceof JoinType jt ? jt : JoinType.INNER;

                JoinSource join = state.addJoin(source, attribute, joinType);

                return Proxies.join(state, join);
            }

            return objectMethodOrUnsupported(proxy, method, args, "From[" + source + "]");
        }
    }

    // ============================================================
    // Path / Expression / Predicate Proxy
    // ============================================================

    private static final class ExpressionHandler implements InvocationHandler {

        private final Expr expression;

        private ExpressionHandler(Expr expression) {
            this.expression = expression;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            if (method.getName().equals("get") && args != null && args.length == 1 && args[0] instanceof String attribute
                    && expression instanceof PathExpr path) {

                List<String> attributes = new ArrayList<>(path.attributes());

                attributes.add(attribute);

                return Proxies.path(new PathExpr(path.source(), List.copyOf(attributes)));
            }

            if (method.getName().equals("not") && proxy instanceof Predicate) {

                return Proxies.predicate(new Not(expression));
            }

            return objectMethodOrUnsupported(proxy, method, args, expression.toString());
        }
    }

    // ============================================================
    // CriteriaBuilder Proxy
    // ============================================================

    private static final class CriteriaBuilderHandler implements InvocationHandler {

        @SuppressWarnings("unused")
        private final RecordingState state;

        private CriteriaBuilderHandler(RecordingState state) {
            this.state = state;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            String name = method.getName();

            return switch (name) {

            case "equal" -> Proxies.predicate(new Equal(expr(args[0]), expr(args[1])));

            case "notEqual" -> Proxies.predicate(new NotEqual(expr(args[0]), expr(args[1])));

            case "greaterThan" -> Proxies.predicate(new GreaterThan(expr(args[0]), expr(args[1])));

            case "greaterThanOrEqualTo" -> Proxies.predicate(new GreaterThanOrEqual(expr(args[0]), expr(args[1])));

            case "lessThan" -> Proxies.predicate(new LessThan(expr(args[0]), expr(args[1])));

            case "lessThanOrEqualTo" -> Proxies.predicate(new LessThanOrEqual(expr(args[0]), expr(args[1])));

            case "isNull" -> Proxies.predicate(new IsNull(expr(args[0])));

            case "isNotNull" -> Proxies.predicate(new IsNotNull(expr(args[0])));

            case "and" -> Proxies.predicate(new And(predicateArguments(args)));

            case "or" -> Proxies.predicate(new Or(predicateArguments(args)));

            case "not" -> Proxies.predicate(new Not(expr(args[0])));

            case "conjunction" -> Proxies.predicate(new TrueExpr());

            case "disjunction" -> Proxies.predicate(new FalseExpr());

            case "literal" -> Proxies.expression(new Constant(args[0]));

            case "prod" -> Proxies.expression(new Product(expr(args[0]), expr(args[1])));

            default -> objectMethodOrUnsupported(proxy, method, args, "CriteriaBuilder");
            };
        }

        private static Expr expr(Object value) {
            return Proxies.expressionOf(value);
        }

        private static List<Expr> predicateArguments(Object[] args) {

            if (args == null || args.length == 0) {
                return List.of();
            }

            /*
             * For CriteriaBuilder.and(Predicate...) reflection sees one Predicate[] argument.
             *
             * For and(Expression<Boolean>, Expression<Boolean>) it sees two arguments.
             */
            if (args.length == 1 && args[0] instanceof Object[] array) {

                return Arrays.stream(array).map(Proxies::expressionOf).toList();
            }

            return Arrays.stream(args).map(Proxies::expressionOf).toList();
        }
    }

    // ============================================================
    // Bean / Property Access
    // ============================================================

    private static final class PropertyAccess {

        private static final Map<Key, Accessor> CACHE = new ConcurrentHashMap<>();

        static @Nullable Object read(@Nullable Object target, String property) {

            if (target == null) {
                return null;
            }

            Accessor accessor = CACHE.computeIfAbsent(new Key(target.getClass(), property), PropertyAccess::findAccessor);

            return accessor.read(target);
        }

        private static Accessor findAccessor(Key key) {

            Class<?> type = key.type();
            String property = key.property();

            /*
             * Records and ordinary methods whose name exactly equals the property.
             */
            try {
                Method method = type.getMethod(property);

                if (method.getParameterCount() == 0) {
                    return new MethodAccessor(method);
                }
            } catch (NoSuchMethodException ignored) {
                // Empty
            }

            String capitalized = Character.toUpperCase(property.charAt(0)) + property.substring(1);

            for (String name : List.of("get" + capitalized, "is" + capitalized)) {

                try {
                    Method method = type.getMethod(name);

                    if (method.getParameterCount() == 0) {
                        return new MethodAccessor(method);
                    }
                } catch (NoSuchMethodException ignored) {
                    // Empty
                }
            }

            Class<?> current = type;

            while (current != null) {
                try {
                    Field field = current.getDeclaredField(property);

                    field.trySetAccessible();

                    return new FieldAccessor(field);

                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }

            throw new IllegalArgumentException("No readable property '" + property + "' on " + type.getName());
        }

        private record Key(Class<?> type, String property) {
        }

        private interface Accessor {
            Object read(Object target);
        }

        private record MethodAccessor(Method method) implements Accessor {

            @Override
            public Object read(Object target) {
                try {
                    return method.invoke(target);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        private record FieldAccessor(Field field) implements Accessor {

            @Override
            public Object read(Object target) {
                try {
                    return field.get(target);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    // ============================================================
    // Proxy Utility
    // ============================================================

    private static Object objectMethodOrUnsupported(Object proxy, Method method, Object[] args, String description) {

        if (method.getName().equals("toString") && method.getParameterCount() == 0) {
            return description;
        } else if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
            return System.identityHashCode(proxy);
        } else if (method.getName().equals("equals") && method.getParameterCount() == 1) {
            return proxy.equals(args[0]);
        } else {
            throw new UnsupportedOperationException("Unsupported Criteria API operation: " + method);
        }
    }
}
