// Aayushi Kale
package simplf; 

import java.util.List;
import java.util.function.Supplier;

import simplf.Stmt.For;

import java.util.ArrayList;


class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {
    public Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {

    }

    public void interpret(List<Stmt> stmts) {
        try {
            for (Stmt stmt : stmts) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Simplf.runtimeError(error);
        }
    }

    @Override
    public Object visitExprStmt(Stmt.Expression stmt) {
        return evaluate(stmt.expr);
    }

    @Override
    public Object visitPrintStmt(Stmt.Print stmt) {
        Object val = evaluate(stmt.expr);
        System.out.println(stringify(val));
        return null;
    }

    @Override
    public Object visitVarStmt(Stmt.Var stmt) {
        Object value = (stmt.initializer != null) ? evaluate(stmt.initializer) : null;
        defineVariable(stmt, value);
        return value;
    }
    
    private void defineVariable(Stmt.Var stmt, Object value) {
        environment = environment.define(stmt.name, stmt.name.lexeme, value);
    }
    


    @Override
    public Object visitBlockStmt(Stmt.Block stmt) {
        return executeBlock(stmt.statements, new Environment(environment));
    }
    
    public Object executeBlock(List<Stmt> statements, Environment newEnv) {
        Environment previous = this.environment;
        List<Object> results = new ArrayList<>(); // Collect results
        try {
            this.environment = newEnv;
            for (Stmt statement : statements) {
                results.add(execute(statement));
            }
        } finally {
            this.environment = previous;
        }
        return results.isEmpty() ? null : results.get(results.size() - 1); // Return last result
    }
    
    @Override
    public Object visitIfStmt(Stmt.If stmt) {
        Object condition = evaluate(stmt.cond);
        Runnable branch = isTruthy(condition) ? () -> execute(stmt.thenBranch) : 
                                                () -> execute(stmt.elseBranch);
        branch.run();
        return null;
    }
    

    @Override
    public Object visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.cond))) {
            execute(stmt.body); 
        }
        return null; 
    }

    @Override
    public Object visitForStmt(For stmt) {
        throw new UnsupportedOperationException("For loops are not interpreted.");
    }

    @Override
public Object visitFunctionStmt(Stmt.Function stmt) {
    SimplfFunction function = createFunction(stmt);
    defineFunction(stmt.name, function);
    return function;
}

private SimplfFunction createFunction(Stmt.Function stmt) {
    return new SimplfFunction(stmt, environment);
}

private void defineFunction(Token name, SimplfFunction function) {
    environment = environment.define(name, name.lexeme, function);
}


    
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.op.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitBinary(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.op.type) {
            case PLUS:
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                throw new RuntimeError(expr.op, "Addition operation not supported for operands.");
            case MINUS:
                checkNumbers(expr.op, left, right);
                return (double) left - (double) right;
            case STAR:
                checkNumbers(expr.op, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumbers(expr.op, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.op, "Cannot divide by zero.");
                }
                return (double) left / (double) right;
            case GREATER:
                checkNumbers(expr.op, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumbers(expr.op, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumbers(expr.op, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumbers(expr.op, left, right);
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            case COMMA:
                return right;
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitUnary(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.op.type) {
            case MINUS:
                checkNumber(expr.op, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitLiteral(Expr.Literal expr) {
        return expr.val;
    }

    @Override
    public Object visitGrouping(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVarExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
    
    @Override
public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);
    if (!(callee instanceof SimplfCallable)) {
        throw new RuntimeError(expr.paren, "Can only call functions.");
    }
    
    List<Object> arguments = new ArrayList<>(expr.args.size());
    expr.args.forEach(arg -> arguments.add(evaluate(arg)));

    SimplfFunction function = (SimplfFunction) callee;
    return validateAndCallFunction(expr.paren, function, arguments);
}

private Object validateAndCallFunction(Token paren, SimplfFunction function, List<Object> arguments) {
    int expectedArgs = function.declaration.params.size();
    if (arguments.size() != expectedArgs) {
        throw new RuntimeError(paren, "Expected " + expectedArgs + " arguments but got " + arguments.size() + ".");
    }
    return function.call(this, arguments);
}


    public Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
public Object visitAssignExpr(Expr.Assign expr) {
    Supplier<Object> valueSupplier = () -> evaluate(expr.value);
    Object value = valueSupplier.get();
    environment.assign(expr.name, value);
    return value;
}


    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        if (isTruthy(evaluate(expr.cond))) {
            return evaluate(expr.thenBranch);
        } else {
            return evaluate(expr.elseBranch);
        }
    }

    private Object execute(Stmt stmt) {
        return stmt.accept(this);
    }

    public Object evaluateExpression(Expr expression) {
    return evaluate(expression); // This uses the private `evaluate` method internally
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return true;
    }


    private boolean isEqual(Object a, Object b) {
        if (a == null)
            return b == null;
        return a.equals(b);
    }

    private void checkNumber(Token op, Object object) {
        if (object instanceof Double)
            return;
        throw new RuntimeError(op, "Operand must be a number");
    }

    private void checkNumbers(Token op, Object a, Object b) {
        if (a instanceof Double && b instanceof Double)
            return;
        throw new RuntimeError(op, "Operand must be numbers");
    }

    private String stringify(Object object) {
        if (object == null)
            return "nil";
        if (object instanceof Double) {
            String num = object.toString();
            if (num.endsWith(".0")) {
                num = num.substring(0, num.length() - 2);
            }
            return num;
        }
        return object.toString();
    }


} 