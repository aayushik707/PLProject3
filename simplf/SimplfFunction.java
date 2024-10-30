package simplf;

import java.util.List;

class SimplfFunction implements SimplfCallable {
    
    final Stmt.Function declaration;
    private Environment closure;
    SimplfFunction (Stmt. Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    public void setClosure (Environment environment) {
        this.closure = environment;
    }

    @Override
public Object call(Interpreter interpreter, List<Object> args) {
    
    Environment environment = new Environment(closure);
    bindParameters(environment, declaration.params, args);
    
    return interpreter.executeBlock(declaration.body, environment);
}
private void bindParameters(Environment environment, List<Token> params, List<Object> args) {
    for (int i = 0; i < params.size(); i++) {
        Token param = params.get(i);
        Object arg = args.get(i);
        environment.define(param, param.lexeme, arg);
    }
}


        @Override
        public String toString() {
        return "<fn "+ declaration.name.lexeme + ">";
        }

}
