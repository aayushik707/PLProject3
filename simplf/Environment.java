package simplf;

class Environment {
    public AssocList assocList;
    public final Environment enclosing;  

    Environment() {
        this.assocList = null;
        this.enclosing = null;  
    }

    Environment(Environment outer) {
        this.assocList = null;
        this.enclosing = outer;  
    }

    Environment(AssocList assocList, Environment enclosing) {
        this.assocList = assocList;
        this.enclosing = enclosing; 
    }

    Environment define(Token varToken, String name, Object value) {
        AssocList newAssocList = createAssocList(name, value);
        this.assocList = newAssocList;
        return new Environment(newAssocList, this);
    }
    
    private AssocList createAssocList(String name, Object value) {
        return new AssocList(name, value, this.assocList);
    }
    

    void assign(Token name, Object value) {
        Environment env = this;  
        while (env != null) {
            AssocList assoc = env.assocList;  
            while (assoc != null) {
                if (assoc.name.equals(name.lexeme)) {
                    assoc.value = value;
                    return;  
                }
                assoc = assoc.next;  
            }
            env = env.enclosing;  
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }
    

    public Object get(Token name) {
        AssocList assoc = this.assocList;
        
        while (assoc != null) {
            if (assoc.name.equals(name.lexeme)) {
                return assoc.value;
            }
            assoc = assoc.next; 
        }
    
        if (this.enclosing  != null) {
            return this.enclosing.get(name);
        }
    
        throw new RuntimeException("Undefined variable '" + name.lexeme + "'");
    }
    

}
